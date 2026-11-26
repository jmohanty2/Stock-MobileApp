package com.joelwhissel.personalstocktracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.joelwhissel.personalstocktracker.domain.enums.Volatility;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IndicatorCalculatorTest {

    private static final double DELTA = 1e-6;

    /** Closes oldest → newest: 100, 101, …, 130 (31 values). */
    private static List<Double> risingBy1() {
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i <= 30; i++) {
            closes.add(100.0 + i);
        }
        return closes;
    }

    /** 31 closes alternating around 100 by ±amplitude %. */
    private static List<Double> zigzag(double amplitudePercent) {
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i <= 30; i++) {
            closes.add(i % 2 == 0 ? 100.0 : 100.0 * (1 + amplitudePercent / 100));
        }
        return closes;
    }

    // ---------- percentChange ----------

    @Test
    public void percentChange_5days() {
        // today 130, 5 days ago 125 → +4%
        assertEquals(4.0, IndicatorCalculator.percentChange(risingBy1(), 5), DELTA);
    }

    @Test
    public void percentChange_30days() {
        // today 130, 30 days ago 100 → +30%
        assertEquals(30.0, IndicatorCalculator.percentChange(risingBy1(), 30), DELTA);
    }

    @Test
    public void percentChange_falling() {
        List<Double> closes = Arrays.asList(200.0, 190.0, 180.0, 170.0, 160.0, 150.0);
        assertEquals(-25.0, IndicatorCalculator.percentChange(closes, 5), DELTA);
    }

    @Test
    public void percentChange_notEnoughData_isNaN() {
        assertTrue(Double.isNaN(IndicatorCalculator.percentChange(Arrays.asList(1.0, 2.0), 5)));
        assertTrue(Double.isNaN(IndicatorCalculator.percentChange(null, 5)));
    }

    // ---------- sma ----------

    @Test
    public void sma_last20() {
        // last 20 closes are 111..130 → mean 120.5
        assertEquals(120.5, IndicatorCalculator.sma(risingBy1(), 20), DELTA);
    }

    @Test
    public void sma_notEnoughData_isNaN() {
        assertTrue(Double.isNaN(IndicatorCalculator.sma(Arrays.asList(1.0, 2.0, 3.0), 20)));
    }

    @Test
    public void isAboveSma_risingPrices_isTrue() {
        assertTrue(IndicatorCalculator.isAboveSma(risingBy1(), 20));
    }

    @Test
    public void isAboveSma_fallingPrices_isFalse() {
        List<Double> falling = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            falling.add(200.0 - i);
        }
        assertFalse(IndicatorCalculator.isAboveSma(falling, 20));
    }

    // ---------- highLow ----------

    @Test
    public void highLow_last30() {
        double[] hl = IndicatorCalculator.highLow(risingBy1(), 30);
        assertEquals(130.0, hl[0], DELTA);   // high
        assertEquals(101.0, hl[1], DELTA);   // low (100 is 31 days back, outside the window)
    }

    @Test
    public void highLow_notEnoughData_isNaN() {
        double[] hl = IndicatorCalculator.highLow(Arrays.asList(5.0), 30);
        assertTrue(Double.isNaN(hl[0]));
        assertTrue(Double.isNaN(hl[1]));
    }

    // ---------- volatility ----------

    @Test
    public void volatility_flatPrices_isLow() {
        List<Double> flat = new ArrayList<>();
        for (int i = 0; i <= 30; i++) {
            flat.add(50.0);
        }
        assertEquals(Volatility.LOW, IndicatorCalculator.volatility(flat));
    }

    @Test
    public void volatility_smallSwings_isMedium() {
        // ±1.5% swings → daily-return std dev ≈ 1.5%
        assertEquals(Volatility.MEDIUM, IndicatorCalculator.volatility(zigzag(1.5)));
    }

    @Test
    public void volatility_bigSwings_isHigh() {
        // ±5% swings → std dev ≈ 5%
        assertEquals(Volatility.HIGH, IndicatorCalculator.volatility(zigzag(5)));
    }

    @Test
    public void volatility_notEnoughData_isNull() {
        assertNull(IndicatorCalculator.volatility(Arrays.asList(1.0, 2.0, 3.0)));
    }

    // ---------- isSlowMover (FR-A5) ----------

    @Test
    public void slowMover_tinyRange_isTrue() {
        // range 100.0 – 101.5 over 10 days = 1.5% < 2%
        List<Double> closes = Arrays.asList(100.0, 100.5, 101.0, 101.5, 100.2,
                100.8, 101.2, 100.4, 100.9, 101.1);
        assertTrue(IndicatorCalculator.isSlowMover(closes, 10, 2.0));
    }

    @Test
    public void slowMover_bigRange_isFalse() {
        // range 100 – 105 = 5% ≥ 2%
        List<Double> closes = Arrays.asList(100.0, 101.0, 102.0, 103.0, 104.0,
                105.0, 104.0, 103.0, 102.0, 101.0);
        assertFalse(IndicatorCalculator.isSlowMover(closes, 10, 2.0));
    }

    @Test
    public void slowMover_usesOnlyLastNDays() {
        // a big jump 11 days ago must be ignored
        List<Double> closes = new ArrayList<>(Arrays.asList(50.0));
        for (int i = 0; i < 10; i++) {
            closes.add(100.0 + i * 0.1);
        }
        assertTrue(IndicatorCalculator.isSlowMover(closes, 10, 2.0));
    }

    @Test
    public void slowMover_notEnoughData_isFalse() {
        assertFalse(IndicatorCalculator.isSlowMover(Arrays.asList(100.0, 100.1), 10, 2.0));
    }
}
