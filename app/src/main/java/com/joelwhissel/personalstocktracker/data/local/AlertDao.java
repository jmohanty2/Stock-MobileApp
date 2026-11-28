package com.joelwhissel.personalstocktracker.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AlertDao {
    @Insert
    long insert(PriceAlertEntity alert);
    @Update
    void update(PriceAlertEntity alert);
    @Delete
    void delete(PriceAlertEntity alert);
    @Query("SELECT * FROM price_alerts WHERE ticker = :ticker ORDER BY createdAt DESC")
    LiveData<List<PriceAlertEntity>> observeForTicker(String ticker);
    @Query("SELECT * FROM price_alerts WHERE active = 1")
    List<PriceAlertEntity> getActiveSync();   // used by AlertRepository.evaluateAndNotify()
    @Query("UPDATE price_alerts SET active = 0, triggeredAt = :now WHERE id = :id AND active = 1")
    int markTriggered(long id, long now);   // returns 1 only for the caller that fired the alert
    @Query("UPDATE price_alerts SET active = :active " +
            "WHERE id = :id AND " +
            "(:active = 0 OR triggeredAt IS NULL)")
    void updateAlertState(long id, boolean active);
    @Query("UPDATE price_alerts SET active = 1, " +
            "triggeredAt = NULL, basePrice = :basePrice " +
            "WHERE id = :id")
    void rearmAlert(long id, double basePrice);



}
