package com.joelwhissel.personalstocktracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "price_history",
        foreignKeys = @ForeignKey(entity = StockEntity.class,
                parentColumns = "ticker", childColumns = "ticker",
                onDelete = ForeignKey.CASCADE),
        primaryKeys = {"ticker", "date"})
public class PriceHistoryEntity {
    @NonNull
    public String ticker;
    @NonNull
    public String date;
    public double close;
    public double high, low;
    public long volume;
}
