package com.joelwhissel.personalstocktracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stocks")
public class StockEntity {
    @PrimaryKey
    @NonNull
    public String ticker;
    public String companyName;
    public double currentPrice, change, changePercent, previousClose, open, dayHigh, dayLow;
    @NonNull
    public Boolean inWatchlist;
    public long lastUpdated;
    public long lastReminderAt;
    public String historyFetchedOn;

}
