package com.joelwhissel.personalstocktracker;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.joelwhissel.personalstocktracker.util.NotificationHelper;
import com.joelwhissel.personalstocktracker.worker.AlertCheckWorker;
import com.joelwhissel.personalstocktracker.worker.SlowMoverWorker;

import java.util.concurrent.TimeUnit;

public class StockTrackerApp extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        ServiceLocator.init(this);

        // Initialize notifications.
        NotificationHelper notificationHelper =
                new NotificationHelper(this);

        notificationHelper.createNotificationChannels();

        ServiceLocator.setAlertNotifier(notificationHelper);

        //Schedule background workers.
        scheduleWorkers();
    }

    private void scheduleWorkers() {

        Constraints networkConstraints =
                new Constraints.Builder()
                        .setRequiredNetworkType(
                                NetworkType.CONNECTED
                        )
                        .build();

        PeriodicWorkRequest alertRequest =
                new PeriodicWorkRequest.Builder(
                        AlertCheckWorker.class,
                        15,
                        TimeUnit.MINUTES
                )
                        .setConstraints(networkConstraints)
                        .build();

        PeriodicWorkRequest slowMoverRequest =
                new PeriodicWorkRequest.Builder(
                        SlowMoverWorker.class,
                        24,
                        TimeUnit.HOURS
                )
                        .setConstraints(networkConstraints)
                        .build();

        WorkManager workManager =
                WorkManager.getInstance(this);

        workManager.enqueueUniquePeriodicWork(
                "alert-check",
                ExistingPeriodicWorkPolicy.KEEP,
                alertRequest
        );

        workManager.enqueueUniquePeriodicWork(
                "slow-mover",
                ExistingPeriodicWorkPolicy.KEEP,
                slowMoverRequest
        );
    }
}