package com.joelwhissel.personalstocktracker;

import android.app.Application;

import com.joelwhissel.personalstocktracker.util.NotificationHelper;

public class StockTrackerApp extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        ServiceLocator.init(this);

        NotificationHelper notificationHelper =
                new NotificationHelper(this);

        notificationHelper.createNotificationChannels();

        ServiceLocator.setAlertNotifier(notificationHelper);

        // A6: Schedule background workers here.
    }
}