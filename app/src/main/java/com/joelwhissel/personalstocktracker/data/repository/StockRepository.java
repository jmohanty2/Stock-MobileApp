package com.joelwhissel.personalstocktracker.data.repository;

import androidx.lifecycle.LiveData;

import com.joelwhissel.personalstocktracker.domain.DailyPrice;
import com.joelwhissel.personalstocktracker.domain.Holding;
import com.joelwhissel.personalstocktracker.domain.StockQuote;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import java.util.List;

/**
 * <p>Threading: methods that take a {@link Callback} return at once and deliver the result on
 * the main thread. Methods ending in {@code Sync} block, so call them off the main thread
 * (workers, the repository's own executor). LiveData is safe to observe from any Activity.
 *
 * <p>Tickers are normalised by the repository (trimmed, upper case), so callers can pass the
 * user's raw input.
 */
public interface StockRepository {

    /** Most stocks that can be tracked across watchlist and portfolio combined. */
    int MAX_TRACKED = 50;

    /** Foreground refresh period while a screen is visible. */
    long REFRESH_INTERVAL_MS = 60_000L;

    /** Undo window after a watchlist remove. */
    long UNDO_WINDOW_MS = 4_000L;

    // ---------------------------------------------------------------- observe

    /** Stocks in the watchlist, ordered by ticker, with the last cached quote. */
    LiveData<List<StockQuote>> observeWatchlist();

    /** Holdings, ordered by ticker, each with its last cached quote. */
    LiveData<List<Holding>> observeHoldings();

    /** Last cached quote for one tracked ticker; emits null if the stock is not tracked. */
    LiveData<StockQuote> observeQuote(String ticker);

    /**
     * Error from the most recent quote refresh (timer, pull-to-refresh, Retry or worker), or
     * null after a successful one.
     */
    LiveData<ErrorType> observeRefreshError();

    // ---------------------------------------------------------------- watchlist

    /**
     * Adds a ticker to the watchlist. A ticker that is already tracked (held, or
     * inside its Undo window) is added without API calls.
     *
     * <p>Result: success with the stock's quote, or {@link ErrorType#DUPLICATE},
     * {@link ErrorType#STOCK_NOT_FOUND}, {@link ErrorType#NETWORK_ERROR},
     * {@link ErrorType#LIMIT_REACHED}.
     */
    void addToWatchlist(String ticker, Callback<StockQuote> callback);

    /** Hides a stock from the watchlist and starts its Undo window; alerts and history are kept. */
    void removeFromWatchlist(String ticker);

    /** Puts a removed stock back in the watchlist, with its alerts and history intact. */
    void undoRemove(String ticker);

    /**
     * Ends the Undo window: call when the Undo snackbar is dismissed without Undo. Deletes the
     * stock row, its alerts and history if the stock is not held.
     */
    void commitRemove(String ticker);

    /**
     * Number of alerts that removing this ticker from the watchlist would delete: 0 if the
     * stock is held, otherwise its alert count. Used for the remove warning.
     */
    void countAlertsDeletedOnRemove(String ticker, Callback<Integer> callback);

    // ---------------------------------------------------------------- portfolio

    /**
     * Adds a holding. Validate shares and price with InputValidator first. Creates the
     * stock row if needed, with API calls only for a new ticker.
     *
     * <p>Result: success with the holding, or {@link ErrorType#DUPLICATE} ("Already in
     * portfolio"), {@link ErrorType#STOCK_NOT_FOUND}, {@link ErrorType#NETWORK_ERROR},
     * {@link ErrorType#LIMIT_REACHED}.
     */
    void addHolding(String ticker, double shares, double avgBuyPrice, Callback<Holding> callback);

    /** Changes shares and average buy price of an existing holding (FR-P3). */
    void updateHolding(String ticker, double shares, double avgBuyPrice);

    /**
     * Deletes a holding. If the stock is not in the watchlist
     * its row, alerts and history are deleted too.
     */
    void removeHolding(String ticker);

    // ---------------------------------------------------------------- refresh

    /**
     * A screen became visible. The repository counts visible screens and runs
     * one shared timer that refreshes every tracked stock every {@link #REFRESH_INTERVAL_MS}.
     */
    void startAutoRefresh();

    /** A screen stopped being visible (Activity.onStop). The timer stops at zero screens. */
    void stopAutoRefresh();

    /** Refreshes every tracked stock now, for pull-to-refresh and Retry. */
    void refreshNow(Callback<List<StockQuote>> callback);

    /**
     * Fetches quotes for the given tickers (only those that are tracked), saves them, then
     * passes the new prices to {@link AlertRepository#evaluateAndNotify}. Blocks.
     *
     * <p>Result: success with the refreshed quotes, or {@link ErrorType#NETWORK_ERROR} with the
     * old rows kept.
     */
    Resource<List<StockQuote>> refreshQuotesSync(List<String> tickers);

    /** Tickers in the watchlist or the portfolio. Blocks. */
    List<String> getTrackedTickersSync();

    // ---------------------------------------------------------------- history

    /**
     * Daily history for a tracked ticker, oldest first, at most {@code days} trading days.
     * Served from the cache; fetched at most once per ticker per day within the daily call
     * budget. Over budget, the cached (older) history is returned;
     *
     * <p>Result: success, or {@link ErrorType#NETWORK_ERROR} when nothing is cached and the
     * fetch fails ("Indicators unavailable").
     */
    void getHistory(String ticker, int days, Callback<List<DailyPrice>> callback);

    /** Blocking form of {@link #getHistory}, for workers. */
    Resource<List<DailyPrice>> getHistorySync(String ticker, int days);

    // ---------------------------------------------------------------- slow-mover reminders

    /** When the last slow-mover reminder was posted for a ticker (epoch ms), or 0. Blocks. */
    long getLastReminderAtSync(String ticker);

    /** Records that a slow-mover reminder was posted for a ticker (FR-A5). Blocks. */
    void setLastReminderAtSync(String ticker, long timeMillis);
}
