package com.joelwhissel.personalstocktracker.ui.watchlist;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.domain.StockQuote;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockAdapter extends ListAdapter<StockQuote, StockAdapter.StockViewHolder> {

    public interface OnStockClickListener {
        void onStockClick(StockQuote stock);
    }

    public interface OnStockRemoveListener {
        void onStockRemove(StockQuote stock);
    }

    private static final DiffUtil.ItemCallback<StockQuote> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull StockQuote oldItem, @NonNull StockQuote newItem) {
                    return oldItem.getTicker().equalsIgnoreCase(newItem.getTicker());
                }

                @Override
                public boolean areContentsTheSame(@NonNull StockQuote oldItem, @NonNull StockQuote newItem) {
                    return Double.compare(oldItem.getPrice(), newItem.getPrice()) == 0
                            && Double.compare(oldItem.getChange(), newItem.getChange()) == 0
                            && Double.compare(oldItem.getChangePercent(), newItem.getChangePercent()) == 0
                            && oldItem.getLastUpdated() == newItem.getLastUpdated();
                }
            };

    private final OnStockClickListener clickListener;
    private final OnStockRemoveListener removeListener;

    public StockAdapter(OnStockClickListener clickListener, OnStockRemoveListener removeListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener, removeListener);
    }

    static class StockViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTicker;
        private final TextView tvCompanyName;
        private final TextView tvPrice;
        private final TextView tvChangePercent;
        private final TextView tvChangeAmount;
        private final LinearLayout layoutChgBox;
        private final LineChart chartTimeline;
        private final ImageButton btnRemove;

        StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicker = itemView.findViewById(R.id.tv_ticker);
            tvCompanyName = itemView.findViewById(R.id.tv_company_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvChangePercent = itemView.findViewById(R.id.tv_change_percent);
            tvChangeAmount = itemView.findViewById(R.id.tv_change_amount);
            layoutChgBox = itemView.findViewById(R.id.layout_chg_box);
            chartTimeline = itemView.findViewById(R.id.chart_timeline);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        void bind(StockQuote stock, OnStockClickListener clickListener, OnStockRemoveListener removeListener) {
            tvTicker.setText(stock.getTicker());
            tvCompanyName.setText(stock.getCompanyName());
            tvPrice.setText(String.format(Locale.US, "%.2f", stock.getPrice()));

            double change = stock.getChange();
            double changePercent = stock.getChangePercent();
            boolean isPositive = change >= 0;

            String sign = isPositive ? "+" : "";
            tvChangePercent.setText(String.format(Locale.US, "%s%.2f%%", sign, changePercent));
            tvChangeAmount.setText(String.format(Locale.US, "%s%.2f", sign, change));

            if (isPositive) {
                layoutChgBox.setBackgroundResource(R.drawable.bg_pill_green);
                tvPrice.setTextColor(Color.parseColor("#4ADE80"));
            } else {
                layoutChgBox.setBackgroundResource(R.drawable.bg_pill_red);
                tvPrice.setTextColor(Color.parseColor("#F87171"));
            }

            bindChart(stock);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onStockClick(stock);
            });

            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onStockRemove(stock);
            });
        }

        private void bindChart(StockQuote stock) {
            if (chartTimeline == null) return;

            chartTimeline.setTouchEnabled(false);
            chartTimeline.setDragEnabled(false);
            chartTimeline.setScaleEnabled(false);
            chartTimeline.setPinchZoomEnabled(false);
            chartTimeline.setDrawGridBackgroundEnabled(false);
            chartTimeline.getDescription().setEnabled(false);
            chartTimeline.getLegend().setEnabled(false);
            chartTimeline.getAxisLeft().setEnabled(false);
            chartTimeline.getAxisRight().setEnabled(false);
            chartTimeline.getXAxis().setEnabled(false);

            double change = stock.getChange();
            boolean isPositive = change >= 0;
            int color = isPositive ? Color.parseColor("#4ADE80") : Color.parseColor("#F87171");

            List<Entry<Object>> entries = new ArrayList<>();
            float basePrice = (float) stock.getPreviousClose();
            int hash = stock.getTicker().hashCode();

            for (int i = 0; i < 7; i++) {
                float val = (float) (basePrice * (1.0 + Math.sin(i * 0.9 + hash % 7) * 0.02));
                if (i == 6) val = (float) stock.getPrice();
                entries.add(new Entry<>((float) i, val, null, null));
            }

            LineDataSet<Object> dataSet = new LineDataSet<>(entries, "");
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawCirclesEnabled(false);
            dataSet.setDrawValuesEnabled(false);
            dataSet.setLineWidth(1.8f);
            dataSet.setColor(color);

            LineData lineData = new LineData(dataSet);
            chartTimeline.setData(lineData);
            chartTimeline.invalidate();
        }
    }
}
