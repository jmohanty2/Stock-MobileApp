package com.joelwhissel.personalstocktracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "holdings", 
        foreignKeys = @ForeignKey(entity = StockEntity.class,
                parentColumns = "ticker", childColumns = "ticker",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("ticker"))
public class HoldingEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String ticker;
    @NonNull
    public Double shares;
    public double avgBuyPrice;
    public long createdAt;
}

