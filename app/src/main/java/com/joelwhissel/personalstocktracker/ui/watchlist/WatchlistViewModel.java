package com.joelwhissel.personalstocktracker.ui.watchlist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.joelwhissel.personalstocktracker.ServiceLocator;
import com.joelwhissel.personalstocktracker.data.repository.Callback;
import com.joelwhissel.personalstocktracker.data.repository.StockRepository;
import com.joelwhissel.personalstocktracker.domain.StockQuote;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;

import java.util.List;

public class WatchlistViewModel extends AndroidViewModel {

    private final StockRepository repository;

    public WatchlistViewModel(@NonNull Application application) {
        super(application);
        repository = ServiceLocator.stockRepository();
    }

    public LiveData<List<StockQuote>> getWatchlist() {
        return repository.observeWatchlist();
    }

    public LiveData<ErrorType> getRefreshError() {
        return repository.observeRefreshError();
    }

    public void addStock(String ticker, Callback<StockQuote> callback) {
        repository.addToWatchlist(ticker, callback);
    }

    public void removeStock(String ticker) {
        repository.removeFromWatchlist(ticker);
    }

    public void undoRemove(String ticker) {
        repository.undoRemove(ticker);
    }

    public void commitRemove(String ticker) {
        repository.commitRemove(ticker);
    }

    public void countAlertsDeletedOnRemove(String ticker, Callback<Integer> callback) {
        repository.countAlertsDeletedOnRemove(ticker, callback);
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
