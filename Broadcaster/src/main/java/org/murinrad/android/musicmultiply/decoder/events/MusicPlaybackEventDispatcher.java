package org.murinrad.android.musicmultiply.decoder.events;

import android.util.Log;

import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.decoder.MusicData;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Radovan Murin on 14.6.2015.
 */
public final class MusicPlaybackEventDispatcher {

    private static Set<WeakReference<OnMusicPlaybackListener>> listeners = new HashSet<>();

    private MusicPlaybackEventDispatcher() {
    }

    public static void registerListener(OnMusicPlaybackListener listener) {
        listeners.add(new WeakReference<OnMusicPlaybackListener>(listener));
    }

    public static void deregisterListener(OnMusicPlaybackListener listener) {
        Iterator<WeakReference<OnMusicPlaybackListener>> iterator = listeners.iterator();
        Log.i(MainActivity.APP_TAG,"Deregistering Music even dispatcher.");
        while (iterator.hasNext()) {
            WeakReference<OnMusicPlaybackListener> reference = iterator.next();
            if (reference.get() == null) {
                iterator.remove();
                continue;
            }
            if (reference.get() == listener) {
                iterator.remove();
                break;
            }
        }
    }

    public static void notifyMusicStart() {
        Iterator<WeakReference<OnMusicPlaybackListener>> iterator = listeners.iterator();
        Log.i(MainActivity.APP_TAG,"notifyMusicStart");
        while (iterator.hasNext()) {
            WeakReference<OnMusicPlaybackListener> reference = iterator.next();
            if (reference.get() == null) {
                iterator.remove();
                continue;
            }
            reference.get().onStart();

        }
    }

    public static void notifyMusicStop() {
        Iterator<WeakReference<OnMusicPlaybackListener>> iterator = listeners.iterator();
        Log.i(MainActivity.APP_TAG,"notifyMusicStop");
        while (iterator.hasNext()) {
            WeakReference<OnMusicPlaybackListener> reference = iterator.next();
            if (reference.get() == null) {
                iterator.remove();
                continue;
            }
            reference.get().onStop();

        }
    }

    public static void notifyMusicPause() {
        Iterator<WeakReference<OnMusicPlaybackListener>> iterator = listeners.iterator();
        Log.i(MainActivity.APP_TAG,"notifyMusicPause");
        while (iterator.hasNext()) {
            WeakReference<OnMusicPlaybackListener> reference = iterator.next();
            if (reference.get() == null) {
                iterator.remove();
                continue;
            }
            reference.get().onPause();

        }
    }

    public static void notifyMusicDatachange(MusicData data) {
        Iterator<WeakReference<OnMusicPlaybackListener>> iterator = listeners.iterator();
        Log.i(MainActivity.APP_TAG,"notifyMusicDatachange");
        while (iterator.hasNext()) {
            WeakReference<OnMusicPlaybackListener> reference = iterator.next();
            if (reference.get() == null) {
                iterator.remove();
                continue;
            }
            reference.get().onMusicInfoChange(data);

        }
    }

}
