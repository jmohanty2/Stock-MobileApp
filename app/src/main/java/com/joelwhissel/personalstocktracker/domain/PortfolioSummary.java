package com.joelwhissel.personalstocktracker.domain;

import java.util.Collections;
import java.util.List;

/**
 * Totals for the Portfolio screen header (FR-P4, FR-P5).
 * Build it with {@link #from(List)}; an empty list gives all zeros.
 */
public class PortfolioSummary {
    private final double totalValue, todayGain, totalGain;
    private final List<Holding> holdings;

    public PortfolioSummary(double totalValue, double todayGain, double totalGain, List<Holding> holdings) {
        this.totalValue = totalValue;
        this.todayGain = todayGain;
        this.totalGain = totalGain;
        this.holdings = holdings == null ? Collections.emptyList() : holdings;
    }

    /** Adds up every holding (design doc 8.4). */
    public static PortfolioSummary from(List<Holding> holdings) {
        if (holdings == null) {
            holdings = Collections.emptyList();
        }
        double value = 0, today = 0, total = 0;
        for (Holding h : holdings) {
            value += h.marketValue();
            today += h.todayGain();
            total += h.totalGain();
        }
        return new PortfolioSummary(value, today, total, Collections.unmodifiableList(holdings));
    }

    public double getTotalValue() {
        return totalValue;
    }

    public double getTodayGain() {
        return todayGain;
    }

    public double getTotalGain() {
        return totalGain;
    }

    public List<Holding> getHoldings() {
        return holdings;
    }

    /** Sum of cost basis of the holdings that have a price. */
    public double getTotalCost() {
        double cost = 0;
        for (Holding h : holdings) {
            if (h.hasQuote()) {
                cost += h.costBasis();
            }
        }
        return cost;
    }

    /** Total gain ÷ total cost × 100; 0 when nothing is priced. */
    public double getTotalGainPercent() {
        double cost = getTotalCost();
        return cost > 0 ? totalGain / cost * 100 : 0;
    }

    /** Today's gain ÷ yesterday's value × 100; 0 when nothing is priced. */
    public double getTodayGainPercent() {
        double yesterday = totalValue - todayGain;
        return yesterday > 0 ? todayGain / yesterday * 100 : 0;
    }
}
