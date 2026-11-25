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

    /** Tracked rows (watchlist or portfolio). */
    @Query("SELECT * FROM stocks WHERE inWatchlist = 1 "
            + "OR ticker IN (SELECT ticker FROM holdings) ORDER BY ticker")
    List<StockEntity> getTracked();

    /** Number of tracked rows. */
    @Query("SELECT COUNT(*) FROM stocks WHERE inWatchlist = 1 "
            + "OR ticker IN (SELECT ticker FROM holdings)")
    int countTracked();

    /** Deletes every orphan row (with its alerts and history); run at app start. Returns rows deleted. */
    @Query("DELETE FROM stocks WHERE inWatchlist = 0 "
            + "AND ticker NOT IN (SELECT ticker FROM holdings)")
    int purgeOrphans();

    /** Deletes one row if it is an orphan; run when its Undo window closes. */
    @Query("DELETE FROM stocks WHERE ticker = :ticker AND inWatchlist = 0 "
            + "AND ticker NOT IN (SELECT ticker FROM holdings)")
    int purgeIfOrphan(String ticker);
}
