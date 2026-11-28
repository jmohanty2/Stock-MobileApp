package com.joelwhissel.personalstocktracker.domain;

import com.joelwhissel.personalstocktracker.domain.enums.Direction;

public class AlertEvaluator {

    public boolean isMet(PriceAlert alert, double price) {

        double threshold = alert.getThresholdPrice();

        if (alert.getDirection() == Direction.UP) {
            return price >= threshold;
        } else {
            return price <= threshold;
        }
    }
}