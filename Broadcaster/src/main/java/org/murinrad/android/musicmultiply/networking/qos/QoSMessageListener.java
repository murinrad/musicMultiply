package org.murinrad.android.musicmultiply.networking.qos;

import android.util.Log;

import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.datamodel.device.IDevice;
import org.murinrad.android.musicmultiply.devices.management.IDeviceManager;
import org.murinrad.android.musicmultiply.networking.PacketConstructor;
import org.murinrad.android.musicmultiply.tags.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Radovan Murin on 19.4.2015.
 */
public class QoSMessageListener implements Runnable, IDeviceManager {

    private static int QoS_ACTION_INTERVAL = 5000;
    Thread runner;
    Set<QosMessageHandler> handlers = new HashSet<>();
    HashMap<InetAddress, Integer> delaysAverages = new HashMap<>();
    HashMap<InetAddress, Integer> packetLosses = new HashMap<>();
    private long lastAction = 0;

    public QoSMessageListener() {

    }

    public void start() {
        runner = new Thread(this);
        runner.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e(MainActivity.APP_TAG, "QoS: Fatal uncaught exception", ex);
            }
        });
        runner.start();

    }

    public void stop() {
        runner.interrupt();
    }

    public void addHandler(QosMessageHandler handler) {
        handlers.add(handler);
    }

    public void removeHandler(QosMessageHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(Constants.QoS_PORT);
            byte[] buffer = new byte[9];
            Log.i(MainActivity.APP_TAG, "QoS:QoSListener active....");
            while (!Thread.interrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    byte datagramType = packet.getData()[0];
                    Constants.QoS_PACKET_TYPE packetType = Constants.QoS_PACKET_TYPE.getDatagramType(datagramType);
                    switch (packetType) {
                        case PACKET_RESEND_REQUEST:
                            processPacketResendReq(packet);
                            break;
                        case STATISTICS_REPORT:
                            processStatistics(packet);
                            break;
                    }

                } catch (IOException e) {
                    Log.i(MainActivity.APP_TAG, "QoS:Cannot receive packet");
                }
            }
        } catch (Exception e) {
            Log.w(MainActivity.APP_TAG, "QoS:Problem in QoS listener", e);
        } finally {
            socket.close();
        }
        Log.i(MainActivity.APP_TAG, "QoS:QoSListener shutdown....");
    }

    private void processPacketResendReq(DatagramPacket packet) {
        int packetID = PacketConstructor.getPacketID(Arrays.copyOfRange(packet.getData(), 1, 5));
        Log.i(MainActivity.APP_TAG, String.format("QoS: Packet resend request received from %s," +
                " requested packet was: %s", packet.getAddress().getHostAddress(), packetID));
        for (QosMessageHandler handler : handlers) {
            handler.onPacketResentRequest(packet.getAddress(), packetID);
        }
    }

    private void processStatistics(DatagramPacket packet) {
        int delayAverage = ByteBuffer.wrap(Arrays.copyOfRange(packet.getData(), 1, 5)).getInt();
        int packetLoss = ByteBuffer.wrap(Arrays.copyOfRange(packet.getData(), 5, 9)).getInt();
        delaysAverages.put(packet.getAddress(), delayAverage);
        packetLosses.put(packet.getAddress(), packetLoss);
        Log.i(MainActivity.APP_TAG, String.format("QoS: New QoS report received from %s, " +
                        "average delay: %s,packet loss: %s", packet.getAddress().getHostAddress(),
                delayAverage, packetLoss));

        synchronized (this) {
            int delaySum = 0;
            for (int delay : delaysAverages.values()) {
                delaySum += delay;
            }
            int delayAverage1 = (int) Math.round((double) delaySum / delaysAverages.values().size());
            if (lastAction == 0 || System.currentTimeMillis() - lastAction > QoS_ACTION_INTERVAL) {
                Log.i(MainActivity.APP_TAG, "QoS: Invoking handler actions on " + handlers.size() + "objects");
                lastAction = System.currentTimeMillis();
                for (QosMessageHandler handler : handlers) {
                    // handler.onDelayRequest(delayAverage);
                }
            }
        }
    }

    @Override
    public void addDevice(IDevice dev) {

    }

    @Override
    public synchronized void removeDevice(IDevice dev) {
        packetLosses.remove(dev.getAddress());
        delaysAverages.remove(dev.getAddress());
    }

    @Override
    public synchronized void removeDevice(String UUID) {

    }

    public void reset() {
        packetLosses.clear();
        delaysAverages.clear();
    }

    @Override
    public synchronized void refreshDevices(IDevice[] devs) {
        Set<InetAddress> toRemove = packetLosses.keySet();
        for (IDevice dev : devs) {
            toRemove.remove(dev.getAddress());
        }
        for (InetAddress del : toRemove) {
            packetLosses.remove(del);
            delaysAverages.remove((del));
        }

    }
}
