package com.joelwhissel.personalstocktracker.ui.portfolio;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.domain.InputValidator;
import com.joelwhissel.personalstocktracker.domain.ValidationResult;

public class EditSharesDialog extends DialogFragment {

    public interface OnSaveListener {
        void onSave(double shares, double buyPrice);
    }

    private static final String ARG_TICKER = "ARG_TICKER";
    private static final String ARG_SHARES = "ARG_SHARES";
    private static final String ARG_BUY_PRICE = "ARG_BUY_PRICE";

    private OnSaveListener saveListener;

    public static EditSharesDialog newInstance(String ticker, double shares, double buyPrice) {
        EditSharesDialog fragment = new EditSharesDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TICKER, ticker);
        args.putDouble(ARG_SHARES, shares);
        args.putDouble(ARG_BUY_PRICE, buyPrice);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String ticker = args != null ? args.getString(ARG_TICKER, "") : "";
        double initialShares = args != null ? args.getDouble(ARG_SHARES, 0.0) : 0.0;
        double initialPrice = args != null ? args.getDouble(ARG_BUY_PRICE, 0.0) : 0.0;

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_shares, null);

        TextInputLayout layoutShares = view.findViewById(R.id.layout_shares_input);
        TextInputLayout layoutPrice = view.findViewById(R.id.layout_buy_price_input);
        TextInputEditText etShares = view.findViewById(R.id.et_shares);
        TextInputEditText etPrice = view.findViewById(R.id.et_buy_price);

        etShares.setText(String.valueOf(initialShares));
        etPrice.setText(String.valueOf(initialPrice));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.action_edit_shares) + " - " + ticker)
                .setView(view)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String sharesStr = etShares.getText() != null ? etShares.getText().toString() : "";
                String priceStr = etPrice.getText() != null ? etPrice.getText().toString() : "";

                ValidationResult sharesVal = InputValidator.validateShares(sharesStr);
                ValidationResult priceVal = InputValidator.validateBuyPrice(priceStr);

                layoutShares.setError(sharesVal.isValid() ? null : getString(R.string.error_invalid_value));
                layoutPrice.setError(priceVal.isValid() ? null : getString(R.string.error_invalid_value));

                if (sharesVal.isValid() && priceVal.isValid()) {
                    if (saveListener != null) {
                        saveListener.onSave(sharesVal.getValue(), priceVal.getValue());
                    }
                    dismiss();
                }
            });
        });

        return dialog;
    }
}
