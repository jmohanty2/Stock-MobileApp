
package com.joelwhissel.personalstocktracker.domain;

import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class AlertEvaluatorTest {

    private final AlertEvaluator evaluator =
            new AlertEvaluator();

    @Test
    public void upAlertMetExactlyAtThreshold() {

        PriceAlert alert = new PriceAlert(
                1, null, "AAPL",
                AlertType.PRICE, Direction.UP,
                200.0, 0.0, true
        );

        assertTrue(evaluator.isMet(alert, 200.0));
    }

    @Test
    public void upAlertNotMetBelowThreshold() {

        PriceAlert alert = new PriceAlert(
                1, null, "AAPL",
                AlertType.PRICE, Direction.UP,
                200.0, 0.0, true
        );

        assertFalse(evaluator.isMet(alert, 199.0));
    }

    @Test
    public void downAlertNotMetAboveThreshold() {

        PriceAlert alert = new PriceAlert(
                1, null, "AAPL",
                AlertType.PRICE, Direction.DOWN,
                180.0, 0.0, true
        );

        assertFalse(evaluator.isMet(alert, 181.0));
    }

    @Test
    public void downAlertMetExactlyAtThreshold() {

        PriceAlert alert = new PriceAlert(
                1, null, "AAPL",
                AlertType.PRICE, Direction.DOWN,
                180.0, 0.0, true
        );

        assertTrue(evaluator.isMet(alert, 180.0));
    }

    @Test
    public void percentageUpAlert() {

        PriceAlert alert = new PriceAlert(
                1, null, "AAPL",
                AlertType.PERCENT, Direction.UP,
                5.0, 100.0, true
        );

        assertFalse(evaluator.isMet(alert, 104.0));
        assertTrue(evaluator.isMet(alert, 105.0));
    }

    @Test
    public void percentageDownAlert() {

        PriceAlert alert = new PriceAlert(
                1, null, "AAPL",
                AlertType.PERCENT, Direction.DOWN,
                5.0, 100.0, true
        );

        assertFalse(evaluator.isMet(alert, 96.0));
        assertTrue(evaluator.isMet(alert, 95.0));
    }
}