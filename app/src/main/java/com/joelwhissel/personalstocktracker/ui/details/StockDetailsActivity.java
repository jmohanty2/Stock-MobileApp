package com.joelwhissel.personalstocktracker.ui.details;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.ui.watchlist.WatchlistActivity;

public class StockDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stock_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView tvStockTicker = findViewById(R.id.tv_stock_ticker);

        String ticker = getIntent().getStringExtra(WatchlistActivity.EXTRA_TICKER);
        if (ticker != null && !ticker.isEmpty()) {
            toolbar.setTitle(ticker);
            if (tvStockTicker != null) {
                tvStockTicker.setText(ticker);
            }
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }
}
