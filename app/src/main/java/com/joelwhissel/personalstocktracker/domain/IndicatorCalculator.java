package com.joelwhissel.personalstocktracker.domain;

import com.joelwhissel.personalstocktracker.domain.enums.Volatility;

import java.util.List;

/**
 * Simple performance indicators from daily closing prices (design doc 8.4, FR-D2, FR-A5).
 * Pure Java: no Android classes.
 *
 * <p><b>Input:</b> {@code closes} ordered <b>oldest first, newest last</b> — the order
 * {@code PriceHistoryDao.getHistoryForTicker()} returns ({@code ORDER BY date ASC}).</p>
 *
 * <p><b>Not enough data:</b> methods returning {@code double} give {@code Double.NaN};
 * check with {@code Double.isNaN(x)} and show "Indicators unavailable".
 * {@link #volatility(List)} returns {@code null}; {@link #isSlowMover} returns {@code false}.</p>
 */
public final class IndicatorCalculator {

    /** Daily-return standard deviation (in %) below this is LOW. */
    static final double LOW_VOLATILITY_MAX = 1.0;
    /** Daily-return standard deviation (in %) above this is HIGH. */
    static final double HIGH_VOLATILITY_MIN = 2.5;
    /** Volatility uses the last 30 daily returns (31 closes). */
    static final int VOLATILITY_DAYS = 30;

    private IndicatorCalculator() {
        // static helpers only
    }

    /**
     * % change over the last {@code n} trading days:
     * (close[today] − close[today−n]) ÷ close[today−n] × 100.
     * Needs at least n + 1 closes.
     */
    public static double percentChange(List<Double> closes, int n) {
        if (closes == null || n <= 0 || closes.size() < n + 1) {
            return Double.NaN;
        }
        double today = closes.get(closes.size() - 1);
        double before = closes.get(closes.size() - 1 - n);
        if (before == 0) {
            return Double.NaN;
        }
        return (today - before) / before * 100;
    }

    /** Simple moving average: mean of the last {@code n} closes. Needs at least n closes. */
    public static double sma(List<Double> closes, int n) {
        if (closes == null || n <= 0 || closes.size() < n) {
            return Double.NaN;
        }
        double sum = 0;
        for (int i = closes.size() - n; i < closes.size(); i++) {
            sum += closes.get(i);
        }
        return sum / n;
    }

    /**
     * Highest and lowest close of the last {@code n} days.
     *
     * @return {@code {high, low}}, or {@code {NaN, NaN}} when fewer than n closes
     */
    public static double[] highLow(List<Double> closes, int n) {
        if (closes == null || n <= 0 || closes.size() < n) {
            return new double[]{Double.NaN, Double.NaN};
        }
        double high = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        for (int i = closes.size() - n; i < closes.size(); i++) {
            double c = closes.get(i);
            high = Math.max(high, c);
            low = Math.min(low, c);
        }
        return new double[]{high, low};
    }

    /**
     * True when the latest price is above its {@code n}-day average
     * (used for "Price is above its 20-day average — short-term uptrend").
     * False when there is not enough data.
     */
    public static boolean isAboveSma(List<Double> closes, int n) {
        double avg = sma(closes, n);
        return !Double.isNaN(avg) && closes.get(closes.size() - 1) > avg;
    }

    /**
     * Sample standard deviation of daily % returns over the last {@code days} returns.
     * Needs at least days + 1 closes.
     */
    public static double dailyReturnStdDev(List<Double> closes, int days) {
        if (closes == null || days < 2 || closes.size() < days + 1) {
            return Double.NaN;
        }
        double[] returns = new double[days];
        int start = closes.size() - days - 1;
        for (int i = 0; i < days; i++) {
            double prev = closes.get(start + i);
            double next = closes.get(start + i + 1);
            if (prev == 0) {
                return Double.NaN;
            }
            returns[i] = (next - prev) / prev * 100;
        }
        double mean = 0;
        for (double r : returns) {
            mean += r;
        }
        mean /= days;
        double squares = 0;
        for (double r : returns) {
            squares += (r - mean) * (r - mean);
        }
        return Math.sqrt(squares / (days - 1));
    }

    /**
     * Volatility label from the last 30 daily returns:
     * below 1% LOW, 1–2.5% MEDIUM, above 2.5% HIGH. {@code null} with fewer than 31 closes.
     */
    public static Volatility volatility(List<Double> closes) {
        double sd = dailyReturnStdDev(closes, VOLATILITY_DAYS);
        if (Double.isNaN(sd)) {
            return null;
        }
        if (sd < LOW_VOLATILITY_MAX) {
            return Volatility.LOW;
        }
        if (sd > HIGH_VOLATILITY_MIN) {
            return Volatility.HIGH;
        }
        return Volatility.MEDIUM;
    }

    /**
     * Slow-mover check (FR-A5): over the last {@code days} closes the price range
     * (max − min) ÷ min × 100 is below {@code thresholdPercent}.
     * Defaults from the design doc: 10 days, 2%.
     */
    public static boolean isSlowMover(List<Double> closes, int days, double thresholdPercent) {
        if (closes == null || days < 2 || thresholdPercent <= 0 || closes.size() < days) {
            return false;
        }
        double[] hl = highLow(closes, days);
        double high = hl[0];
        double low = hl[1];
        if (low <= 0) {
            return false;
        }
        return (high - low) / low * 100 < thresholdPercent;
    }
}
