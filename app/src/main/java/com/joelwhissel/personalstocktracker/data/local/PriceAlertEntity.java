package com.joelwhissel.personalstocktracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "price_alerts",
        foreignKeys = @ForeignKey(entity = StockEntity.class,
                parentColumns = "ticker", childColumns = "ticker",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("ticker"))
public class PriceAlertEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String ticker;
    @NonNull
    public String type;       // "PERCENT" or "PRICE"
    @NonNull
    public String direction;  // "UP" or "DOWN"
    public double targetValue;
    public double basePrice;
    public boolean active = true;
    public Long triggeredAt;           // null until fired
    public long createdAt;
}

