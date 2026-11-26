package com.joelwhissel.personalstocktracker.ui;

import androidx.annotation.StringRes;

import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    @StringRes
    public static int forError(ErrorType e, boolean portfolio) {
        if (e == null) {
            return R.string.error_network;
        }
        switch (e) {
            case STOCK_NOT_FOUND:
                return R.string.error_stock_not_found;
            case NETWORK_ERROR:
                return R.string.error_network;
            case INVALID_VALUE:
                return R.string.error_invalid_value;
            case DUPLICATE:
                return portfolio ? R.string.error_already_in_portfolio : R.string.error_already_in_watchlist;
            case LIMIT_REACHED:
                return R.string.error_limit_reached;
            default:
                return R.string.error_network;
        }
    }
}
