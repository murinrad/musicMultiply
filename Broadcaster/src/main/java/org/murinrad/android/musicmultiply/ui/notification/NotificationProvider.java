package org.murinrad.android.musicmultiply.ui.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import org.murinrad.android.musicmultiply.decoder.MusicData;
import org.murinrad.android.musicmultiply.decoder.events.MusicPlaybackEventDispatcher;
import org.murinrad.android.musicmultiply.decoder.events.OnMusicPlaybackListener;

/**
 * Created by Radovan Murin on 12.4.2015.
 */
public class NotificationProvider extends org.murinrad.android.musicmultiply.NotificationProvider  implements OnMusicPlaybackListener {


    public static final int NOTIFICATION_ID = 198259636;
    private Context ctx;
    private NotificationManager notificationManager;

    public NotificationProvider(Context ctx) {
        this.ctx = ctx;
        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = buildNotification("Online");
        notificationManager.notify(NOTIFICATION_ID, n);
        MusicPlaybackEventDispatcher.registerListener(this);

    }





    @Override
    public void onStop() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification("Playback stopped"));

    }

    @Override
    public void onPause() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification("Playback paused"));

    }

    @Override
    public void onStart() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification("Playing"));

    }

    @Override
    public void onMusicInfoChange(MusicData data) {
        //unsuported yet
    }

    public Notification buildNotification(String text) {
        return buildNotification(text, ctx, true, NOTIFICATION_ID, "MusicMultiply service is running", android.R.drawable.ic_media_play, null);
    }

    public void dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
