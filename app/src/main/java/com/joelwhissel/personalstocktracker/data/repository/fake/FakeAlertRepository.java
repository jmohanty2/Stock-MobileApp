package com.joelwhissel.personalstocktracker.data.repository.fake;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.joelwhissel.personalstocktracker.data.repository.AlertRepository;
import com.joelwhissel.personalstocktracker.data.repository.Callback;
import com.joelwhissel.personalstocktracker.data.repository.Resource;
import com.joelwhissel.personalstocktracker.domain.AlertNotifier;
import com.joelwhissel.personalstocktracker.domain.PriceAlert;
import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * In-memory AlertRepository for building screens and tests before the real one exists.
 *
 * <p>Error switch: {@link #setError(ErrorType)} makes {@link #insert} fail with that error until
 * it is cleared with {@code setError(null)}.
 */
public class FakeAlertRepository implements AlertRepository {

    private final Object lock = new Object();
    private final Executor callbackExecutor;
    private final List<PriceAlert> alerts = new ArrayList<>();
    private final Map<String, MutableLiveData<List<PriceAlert>>> liveByTicker = new HashMap<>();
    private final List<PriceAlert> notified = new ArrayList<>();
    private long nextId = 1;

    private volatile ErrorType error;
    private volatile AlertNotifier notifier;

    /** Delivers callbacks on the main thread, as the real repository does. */
    public FakeAlertRepository() {
        this(new Handler(Looper.getMainLooper())::post);
    }

    /** Delivers callbacks on the given executor; pass {@code Runnable::run} in unit tests. */
    public FakeAlertRepository(Executor callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }

    // ---------------------------------------------------------------- test controls

    /** Makes insert() fail with this error; null switches errors off. */
    public void setError(ErrorType error) {
        this.error = error;
    }

    /** Where fired alerts are sent; may be null (fired alerts are still recorded). */
    public void setNotifier(AlertNotifier notifier) {
        this.notifier = notifier;
    }

    /** Alerts that fired through evaluateAndNotify(), in order, for assertions. */
    public List<PriceAlert> getNotifiedAlerts() {
        synchronized (lock) {
            return new ArrayList<>(notified);
        }
    }

    /** Adds one sample alert so the Stock Details list is not empty. */
    public void seedSampleData() {
        insertNow(new PriceAlert(0, 0, "AAPL", AlertType.PERCENT, Direction.UP,
                5, 228.00, true));
    }

    // ---------------------------------------------------------------- AlertRepository

    @Override
    public LiveData<List<PriceAlert>> observe(String ticker) {
        synchronized (lock) {
            return liveFor(normalize(ticker));
        }
    }

    @Override
    public void insert(PriceAlert alert, Callback<PriceAlert> callback) {
        ErrorType e = error;
        Resource<PriceAlert> result = e != null ? Resource.error(e) : Resource.success(insertNow(alert));
        if (callback != null) {
            callbackExecutor.execute(() -> callback.onResult(result));
        }
    }

    @Override
    public void update(PriceAlert alert) {
        synchronized (lock) {
            int i = indexOf(alert.getId());
            if (i >= 0) {
                alerts.set(i, alert);
                publish(alert.getTicker());
            }
        }
    }

    @Override
    public void delete(PriceAlert alert) {
        synchronized (lock) {
            int i = indexOf(alert.getId());
            if (i >= 0) {
                alerts.remove(i);
                publish(alert.getTicker());
            }
        }
    }

    @Override
    public void rearm(PriceAlert alert, double basePrice) {
        update(new PriceAlert(alert.getId(), 0, alert.getTicker(), alert.getType(),
                alert.getDirection(), alert.getTargetValue(), basePrice, true));
    }

    @Override
    public List<String> getActiveTickersSync() {
        synchronized (lock) {
            Set<String> tickers = new LinkedHashSet<>();
            for (PriceAlert a : alerts) {
                if (a.isActive()) tickers.add(a.getTicker());
            }
            return new ArrayList<>(tickers);
        }
    }

    @Override
    public int evaluateAndNotify(Map<String, Double> prices) {
        List<PriceAlert> fired = new ArrayList<>();
        Set<String> changed = new LinkedHashSet<>();
        long now = System.currentTimeMillis();
        synchronized (lock) {
            for (int i = 0; i < alerts.size(); i++) {
                PriceAlert a = alerts.get(i);
                Double price = prices.get(a.getTicker());
                if (!a.isActive() || price == null || !isMet(a, price)) continue;
                PriceAlert triggered = new PriceAlert(a.getId(), now, a.getTicker(), a.getType(),
                        a.getDirection(), a.getTargetValue(), a.getBasePrice(), false);
                alerts.set(i, triggered);   // the "markTriggered" step: only this pass sees it active
                notified.add(triggered);
                fired.add(triggered);
                changed.add(a.getTicker());
            }
            for (String t : changed) publish(t);
        }
        AlertNotifier n = notifier;
        if (n != null) {
            for (PriceAlert a : fired) n.showPriceAlert(a, prices.get(a.getTicker()));
        }
        return fired.size();
    }

    // ---------------------------------------------------------------- used by FakeStockRepository

    /** Mirrors the CASCADE delete when a stock row is purged. */
    void deleteAllForTicker(String ticker) {
        synchronized (lock) {
            if (alerts.removeIf(a -> a.getTicker().equals(ticker))) publish(ticker);
        }
    }

    int countForTicker(String ticker) {
        synchronized (lock) {
            int n = 0;
            for (PriceAlert a : alerts) {
                if (a.getTicker().equals(ticker)) n++;
            }
            return n;
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Simple stand-in for AlertEvaluator.isMet() (design doc 8.4). */
    private static boolean isMet(PriceAlert a, double price) {
        double threshold = a.getThresholdPrice();
        return a.getDirection() == Direction.UP ? price >= threshold : price <= threshold;
    }

    private PriceAlert insertNow(PriceAlert alert) {
        synchronized (lock) {
            String ticker = normalize(alert.getTicker());
            PriceAlert saved = new PriceAlert(nextId++, alert.getTriggeredAt(), ticker,
                    alert.getType(), alert.getDirection(), alert.getTargetValue(),
                    alert.getBasePrice(), alert.isActive());
            alerts.add(saved);
            publish(ticker);
            return saved;
        }
    }

    private int indexOf(long id) {
        for (int i = 0; i < alerts.size(); i++) {
            if (alerts.get(i).getId() == id) return i;
        }
        return -1;
    }

    /** Call while holding the lock. */
    private MutableLiveData<List<PriceAlert>> liveFor(String ticker) {
        MutableLiveData<List<PriceAlert>> live = liveByTicker.get(ticker);
        if (live == null) {
            live = new MutableLiveData<>(snapshot(ticker));
            liveByTicker.put(ticker, live);
        }
        return live;
    }

    /** Call while holding the lock. */
    private void publish(String ticker) {
        MutableLiveData<List<PriceAlert>> live = liveByTicker.get(ticker);
        if (live != null) live.postValue(snapshot(ticker));
    }

    /** Newest first, like AlertDao.observeForTicker(). Call while holding the lock. */
    private List<PriceAlert> snapshot(String ticker) {
        List<PriceAlert> list = new ArrayList<>();
        for (PriceAlert a : alerts) {
            if (a.getTicker().equals(ticker)) list.add(a);
        }
        Collections.reverse(list);
        return Collections.unmodifiableList(list);
    }

    private static String normalize(String ticker) {
        return ticker == null ? "" : ticker.trim().toUpperCase(Locale.US);
    }
}
