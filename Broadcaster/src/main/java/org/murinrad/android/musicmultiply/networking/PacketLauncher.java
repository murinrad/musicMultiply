package org.murinrad.android.musicmultiply.networking;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.datamodel.device.IDevice;
import org.murinrad.android.musicmultiply.decoder.events.OnDataSentListener;
import org.murinrad.android.musicmultiply.devices.management.IDeviceManager;
import org.murinrad.android.musicmultiply.networking.qos.QosMessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by rmurin on 21/03/2015.
 */
public class PacketLauncher implements Runnable, OnDataSentListener, IDeviceManager, QosMessageHandler {
    final int QUEUE_SIZE = 10000;
    BlockingQueue<byte[]> dataQueue;
    BlockingQueue<byte[]> pciDataQueue;
    PacketConstructor packetConstructor;
    Set<IDevice> addresses = new HashSet<>();
    int port;
    Thread runner;
    TRANSMISSION_STATE state = TRANSMISSION_STATE.NORMAL;
    DatagramSocket socket;
    Context ctx;
    PowerManager.WakeLock wakeLock;
    WifiManager.MulticastLock multicastLock;
    WifiManager.WifiLock wifiLock;
    PacketCache cache;
    boolean isActive = false;
    private long lastPacketTimeStamp = 0;

    public PacketLauncher(String address, int port, Context ctx) throws IOException {
        this(port, ctx);
        // constructor for multicast
        throw new RuntimeException("Not implemented");
        //addresses.add(InetAddress.getByName(address));


    }

    public PacketLauncher(int port, Context ctx) throws IOException {
        this.port = port;
        this.dataQueue = new ArrayBlockingQueue<byte[]>(QUEUE_SIZE);
        this.pciDataQueue = new ArrayBlockingQueue<byte[]>(QUEUE_SIZE);
        socket = new MulticastSocket(port);
        packetConstructor = new PacketConstructor(pciDataQueue, dataQueue);
        packetConstructor.start();
        this.ctx = ctx;
        cache = new PacketCache(100);
        StalenessDetector stalenessDetector = new StalenessDetector();
        Timer t = new Timer(true);
        t.schedule(stalenessDetector, 0, 30000);
    }

    public void start() {
        if (isActive) return;
        runner = new Thread(this);
        runner.start();
        isActive = true;
    }

    public void stop() {
        if (!isActive) return;
        packetConstructor.stop();
        runner.interrupt();
        unlockPerformance();
    }

    @Override
    public void run() {
        lockPerformance();
        byte[] data;
        while (!runner.isInterrupted()) {
            try {
                data = dataQueue.take();
                for (IDevice addr : addresses) {
                    sentDataToDevice(data, addr.getAddress());
                }
                lastPacketTimeStamp = System.currentTimeMillis();
                Log.v("Packet Launcher", "Packet sent!");
                cache.put(PacketConstructor.getPacketID(data), data);
                Thread.sleep(state.delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        unlockPerformance();
        isActive = false;

    }

    private void sentDataToDevice(byte[] data, InetAddress addr) throws IOException {
        DatagramPacket dp;
        dp = new DatagramPacket(data, data.length);
        dp.setAddress(addr);
        dp.setPort(port);
        socket.send(dp);
    }

    @Override
    public void dataSent(byte[] data) {
        start();
        pciDataQueue.add(data);
    }

    private void lockPerformance() {
        WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MainActivity.APP_TAG);
        wakeLock.acquire();
        multicastLock = wm.createMulticastLock(MainActivity.APP_TAG);
        wifiLock = wm.createWifiLock(MainActivity.APP_TAG);
        Log.i(MainActivity.APP_TAG, "Performance locks aquired");
    }

    private void unlockPerformance() {
        if (wakeLock != null) {
            wakeLock.release();
        }
        if (multicastLock != null) {
            multicastLock.release();
        }
        if (wifiLock != null) {
            wifiLock.release();
        }
        Log.i(MainActivity.APP_TAG, "Performance locks released");
    }

    @Override
    public void addDevice(IDevice dev) {
        addresses.add(dev);

    }

    @Override
    public void removeDevice(IDevice dev) {
        addresses.remove(dev);
    }

    @Override
    public void removeDevice(String UUID) {

    }

    @Override
    public void refreshDevices(IDevice[] devs) {
        addresses.clear();
        addresses.addAll(Arrays.asList(devs));
    }

    @Override
    public void onSlowDownRequest() {
        switch (state) {
            case NORMAL:
                state = TRANSMISSION_STATE.SLOW;
                break;
            case SLOW:
                state = TRANSMISSION_STATE.DOUBLE_EFFORT;
                break;
            default:
                Log.w(MainActivity.APP_TAG, "Client asked to slow down but I cant go slower.");
        }

    }

    @Override
    public void onRequestRetransmission(int startPacketID) {

    }

    @Override
    public void onDelayRequest(int delayMs) {
        //cant do much here
    }

    @Override
    public void onPacketResentRequest(InetAddress address, int packetID) {
        Log.i(MainActivity.APP_TAG, "Packet resent requested. Packet ID: " + packetID);
        try {
            byte[] data = cache.retrieve(packetID);

            sentDataToDevice(data, address);
        } catch (IOException e) {
            Log.w(MainActivity.APP_TAG, "Problem while resending packet", e);
        } catch (PacketCache.CacheMissException e) {
            Log.v(MainActivity.APP_TAG, "Cache miss");
        }
    }

    public void generateNewID() {
        //packetConstructor.generateNewID();
    }


    private enum TRANSMISSION_STATE {
        NORMAL(15, false),
        SLOW(25, false),
        DOUBLE_EFFORT(25, true);

        private int delay;
        private boolean doubleEffort = false;

        TRANSMISSION_STATE(int delay, boolean doubleEffort) {
            this.delay = delay;
            this.doubleEffort = doubleEffort;

        }
    }

    private class StalenessDetector extends TimerTask {
        @Override
        public void run() {
            if (System.currentTimeMillis() - lastPacketTimeStamp > 10000) {
                stop();
                Log.i(PacketLauncher.class.getCanonicalName(), "Packet launcher stopped due to inactivity");
            }
        }
    }
}
