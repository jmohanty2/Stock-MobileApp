package com.joelwhissel.personalstocktracker.domain;

import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

public final class ValidationResult {
    private final boolean valid;
    private final double value;
    private final ErrorType error;

    private ValidationResult(boolean valid, double value, ErrorType error) {
        this.valid = valid;
        this.value = value;
        this.error = error;
    }

    public static ValidationResult ok(double value) {
        return new ValidationResult(true, value, null);
    }

    public static ValidationResult invalid() {
        return new ValidationResult(false, Double.NaN, ErrorType.INVALID_VALUE);
    }

    public boolean isValid() {
        return valid;
    }

    /** The parsed number. Only meaningful when {@link #isValid()} is true. */
    public double getValue() {
        return value;
    }

    /** {@code null} when valid, otherwise {@link ErrorType#INVALID_VALUE}. */
    public ErrorType getError() {
        return error;
    }
}
