package com.joelwhissel.personalstocktracker;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.joelwhissel.personalstocktracker.data.repository.AlertRepository;
import com.joelwhissel.personalstocktracker.data.repository.SettingsRepository;
import com.joelwhissel.personalstocktracker.data.repository.StockRepository;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeAlertRepository;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeSettingsRepository;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeStockRepository;
import com.joelwhissel.personalstocktracker.domain.AlertNotifier;

/**
 * Hands out the app's single repository instances. ViewModels and workers get repositories
 * from here so fakes and real code switch with one flag.
 *
 * <pre>
 * StockRepository repo = ServiceLocator.stockRepository();
 * </pre>
 */
public final class ServiceLocator {

    /**
     * true: in-memory fakes with sample data, no API keys or network needed.
     * false: Room and the market-data APIs (wired in phase 2).
     */
    public static final boolean USE_FAKES = true;

    private static Context appContext;
    private static AlertNotifier alertNotifier;
    private static StockRepository stockRepository;
    private static AlertRepository alertRepository;
    private static SettingsRepository settingsRepository;

    private ServiceLocator() {
    }

    /** Called once from StockTrackerApp.onCreate(). */
    public static synchronized void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static synchronized StockRepository stockRepository() {
        if (stockRepository == null) createRepositories();
        return stockRepository;
    }

    public static synchronized AlertRepository alertRepository() {
        if (alertRepository == null) createRepositories();
        return alertRepository;
    }

    public static synchronized SettingsRepository settingsRepository() {
        if (settingsRepository == null) createRepositories();
        return settingsRepository;
    }

    /**
     * Sets where fired price alerts are posted (NotificationHelper). Call from
     * StockTrackerApp.onCreate() before any repository is used.
     */
    public static synchronized void setAlertNotifier(AlertNotifier notifier) {
        alertNotifier = notifier;
        if (alertRepository instanceof FakeAlertRepository) {
            ((FakeAlertRepository) alertRepository).setNotifier(notifier);
        }
    }

    /** Replaces the repositories, e.g. with fakes in a known state for a ViewModel or Espresso test. */
    @VisibleForTesting
    public static synchronized void setRepositories(StockRepository stocks, AlertRepository alerts,
                                                    SettingsRepository settings) {
        stockRepository = stocks;
        alertRepository = alerts;
        settingsRepository = settings;
    }

    /** Drops every instance so the next call creates fresh ones. */
    @VisibleForTesting
    public static synchronized void reset() {
        stockRepository = null;
        alertRepository = null;
        settingsRepository = null;
    }

    private static void createRepositories() {
        if (USE_FAKES) {
            FakeAlertRepository alerts = new FakeAlertRepository();
            alerts.setNotifier(alertNotifier);
            alerts.seedSampleData();
            FakeStockRepository stocks = new FakeStockRepository(alerts);
            stocks.seedSampleData();
            stockRepository = stocks;
            alertRepository = alerts;
            settingsRepository = new FakeSettingsRepository();
        } else {
            if (appContext == null) {
                throw new IllegalStateException("ServiceLocator.init() was not called");
            }
            throw new IllegalStateException(
                    "Real repositories are not wired yet; keep USE_FAKES = true until phase 2");
        }
    }
}
