package com.joelwhissel.personalstocktracker.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.joelwhissel.personalstocktracker.ServiceLocator;
import com.joelwhissel.personalstocktracker.data.repository.Resource;
import com.joelwhissel.personalstocktracker.data.repository.SettingsRepository;
import com.joelwhissel.personalstocktracker.data.repository.StockRepository;
import com.joelwhissel.personalstocktracker.domain.DailyPrice;
import com.joelwhissel.personalstocktracker.domain.IndicatorCalculator;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;
import com.joelwhissel.personalstocktracker.util.NotificationHelper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SlowMoverWorker extends Worker {

    private static final long REMINDER_INTERVAL_MS =
            TimeUnit.DAYS.toMillis(7);

    public SlowMoverWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        StockRepository stocks =
                ServiceLocator.stockRepository();

        SettingsRepository settings =
                ServiceLocator.settingsRepository();

        NotificationHelper notifier =
                new NotificationHelper(getApplicationContext());

        if (!notifier.canPostNotifications()) {
            return Result.success();
        }

        double threshold = settings.slowMoverPct();
        int days = settings.slowMoverDays();

        long now = System.currentTimeMillis();

        boolean networkError = false;

        for (String ticker : stocks.getTrackedTickersSync()) {

            if (isStopped()) {
                return Result.retry();
            }

            long lastReminder =
                    stocks.getLastReminderAtSync(ticker);

            // Do not send another reminder within seven days.
            if (lastReminder > 0
                    && now - lastReminder < REMINDER_INTERVAL_MS) {

                continue;
            }

            Resource<List<DailyPrice>> response =
                    stocks.getHistorySync(ticker, days);

            if (response.status == Resource.Status.ERROR) {

                if (response.error == ErrorType.NETWORK_ERROR) {
                    networkError = true;
                }

                continue;
            }

            if (response.status != Resource.Status.SUCCESS
                    || response.data == null
                    || response.data.size() < days) {

                continue;
            }

            List<DailyPrice> history = response.data;

            // History is ordered oldest first.
            DailyPrice latest =
                    history.get(history.size() - 1);

            if (isHistoryStale(latest.getDate())) {
                continue;
            }

            List<Double> closes = new ArrayList<>();

            for (DailyPrice dailyPrice : history) {
                closes.add(dailyPrice.getClose());
            }

            if (IndicatorCalculator.isSlowMover(
                    closes, days, threshold)) {

                if (!notifier.canPostNotifications()) {
                    return Result.success();
                }

                notifier.notifySlowMover(
                        ticker,
                        threshold,
                        days
                );

                stocks.setLastReminderAtSync(
                        ticker,
                        System.currentTimeMillis()
                );
            }
        }

        return networkError
                ? Result.retry()
                : Result.success();
    }

    private boolean isHistoryStale(String latestDate) {

        if (latestDate == null) {
            return true;
        }

        try {

            LocalDate latest = LocalDate.parse(latestDate);

            LocalDate today = LocalDate.now(
                    ZoneId.of("America/New_York")
            );

            if (latest.isAfter(today)) {
                return true;
            }

            int tradingDays = 0;

            LocalDate date = latest.plusDays(1);

            while (!date.isAfter(today)) {

                DayOfWeek day = date.getDayOfWeek();

                if (day != DayOfWeek.SATURDAY
                        && day != DayOfWeek.SUNDAY) {

                    tradingDays++;
                }

                if (tradingDays > 2) {
                    return true;
                }

                date = date.plusDays(1);
            }

            return false;

        } catch (DateTimeParseException e) {
            return true;
        }
    }
}