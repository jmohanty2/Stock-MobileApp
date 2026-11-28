package com.joelwhissel.personalstocktracker.ui.details;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.joelwhissel.personalstocktracker.ServiceLocator;
import com.joelwhissel.personalstocktracker.data.repository.StockRepository;
import com.joelwhissel.personalstocktracker.data.repository.Resource;
import com.joelwhissel.personalstocktracker.domain.StockQuote;
import com.joelwhissel.personalstocktracker.domain.DailyPrice;
import com.joelwhissel.personalstocktracker.domain.IndicatorCalculator;
import com.joelwhissel.personalstocktracker.domain.enums.Volatility;
import com.joelwhissel.personalstocktracker.data.repository.AlertRepository;
import com.joelwhissel.personalstocktracker.data.repository.Callback;
import com.joelwhissel.personalstocktracker.domain.PriceAlert;
import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;

import java.util.ArrayList;
import java.util.List;

public class StockDetailsViewModel extends AndroidViewModel{

    private final StockRepository repository;
    private final AlertRepository alertRepository;
    private LiveData<List<PriceAlert>> alerts;
    private LiveData<StockQuote> quote;

    private final MutableLiveData<List<DailyPrice>> history =
            new MutableLiveData<>();

    private final MutableLiveData<Indicators> indicators =
            new MutableLiveData<>();

    private String ticker;

    public StockDetailsViewModel(@NonNull Application application) {
        super(application);
        repository = ServiceLocator.stockRepository();

        alertRepository = ServiceLocator.alertRepository();
    }

    public void loadStock(String ticker) {

        if (ticker.equals(this.ticker)) {
            return;
        }

        this.ticker = ticker;

        quote = repository.observeQuote(ticker);
        alerts = alertRepository.observe(ticker);

        repository.getHistory(ticker, 40, result -> {

            if (result.status == Resource.Status.SUCCESS
                    && result.data != null
                    && !result.data.isEmpty()) {

                history.setValue(result.data);

                List<Double> closes = new ArrayList<>();

                for (DailyPrice day : result.data) {
                    closes.add(day.getClose());
                }

                Indicators values = new Indicators();

                values.fiveDayChange =
                        IndicatorCalculator.percentChange(closes, 5);

                values.thirtyDayChange =
                        IndicatorCalculator.percentChange(closes, 30);

                values.sma20 =
                        IndicatorCalculator.sma(closes, 20);

                values.aboveSma =
                        IndicatorCalculator.isAboveSma(closes, 20);

                double[] range =
                        IndicatorCalculator.highLow(closes, 30);

                values.high = range[0];
                values.low = range[1];

                values.volatility =
                        IndicatorCalculator.volatility(closes);

                values.asOfDate = result.data
                        .get(result.data.size() - 1)
                        .getDate();

                if (Double.isNaN(values.fiveDayChange)
                        || Double.isNaN(values.thirtyDayChange)
                        || Double.isNaN(values.sma20)
                        || Double.isNaN(values.high)
                        || Double.isNaN(values.low)
                        || values.volatility == null) {

                    indicators.setValue(null);

                } else {
                    indicators.setValue(values);
                }

            } else if (result.status == Resource.Status.ERROR
                    || result.status == Resource.Status.SUCCESS) {

                history.setValue(new ArrayList<>());
                indicators.setValue(null);
            }
        });
    }

    public LiveData<StockQuote> getQuote() {
        return quote;
    }

    public LiveData<List<DailyPrice>> getHistory() {
        return history;
    }

    public LiveData<Indicators> getIndicators() {
        return indicators;
    }

    public LiveData<List<PriceAlert>> getAlerts() {
        return alerts;
    }

    public double getCurrentPrice() {

        StockQuote currentQuote =
                quote == null ? null : quote.getValue();

        return currentQuote == null
                ? 0.0
                : currentQuote.getPrice();
    }

    public void saveAlert(AlertType type,
                          Direction direction,
                          double value,
                          double currentPrice,
                          Callback<PriceAlert> callback) {

        double basePrice = type == AlertType.PERCENT
                ? currentPrice
                : 0.0;

        PriceAlert alert = new PriceAlert(
                0,
                null,
                ticker,
                type,
                direction,
                value,
                basePrice,
                true
        );

        alertRepository.insert(alert, callback);
    }

    public void updateAlert(PriceAlert alert, boolean enabled) {

        PriceAlert updatedAlert = new PriceAlert(
                alert.getId(),
                alert.getTriggeredAt(),
                alert.getTicker(),
                alert.getType(),
                alert.getDirection(),
                alert.getTargetValue(),
                alert.getBasePrice(),
                enabled
        );

        alertRepository.update(updatedAlert);
    }

    public void deleteAlert(PriceAlert alert) {
        alertRepository.delete(alert);
    }

    public void rearmAlert(PriceAlert alert) {
        alertRepository.rearm(alert, getCurrentPrice());
    }
    public static class Indicators {

        public double fiveDayChange;
        public double thirtyDayChange;
        public double sma20;
        public boolean aboveSma;
        public double high;
        public double low;
        public Volatility volatility;
        public String asOfDate;
    }

}
