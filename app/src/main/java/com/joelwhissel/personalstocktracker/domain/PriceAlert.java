package com.joelwhissel.personalstocktracker.domain;

import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;

public class PriceAlert {
    private final long id, triggeredAt;
    private final String ticker;
    private final AlertType type;
    private final Direction direction;
    /** For PERCENT alerts, targetValue is a percentage change from basePrice in the alert's direction. */
    private final double targetValue;
    private final double basePrice;
    private final boolean active;

    public PriceAlert(long id, long triggeredAt, String ticker, AlertType type, Direction direction,
                      double targetValue, double basePrice, boolean active) {
        this.id = id;
        this.triggeredAt = triggeredAt;
        this.ticker = ticker;
        this.type = type;
        this.direction = direction;
        this.targetValue = targetValue;
        this.basePrice = basePrice;
        this.active = active;
    }
    /** Price at which the alert fires. */
    public double getThresholdPrice() {
        if (type == AlertType.PRICE) {
            return targetValue;
        }
        double multiplier = targetValue / 100.0;
        return direction == Direction.UP
                ? basePrice * (1 + multiplier)
                : basePrice * (1 - multiplier);
    }

    public long getId() {
        return id;
    }

    public long getTriggeredAt() {
        return triggeredAt;
    }

    public String getTicker() {
        return ticker;
    }

    public AlertType getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public boolean isActive() {
        return active;
    }
}
