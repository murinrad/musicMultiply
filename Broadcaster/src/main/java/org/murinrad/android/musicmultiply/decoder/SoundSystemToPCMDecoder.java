package org.murinrad.android.musicmultiply.decoder;


import android.util.Log;

import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.decoder.events.MusicPlaybackEventDispatcher;
import org.murinrad.android.musicmultiply.decoder.events.OnDataSentListener;
import org.murinrad.android.musicmultiply.networking.qos.QosMessageHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Rado on 8.3.2015.
 */
public abstract class SoundSystemToPCMDecoder implements QosMessageHandler {
    protected Set<OnDataSentListener> onDataSendListeners = new HashSet<>();
    protected boolean isPaused = false;

    public abstract void play();

    public abstract void stop();

    public void registerOnDataSentListener(OnDataSentListener l) {
        onDataSendListeners.add(l);
    }

    public void deregisterOnDataSentListener(OnDataSentListener l) {
        if (!onDataSendListeners.remove(l)) {
            Log.w(MainActivity.APP_TAG, "Attempted to deregister a listener which was not found");
        }


    }



    protected void notifyOnStop() {
        MusicPlaybackEventDispatcher.notifyMusicStop();
    }

    protected void notifyOnStart() {
        MusicPlaybackEventDispatcher.notifyMusicStart();

    }

    protected void notifyOnPause() {
        MusicPlaybackEventDispatcher.notifyMusicPause();

    }


    public void pause() {

        if (isPaused) {
            notifyOnPause();
        } else {
            notifyOnStart();
        }
    }


    public boolean isPaused() {
        return isPaused;
    }
}
