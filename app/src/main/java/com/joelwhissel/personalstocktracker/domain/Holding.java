package com.joelwhissel.personalstocktracker.domain;

/**
 * One stock the user owns: how many shares, the average price paid,
 * and the latest quote. Gain/loss formulas follow design doc 8.4.
 *
 * <p>When no quote has been loaded yet ({@code quote == null}) the price is
 * treated as unknown: market value and gains are 0.</p>
 */
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

    /** True when a current price is available. */
    public boolean hasQuote() {
        return quote != null;
    }

    /** Amount paid in total: shares × average buy price. */
    public double costBasis() {
        return shares * avgBuyPrice;
    }

    /** Current worth: shares × current price. */
    public double marketValue() {
        return hasQuote() ? shares * quote.getPrice() : 0;
    }

    /** Today's gain/loss in $: shares × today's price change. */
    public double todayGain() {
        return hasQuote() ? shares * quote.getChange() : 0;
    }

    /** Today's gain/loss in %: the stock's own daily change. */
    public double todayGainPercent() {
        return hasQuote() ? quote.getChangePercent() : 0;
    }

    /** Total gain/loss in $: market value − cost basis. */
    public double totalGain() {
        return hasQuote() ? marketValue() - costBasis() : 0;
    }

    /** Total gain/loss in %: total gain ÷ cost basis × 100. */
    public double totalGainPercent() {
        double cost = costBasis();
        return (hasQuote() && cost > 0) ? totalGain() / cost * 100 : 0;
    }
}
