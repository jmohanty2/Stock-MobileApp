package com.joelwhissel.personalstocktracker.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.joelwhissel.personalstocktracker.ServiceLocator;
import com.joelwhissel.personalstocktracker.data.repository.Resource;
import com.joelwhissel.personalstocktracker.data.repository.StockRepository;
import com.joelwhissel.personalstocktracker.domain.StockQuote;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import java.util.List;

public class AlertCheckWorker extends Worker {

    public AlertCheckWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        StockRepository repository =
                ServiceLocator.stockRepository();

        List<String> tickers =
                repository.getTrackedTickersSync();

        if (tickers.isEmpty()) {
            return Result.success();
        }

        Resource<List<StockQuote>> response =
                repository.refreshQuotesSync(tickers);

        if (response.status == Resource.Status.ERROR
                && response.error == ErrorType.NETWORK_ERROR) {

            return Result.retry();
        }

        return Result.success();
    }
}