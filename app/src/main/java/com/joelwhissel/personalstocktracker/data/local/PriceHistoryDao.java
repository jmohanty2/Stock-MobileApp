package com.joelwhissel.personalstocktracker.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PriceHistoryDao {
    @Insert
    long insert(PriceHistoryEntity priceHistory);
    @Update
    void update(PriceHistoryEntity priceHistory);
    @Query("SELECT * FROM price_history WHERE ticker = :ticker AND date >= :date")
    List<PriceHistoryEntity> getHistoryForTicker(String ticker, String date);
}
