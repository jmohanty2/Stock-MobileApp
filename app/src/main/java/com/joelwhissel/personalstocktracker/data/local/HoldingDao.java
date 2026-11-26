package com.joelwhissel.personalstocktracker.data.local;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface HoldingDao {
    @Insert
    long insert(HoldingEntity holding);
    @Update
    void update(HoldingEntity holding);
    @Delete
    void delete(HoldingEntity holding);
    @Query("SELECT * FROM holdings WHERE ticker = :ticker")
    LiveData<List<HoldingEntity>> observeForTicker(String ticker);

    /** Every holding, for the Portfolio screen. */
    @Query("SELECT * FROM holdings ORDER BY ticker")
    LiveData<List<HoldingEntity>> observeAll();

    /** The holding for a ticker, or null if the ticker is not held (tickers are unique). */
    @Nullable
    @Query("SELECT * FROM holdings WHERE ticker = :ticker")
    HoldingEntity get(String ticker);
}
