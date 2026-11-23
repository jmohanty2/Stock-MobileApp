package com.joelwhissel.personalstocktracker.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StockDao {
    @Insert
    long insert(StockEntity stock);
    @Update
    void update(StockEntity stock);
    @Delete
    void delete(StockEntity stock);
    @Query("SELECT * FROM stocks WHERE ticker = :ticker")
    StockEntity getStock(String ticker);
    @Query("UPDATE stocks SET inWatchlist = :status WHERE ticker = :ticker")
    void setInWatchlist(String ticker, boolean status);
    @Query("SELECT * FROM stocks WHERE inWatchlist = 1")
    LiveData<List<StockEntity>> observeWatchlist();

}
