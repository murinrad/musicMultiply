package org.murinrad.android.musicmultiply.receiver;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import org.murinrad.android.musicmultiply.receiver.utils.ComparableTuple;
import org.murinrad.android.musicmultiply.receiver.utils.ReceiverCache;

/**
 * Created by rmurin on 21/03/2015.
 */
public class MusicPlayer implements Runnable {

    private static final int BUFFER_SIZE = 25;
    AudioTrack audioTrack;
    ReceiverCache dataIn;
    Thread runner;

    public MusicPlayer() {
        init();
    }

    private void init() {
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(
                        44100,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );
        audioTrack.play();
        dataIn = new ReceiverCache(BUFFER_SIZE);
    }

    public void start() {
        runner = new Thread(this);
        runner.setDaemon(true);
        runner.start();
    }

    public void stop() {
        runner.interrupt();
        audioTrack.flush();
        audioTrack.release();
    }

    @Override
    public void run() {
        byte[] data;
        try {
            ComparableTuple tuple;
            while (!Thread.interrupted()) {
                tuple = dataIn.take();
                if (tuple == null) continue;
                data = tuple.getSecond();
                audioTrack.write(data, 0, data.length);
                //wait(673);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void write(byte[] data, int packetID) {
        ComparableTuple tuple = new ComparableTuple(packetID, data);
        if (!dataIn.offer(tuple)) {
            Log.w(MainActivity.APP_TAG, "Music player queues cannot handle the data...dropping bytes!!!");
        }
    }

    public void reset() {
        audioTrack.flush();
        dataIn.reset();
    }
}
