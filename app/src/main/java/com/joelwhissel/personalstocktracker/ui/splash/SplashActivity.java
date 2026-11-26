package com.joelwhissel.personalstocktracker.ui.splash;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.ui.login.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tvWelcomeTitle = findViewById(R.id.tv_welcome_title);
        TextView tvWelcomeSubtitle = findViewById(R.id.tv_welcome_subtitle);
        ImageView ivLogo = findViewById(R.id.iv_logo);
        View viewGlow = findViewById(R.id.view_background_glow);
        MaterialButton btnBegin = findViewById(R.id.btn_begin);

        String htmlText = "WELCOME TO <b>APEX STOCKS</b>";
        if (tvWelcomeTitle != null) {
            tvWelcomeTitle.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));
        }

        startDramaticAnimations(ivLogo, viewGlow, tvWelcomeTitle, tvWelcomeSubtitle, btnBegin);

        if (btnBegin != null) {
            btnBegin.setOnClickListener(v -> {
                v.animate()
                        .scaleX(0.92f)
                        .scaleY(0.92f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                            openLogin();
                        })
                        .start();
            });
        }

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null && (e1.getY() - e2.getY() > 100) && Math.abs(velocityY) > 100) {
                    openLogin();
                    return true;
                }
                return false;
            }
        });
    }

    private void startDramaticAnimations(ImageView logo, View bgGlow, TextView title, TextView subtitle, MaterialButton button) {
        if (logo != null) {
            logo.setAlpha(0f);
            logo.setScaleX(0.1f);
            logo.setScaleY(0.1f);
            logo.setRotation(-180f);
        }
        if (title != null) {
            title.setAlpha(0f);
            title.setTranslationY(100f);
            title.setScaleX(0.8f);
        }
        if (subtitle != null) {
            subtitle.setAlpha(0f);
            subtitle.setTranslationY(80f);
        }
        if (button != null) {
            button.setAlpha(0f);
            button.setTranslationY(120f);
            button.setScaleX(0.7f);
            button.setScaleY(0.7f);
        }
        if (bgGlow != null) {
            bgGlow.setAlpha(0f);
            bgGlow.setScaleX(0.2f);
            bgGlow.setScaleY(0.2f);
        }

        if (logo != null) {
            logo.animate()
                    .alpha(1f)
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .rotation(0f)
                    .setDuration(800)
                    .setInterpolator(new OvershootInterpolator(2.2f))
                    .withEndAction(() -> {
                        logo.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .start();

                        ObjectAnimator logoPulse = ObjectAnimator.ofPropertyValuesHolder(
                                logo,
                                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.08f),
                                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.08f),
                                PropertyValuesHolder.ofFloat(View.ROTATION, -3f, 3f)
                        );
                        logoPulse.setDuration(1600);
                        logoPulse.setRepeatCount(ValueAnimator.INFINITE);
                        logoPulse.setRepeatMode(ValueAnimator.REVERSE);
                        logoPulse.setInterpolator(new AccelerateDecelerateInterpolator());
                        logoPulse.start();
                    })
                    .start();
        }

        if (bgGlow != null) {
            bgGlow.animate()
                    .alpha(0.8f)
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(1000)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        ObjectAnimator glowPulse = ObjectAnimator.ofPropertyValuesHolder(
                                bgGlow,
                                PropertyValuesHolder.ofFloat(View.ALPHA, 0.4f, 0.85f),
                                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.1f, 1.35f),
                                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.1f, 1.35f)
                        );
                        glowPulse.setDuration(2200);
                        glowPulse.setRepeatCount(ValueAnimator.INFINITE);
                        glowPulse.setRepeatMode(ValueAnimator.REVERSE);
                        glowPulse.start();
                    })
                    .start();
        }

        if (title != null) {
            title.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1.0f)
                    .setStartDelay(300)
                    .setDuration(700)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .start();
        }

        if (subtitle != null) {
            subtitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(500)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (button != null) {
            button.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setStartDelay(700)
                    .setDuration(700)
                    .setInterpolator(new OvershootInterpolator(2.0f))
                    .withEndAction(() -> {
                        ObjectAnimator btnPulse = ObjectAnimator.ofPropertyValuesHolder(
                                button,
                                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.04f),
                                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.04f)
                        );
                        btnPulse.setDuration(1000);
                        btnPulse.setRepeatCount(ValueAnimator.INFINITE);
                        btnPulse.setRepeatMode(ValueAnimator.REVERSE);
                        btnPulse.start();
                    })
                    .start();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector != null && gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void openLogin() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
