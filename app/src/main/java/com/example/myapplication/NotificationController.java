package com.example.myapplication;

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
import androidx.core.content.ContextCompat;

import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.Status;

public final class NotificationController {
    private static final String CHANNEL_NEW_ACCIDENT = "new_accident_alerts";
    private static final String CHANNEL_STATUS_UPDATES = "intervention_status_updates";
    private static final int NOTIFICATION_NEW_ACCIDENT = 1001;
    private static final int NOTIFICATION_STATUS_UPDATE = 1002;

    private NotificationController() {}

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel newAccidentChannel = new NotificationChannel(
                CHANNEL_NEW_ACCIDENT,
                context.getString(R.string.notification_channel_new_accident_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        newAccidentChannel.setDescription(
                context.getString(R.string.notification_channel_new_accident_desc)
        );

        NotificationChannel statusChannel = new NotificationChannel(
                CHANNEL_STATUS_UPDATES,
                context.getString(R.string.notification_channel_status_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        statusChannel.setDescription(
                context.getString(R.string.notification_channel_status_desc)
        );

        manager.createNotificationChannel(newAccidentChannel);
        manager.createNotificationChannel(statusChannel);
    }

    public static void notifyNewAccidentForRescue(Context context, Issue issue) {
        if (issue == null || !canPostNotifications(context)) return;

        showNotification(
                context,
                CHANNEL_NEW_ACCIDENT,
                NOTIFICATION_NEW_ACCIDENT,
                context.getString(R.string.notification_new_accident_title),
                context.getString(R.string.notification_new_accident_text, issue.getTitle()),
                NotificationCompat.PRIORITY_HIGH
        );
    }

    public static void notifyStatusUpdateForUser(Context context, Issue issue) {
        if (issue == null || !canPostNotifications(context)) return;

        showNotification(
                context,
                CHANNEL_STATUS_UPDATES,
                NOTIFICATION_STATUS_UPDATE,
                context.getString(R.string.notification_status_update_title),
                context.getString(
                        R.string.notification_status_update_text,
                        issue.getTitle(),
                        getStatusLabel(context, issue.getStatus())
                ),
                NotificationCompat.PRIORITY_DEFAULT
        );
    }

    public static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static void showNotification(
            Context context,
            String channelId,
            int notificationId,
            String title,
            String text,
            int priority
    ) {
        createChannels(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_menu_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(priority);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Permission retiree juste avant l'envoi.
        }
    }

    private static String getStatusLabel(Context context, Status status) {
        switch (status) {
            case AID_SENT:
                return context.getString(R.string.status_sent);
            case RESOLVED:
                return context.getString(R.string.status_resolved);
            case RECEIVED:
            default:
                return context.getString(R.string.status_received);
        }
    }
}
