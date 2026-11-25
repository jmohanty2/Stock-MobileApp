package com.joelwhissel.personalstocktracker.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * A stock row is "tracked" while it is in the watchlist (inWatchlist = 1) or held in the
 * portfolio (has a holdings row). A row that is neither is an orphan: it only exists during
 * the 4-second Undo window after a watchlist remove, and is then purged. Purging deletes the
 * row's alerts and history through the CASCADE foreign keys (design doc 7).
 */
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

    /** Tracked rows (watchlist or portfolio), the set refreshed by refreshQuotesSync(). */
    @Query("SELECT * FROM stocks WHERE inWatchlist = 1 "
            + "OR ticker IN (SELECT ticker FROM holdings) ORDER BY ticker")
    List<StockEntity> getTracked();

    /** Number of tracked rows, checked against the 50-stock limit before a new ticker is added. */
    @Query("SELECT COUNT(*) FROM stocks WHERE inWatchlist = 1 "
            + "OR ticker IN (SELECT ticker FROM holdings)")
    int countTracked();

    /** Deletes every orphan row (with its alerts and history); run at app start. Returns rows deleted. */
    @Query("DELETE FROM stocks WHERE inWatchlist = 0 "
            + "AND ticker NOT IN (SELECT ticker FROM holdings)")
    int purgeOrphans();

    /**
     * Deletes one row if it is an orphan; run when its Undo window closes. Scoped to one ticker
     * so a second stock still inside its own Undo window is not purged early. Returns 1 if deleted.
     */
    @Query("DELETE FROM stocks WHERE ticker = :ticker AND inWatchlist = 0 "
            + "AND ticker NOT IN (SELECT ticker FROM holdings)")
    int purgeIfOrphan(String ticker);
}
