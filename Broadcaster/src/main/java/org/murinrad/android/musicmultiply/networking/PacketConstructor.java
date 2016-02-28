package org.murinrad.android.musicmultiply.networking;

import android.util.Log;

import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.decoder.MusicData;
import org.murinrad.android.musicmultiply.decoder.events.MusicPlaybackEventDispatcher;
import org.murinrad.android.musicmultiply.decoder.events.OnDataSentListener;
import org.murinrad.android.musicmultiply.decoder.events.OnMusicPlaybackListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Radovan Murin on 3/29/2015.
 */
public class PacketConstructor implements OnDataSentListener, OnMusicPlaybackListener, Runnable {
    public static final int MTU_SIZE = 8192;
    boolean active = true;
    BlockingQueue<byte[]> dataInQueue;
    BlockingQueue<byte[]> dataOutQueue;
    Thread runner;
    int packetID = 0;
    byte[] number = new byte[4];
    private byte transmissionID;

    public PacketConstructor(BlockingQueue inQueue, BlockingQueue outQueue) throws IOException {
        dataInQueue = inQueue;
        dataOutQueue = outQueue;
        runner = new Thread(this);
        generateNewID();
        MusicPlaybackEventDispatcher.registerListener(this);
    }

    public static int getPacketID(byte[] dataPacket) {
        if (dataPacket.length < 4) {
            Log.e(MainActivity.APP_TAG, "Packet less than 4 bytes long, cannot get ID");
            throw new RuntimeException("Packet less than 4 bytes long");
        }
        ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(4).put(dataPacket, 0, 4).rewind();
        return bb.getInt();
    }

    public static byte[] getPacketIDBytes(int packetID) {
        return ByteBuffer.allocate(4).putInt(packetID).array();

    }

    @Override
    public void run() {
        byte[] remainder = null;
        while (!Thread.interrupted()) {
            packetID++;
            byte[] constructedPacket = new byte[MTU_SIZE];
            int remainingBytes = MTU_SIZE - (4 + 8 + 4);
            byte[] data;
            number = getPacketIDBytes(packetID);
            System.arraycopy(number, 0, constructedPacket, 0, 4);
            constructedPacket[12] = transmissionID;
            int lastPos = 4 + 8 + 1; //4-packetID + 8 bits for timestamp at queue time + 1 for transmission ID
            if (remainder != null) {
                System.arraycopy(remainder, 0, constructedPacket, lastPos, remainder.length);
                lastPos += remainder.length;
                remainingBytes -= remainder.length;
                remainder = null;
            }
            while (remainingBytes > 0) {
                try {
                    data = dataInQueue.take();
                    if (remainingBytes - data.length >= 0) {
                        System.arraycopy(data, 0, constructedPacket, lastPos, data.length);
                        remainingBytes -= data.length;
                        lastPos += data.length;
                    } else {
                        int carryOverSize = data.length - remainingBytes;
                        remainder = new byte[carryOverSize];
                        System.arraycopy(data, remainingBytes, remainder, 0, remainder.length);
                        System.arraycopy(data, 0, constructedPacket, lastPos, remainingBytes);
                        remainingBytes = 0;
                    }
                } catch (InterruptedException e) {
                    active = false;
                    e.printStackTrace();
                }
            }
            if (dataOutQueue.remainingCapacity() > 0) {
                byte[] enqueueTime = ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();
                System.arraycopy(enqueueTime, 0, constructedPacket, 4, enqueueTime.length);
                dataOutQueue.add(constructedPacket);
            } else {
                Log.w("Decoder", "Datagram discarted because queue is full");
            }
        }

    }

    void start() {
        runner.start();
    }

    void stop() {
        runner.interrupt();
    }


    @Override
    public void dataSent(byte[] data) {
        addToQueue(data);
    }

    private void addToQueue(byte[] data) {
        if (dataInQueue.remainingCapacity() > 0) {
            dataInQueue.offer(data);
        } else {
            Log.w("Sandbox", "Data sending queue full, skipping packet");
        }
    }


    @Override
    public void onStop() {
        active = false;
        if (runner != null) {
            runner.interrupt();
        }


    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStart() {
        generateNewID();

    }

    @Override
    public void onMusicInfoChange(MusicData data) {
        //We dont really care
    }

    public void generateNewID() {
        byte[] buffer = new byte[1];
        new Random().nextBytes(buffer);
        transmissionID = buffer[0];
    }
}
