package com.joelwhissel.personalstocktracker.ui.details;

import android.view.View;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.google.android.material.appbar.MaterialToolbar;

import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.domain.StockQuote;
import com.joelwhissel.personalstocktracker.domain.DailyPrice;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class StockDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_TICKER = "EXTRA_TICKER";

    private StockDetailsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stock_details);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom);

                    return insets;
                });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        TextView tvStockTicker =
                findViewById(R.id.tv_stock_ticker);

        // Receive ticker from Watchlist or Portfolio.
        String ticker = getIntent().getStringExtra(EXTRA_TICKER);

        if (ticker == null || ticker.trim().isEmpty()) {
            finish();
            return;
        }

        toolbar.setTitle(ticker);
        tvStockTicker.setText(ticker);

        // Back button.
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize ViewModel.
        viewModel = new ViewModelProvider(this)
                .get(StockDetailsViewModel.class);

        // Load stock information.
        viewModel.loadStock(ticker);

        // Observe changes to the stock quote.
        viewModel.getQuote().observe(this, quote -> {
            if (quote != null) {
                displayQuote(quote);
            }
        });

        // Observe calculated indicators.
        viewModel.getIndicators().observe(this,
                this::displayIndicators);

        // Observe price history.
        viewModel.getHistory().observe(this,
                this::displayChart);
    }

    private void displayQuote(StockQuote quote) {

        NumberFormat currency =
                NumberFormat.getCurrencyInstance(Locale.US);

        setText(R.id.tv_stock_ticker,
                quote.getTicker());

        setText(R.id.tv_company_name,
                quote.getCompanyName());

        setText(R.id.tv_stock_price,
                currency.format(quote.getPrice()));

        setText(R.id.tv_open,
                currency.format(quote.getOpen()));

        setText(R.id.tv_day_high,
                currency.format(quote.getDayHigh()));

        setText(R.id.tv_day_low,
                currency.format(quote.getDayLow()));

        setText(R.id.tv_previous_close,
                currency.format(quote.getPreviousClose()));

        // Determine price movement.
        String arrow;

        if (quote.getChange() > 0) {
            arrow = "▲";
        } else if (quote.getChange() < 0) {
            arrow = "▼";
        } else {
            arrow = "—";
        }

        // Display daily price change.
        setText(R.id.tv_stock_change,
                String.format(Locale.US,
                        "%s %+.2f USD (%+.2f%%)",
                        arrow,
                        quote.getChange(),
                        quote.getChangePercent()));

        TextView change =
                findViewById(R.id.tv_stock_change);

        // Set gain, loss or neutral color.
        int color;

        if (quote.getChange() > 0) {
            color = R.color.gain;
        } else if (quote.getChange() < 0) {
            color = R.color.loss;
        } else {
            color = R.color.flat;
        }

        change.setTextColor(
                ContextCompat.getColor(this, color));
    }

    private void displayIndicators(
            StockDetailsViewModel.Indicators values) {

        TextView unavailable =
                findViewById(R.id.tv_indicators_unavailable);

        if (values == null) {
            unavailable.setVisibility(View.VISIBLE);
            return;
        }

        unavailable.setVisibility(View.GONE);

        NumberFormat currency =
                NumberFormat.getCurrencyInstance(Locale.US);

        // 5-day change.
        setText(R.id.tv_five_day_change,
                String.format(Locale.US,
                        "%+.2f%%", values.fiveDayChange));

        setText(R.id.tv_five_day_description,
                describeChange(values.fiveDayChange, 5));

        // 30-day change.
        setText(R.id.tv_thirty_day_change,
                String.format(Locale.US,
                        "%+.2f%%", values.thirtyDayChange));

        setText(R.id.tv_thirty_day_description,
                describeChange(values.thirtyDayChange, 30));

        // 20-day moving average.
        setText(R.id.tv_sma20,
                currency.format(values.sma20));

        setText(R.id.tv_sma_description,
                getString(values.aboveSma
                        ? R.string.details_above_sma
                        : R.string.details_not_above_sma));

        // 30-day range.
        setText(R.id.tv_range,
                currency.format(values.low)
                        + " - "
                        + currency.format(values.high));

        setText(R.id.tv_range_description,
                getString(R.string.details_range_description,
                        currency.format(values.low),
                        currency.format(values.high)));

        // Volatility.
        switch (values.volatility) {
            case LOW:
                setText(R.id.tv_volatility,
                        getString(R.string.volatility_low));

                setText(R.id.tv_volatility_description,
                        getString(R.string.volatility_low_description));
                break;

            case MEDIUM:
                setText(R.id.tv_volatility,
                        getString(R.string.volatility_medium));

                setText(R.id.tv_volatility_description,
                        getString(R.string.volatility_medium_description));
                break;

            case HIGH:
                setText(R.id.tv_volatility,
                        getString(R.string.volatility_high));

                setText(R.id.tv_volatility_description,
                        getString(R.string.volatility_high_description));
                break;
        }

        // History date.
        setText(R.id.tv_history_as_of,
                getString(R.string.history_as_of,
                        values.asOfDate));
    }

    private String describeChange(double change, int days) {

        if (change > 0) {
            return getString(R.string.details_change_up,
                    change, days);

        } else if (change < 0) {
            return getString(R.string.details_change_down,
                    Math.abs(change), days);

        } else {
            return getString(R.string.details_change_flat,
                    days);
        }
    }

    private void displayChart(List<DailyPrice> history) {

        LineChart chart = findViewById(R.id.price_chart);

        if (history == null || history.isEmpty()) {
            chart.clear();
            chart.setNoDataText(
                    getString(R.string.indicators_unavailable));
            chart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();

        int start = Math.max(0, history.size() - 30);

        for (int i = start; i < history.size(); i++) {

            entries.add(new Entry(i - start, (float) history.get(i).getClose(), null, null));
        }

        LineDataSet dataSet = new LineDataSet(
                entries, getString(R.string.details_chart));

        int lineColor = ContextCompat.getColor(
                this, R.color.cyan_accent);

        int textColor = ContextCompat.getColor(
                this, R.color.text_secondary);

        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCirclesEnabled(false);
        dataSet.setDrawCirclesEnabled(false);

        chart.setData(new LineData(dataSet));

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        chart.getAxisLeft().setTextColor(textColor);
        chart.getXAxis().setTextColor(textColor);

        chart.getXAxis().setPosition(
                com.github.mikephil.charting.components
                        .XAxis.XAxisPosition.BOTTOM);

        chart.invalidate();
    }

    // Helper method for updating TextViews.
    private void setText(int id, String value) {
        TextView textView = findViewById(id);
        textView.setText(value);
    }
}
