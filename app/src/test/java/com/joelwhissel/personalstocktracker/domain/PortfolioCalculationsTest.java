package com.joelwhissel.personalstocktracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PortfolioCalculationsTest {

    private static final double DELTA = 1e-6;

    /** Quote with a price and today's change; other fields don't matter here. */
    private static StockQuote quote(String ticker, double price, double change) {
        double previousClose = price - change;
        double changePercent = change / previousClose * 100;
        return new StockQuote(ticker, ticker + " Inc.", price, change, changePercent,
                previousClose, price, previousClose, previousClose, 0L);
    }

    // 10 shares bought at $100, now $110, up $2 today
    private final Holding aapl = new Holding("AAPL", 10, 100, quote("AAPL", 110, 2));
    // 5 shares bought at $300, now $280, down $5 today
    private final Holding msft = new Holding("MSFT", 5, 300, quote("MSFT", 280, -5));

    // ---------- Holding ----------

    @Test
    public void holding_marketValue() {
        assertEquals(1100.0, aapl.marketValue(), DELTA);
    }

    @Test
    public void holding_costBasis() {
        assertEquals(1000.0, aapl.costBasis(), DELTA);
    }

    @Test
    public void holding_todayGain() {
        assertEquals(20.0, aapl.todayGain(), DELTA);
        assertEquals(-25.0, msft.todayGain(), DELTA);
    }

    @Test
    public void holding_totalGainAndPercent() {
        assertEquals(100.0, aapl.totalGain(), DELTA);
        assertEquals(10.0, aapl.totalGainPercent(), DELTA);
        assertEquals(-100.0, msft.totalGain(), DELTA);          // 1400 − 1500
        assertEquals(-6.666667, msft.totalGainPercent(), DELTA);
    }

    @Test
    public void holding_todayGainPercent_isStockChangePercent() {
        assertEquals(2.0 / 108 * 100, aapl.todayGainPercent(), DELTA);
    }

    @Test
    public void holding_fractionalShares() {
        Holding h = new Holding("TSLA", 0.5, 200, quote("TSLA", 250, 10));
        assertEquals(125.0, h.marketValue(), DELTA);
        assertEquals(25.0, h.totalGain(), DELTA);
    }

    @Test
    public void holding_withoutQuote_isZeroNotCrash() {
        Holding h = new Holding("NVDA", 3, 400, null);
        assertFalse(h.hasQuote());
        assertEquals(0.0, h.marketValue(), DELTA);
        assertEquals(0.0, h.todayGain(), DELTA);
        assertEquals(0.0, h.totalGain(), DELTA);
        assertEquals(0.0, h.totalGainPercent(), DELTA);
        assertEquals(1200.0, h.costBasis(), DELTA);
    }

    // ---------- PortfolioSummary ----------

    @Test
    public void summary_addsUpHoldings() {
        PortfolioSummary s = PortfolioSummary.from(Arrays.asList(aapl, msft));
        assertEquals(2500.0, s.getTotalValue(), DELTA);   // 1100 + 1400
        assertEquals(-5.0, s.getTodayGain(), DELTA);      // 20 − 25
        assertEquals(0.0, s.getTotalGain(), DELTA);       // 100 − 100
        assertEquals(2500.0, s.getTotalCost(), DELTA);    // 1000 + 1500
        assertEquals(0.0, s.getTotalGainPercent(), DELTA);
        assertEquals(-5.0 / 2505 * 100, s.getTodayGainPercent(), DELTA);
        assertEquals(2, s.getHoldings().size());
    }

    @Test
    public void summary_emptyPortfolio_isAllZeros() {
        PortfolioSummary s = PortfolioSummary.from(new ArrayList<>());
        assertEquals(0.0, s.getTotalValue(), DELTA);
        assertEquals(0.0, s.getTodayGain(), DELTA);
        assertEquals(0.0, s.getTotalGain(), DELTA);
        assertEquals(0.0, s.getTotalGainPercent(), DELTA);
        assertEquals(0.0, s.getTodayGainPercent(), DELTA);
        assertTrue(s.getHoldings().isEmpty());
    }

    @Test
    public void summary_nullList_isAllZeros() {
        assertEquals(0.0, PortfolioSummary.from(null).getTotalValue(), DELTA);
    }

    @Test
    public void summary_ignoresUnpricedHoldingInPercent() {
        Holding unpriced = new Holding("NVDA", 3, 400, null);
        PortfolioSummary s = PortfolioSummary.from(Arrays.asList(aapl, unpriced));
        assertEquals(1100.0, s.getTotalValue(), DELTA);
        assertEquals(10.0, s.getTotalGainPercent(), DELTA);   // only AAPL counts
    }

    @Test
    public void summary_singleHolding_matchesHolding() {
        PortfolioSummary s = PortfolioSummary.from(Collections.singletonList(aapl));
        assertEquals(aapl.marketValue(), s.getTotalValue(), DELTA);
        assertEquals(aapl.totalGainPercent(), s.getTotalGainPercent(), DELTA);
    }
}
