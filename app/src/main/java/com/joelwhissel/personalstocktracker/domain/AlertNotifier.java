package com.joelwhissel.personalstocktracker.domain;

/**
 * Posts a price-alert notification. Plain Java so AlertRepository does not depend on Android;
 * NotificationHelper implements it (design doc 8.3).
 */
public interface AlertNotifier {

    /**
     * Posts "AAPL rose 5.2% to $198.40" on the "Price alerts" channel (FR-A2). Called once per
     * fired alert, from a background thread. Does nothing if notification permission is denied.
     */
    void showPriceAlert(PriceAlert alert, double price);
}
