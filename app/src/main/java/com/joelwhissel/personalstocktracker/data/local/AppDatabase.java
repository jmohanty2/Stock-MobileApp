package com.joelwhissel.personalstocktracker.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities={StockEntity.class, PriceAlertEntity.class,
        HoldingEntity.class, PriceHistoryEntity.class},
        version=1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME="stock_tracker.db";
    private static volatile AppDatabase appDatabase;

    public abstract StockDao stockDao();
    public abstract AlertDao alertDao();
    public abstract HoldingDao holdingDao();
    public abstract PriceHistoryDao priceHistoryDao();

    public static AppDatabase getInstance(Context context) {
        if (appDatabase == null) {
            synchronized (AppDatabase.class) {
                if (appDatabase == null) {
                    appDatabase = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }
        return appDatabase;
    }
}

