package com.joelwhissel.personalstocktracker.domain;

/** One trading day of price history (Alpha Vantage TIME_SERIES_DAILY). */
public class DailyPrice {
    private final String date;   // yyyy-MM-dd
    private final double close, high, low;
    private final long volume;

    public DailyPrice(String date, double close, double high, double low, long volume) {
        this.date = date;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
    }

    public String getDate() {
        return date;
    }

    public double getClose() {
        return close;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public long getVolume() {
        return volume;
    }
}
