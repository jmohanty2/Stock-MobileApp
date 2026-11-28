package com.joelwhissel.personalstocktracker.worker;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import com.joelwhissel.personalstocktracker.ServiceLocator;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeAlertRepository;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeSettingsRepository;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeStockRepository;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AlertCheckWorkerTest {

    private Context context;
    private FakeStockRepository stocks;

    @Before
    public void setUp() {

        context = ApplicationProvider.getApplicationContext();

        FakeAlertRepository alerts =
                new FakeAlertRepository();

        stocks = new FakeStockRepository(alerts);

        stocks.seedSampleData();

        ServiceLocator.setRepositories(
                stocks,
                alerts,
                new FakeSettingsRepository()
        );
    }

    @After
    public void tearDown() {
        ServiceLocator.reset();
    }

    @Test
    public void successfulRefresh_returnsSuccess() {

        AlertCheckWorker worker =
                TestListenableWorkerBuilder
                        .from(context, AlertCheckWorker.class)
                        .build();

        assertEquals(
                ListenableWorker.Result.success(),
                worker.doWork()
        );
    }

    @Test
    public void networkError_returnsRetry() {

        stocks.setError(ErrorType.NETWORK_ERROR);

        AlertCheckWorker worker =
                TestListenableWorkerBuilder
                        .from(context, AlertCheckWorker.class)
                        .build();

        assertEquals(
                ListenableWorker.Result.retry(),
                worker.doWork()
        );
    }
}