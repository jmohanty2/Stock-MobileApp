package com.joelwhissel.personalstocktracker.domain;

import com.joelwhissel.personalstocktracker.domain.enums.Trend;

public class StockQuote {
    private final String ticker, companyName;
    private final double price, change, changePercent, open, dayHigh, dayLow, previousClose;
    private final long lastUpdated;

    public StockQuote(String ticker, String name, double price, double change, double changePercent,
                      double open, double high, double low, double close, long lastUpdated) {
        this.companyName = name;
        this.ticker = ticker;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
        this.open = open;
        this.dayHigh = high;
        this.dayLow = low;
        this.previousClose = close;
        this.lastUpdated = lastUpdated;
    }

    /** FLAT if change is < 0.25% */
    public Trend getTrend() {
        return changePercent > 0.25
                ? Trend.UP
                : (changePercent < -0.25
                   ? Trend.DOWN
                   : Trend.FLAT
        );
    }

    public String getTicker() {
        return ticker;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getChange() {
        return change;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public double getDayHigh() {
        return dayHigh;
    }

    public double getDayLow() {
        return dayLow;
    }

    public double getOpen() {
        return open;
    }

    public double getPrice() {
        return price;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}