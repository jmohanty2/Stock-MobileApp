package com.joelwhissel.personalstocktracker.data.repository;

import com.joelwhissel.personalstocktracker.data.local.AlertDao;
import com.joelwhissel.personalstocktracker.data.local.PriceAlertEntity;
import com.joelwhissel.personalstocktracker.domain.AlertNotifier;
import com.joelwhissel.personalstocktracker.domain.PriceAlert;
import com.joelwhissel.personalstocktracker.data.local.StockDao;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AlertRepositoryImplTest {

    private AlertDao alertDao;
    private CountingNotifier notifier;
    private AlertRepositoryImpl repository;
    private StockDao stockDao;

    @Before
    public void setUp() {

        alertDao = mock(AlertDao.class);
        stockDao = mock(StockDao.class);

        notifier = new CountingNotifier();

        repository = new AlertRepositoryImpl(
                alertDao,
                stockDao,
                notifier,
                Runnable::run,
                Runnable::run
        );
    }

    private PriceAlertEntity createAlert() {

        PriceAlertEntity entity = new PriceAlertEntity();

        entity.id = 1;
        entity.ticker = "AAPL";
        entity.type = "PRICE";
        entity.direction = "UP";
        entity.targetValue = 150.0;
        entity.basePrice = 0.0;
        entity.active = true;
        entity.triggeredAt = null;

        return entity;
    }

    @Test
    public void markTriggeredReturnsZero_noNotification() {

        when(alertDao.getActiveSync())
                .thenReturn(
                        Collections.singletonList(createAlert()));

        when(alertDao.markTriggered(eq(1L), anyLong()))
                .thenReturn(0);

        int fired = repository.evaluateAndNotify(
                Map.of("AAPL", 160.0));

        assertEquals(0, fired);
        assertEquals(0, notifier.count);
    }

    @Test
    public void twoEvaluations_onlyOneNotification() {

        when(alertDao.getActiveSync())
                .thenReturn(
                        Collections.singletonList(createAlert()));

        when(alertDao.markTriggered(eq(1L), anyLong()))
                .thenReturn(1, 0);

        Map<String, Double> prices =
                Map.of("AAPL", 160.0);

        int first = repository.evaluateAndNotify(prices);
        int second = repository.evaluateAndNotify(prices);

        assertEquals(1, first);
        assertEquals(0, second);
        assertEquals(1, notifier.count);
    }

    @Test
    public void thresholdNotMet_noNotification() {

        when(alertDao.getActiveSync())
                .thenReturn(
                        Collections.singletonList(createAlert()));

        int fired = repository.evaluateAndNotify(
                Map.of("AAPL", 140.0));

        assertEquals(0, fired);
        assertEquals(0, notifier.count);

        verify(alertDao, never())
                .markTriggered(anyLong(), anyLong());
    }

    @Test
    public void missingTicker_noNotification() {

        when(alertDao.getActiveSync())
                .thenReturn(
                        Collections.singletonList(createAlert()));

        int fired = repository.evaluateAndNotify(
                Map.of("MSFT", 200.0));

        assertEquals(0, fired);
        assertEquals(0, notifier.count);
    }

    private static class CountingNotifier
            implements AlertNotifier {

        int count = 0;

        @Override
        public void showPriceAlert(
                PriceAlert alert, double price) {

            count++;
        }
    }
}