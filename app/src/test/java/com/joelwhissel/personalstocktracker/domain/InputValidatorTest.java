package com.joelwhissel.personalstocktracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import org.junit.Test;

public class InputValidatorTest {

    private static final double DELTA = 1e-9;
    private static final double PRICE = 150.0;   // current price used in alert tests

    // ---------- Shares (FR-P8) ----------

    @Test
    public void shares_wholeNumber_isValid() {
        ValidationResult r = InputValidator.validateShares("10");
        assertTrue(r.isValid());
        assertEquals(10.0, r.getValue(), DELTA);
        assertNull(r.getError());
    }

    @Test
    public void shares_fourDecimals_isValid() {
        assertTrue(InputValidator.validateShares("0.1234").isValid());
    }

    @Test
    public void shares_trailingZerosBeyondFourDecimals_isValid() {
        assertTrue(InputValidator.validateShares("2.500000").isValid());
    }

    @Test
    public void shares_spacesAround_areTrimmed() {
        assertEquals(5.0, InputValidator.validateShares("  5 ").getValue(), DELTA);
    }

    @Test
    public void shares_fiveDecimals_isInvalid() {
        assertFalse(InputValidator.validateShares("1.23456").isValid());
    }

    @Test
    public void shares_zero_isInvalid() {
        ValidationResult r = InputValidator.validateShares("0");
        assertFalse(r.isValid());
        assertEquals(ErrorType.INVALID_VALUE, r.getError());
    }

    @Test
    public void shares_negative_isInvalid() {
        assertFalse(InputValidator.validateShares("-3").isValid());
    }

    @Test
    public void shares_empty_isInvalid() {
        assertFalse(InputValidator.validateShares("").isValid());
        assertFalse(InputValidator.validateShares("   ").isValid());
        assertFalse(InputValidator.validateShares(null).isValid());
    }

    @Test
    public void shares_notANumber_isInvalid() {
        assertFalse(InputValidator.validateShares("abc").isValid());
        assertFalse(InputValidator.validateShares("1,5").isValid());
    }

    // ---------- Buy price (FR-P8) ----------

    @Test
    public void buyPrice_positive_isValid() {
        assertEquals(189.99, InputValidator.validateBuyPrice("189.99").getValue(), DELTA);
    }

    @Test
    public void buyPrice_zeroOrNegative_isInvalid() {
        assertFalse(InputValidator.validateBuyPrice("0").isValid());
        assertFalse(InputValidator.validateBuyPrice("-1").isValid());
    }

    // ---------- Alerts (FR-D7) ----------

    @Test
    public void alert_percentUp_isValid() {
        assertTrue(InputValidator.validateAlert(AlertType.PERCENT, Direction.UP, "5", PRICE).isValid());
    }

    @Test
    public void alert_percentUpAbove100_isValid() {
        // a rise of 150% is possible
        assertTrue(InputValidator.validateAlert(AlertType.PERCENT, Direction.UP, "150", PRICE).isValid());
    }

    @Test
    public void alert_percentDown_isValid() {
        assertTrue(InputValidator.validateAlert(AlertType.PERCENT, Direction.DOWN, "10", PRICE).isValid());
    }

    @Test
    public void alert_percentDown100OrMore_isInvalid() {
        assertFalse(InputValidator.validateAlert(AlertType.PERCENT, Direction.DOWN, "100", PRICE).isValid());
        assertFalse(InputValidator.validateAlert(AlertType.PERCENT, Direction.DOWN, "120", PRICE).isValid());
    }

    @Test
    public void alert_negativeValue_isInvalid() {
        assertFalse(InputValidator.validateAlert(AlertType.PRICE, Direction.DOWN, "-3", PRICE).isValid());
        assertFalse(InputValidator.validateAlert(AlertType.PERCENT, Direction.UP, "-5", PRICE).isValid());
    }

    @Test
    public void alert_emptyOrText_isInvalid() {
        assertFalse(InputValidator.validateAlert(AlertType.PRICE, Direction.UP, "", PRICE).isValid());
        assertFalse(InputValidator.validateAlert(AlertType.PRICE, Direction.UP, "ten", PRICE).isValid());
    }

    @Test
    public void alert_priceUpAboveCurrent_isValid() {
        assertTrue(InputValidator.validateAlert(AlertType.PRICE, Direction.UP, "160", PRICE).isValid());
    }

    @Test
    public void alert_priceUpBelowOrEqualCurrent_isInvalid() {
        assertFalse(InputValidator.validateAlert(AlertType.PRICE, Direction.UP, "140", PRICE).isValid());
        assertFalse(InputValidator.validateAlert(AlertType.PRICE, Direction.UP, "150", PRICE).isValid());
    }

    @Test
    public void alert_priceDownBelowCurrent_isValid() {
        assertTrue(InputValidator.validateAlert(AlertType.PRICE, Direction.DOWN, "140", PRICE).isValid());
    }

    @Test
    public void alert_priceDownAboveOrEqualCurrent_isInvalid() {
        assertFalse(InputValidator.validateAlert(AlertType.PRICE, Direction.DOWN, "160", PRICE).isValid());
        assertFalse(InputValidator.validateAlert(AlertType.PRICE, Direction.DOWN, "150", PRICE).isValid());
    }

    @Test
    public void alert_priceWithUnknownCurrentPrice_skipsSideCheck() {
        assertTrue(InputValidator.validateAlert(AlertType.PRICE, Direction.UP, "50", 0).isValid());
    }

    @Test
    public void alert_missingTypeOrDirection_isInvalid() {
        assertFalse(InputValidator.validateAlert(null, Direction.UP, "5", PRICE).isValid());
        assertFalse(InputValidator.validateAlert(AlertType.PERCENT, null, "5", PRICE).isValid());
    }
}
