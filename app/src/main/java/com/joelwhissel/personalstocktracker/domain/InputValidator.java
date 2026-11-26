package com.joelwhissel.personalstocktracker.domain;

import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;

import java.math.BigDecimal;

/**
 * Checks numbers typed by the user before anything is saved
 * (design doc FR-P8 and FR-D7). Pure Java: no Android classes, easy to unit test.
 *
 * <p>Every method returns a {@link ValidationResult}; the screen shows
 * "Invalid Value" on the field when the result is not valid.</p>
 */
public final class InputValidator {

    /** Shares may have fractions, up to 4 decimals (FR-P8). */
    static final int MAX_SHARE_DECIMALS = 4;
    /** Prices are accepted with up to 4 decimals (some stocks trade below $1). */
    static final int MAX_PRICE_DECIMALS = 4;

    private InputValidator() {
        // static helpers only
    }

    /** Number of shares owned: positive, at most 4 decimals (FR-P8). */
    public static ValidationResult validateShares(String input) {
        return parsePositive(input, MAX_SHARE_DECIMALS);
    }

    /** Average buy price per share: positive amount, at most 4 decimals (FR-P8). */
    public static ValidationResult validateBuyPrice(String input) {
        return parsePositive(input, MAX_PRICE_DECIMALS);
    }

    /**
     * Value of a price alert (FR-D7).
     *
     * <ul>
     *   <li>Empty, non-numeric, zero or negative values are invalid.</li>
     *   <li>PERCENT + DOWN: a fall of 100% or more is invalid (the price cannot reach 0).</li>
     *   <li>PRICE + UP: the target must be above the current price.</li>
     *   <li>PRICE + DOWN: the target must be below the current price.</li>
     * </ul>
     *
     * @param currentPrice latest quote; pass 0 when no quote is known yet,
     *                     and the above/below check is skipped
     */
    public static ValidationResult validateAlert(AlertType type, Direction direction,
                                                 String input, double currentPrice) {
        if (type == null || direction == null) {
            return ValidationResult.invalid();
        }
        ValidationResult parsed = parsePositive(input, MAX_PRICE_DECIMALS);
        if (!parsed.isValid()) {
            return parsed;
        }
        double value = parsed.getValue();

        if (type == AlertType.PERCENT) {
            if (direction == Direction.DOWN && value >= 100) {
                return ValidationResult.invalid();
            }
            return parsed;
        }

        // AlertType.PRICE
        boolean priceKnown = currentPrice > 0 && !Double.isNaN(currentPrice)
                && !Double.isInfinite(currentPrice);
        if (priceKnown) {
            if (direction == Direction.UP && value <= currentPrice) {
                return ValidationResult.invalid();
            }
            if (direction == Direction.DOWN && value >= currentPrice) {
                return ValidationResult.invalid();
            }
        }
        return parsed;
    }

    /** Trims, parses, and checks: finite, greater than 0, at most {@code maxDecimals} decimals. */
    private static ValidationResult parsePositive(String input, int maxDecimals) {
        if (input == null) {
            return ValidationResult.invalid();
        }
        String text = input.trim();
        if (text.isEmpty()) {
            return ValidationResult.invalid();
        }
        BigDecimal number;
        try {
            number = new BigDecimal(text);
        } catch (NumberFormatException e) {
            return ValidationResult.invalid();          // "abc", "1,5", "--2"
        }
        if (number.signum() <= 0) {
            return ValidationResult.invalid();          // 0 or negative
        }
        if (number.stripTrailingZeros().scale() > maxDecimals) {
            return ValidationResult.invalid();          // too many decimals
        }
        double value = number.doubleValue();
        if (Double.isInfinite(value)) {
            return ValidationResult.invalid();          // absurdly large
        }
        return ValidationResult.ok(value);
    }
}
