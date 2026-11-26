package com.joelwhissel.personalstocktracker.ui.portfolio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.domain.Holding;

import java.text.NumberFormat;
import java.util.Locale;

public class HoldingAdapter extends ListAdapter<Holding, HoldingAdapter.HoldingViewHolder> {

    public interface OnHoldingClickListener {
        void onHoldingClick(Holding holding);
    }

    public interface OnEditSharesListener {
        void onEditShares(Holding holding);
    }

    public interface OnRemoveHoldingListener {
        void onRemoveHolding(Holding holding);
    }

    private static final DiffUtil.ItemCallback<Holding> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Holding oldItem, @NonNull Holding newItem) {
                    return oldItem.getTicker().equalsIgnoreCase(newItem.getTicker());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Holding oldItem, @NonNull Holding newItem) {
                    return Double.compare(oldItem.getShares(), newItem.getShares()) == 0
                            && Double.compare(oldItem.getAvgBuyPrice(), newItem.getAvgBuyPrice()) == 0
                            && oldItem.marketValue() == newItem.marketValue();
                }
            };

    private final OnHoldingClickListener clickListener;
    private final OnEditSharesListener editListener;
    private final OnRemoveHoldingListener removeListener;

    public HoldingAdapter(OnHoldingClickListener clickListener,
                          OnEditSharesListener editListener,
                          OnRemoveHoldingListener removeListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.editListener = editListener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public HoldingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_holding, parent, false);
        return new HoldingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HoldingViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener, editListener, removeListener);
    }

    static class HoldingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTicker;
        private final TextView tvSharesInfo;
        private final TextView tvMarketValue;
        private final TextView tvTodayGain;
        private final TextView tvTotalGain;
        private final Button btnEditShares;
        private final Button btnRemove;
        private final NumberFormat currencyFormatter;

        HoldingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicker = itemView.findViewById(R.id.tv_ticker);
            tvSharesInfo = itemView.findViewById(R.id.tv_shares_info);
            tvMarketValue = itemView.findViewById(R.id.tv_market_value);
            tvTodayGain = itemView.findViewById(R.id.tv_today_gain);
            tvTotalGain = itemView.findViewById(R.id.tv_total_gain);
            btnEditShares = itemView.findViewById(R.id.btn_edit_shares);
            btnRemove = itemView.findViewById(R.id.btn_remove);

            currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        }

        void bind(Holding holding, OnHoldingClickListener clickListener,
                  OnEditSharesListener editListener, OnRemoveHoldingListener removeListener) {
            tvTicker.setText(holding.getTicker());

            String sharesStr = formatShares(holding.getShares());
            tvSharesInfo.setText(String.format(Locale.US, "%s shares @ %s",
                    sharesStr, currencyFormatter.format(holding.getAvgBuyPrice())));

            tvMarketValue.setText(currencyFormatter.format(holding.marketValue()));

            Context context = itemView.getContext();

            double todayGain = holding.todayGain();
            double todayGainPercent = holding.todayGainPercent();
            String todaySign = todayGain > 0 ? "+" : "";
            tvTodayGain.setText(String.format(Locale.US, "Today: %s%s (%s%.2f%%)",
                    todaySign, currencyFormatter.format(todayGain), todaySign, todayGainPercent));
            tvTodayGain.setTextColor(ContextCompat.getColor(context, getGainColor(todayGain)));

            double totalGain = holding.totalGain();
            double totalGainPercent = holding.totalGainPercent();
            String totalSign = totalGain > 0 ? "+" : "";
            tvTotalGain.setText(String.format(Locale.US, "Total: %s%s (%s%.2f%%)",
                    totalSign, currencyFormatter.format(totalGain), totalSign, totalGainPercent));
            tvTotalGain.setTextColor(ContextCompat.getColor(context, getGainColor(totalGain)));

            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onHoldingClick(holding);
            });

            btnEditShares.setOnClickListener(v -> {
                if (editListener != null) editListener.onEditShares(holding);
            });

            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onRemoveHolding(holding);
            });
        }

        private static int getGainColor(double val) {
            if (val > 0) return R.color.gain;
            if (val < 0) return R.color.loss;
            return R.color.flat;
        }

        private static String formatShares(double shares) {
            if (shares == (long) shares) {
                return String.format(Locale.US, "%d", (long) shares);
            }
            return String.format(Locale.US, "%.4f", shares).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }
}
