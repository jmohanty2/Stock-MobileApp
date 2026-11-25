package com.joelwhissel.personalstocktracker.data.repository;

import androidx.lifecycle.LiveData;

import com.joelwhissel.personalstocktracker.domain.PriceAlert;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import java.util.List;
import java.util.Map;

/**
 * <p>Threading: write methods return at once and run on the repository's executor; callbacks
 * are delivered on the main thread. Methods ending in {@code Sync}, and
 * {@link #evaluateAndNotify}, block, so call them off the main thread.
 */
public interface AlertRepository {

    /** Alerts for one ticker, newest first, for the Stock Details list. */
    LiveData<List<PriceAlert>> observe(String ticker);

    /**
     * Saves a new alert. Validate with InputValidator first; the alert's id is
     * ignored. Result: success with the saved alert (with its id), or
     * {@link ErrorType#STOCK_NOT_FOUND} if the ticker is no longer tracked. Callback may be null.
     */
    void insert(PriceAlert alert, Callback<PriceAlert> callback);

    /** Replaces a saved alert by id, e.g. to disable it with the enable switch. */
    void update(PriceAlert alert);

    /** Deletes a saved alert. */
    void delete(PriceAlert alert);

    /**
     * Turns an alert back on: sets active = true, clears triggeredAt and,
     * for a percent alert, uses the new base price.
     */
    void rearm(PriceAlert alert, double basePrice);

    /** Distinct tickers that have at least one active alert. Blocks. */
    List<String> getActiveTickersSync();

    /**
     * Checks every active alert against the new prices (ticker to current price). For each met
     * alert, marks it triggered with a conditional update and notifies through AlertNotifier
     * only if that update changed a row. Tickers missing from the map are skipped. Blocks.
     *
     * @return the number of alerts that fired
     */
    int evaluateAndNotify(Map<String, Double> prices);
}
