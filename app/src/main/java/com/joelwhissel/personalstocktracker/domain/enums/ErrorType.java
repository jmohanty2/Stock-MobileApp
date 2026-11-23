package com.joelwhissel.personalstocktracker.domain.enums;

public enum ErrorType {
    STOCK_NOT_FOUND,
    NETWORK_ERROR,
    /** Bad alert or share input. */
    INVALID_VALUE,
    /** Adding a ticker already in a list. */
    DUPLICATE
}