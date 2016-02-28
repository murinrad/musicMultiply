package org.murinrad.android.musicmultiply.decoder.events;

import org.murinrad.android.musicmultiply.decoder.MusicData;

/**
 * Created by Radovan Murin on 8.3.2015.
 */
public interface OnMusicPlaybackListener {

    void onStop();

    void onPause();

    void onStart();

    void onMusicInfoChange(MusicData data);


}
