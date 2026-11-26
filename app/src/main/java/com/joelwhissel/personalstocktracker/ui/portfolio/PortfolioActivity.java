package com.joelwhissel.personalstocktracker.ui.portfolio;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.data.repository.Resource;
import com.joelwhissel.personalstocktracker.domain.Holding;
import com.joelwhissel.personalstocktracker.domain.InputValidator;
import com.joelwhissel.personalstocktracker.domain.PortfolioSummary;
import com.joelwhissel.personalstocktracker.domain.ValidationResult;
import com.joelwhissel.personalstocktracker.domain.enums.ErrorType;
import com.joelwhissel.personalstocktracker.ui.ErrorMessages;
import com.joelwhissel.personalstocktracker.ui.details.StockDetailsActivity;
import com.joelwhissel.personalstocktracker.ui.profile.ProfileActivity;
import com.joelwhissel.personalstocktracker.ui.watchlist.WatchlistActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PortfolioActivity extends AppCompatActivity {

    private PortfolioViewModel viewModel;
    private HoldingAdapter adapter;

    private TextView tvTotalValue;
    private TextView tvTodayGainSummary;
    private TextView tvTotalGainSummary;

    private LineChart chartPortfolio;

    private TextInputLayout layoutTickerInput;
    private TextInputLayout layoutSharesInput;
    private TextInputLayout layoutPriceInput;
    private TextInputEditText etTicker;
    private TextInputEditText etShares;
    private TextInputEditText etPrice;
    private MaterialButton btnAddHolding;

    private MaterialCardView cardErrorBanner;
    private TextView tvBannerError;
    private MaterialButton btnRetry;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvPortfolio;
    private BottomNavigationView bottomNavigation;

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_portfolio);

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
        tvTotalValue = findViewById(R.id.tv_total_value);
        tvTodayGainSummary = findViewById(R.id.tv_today_gain_summary);
        tvTotalGainSummary = findViewById(R.id.tv_total_gain_summary);

        chartPortfolio = findViewById(R.id.chart_portfolio);
        setupChart();

        layoutTickerInput = findViewById(R.id.layout_ticker_input);
        layoutSharesInput = findViewById(R.id.layout_shares_input);
        layoutPriceInput = findViewById(R.id.layout_price_input);
        etTicker = findViewById(R.id.et_ticker);
        etShares = findViewById(R.id.et_shares);
        etPrice = findViewById(R.id.et_price);
        btnAddHolding = findViewById(R.id.btn_add_holding);

        cardErrorBanner = findViewById(R.id.card_error_banner);
        tvBannerError = findViewById(R.id.tv_banner_error);
        btnRetry = findViewById(R.id.btn_retry);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        rvPortfolio = findViewById(R.id.rv_portfolio);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupChart() {
        if (chartPortfolio == null) return;
        chartPortfolio.setTouchEnabled(false);
        chartPortfolio.setDragEnabled(false);
        chartPortfolio.setScaleEnabled(false);
        chartPortfolio.setPinchZoomEnabled(false);
        chartPortfolio.setDrawGridBackgroundEnabled(false);
        chartPortfolio.getDescription().setEnabled(false);
        chartPortfolio.getLegend().setEnabled(false);
        chartPortfolio.getAxisLeft().setEnabled(false);
        chartPortfolio.getAxisRight().setEnabled(false);
        chartPortfolio.getXAxis().setEnabled(false);
    }

    private void updateChartData(List<Holding> holdings) {
        if (chartPortfolio == null) return;

        List<Entry<Object>> entries = new ArrayList<>();
        if (holdings == null || holdings.isEmpty()) {
            entries.add(new Entry<>(0f, 100f, null, null));
            entries.add(new Entry<>(1f, 102f, null, null));
            entries.add(new Entry<>(2f, 99f, null, null));
            entries.add(new Entry<>(3f, 105f, null, null));
            entries.add(new Entry<>(4f, 108f, null, null));
            entries.add(new Entry<>(5f, 112f, null, null));
        } else {
            for (int i = 0; i < 8; i++) {
                float total = 0f;
                for (Holding h : holdings) {
                    double val = h.marketValue();
                    total += (float) (val * (1.0 + Math.sin(i * 0.8 + h.getTicker().hashCode() % 5) * 0.03));
                }
                if (total == 0) total = 100f + i * 5f;
                entries.add(new Entry<>((float) i, total, null, null));
            }
        }

        LineDataSet<Object> dataSet = new LineDataSet<>(entries, "Portfolio");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawCirclesEnabled(false);
        dataSet.setDrawValuesEnabled(false);
        dataSet.setLineWidth(2.5f);
        dataSet.setColor(Color.parseColor("#FBBF24"));
        dataSet.setDrawFilledEnabled(true);
        dataSet.setFillColor(Color.parseColor("#30FBBF24"));

        LineData lineData = new LineData(dataSet);
        chartPortfolio.setData(lineData);
        chartPortfolio.invalidate();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PortfolioViewModel.class);

        viewModel.getHoldings().observe(this, list -> {
            swipeRefresh.setRefreshing(false);
            adapter.submitList(list);
            updateChartData(list);
        });

        viewModel.getPortfolioSummary().observe(this, this::updateSummaryHeader);

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

    private void updateSummaryHeader(PortfolioSummary summary) {
        if (summary == null) return;

        tvTotalValue.setText(currencyFormatter.format(summary.getTotalValue()));

        double todayGain = summary.getTodayGain();
        double todayGainPercent = summary.getTodayGainPercent();
        String todaySign = todayGain > 0 ? "+" : "";
        tvTodayGainSummary.setText(String.format(Locale.US, "Today: %s%s (%s%.2f%%)",
                todaySign, currencyFormatter.format(todayGain), todaySign, todayGainPercent));
        tvTodayGainSummary.setTextColor(ContextCompat.getColor(this, getGainColor(todayGain)));

        double totalGain = summary.getTotalGain();
        double totalGainPercent = summary.getTotalGainPercent();
        String totalSign = totalGain > 0 ? "+" : "";
        tvTotalGainSummary.setText(String.format(Locale.US, "Total: %s%s (%s%.2f%%)",
                totalSign, currencyFormatter.format(totalGain), totalSign, totalGainPercent));
        tvTotalGainSummary.setTextColor(ContextCompat.getColor(this, getGainColor(totalGain)));
    }

    private int getGainColor(double val) {
        if (val > 0) return R.color.gain;
        if (val < 0) return R.color.loss;
        return R.color.flat;
    }

    private void setupRecyclerView() {
        adapter = new HoldingAdapter(
                holding -> {
                    Intent intent = new Intent(PortfolioActivity.this, StockDetailsActivity.class);
                    intent.putExtra(WatchlistActivity.EXTRA_TICKER, holding.getTicker());
                    startActivity(intent);
                },
                this::showEditSharesDialog,
                this::confirmAndRemoveHolding
        );
        rvPortfolio.setLayoutManager(new LinearLayoutManager(this));
        rvPortfolio.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAddHolding.setOnClickListener(v -> handleAddHolding());

        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layoutTickerInput.setError(null);
                layoutSharesInput.setError(null);
                layoutPriceInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etTicker.addTextChangedListener(clearErrorWatcher);
        etShares.addTextChangedListener(clearErrorWatcher);
        etPrice.addTextChangedListener(clearErrorWatcher);

        btnRetry.setOnClickListener(v -> viewModel.refreshNow());
        swipeRefresh.setOnRefreshListener(() -> viewModel.refreshNow());
    }

    private void handleAddHolding() {
        String ticker = etTicker.getText() != null ? etTicker.getText().toString().trim().toUpperCase(Locale.US) : "";
        String sharesStr = etShares.getText() != null ? etShares.getText().toString() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString() : "";

        ValidationResult sharesVal = InputValidator.validateShares(sharesStr);
        ValidationResult priceVal = InputValidator.validateBuyPrice(priceStr);

        boolean valid = true;
        if (ticker.isEmpty()) {
            layoutTickerInput.setError(getString(R.string.error_invalid_value));
            valid = false;
        }
        if (!sharesVal.isValid()) {
            layoutSharesInput.setError(getString(R.string.error_invalid_value));
            valid = false;
        }
        if (!priceVal.isValid()) {
            layoutPriceInput.setError(getString(R.string.error_invalid_value));
            valid = false;
        }

        if (!valid) return;

        viewModel.addHolding(ticker, sharesVal.getValue(), priceVal.getValue(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                etTicker.setText("");
                etShares.setText("");
                etPrice.setText("");
            } else if (resource.error != null) {
                layoutTickerInput.setError(getString(ErrorMessages.forError(resource.error, true)));
            }
        });
    }

    private void showEditSharesDialog(Holding holding) {
        EditSharesDialog dialog = EditSharesDialog.newInstance(
                holding.getTicker(), holding.getShares(), holding.getAvgBuyPrice());
        dialog.setOnSaveListener((shares, buyPrice) ->
                viewModel.updateHolding(holding.getTicker(), shares, buyPrice));
        dialog.show(getSupportFragmentManager(), "EditSharesDialog");
    }

    private void confirmAndRemoveHolding(Holding holding) {
        String message = getString(R.string.confirm_remove_holding, holding.getTicker());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_remove)
                .setMessage(message)
                .setPositiveButton(R.string.action_remove, (d, which) ->
                        viewModel.removeHolding(holding.getTicker()))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_portfolio);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_portfolio) {
                return true;
            } else if (itemId == R.id.nav_watchlist) {
                Intent intent = new Intent(this, WatchlistActivity.class);
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
        bottomNavigation.setSelectedItemId(R.id.nav_portfolio);
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
