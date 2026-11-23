package com.joelwhissel.personalstocktracker.domain;

import java.util.List;

public class PortfolioSummary {
    private final double totalValue, todayGain, totalGain;
    private final List<Holding> holdings;

    public PortfolioSummary(double totalValue, double todayGain, double totalGain, List<Holding> holdings) {
        this.totalValue = totalValue;
        this.todayGain = todayGain;
        this.totalGain = totalGain;
        this.holdings = holdings;
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
}
