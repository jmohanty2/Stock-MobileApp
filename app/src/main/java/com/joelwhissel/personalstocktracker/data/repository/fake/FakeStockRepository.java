package com.joelwhissel.personalstocktracker.data.repository.fake;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.joelwhissel.personalstocktracker.data.repository.Callback;
import com.joelwhissel.personalstocktracker.data.repository.Resource;
import com.joelwhissel.personalstocktracker.data.repository.StockRepository;
import com.joelwhissel.personalstocktracker.domain.DailyPrice;
import com.joelwhissel.personalstocktracker.domain.Holding;
import com.joelwhissel.personalstocktracker.domain.StockQuote;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory StockRepository for building screens and tests before the real one exists. Follows
 * the same rules as the real repository: duplicate check, 50-stock limit, shared watchlist and
 * portfolio rows, Undo window and purge, and alert evaluation after every refresh.
 *
 * <p>The fake "API" knows the tickers in {@link #UNIVERSE}; any other ticker is Stock Not
 * Found. Each refresh moves prices by up to ±1% so the UI visibly changes.
 *
 * <p>Error switch: {@link #setError(ErrorType)} makes every call that would reach the network
 * (adding a new ticker, refreshing, loading history) fail with that error until it is cleared
 * with {@code setError(null)}. Checks the real repository does in Room first (duplicate,
 * already tracked) still run before it.
 */
public class FakeStockRepository implements StockRepository {

    /** Ticker, company name, previous close. */
    private static final Object[][] UNIVERSE = {
            {"AAPL", "Apple Inc", 228.00},
            {"MSFT", "Microsoft Corp", 415.00},
            {"GOOGL", "Alphabet Inc Class A", 172.00},
            {"AMZN", "Amazon.com Inc", 198.00},
            {"NVDA", "NVIDIA Corp", 142.00},
            {"META", "Meta Platforms Inc Class A", 560.00},
            {"TSLA", "Tesla Inc", 245.00},
            {"JPM", "JPMorgan Chase & Co", 238.00},
            {"V", "Visa Inc Class A", 310.00},
            {"KO", "Coca-Cola Co", 63.00},
            {"DIS", "Walt Disney Co", 112.00},
            {"NFLX", "Netflix Inc", 880.00},
            {"SPY", "SPDR S&P 500 ETF Trust", 590.00},
            {"VOO", "Vanguard S&P 500 ETF", 542.00},
    };

    private static final int MAX_HISTORY_DAYS = 100;   // Alpha Vantage "compact" output size

    private final FakeAlertRepository alertRepository;
    private final Executor callbackExecutor;
    private final Object lock = new Object();
    private final Random random = new Random(42);

    /** Every stock row, tracked or inside its Undo window. */
    private final Map<String, StockQuote> rows = new TreeMap<>();
    private final Set<String> watchlist = new TreeSet<>();
    /** ticker -> {shares, avgBuyPrice} */
    private final Map<String, double[]> holdings = new TreeMap<>();
    private final Map<String, Long> lastReminderAt = new HashMap<>();

    private final MutableLiveData<List<StockQuote>> watchlistLive =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Holding>> holdingsLive =
            new MutableLiveData<>(Collections.emptyList());
    private final Map<String, MutableLiveData<StockQuote>> quoteLive = new HashMap<>();
    private final MutableLiveData<ErrorType> refreshError = new MutableLiveData<>(null);

    private volatile ErrorType error;
    private int visibleScreens;
    private ScheduledExecutorService timer;

    /** Delivers callbacks on the main thread, as the real repository does. */
    public FakeStockRepository(FakeAlertRepository alertRepository) {
        this(alertRepository, new Handler(Looper.getMainLooper())::post);
    }

    /** Delivers callbacks on the given executor; pass {@code Runnable::run} in unit tests. */
    public FakeStockRepository(FakeAlertRepository alertRepository, Executor callbackExecutor) {
        this.alertRepository = alertRepository;
        this.callbackExecutor = callbackExecutor;
    }

    // ---------------------------------------------------------------- test controls

    /** Makes network calls fail with this error; null switches errors off. */
    public void setError(ErrorType error) {
        this.error = error;
    }

    /** Sample data: three watchlist stocks and two holdings (AAPL is in both). */
    public void seedSampleData() {
        synchronized (lock) {
            for (String t : new String[]{"AAPL", "NVDA", "KO"}) {
                rows.put(t, newQuote(t));
                watchlist.add(t);
            }
            rows.put("MSFT", newQuote("MSFT"));
            holdings.put("AAPL", new double[]{10, 185.50});
            holdings.put("MSFT", new double[]{2.5, 430.00});
            publish();
        }
    }

    // ---------------------------------------------------------------- observe

    @Override
    public LiveData<List<StockQuote>> observeWatchlist() {
        return watchlistLive;
    }

    @Override
    public LiveData<List<Holding>> observeHoldings() {
        return holdingsLive;
    }

    @Override
    public LiveData<StockQuote> observeQuote(String ticker) {
        String t = normalize(ticker);
        synchronized (lock) {
            MutableLiveData<StockQuote> live = quoteLive.get(t);
            if (live == null) {
                live = new MutableLiveData<>(isTracked(t) ? rows.get(t) : null);
                quoteLive.put(t, live);
            }
            return live;
        }
    }

    @Override
    public LiveData<ErrorType> observeRefreshError() {
        return refreshError;
    }

    // ---------------------------------------------------------------- watchlist

    @Override
    public void addToWatchlist(String ticker, Callback<StockQuote> callback) {
        String t = normalize(ticker);
        Resource<StockQuote> result;
        synchronized (lock) {
            if (watchlist.contains(t)) {
                result = Resource.error(ErrorType.DUPLICATE);
            } else {
                result = track(t);
                if (result.status == Resource.Status.SUCCESS) {
                    watchlist.add(t);
                    publish();
                }
            }
        }
        deliver(callback, result);
    }

    @Override
    public void removeFromWatchlist(String ticker) {
        synchronized (lock) {
            if (watchlist.remove(normalize(ticker))) publish();
        }
    }

    @Override
    public void undoRemove(String ticker) {
        String t = normalize(ticker);
        synchronized (lock) {
            if (rows.containsKey(t) && watchlist.add(t)) publish();
        }
    }

    @Override
    public void commitRemove(String ticker) {
        synchronized (lock) {
            purgeIfOrphan(normalize(ticker));
        }
    }

    @Override
    public void countAlertsDeletedOnRemove(String ticker, Callback<Integer> callback) {
        String t = normalize(ticker);
        int count;
        synchronized (lock) {
            count = holdings.containsKey(t) ? 0 : alertRepository.countForTicker(t);
        }
        deliver(callback, Resource.success(count));
    }

    // ---------------------------------------------------------------- portfolio

    @Override
    public void addHolding(String ticker, double shares, double avgBuyPrice,
                           Callback<Holding> callback) {
        String t = normalize(ticker);
        Resource<Holding> result;
        synchronized (lock) {
            if (holdings.containsKey(t)) {
                result = Resource.error(ErrorType.DUPLICATE);
            } else {
                Resource<StockQuote> tracked = track(t);
                if (tracked.status == Resource.Status.SUCCESS) {
                    holdings.put(t, new double[]{shares, avgBuyPrice});
                    publish();
                    result = Resource.success(new Holding(t, shares, avgBuyPrice, tracked.data));
                } else {
                    result = Resource.error(tracked.error);
                }
            }
        }
        deliver(callback, result);
    }

    @Override
    public void updateHolding(String ticker, double shares, double avgBuyPrice) {
        String t = normalize(ticker);
        synchronized (lock) {
            if (holdings.containsKey(t)) {
                holdings.put(t, new double[]{shares, avgBuyPrice});
                publish();
            }
        }
    }

    @Override
    public void removeHolding(String ticker) {
        String t = normalize(ticker);
        synchronized (lock) {
            if (holdings.remove(t) != null) {
                publish();
                purgeIfOrphan(t);
            }
        }
    }

    // ---------------------------------------------------------------- refresh

    @Override
    public void startAutoRefresh() {
        synchronized (lock) {
            visibleScreens++;
            if (visibleScreens == 1) {
                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleWithFixedDelay(
                        () -> refreshQuotesSync(getTrackedTickersSync()),
                        0, REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void stopAutoRefresh() {
        synchronized (lock) {
            if (visibleScreens == 0) return;
            visibleScreens--;
            if (visibleScreens == 0 && timer != null) {
                timer.shutdownNow();
                timer = null;
            }
        }
    }

    @Override
    public void refreshNow(Callback<List<StockQuote>> callback) {
        deliver(callback, refreshQuotesSync(getTrackedTickersSync()));
    }

    @Override
    public Resource<List<StockQuote>> refreshQuotesSync(List<String> tickers) {
        ErrorType e = error;
        if (e != null) {
            refreshError.postValue(e);
            return Resource.error(e);
        }
        List<StockQuote> refreshed = new ArrayList<>();
        Map<String, Double> prices = new LinkedHashMap<>();
        synchronized (lock) {
            for (String raw : tickers) {
                String t = normalize(raw);
                if (!isTracked(t)) continue;
                StockQuote q = movePrice(rows.get(t));
                rows.put(t, q);
                refreshed.add(q);
                prices.put(t, q.getPrice());
            }
            publish();
        }
        refreshError.postValue(null);
        alertRepository.evaluateAndNotify(prices);
        return Resource.success(refreshed);
    }

    @Override
    public List<String> getTrackedTickersSync() {
        synchronized (lock) {
            List<String> tickers = new ArrayList<>();
            for (String t : rows.keySet()) {
                if (isTracked(t)) tickers.add(t);
            }
            return tickers;
        }
    }

    // ---------------------------------------------------------------- history

    @Override
    public void getHistory(String ticker, int days, Callback<List<DailyPrice>> callback) {
        deliver(callback, getHistorySync(ticker, days));
    }

    @Override
    public Resource<List<DailyPrice>> getHistorySync(String ticker, int days) {
        String t = normalize(ticker);
        ErrorType e = error;
        if (e != null) return Resource.error(e);
        double lastPrice;
        synchronized (lock) {
            if (!isTracked(t)) return Resource.error(ErrorType.STOCK_NOT_FOUND);
            lastPrice = rows.get(t).getPrice();
        }
        return Resource.success(syntheticHistory(t, Math.min(days, MAX_HISTORY_DAYS), lastPrice));
    }

    // ---------------------------------------------------------------- slow-mover reminders

    @Override
    public long getLastReminderAtSync(String ticker) {
        synchronized (lock) {
            Long at = lastReminderAt.get(normalize(ticker));
            return at == null ? 0 : at;
        }
    }

    @Override
    public void setLastReminderAtSync(String ticker, long timeMillis) {
        synchronized (lock) {
            lastReminderAt.put(normalize(ticker), timeMillis);
        }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * The shared track() step (design doc 9.1). Call while holding the lock. A row that already
     * exists is reused without "API calls"; a new ticker checks the limit, then the error
     * switch, then the fake search.
     */
    private Resource<StockQuote> track(String t) {
        StockQuote existing = rows.get(t);
        if (existing != null) return Resource.success(existing);
        if (countTracked() >= MAX_TRACKED) return Resource.error(ErrorType.LIMIT_REACHED);
        ErrorType e = error;
        if (e != null) return Resource.error(e);
        if (findInUniverse(t) == null) return Resource.error(ErrorType.STOCK_NOT_FOUND);
        StockQuote q = newQuote(t);
        rows.put(t, q);
        return Resource.success(q);
    }

    /** Call while holding the lock. */
    private void purgeIfOrphan(String t) {
        if (rows.containsKey(t) && !isTracked(t)) {
            rows.remove(t);
            lastReminderAt.remove(t);
            alertRepository.deleteAllForTicker(t);
            publish();
        }
    }

    /** Call while holding the lock. */
    private boolean isTracked(String t) {
        return rows.containsKey(t) && (watchlist.contains(t) || holdings.containsKey(t));
    }

    /** Call while holding the lock. */
    private int countTracked() {
        int n = 0;
        for (String t : rows.keySet()) {
            if (isTracked(t)) n++;
        }
        return n;
    }

    /** Pushes the current state to every LiveData. Call while holding the lock. */
    private void publish() {
        List<StockQuote> watch = new ArrayList<>();
        for (String t : watchlist) watch.add(rows.get(t));
        watchlistLive.postValue(Collections.unmodifiableList(watch));

        List<Holding> held = new ArrayList<>();
        for (Map.Entry<String, double[]> h : holdings.entrySet()) {
            double[] v = h.getValue();
            held.add(new Holding(h.getKey(), v[0], v[1], rows.get(h.getKey())));
        }
        holdingsLive.postValue(Collections.unmodifiableList(held));

        for (Map.Entry<String, MutableLiveData<StockQuote>> e : quoteLive.entrySet()) {
            e.getValue().postValue(isTracked(e.getKey()) ? rows.get(e.getKey()) : null);
        }
    }

    private <T> void deliver(Callback<T> callback, Resource<T> result) {
        if (callback != null) callbackExecutor.execute(() -> callback.onResult(result));
    }

    private static Object[] findInUniverse(String t) {
        for (Object[] row : UNIVERSE) {
            if (row[0].equals(t)) return row;
        }
        return null;
    }

    private StockQuote newQuote(String t) {
        Object[] row = findInUniverse(t);
        double previousClose = (Double) row[2];
        double price = round2(previousClose * (1 + (random.nextDouble() - 0.5) * 0.04));
        return quote(t, (String) row[1], price, previousClose, previousClose);
    }

    /** Moves the price by up to ±1% and keeps the day's open, high and low consistent. */
    private StockQuote movePrice(StockQuote q) {
        double price = round2(q.getPrice() * (1 + (random.nextDouble() - 0.5) * 0.02));
        return quote(q.getTicker(), q.getCompanyName(), price, q.getPreviousClose(), q.getOpen(),
                Math.max(q.getDayHigh(), price), Math.min(q.getDayLow(), price));
    }

    private static StockQuote quote(String t, String name, double price, double previousClose,
                                    double open) {
        return quote(t, name, price, previousClose, open,
                Math.max(open, price), Math.min(open, price));
    }

    private static StockQuote quote(String t, String name, double price, double previousClose,
                                    double open, double dayHigh, double dayLow) {
        double change = round2(price - previousClose);
        double changePercent = round2(change / previousClose * 100);
        return new StockQuote(t, name, price, change, changePercent, open, dayHigh, dayLow,
                previousClose, System.currentTimeMillis());
    }

    /** A repeatable random walk per ticker that ends at the current price, oldest first. */
    private static List<DailyPrice> syntheticHistory(String t, int days, double lastPrice) {
        Random r = new Random(t.hashCode());
        List<DailyPrice> newestFirst = new ArrayList<>();
        LocalDate date = LocalDate.now();
        double close = lastPrice;
        while (newestFirst.size() < days) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                double c = round2(close);
                newestFirst.add(new DailyPrice(date.toString(), c, round2(c * 1.01),
                        round2(c * 0.99), 1_000_000L + r.nextInt(9_000_000)));
                close = close / (1 + (r.nextDouble() - 0.5) * 0.04);
            }
            date = date.minusDays(1);
        }
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static String normalize(String ticker) {
        return ticker == null ? "" : ticker.trim().toUpperCase(Locale.US);
    }
}
