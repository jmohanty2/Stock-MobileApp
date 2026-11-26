package com.joelwhissel.personalstocktracker.ui.login;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.ui.watchlist.WatchlistActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        MaterialButton btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, WatchlistActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
