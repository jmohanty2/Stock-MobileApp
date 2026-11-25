package com.joelwhissel.personalstocktracker.data.local;

import androidx.annotation.Nullable;
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

    /**
     * Most recent cached date (yyyy-MM-dd) for a ticker, or null if none is cached. Used to
     * refresh history oldest-first and to show "History as of" on Stock Details.
     */
    @Nullable
    @Query("SELECT MAX(date) FROM price_history WHERE ticker = :ticker")
    String latestDate(String ticker);
}
