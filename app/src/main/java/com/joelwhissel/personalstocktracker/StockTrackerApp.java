package com.joelwhissel.personalstocktracker;

import android.app.Application;

/**
 * App entry point, registered in AndroidManifest.xml. Sets up app-wide services before any
 * screen or worker runs.
 */
public class StockTrackerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceLocator.init(this);

        // Phase 2: create the notification channels, pass NotificationHelper to
        // ServiceLocator.setAlertNotifier(), and enqueue AlertCheckWorker (15 min) and
        // SlowMoverWorker (24 h) with ExistingPeriodicWorkPolicy.KEEP.
        // Phase 2: purge orphan stock rows left by a kill during an Undo window.
    }
}
