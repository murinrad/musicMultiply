package org.murinrad.android.musicmultiply.decoder.impl;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.decoder.SoundSystemToPCMDecoder;
import org.murinrad.android.musicmultiply.decoder.events.MusicPlaybackEventDispatcher;
import org.murinrad.android.musicmultiply.decoder.events.OnDataSentListener;
import org.murinrad.android.musicmultiply.networking.qos.QosMessageHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by Radovan Murin on 8.3.2015.
 */
public class MP3ToPCMSender extends SoundSystemToPCMDecoder implements QosMessageHandler {
    private static final String LOG_TAG = "Sandbox";
    protected MediaExtractor extractor;
    protected MediaCodec mediaCodec;
    protected AudioTrack audioTrack;
    protected Boolean doStop = false;
    String sourceURI;
    BufferedSender buffer;
    private int currentDelay = 0;
    private int delayThreshHold = 25;
    private Context context;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public MP3ToPCMSender(String source,Context ctx) throws IOException {
        this.sourceURI = source;
        this.context = ctx;
        extractor = new MediaExtractor();
        if(source.startsWith("content://")) {

            AssetFileDescriptor fd = ctx.getContentResolver().openAssetFileDescriptor(Uri.parse(source),"r");
            extractor.setDataSource(fd.getFileDescriptor());
        } else {
            extractor.setDataSource(source);
        }
        buffer = new BufferedSender(512);
    }



    public void play() {
        DecodeOperation decoder = new DecodeOperation();
        decoder.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        buffer.start();
        notifyOnStart();
    }

    public void stop() {
        doStop = true;
        buffer.stop();
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }
        notifyOnStop();


    }

    @Override
    public void pause() {
        if(!isPaused()) {
            audioTrack.pause();
        } else {
            audioTrack.play();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void decode() throws IOException {
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;
        MediaFormat mf = extractor.getTrackFormat(0);
        String mime = mf.getString(MediaFormat.KEY_MIME);
        mediaCodec = MediaCodec.createDecoderByType(mime);
        mediaCodec.configure(mf, null, null, 0);
        mediaCodec.start();
        codecInputBuffers = mediaCodec.getInputBuffers();
        codecOutputBuffers = mediaCodec.getOutputBuffers();
        int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );

        // start playing, we will feed you later
        audioTrack.play();
        extractor.selectTrack(0);
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 50;
        int inputBufIndex = 0;
        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !doStop) {
            // Log.i(LOG_TAG, "loop ");
            noOutputCounter++;
            if (!sawInputEOS) {
                inputBufIndex = mediaCodec.dequeueInputBuffer(kTimeOutUs);
                // Log.d(LOG_TAG, " bufIndexCheck " + bufIndexCheck);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize = extractor.readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0) {
                        Log.d(LOG_TAG, "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    } // can throw illegal state exception (???)
                    mediaCodec.queueInputBuffer(inputBufIndex, 0 /* offset */, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                } else {
                    Log.e(LOG_TAG, "inputBufIndex " + inputBufIndex);
                }
            }
            int res = mediaCodec.dequeueOutputBuffer(info, kTimeOutUs);
            if (res >= 0) {

                // Log.d(LOG_TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                if (info.size > 0) {
                    noOutputCounter = 0;
                }

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if (chunk.length > 0) {
                    buffer.put(chunk);
                }
                mediaCodec.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
                while (isPaused && !doStop) {
                    // a very crude way to implement a performPause button
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = mediaCodec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = mediaCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + oformat);
            } else {
                Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        Log.d(LOG_TAG, "stopping...");


        if (mediaCodec != null) {

            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;

        }
        doStop = true;
        MusicPlaybackEventDispatcher.notifyMusicStop();

    }

    @Override
    public void onSlowDownRequest() {
        //do nothing
    }

    @Override
    public void onRequestRetransmission(int startPacketID) {
        //do nothing
    }

    @Override
    public void onDelayRequest(final int delayMs) {
        if (Math.abs(delayMs - currentDelay) < delayThreshHold) {
            Log.i(MainActivity.APP_TAG, "Delay request ignored as delta is too small");
            return;
        } else if (delayMs - currentDelay >= delayThreshHold) {
            currentDelay = delayMs;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    audioTrack.pause();
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    audioTrack.play();
                    Log.i(MainActivity.APP_TAG, String.format("Music playback paused for %s ms to compensate delay", delayMs));

                }
            });
            t.start();
        } else {
            Log.w(MainActivity.APP_TAG, "Music playback is behind the client...  ");
        }
    }

    @Override
    public void onPacketResentRequest(InetAddress address, int packetID) {
        //do nothing
    }

    private class BufferedSender implements Runnable {
        BlockingQueue<byte[]> queue;
        int bufferSize;
        int INIT_QUEUE_SIZE = 1024;
        Thread runner;
        AtomicInteger dataIn = new AtomicInteger(0);

        BufferedSender(int buffSize) {
            this.bufferSize = buffSize;
            queue = new ArrayBlockingQueue<byte[]>(INIT_QUEUE_SIZE);
        }

        void start() {
            if (runner != null) runner.interrupt();
            runner = new Thread(this);
            runner.start();
        }

        void stop() {
            runner.interrupt();
        }


        @Override
        public void run() {
            boolean active = false;
            byte[] data;
            try {
                while (!Thread.interrupted()) {
                    if (active || dataIn.get() > bufferSize) {
                        active = true;
                        data = queue.take();
                        audioTrack.write(data, 0, data.length);
                        for (OnDataSentListener l : onDataSendListeners) {
                            l.dataSent(data);
                        }
                        dataIn.addAndGet(data.length * -1);

                    } else if (dataIn.get() == 0) {
                        active = false;
                        //Log.i(MainActivity.APP_TAG, "Buffering...");
                    }
                }
            } catch (InterruptedException ex) {
                Log.i(MainActivity.APP_TAG, "Buffer thread interrupted");
            }
        }

        void put(byte[] data) {
            try {
                while (queue.remainingCapacity() == 0) {
                    Thread.sleep(5);
                    //ugly. I know
                    if (doStop) {
                        throw new InterruptedException("");
                    }
                }
                queue.add(data);
                dataIn.addAndGet(data.length);
            } catch (InterruptedException e) {

            }
        }
    }

    private class DecodeOperation extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... values) {
            try {
                decode();
            } catch (Exception e) {
                Log.e("Sandbox", "FATAL ERROR OCCURRED", e);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
