package org.murinrad.android.musicmultiply.devices.management;

import android.content.Context;

import org.murinrad.android.musicmultiply.datamodel.device.DeviceImpl;
import org.murinrad.android.musicmultiply.datamodel.device.IDevice;

import java.io.IOException;
import java.io.InvalidClassException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Radovan Murin on 5.4.2015.
 */
public class DeviceAdvertListener extends IDeviceAdvertListener implements Runnable {

    private static final Integer INITIAL_DEVICE_TTL = 5;
    DatagramSocket socket;
    Thread runner;
    Timer cleaner;
    Map<String, Integer> deviceCleanHelper;
    private int port;

    public DeviceAdvertListener(int port, Context ctx) {
        super(ctx);
        this.port = port;
        cleaner = new Timer(true);
        deviceCleanHelper = new HashMap<>();
        start();

    }

    public void start() {
        runner = new Thread(this);
        runner.setDaemon(true);
        runner.start();
        cleaner.schedule(new DeviceCleaner(), 0, 1000);
    }

    public void stop() {
        if (runner != null)
            runner.interrupt();
        cleaner.cancel();
    }


    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        DatagramPacket packet;
        byte[] buffer = new byte[1024];
        while (!runner.isInterrupted()) {
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                analyze(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    private void analyze(DatagramPacket packet) throws InvalidClassException {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
        String serializedDevice = new String(data);
        IDevice device = new DeviceImpl(serializedDevice, packet.getAddress());
        addDevice(device);

    }

    @Override
    public synchronized void addDevice(IDevice dev) {
        super.addDevice(dev);
        resetCounter(dev.getDeviceUUID());
    }

    @Override
    public synchronized void removeDevice(String UUID) {
        super.removeDevice(UUID);
        deviceCleanHelper.remove(UUID);
    }

    @Override
    public synchronized void removeDevice(IDevice dev) {
        super.removeDevice(dev);
        deviceCleanHelper.remove(dev.getDeviceUUID());
    }

    public synchronized void resetCounter(String uuid) {
        deviceCleanHelper.put(uuid, INITIAL_DEVICE_TTL);
    }

    public synchronized void decreaseValue(String UUID) {
        int val = deviceCleanHelper.get(UUID);
        val--;
        deviceCleanHelper.put(UUID, val);
    }

    private class DeviceCleaner extends TimerTask {

        @Override
        public void run() {
            synchronized (this) {
                int val;
                for (String uuid : deviceCleanHelper.keySet()) {
                    val = deviceCleanHelper.get(uuid);
                    if (val == 0) {
                        removeDevice(uuid);
                    } else {
                        decreaseValue(uuid);
                    }
                }
            }
        }
    }
}
