package com.joelwhissel.personalstocktracker.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.database.sqlite.SQLiteConstraintException;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.joelwhissel.personalstocktracker.data.local.AlertDao;
import com.joelwhissel.personalstocktracker.data.local.StockDao;
import com.joelwhissel.personalstocktracker.data.local.StockEntity;
import com.joelwhissel.personalstocktracker.data.local.PriceAlertEntity;

import com.joelwhissel.personalstocktracker.domain.AlertEvaluator;
import com.joelwhissel.personalstocktracker.domain.AlertNotifier;
import com.joelwhissel.personalstocktracker.domain.PriceAlert;

import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.Executor;

public class AlertRepositoryImpl implements AlertRepository {

    private final AlertDao alertDao;
    private final StockDao stockDao;
    private final AlertNotifier notifier;

    private final AlertEvaluator evaluator =
            new AlertEvaluator();

    private final Executor executor;
    private final Executor callbackExecutor;

    public AlertRepositoryImpl(
            AlertDao alertDao,
            StockDao stockDao,
            AlertNotifier notifier,
            Executor executor) {

        this(alertDao, stockDao, notifier, executor,
                new Handler(Looper.getMainLooper())::post);
    }

    // Allows direct executors to be used in unit tests.
    public AlertRepositoryImpl(
            AlertDao alertDao,
            StockDao stockDao,
            AlertNotifier notifier,
            Executor executor,
            Executor callbackExecutor) {

        this.alertDao = alertDao;
        this.stockDao = stockDao;
        this.notifier = notifier;
        this.executor = executor;
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    public LiveData<List<PriceAlert>> observe(String ticker) {

        return Transformations.map(
                alertDao.observeForTicker(normalize(ticker)),
                entities -> {

                    List<PriceAlert> alerts = new ArrayList<>();

                    if (entities != null) {
                        for (PriceAlertEntity entity : entities) {
                            alerts.add(toDomain(entity));
                        }
                    }

                    return alerts;
                });
    }

    @Override
    public void insert(PriceAlert alert,
                       Callback<PriceAlert> callback) {

        executor.execute(() -> {

            String ticker = normalize(alert.getTicker());

            boolean tracked = false;

            for (StockEntity stock : stockDao.getTracked()) {
                if (ticker.equals(stock.ticker)) {
                    tracked = true;
                    break;
                }
            }

            if (!tracked) {

                deliver(callback,
                        Resource.error(ErrorType.STOCK_NOT_FOUND));

                return;
            }

            PriceAlertEntity entity = toEntity(alert);

            entity.id = 0;
            entity.ticker = ticker;
            entity.triggeredAt = null;
            entity.createdAt = System.currentTimeMillis();

            try {
                entity.id = alertDao.insert(entity);

                deliver(callback,
                        Resource.success(toDomain(entity)));

            } catch (SQLiteConstraintException e) {

                deliver(callback,
                        Resource.error(ErrorType.STOCK_NOT_FOUND));
            }
        });
    }

    @Override
    public void update(PriceAlert alert) {

        executor.execute(() -> {

            alertDao.updateAlertState(
                    alert.getId(),
                    alert.isActive()
            );
        });
    }

    @Override
    public void delete(PriceAlert alert) {

        executor.execute(() ->
                alertDao.delete(toEntity(alert)));
    }

    @Override
    public void rearm(PriceAlert alert, double basePrice) {

        executor.execute(() -> {

            double newBasePrice =
                    alert.getType() == AlertType.PERCENT
                            ? basePrice
                            : alert.getBasePrice();

            if (alert.getType() == AlertType.PERCENT
                    && (!Double.isFinite(newBasePrice)
                    || newBasePrice <= 0)) {
                return;
            }

            alertDao.rearmAlert(
                    alert.getId(),
                    newBasePrice
            );
        });
    }

    @Override
    public List<String> getActiveTickersSync() {

        Set<String> tickers = new LinkedHashSet<>();

        for (PriceAlertEntity entity : alertDao.getActiveSync()) {
            tickers.add(entity.ticker);
        }

        return new ArrayList<>(tickers);
    }

    @Override
    public int evaluateAndNotify(Map<String, Double> prices) {

        int firedCount = 0;

        for (PriceAlertEntity entity : alertDao.getActiveSync()) {

            Double price = prices.get(entity.ticker);

            if (price == null
                    || !Double.isFinite(price)
                    || price <= 0) {
                continue;
            }

            PriceAlert alert = toDomain(entity);

            if (!evaluator.isMet(alert, price)) {
                continue;
            }

            long now = System.currentTimeMillis();

            int updated = alertDao.markTriggered(
                    entity.id, now);

            if (updated == 1) {

                firedCount++;

                entity.active = false;
                entity.triggeredAt = now;

                if (notifier != null) {
                    notifier.showPriceAlert(
                            toDomain(entity), price);
                }
            }
        }

        return firedCount;
    }

    private void deliver(
            Callback<PriceAlert> callback,
            Resource<PriceAlert> result) {

        if (callback != null) {

            callbackExecutor.execute(() ->
                    callback.onResult(result));
        }
    }

    private PriceAlert toDomain(PriceAlertEntity entity) {

        return new PriceAlert(
                entity.id,
                entity.triggeredAt,
                entity.ticker,
                AlertType.valueOf(entity.type),
                Direction.valueOf(entity.direction),
                entity.targetValue,
                entity.basePrice,
                entity.active
        );
    }

    private PriceAlertEntity toEntity(PriceAlert alert) {

        PriceAlertEntity entity = new PriceAlertEntity();

        entity.id = alert.getId();
        entity.ticker = alert.getTicker();
        entity.type = alert.getType().name();
        entity.direction = alert.getDirection().name();
        entity.targetValue = alert.getTargetValue();
        entity.basePrice = alert.getBasePrice();
        entity.active = alert.isActive();
        entity.triggeredAt = alert.getTriggeredAt();

        return entity;
    }

    private String normalize(String ticker) {

        return ticker == null
                ? ""
                : ticker.trim().toUpperCase(Locale.US);
    }
}