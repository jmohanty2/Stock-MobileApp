package com.joelwhissel.personalstocktracker.ui.portfolio;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.joelwhissel.personalstocktracker.ServiceLocator;
import com.joelwhissel.personalstocktracker.data.repository.Callback;
import com.joelwhissel.personalstocktracker.data.repository.StockRepository;
import com.joelwhissel.personalstocktracker.domain.Holding;
import com.joelwhissel.personalstocktracker.domain.PortfolioSummary;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import java.util.List;

public class PortfolioViewModel extends AndroidViewModel {

    private final StockRepository repository;
    private final LiveData<List<Holding>> holdings;
    private final LiveData<PortfolioSummary> portfolioSummary;

    public PortfolioViewModel(@NonNull Application application) {
        super(application);
        repository = ServiceLocator.stockRepository();
        holdings = repository.observeHoldings();
        portfolioSummary = Transformations.map(holdings, PortfolioSummary::from);
    }

    public LiveData<List<Holding>> getHoldings() {
        return holdings;
    }

    public LiveData<PortfolioSummary> getPortfolioSummary() {
        return portfolioSummary;
    }

    public LiveData<ErrorType> getRefreshError() {
        return repository.observeRefreshError();
    }

    public void addHolding(String ticker, double shares, double avgBuyPrice, Callback<Holding> callback) {
        repository.addHolding(ticker, shares, avgBuyPrice, callback);
    }

    public void updateHolding(String ticker, double shares, double avgBuyPrice) {
        repository.updateHolding(ticker, shares, avgBuyPrice);
    }

    public void removeHolding(String ticker) {
        repository.removeHolding(ticker);
    }

    public void refreshNow() {
        repository.refreshNow(null);
    }

    public void startAutoRefresh() {
        repository.startAutoRefresh();
    }

    public void stopAutoRefresh() {
        repository.stopAutoRefresh();
    }
}
