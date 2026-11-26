package com.joelwhissel.personalstocktracker.data.repository.fake;

import com.joelwhissel.personalstocktracker.data.repository.SettingsRepository;

import java.time.LocalDate;

/** In-memory SettingsRepository with the default thresholds; nothing survives a restart. */
public class FakeSettingsRepository implements SettingsRepository {

    private double slowMoverPct = DEFAULT_SLOW_MOVER_PCT;
    private int slowMoverDays = DEFAULT_SLOW_MOVER_DAYS;
    private int historyCalls;
    private LocalDate historyCallsDate = LocalDate.now();

    @Override
    public synchronized double slowMoverPct() {
        return slowMoverPct;
    }

    @Override
    public synchronized int slowMoverDays() {
        return slowMoverDays;
    }

    @Override
    public synchronized void setSlowMoverThresholds(double pct, int days) {
        slowMoverPct = pct;
        slowMoverDays = days;
    }

    @Override
    public synchronized int historyCallsToday() {
        resetIfNewDay();
        return historyCalls;
    }

    @Override
    public synchronized boolean tryUseHistoryCall() {
        resetIfNewDay();
        if (historyCalls >= HISTORY_CALLS_PER_DAY) return false;
        historyCalls++;
        return true;
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(historyCallsDate)) {
            historyCallsDate = today;
            historyCalls = 0;
        }
    }
}
