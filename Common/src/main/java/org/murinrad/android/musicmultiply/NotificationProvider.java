package org.murinrad.android.musicmultiply;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Radovan Murin on 20.4.2015.
 */
public class NotificationProvider {
    protected static Random r = new Random();
    protected static Map<Integer, Notification.Builder> managedNotifications = new HashMap<>();
    protected static Map<Integer, NotificationCompat.Builder> managedNotificationsCompat = new HashMap<>();

    public static void dismissAllNotifications(Context ctx) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        for (Integer i : managedNotifications.keySet()) {
            notificationManager.cancel(i);
        }
        for (Integer i : managedNotificationsCompat.keySet()) {
            notificationManager.cancel(i);
        }
        Log.i("Music Multiply general", "All notifications were dismissed");

    }

    public static int postNotification(String message, Integer icon, Intent onClick, Integer id, Context ctx, String title) {
        if (id == null)
            id = r.nextInt();
        if (icon == null) {
            icon = android.R.drawable.stat_sys_speakerphone;
        }
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            n = buildNotification(message, ctx, false, id, title,
                    icon, onClick);
        } else {
            n = buildNotificationCompat(message, ctx, false, id, title,
                    icon, onClick);
        }
        notificationManager.notify(id, n);
        return id;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Notification buildNotification(String text, Context ctx, boolean isPersistent,
                                                    Integer notificationID, String title, int iconID, Intent onClick) {
        Notification.Builder builder = managedNotifications.get(notificationID) == null ?
                new Notification.Builder(ctx) : managedNotifications.get(notificationID);
        managedNotifications.put(notificationID, builder);
        builder.setSmallIcon(iconID).
                setContentTitle(title).
                setContentText(text);
        if (onClick != null) {
            PendingIntent pending = PendingIntent.getActivity(ctx, 0, onClick, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.setContentIntent(pending);
        }
        Notification retVal = builder.build();
        if (isPersistent)
            retVal.flags = Notification.FLAG_ONGOING_EVENT;
        return retVal;
    }

    public static Notification buildNotificationCompat(String text, Context ctx, boolean isPersistent,
                                                          Integer notificationID, String title, int iconID, Intent onClick) {
        NotificationCompat.Builder builder = managedNotificationsCompat.get(notificationID) == null ?
                new NotificationCompat.Builder(ctx) : managedNotificationsCompat.get(notificationID);
        managedNotificationsCompat.put(notificationID, builder);
        builder.setSmallIcon(iconID).
                setContentTitle(title).
                setContentText(text);
        if (onClick != null) {
            PendingIntent pending = PendingIntent.getActivity(ctx, 0, onClick, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.setContentIntent(pending);
        }
        Notification retVal = builder.build();
        if (isPersistent)
            retVal.flags = Notification.FLAG_ONGOING_EVENT;
        return retVal;
    }

}
