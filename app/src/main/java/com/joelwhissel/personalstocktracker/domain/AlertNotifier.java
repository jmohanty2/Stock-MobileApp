package com.joelwhissel.personalstocktracker.domain;

/**
 * Posts a price-alert notification.
 */
public interface AlertNotifier {

    /**
     * Called once per fired alert, from a background thread.
     * Does nothing if notification permission is denied.
     */
    void showPriceAlert(PriceAlert alert, double price);
}
