package com.joelwhissel.personalstocktracker.data.repository;

public interface SettingsRepository {

    /** Default slow-mover threshold: moved less than this % ... */
    double DEFAULT_SLOW_MOVER_PCT = 2.0;

    /** ... over this many trading days. */
    int DEFAULT_SLOW_MOVER_DAYS = 10;

    /** Alpha Vantage history calls allowed per day (free tier is 25; 3 left for retries). */
    int HISTORY_CALLS_PER_DAY = 22;

    /** Slow-mover % threshold set in Reminder settings. */
    double slowMoverPct();

    /** Slow-mover trading-day window set in Reminder settings. */
    int slowMoverDays();

    /** Saves both Reminder settings values (validate first). */
    void setSlowMoverThresholds(double pct, int days);

    /** History calls made today (the counter resets when the date changes). */
    int historyCallsToday();

    /**
     * Uses one history call from today's budget. Returns false, without counting, when
     * {@link #HISTORY_CALLS_PER_DAY} calls were already made today.
     */
    boolean tryUseHistoryCall();
}
