package com.joelwhissel.personalstocktracker.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities={StockEntity.class, PriceAlertEntity.class,
        HoldingEntity.class, PriceHistoryEntity.class}, version=1)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME="stock_tracker.db";
    private static AppDatabase appDatabase;

    public static AppDatabase getInstance(Context context) {
        if (appDatabase == null) {
            appDatabase = Room.databaseBuilder(context, AppDatabase.class,
                            DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return appDatabase;
    }
}

