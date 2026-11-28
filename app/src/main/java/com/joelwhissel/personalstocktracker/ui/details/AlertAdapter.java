package com.joelwhissel.personalstocktracker.ui.details;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.domain.PriceAlert;
import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlertAdapter
        extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private final List<PriceAlert> alerts = new ArrayList<>();

    private final AlertListener listener;

    public interface AlertListener {

        void onEnabledChanged(PriceAlert alert, boolean enabled);

        void onDelete(PriceAlert alert);
    }

    public AlertAdapter(AlertListener listener) {
        this.listener = listener;
    }

    public void setAlerts(List<PriceAlert> newAlerts) {

        alerts.clear();

        if (newAlerts != null) {
            alerts.addAll(newAlerts);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);

        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AlertViewHolder holder, int position) {

        PriceAlert alert = alerts.get(position);

        holder.rule.setText(formatRule(holder, alert));

        holder.status.setVisibility(
                alert.getTriggeredAt() != null
                        ? View.VISIBLE
                        : View.GONE
        );

        // Remove the previous listener before setting the switch.
        holder.enabledSwitch.setOnCheckedChangeListener(null);

        holder.enabledSwitch.setChecked(alert.isActive());

        holder.enabledSwitch.setOnCheckedChangeListener(
                (button, isChecked) ->
                        listener.onEnabledChanged(alert, isChecked)
        );

        holder.deleteButton.setOnClickListener(
                v -> listener.onDelete(alert)
        );
    }

    private String formatRule(AlertViewHolder holder,
                              PriceAlert alert) {

        boolean rises = alert.getDirection() == Direction.UP;

        if (alert.getType() == AlertType.PERCENT) {

            NumberFormat number =
                    NumberFormat.getNumberInstance(Locale.US);

            number.setMaximumFractionDigits(4);

            return holder.itemView.getContext().getString(
                    rises
                            ? R.string.alert_rises_percent
                            : R.string.alert_falls_percent,
                    number.format(alert.getTargetValue())
            );
        }

        NumberFormat currency =
                NumberFormat.getCurrencyInstance(Locale.US);

        return holder.itemView.getContext().getString(
                rises
                        ? R.string.alert_rises_price
                        : R.string.alert_falls_price,
                currency.format(alert.getThresholdPrice())
        );
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {

        TextView rule;
        TextView status;
        MaterialSwitch enabledSwitch;
        MaterialButton deleteButton;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);

            rule = itemView.findViewById(R.id.tv_alert_rule);

            status = itemView.findViewById(R.id.tv_alert_status);

            enabledSwitch = itemView.findViewById(
                    R.id.switch_alert_enabled);

            deleteButton = itemView.findViewById(
                    R.id.btn_delete_alert);
        }
    }
}
