package com.joelwhissel.personalstocktracker.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PriceHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PriceHistoryEntity> rows);
    @Update
    void update(PriceHistoryEntity priceHistory);
    @Query("SELECT * FROM price_history WHERE ticker = :ticker AND date >= :fromDate ORDER BY date ASC")
    List<PriceHistoryEntity> getHistoryForTicker(String ticker, String fromDate);
}
