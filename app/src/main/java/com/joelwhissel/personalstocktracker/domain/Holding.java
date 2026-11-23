package com.joelwhissel.personalstocktracker.domain;

public class Holding {
    private final String ticker;
    private final double shares, avgBuyPrice;
    private final StockQuote quote;

    public Holding(String ticker, double shares, double avgBuyPrice, StockQuote quote) {
        this.ticker = ticker;
        this.shares = shares;
        this.avgBuyPrice = avgBuyPrice;
        this.quote = quote;
    }

    public String getTicker() {
        return ticker;
    }

    public double getShares() {
        return shares;
    }

    public double getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public StockQuote getQuote() {
        return quote;
    }
}
