package com.joelwhissel.personalstocktracker.ui.watchlist;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.data.repository.Resource;
import com.joelwhissel.personalstocktracker.domain.StockQuote;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;
import com.joelwhissel.personalstocktracker.ui.ErrorMessages;
import com.joelwhissel.personalstocktracker.ui.details.StockDetailsActivity;
import com.joelwhissel.personalstocktracker.ui.portfolio.PortfolioActivity;
import com.joelwhissel.personalstocktracker.ui.profile.ProfileActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WatchlistActivity extends AppCompatActivity {

    public static final String EXTRA_TICKER = "EXTRA_TICKER";

    private WatchlistViewModel viewModel;
    private StockAdapter adapter;

    private TextInputLayout layoutTickerInput;
    private TextInputEditText etTicker;
    private MaterialButton btnAdd;
    private MaterialCardView cardErrorBanner;
    private TextView tvBannerError;
    private MaterialButton btnRetry;
    private TextView tvLastUpdated;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvWatchlist;
    private TextView tvEmptyState;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_watchlist);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupViewModel();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_reminder_settings) {
                // Phase 2: Show Reminder Settings Dialog
                return true;
            }
            return false;
        });

        layoutTickerInput = findViewById(R.id.layout_ticker_input);
        etTicker = findViewById(R.id.et_ticker);
        btnAdd = findViewById(R.id.btn_add);
        cardErrorBanner = findViewById(R.id.card_error_banner);
        tvBannerError = findViewById(R.id.tv_banner_error);
        btnRetry = findViewById(R.id.btn_retry);
        tvLastUpdated = findViewById(R.id.tv_last_updated);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        rvWatchlist = findViewById(R.id.rv_watchlist);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(WatchlistViewModel.class);

        viewModel.getWatchlist().observe(this, list -> {
            swipeRefresh.setRefreshing(false);
            if (list == null || list.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                rvWatchlist.setVisibility(View.GONE);
                tvLastUpdated.setVisibility(View.GONE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
                rvWatchlist.setVisibility(View.VISIBLE);
                tvLastUpdated.setVisibility(View.VISIBLE);
                adapter.submitList(list);

                String formattedTime = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                tvLastUpdated.setText(getString(R.string.last_updated, formattedTime));
            }
        });

        viewModel.getRefreshError().observe(this, error -> {
            swipeRefresh.setRefreshing(false);
            if (error == ErrorType.NETWORK_ERROR) {
                tvBannerError.setText(getString(R.string.error_network));
                cardErrorBanner.setVisibility(View.VISIBLE);
            } else {
                cardErrorBanner.setVisibility(View.GONE);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new StockAdapter(
                stock -> {
                    Intent intent = new Intent(WatchlistActivity.this, StockDetailsActivity.class);
                    intent.putExtra(EXTRA_TICKER, stock.getTicker());
                    startActivity(intent);
                },
                this::confirmAndRemoveStock
        );
        rvWatchlist.setLayoutManager(new LinearLayoutManager(this));
        rvWatchlist.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    StockQuote stock = adapter.getCurrentList().get(position);
                    confirmAndRemoveStock(stock);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvWatchlist);
    }

    private void setupListeners() {
        btnAdd.setOnClickListener(v -> handleAddStock());

        etTicker.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layoutTickerInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnRetry.setOnClickListener(v -> viewModel.refreshNow());
        swipeRefresh.setOnRefreshListener(() -> viewModel.refreshNow());
    }

    private void handleAddStock() {
        String ticker = etTicker.getText() != null ? etTicker.getText().toString().trim().toUpperCase(Locale.US) : "";
        if (ticker.isEmpty()) return;

        layoutTickerInput.setError(null);
        viewModel.addStock(ticker, resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                etTicker.setText("");
            } else if (resource.error != null) {
                layoutTickerInput.setError(getString(ErrorMessages.forError(resource.error, false)));
            }
        });
    }

    private void confirmAndRemoveStock(StockQuote stock) {
        String ticker = stock.getTicker();
        viewModel.countAlertsDeletedOnRemove(ticker, resource -> {
            int alertsDeleted = resource.data != null ? resource.data : 0;
            String message = alertsDeleted > 0
                    ? getString(R.string.undo_alerts_deleted, ticker)
                    : getString(R.string.undo_removed, ticker);

            viewModel.removeStock(ticker);

            Snackbar snackbar = Snackbar.make(rvWatchlist, message, 4000);
            snackbar.setAction(R.string.action_undo, v -> viewModel.undoRemove(ticker));
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    if (event != DISMISS_EVENT_ACTION) {
                        viewModel.commitRemove(ticker);
                    }
                }
            });
            snackbar.show();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_watchlist);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_watchlist) {
                return true;
            } else if (itemId == R.id.nav_portfolio) {
                Intent intent = new Intent(this, PortfolioActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_watchlist);
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.startAutoRefresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.stopAutoRefresh();
    }
}
