package com.joelwhissel.personalstocktracker.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.domain.AlertNotifier;
import com.joelwhissel.personalstocktracker.domain.PriceAlert;
import com.joelwhissel.personalstocktracker.domain.enums.AlertType;
import com.joelwhissel.personalstocktracker.domain.enums.Direction;
import com.joelwhissel.personalstocktracker.ui.details.StockDetailsActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class NotificationHelper implements AlertNotifier {

    public static final String CHANNEL_PRICE_ALERTS =
            "price_alerts";

    public static final String CHANNEL_REMINDERS =
            "stock_reminders";

    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void createNotificationChannels() {

        NotificationChannel priceChannel =
                new NotificationChannel(
                        CHANNEL_PRICE_ALERTS,
                        context.getString(R.string.channel_price_alerts),
                        NotificationManager.IMPORTANCE_HIGH
                );

        NotificationChannel reminderChannel =
                new NotificationChannel(
                        CHANNEL_REMINDERS,
                        context.getString(R.string.channel_reminders),
                        NotificationManager.IMPORTANCE_DEFAULT
                );

        NotificationManagerCompat manager =
                NotificationManagerCompat.from(context);

        manager.createNotificationChannel(priceChannel);
        manager.createNotificationChannel(reminderChannel);
    }

    public boolean canPostNotifications() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            return false;
        }

        return NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled();
    }

    @Override
    public void showPriceAlert(PriceAlert alert, double price) {

        NumberFormat currency =
                NumberFormat.getCurrencyInstance(Locale.US);

        String formattedPrice = currency.format(price);

        String message;

        if (alert.getType() == AlertType.PERCENT
                && alert.getBasePrice() > 0) {

            double percent = Math.abs(
                    (price - alert.getBasePrice())
                            / alert.getBasePrice() * 100.0
            );

            int messageId =
                    alert.getDirection() == Direction.UP
                            ? R.string.notification_percent_up
                            : R.string.notification_percent_down;

            message = context.getString(
                    messageId,
                    alert.getTicker(),
                    percent,
                    formattedPrice
            );

        } else {

            int messageId =
                    alert.getDirection() == Direction.UP
                            ? R.string.notification_price_up
                            : R.string.notification_price_down;

            message = context.getString(
                    messageId,
                    alert.getTicker(),
                    formattedPrice
            );
        }

        postNotification(
                CHANNEL_PRICE_ALERTS,
                "alert",
                (int) alert.getId(),
                alert.getTicker(),
                context.getString(R.string.notification_alert_title),
                message,
                NotificationCompat.PRIORITY_HIGH
        );
    }

    public void notifySlowMover(
            String ticker, double percent, int days) {

        String message = context.getString(
                R.string.notification_reminder_message,
                ticker,
                percent,
                days
        );

        postNotification(
                CHANNEL_REMINDERS,
                "reminder",
                ticker.hashCode(),
                ticker,
                context.getString(R.string.notification_reminder_title),
                message,
                NotificationCompat.PRIORITY_DEFAULT
        );
    }

    private PendingIntent createDetailsIntent(
            String ticker, String tag, int requestCode) {

        Intent intent = new Intent(
                context,
                StockDetailsActivity.class
        );

        intent.putExtra(
                StockDetailsActivity.EXTRA_TICKER,
                ticker
        );

        intent.setAction(
                context.getPackageName()
                        + "." + tag + "." + requestCode
        );

        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(
                        requestCode,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );
    }

    private void postNotification(
            String channelId,
            String tag,
            int notificationId,
            String ticker,
            String title,
            String message,
            int priority) {

        if (!canPostNotifications()) {
            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(priority)
                        .setAutoCancel(true)
                        .setContentIntent(
                                createDetailsIntent(
                                        ticker,
                                        tag,
                                        notificationId
                                )
                        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            NotificationManagerCompat.from(context)
                    .notify(tag, notificationId, builder.build());

        } catch (SecurityException ignored) {
            // Permission may have been revoked before posting.
        }
    }
}