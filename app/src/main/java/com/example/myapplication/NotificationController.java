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
                getNotificationId(NOTIFICATION_NEW_ACCIDENT, issue),
                context.getString(R.string.notification_new_accident_title),
                context.getString(R.string.notification_new_accident_text, issue.getTitle()),
                NotificationCompat.PRIORITY_HIGH,
                createRescueAlertsPendingIntent(context, issue)
        );
    }

    public static void notifyStatusUpdateForUser(Context context, Issue issue) {
        if (issue == null || !canPostNotifications(context)) return;

        showNotification(
                context,
                CHANNEL_STATUS_UPDATES,
                getNotificationId(NOTIFICATION_STATUS_UPDATE, issue),
                context.getString(R.string.notification_status_update_title),
                context.getString(
                        R.string.notification_status_update_text,
                        issue.getTitle(),
                        getStatusLabel(context, issue.getStatus())
                ),
                NotificationCompat.PRIORITY_DEFAULT,
                createUserIssueDetailPendingIntent(context, issue)
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
            int priority,
            PendingIntent pendingIntent
    ) {
        createChannels(context);

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

    private static PendingIntent createRescueAlertsPendingIntent(Context context, Issue issue) {
        Intent intent = new Intent(context, ControlActivity.class);
        intent.putExtra(ControlActivity.EXTRA_ROLE, ControlActivity.ROLE_RESCUE);
        intent.putExtra(ControlActivity.EXTRA_INDEX, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(
                context,
                getRequestCode(NOTIFICATION_NEW_ACCIDENT, issue),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent createUserIssueDetailPendingIntent(Context context, Issue issue) {
        Intent intent = new Intent(context, ControlActivity.class);
        intent.putExtra(ControlActivity.EXTRA_ROLE, ControlActivity.ROLE_VICTIM);
        intent.putExtra(ControlActivity.EXTRA_INDEX, 2);
        intent.putExtra(ControlActivity.EXTRA_ISSUE_ID, issue.getId());
        intent.putExtra(ControlActivity.EXTRA_ISSUE, issue);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(
                context,
                getRequestCode(NOTIFICATION_STATUS_UPDATE, issue),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int getNotificationId(int baseId, Issue issue) {
        return baseId + getIssueHash(issue);
    }

    private static int getRequestCode(int baseId, Issue issue) {
        return baseId + getIssueHash(issue);
    }

    private static int getIssueHash(Issue issue) {
        if (issue == null || issue.getId() == null) return 0;
        return Math.abs(issue.getId().hashCode() % 100000);
    }

    private static String getStatusLabel(Context context, Status status) {
        switch (status) {
            case AID_NOT_SENT:
                return context.getString(R.string.status_not_sent);
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
