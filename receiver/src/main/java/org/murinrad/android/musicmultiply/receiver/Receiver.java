package org.murinrad.android.musicmultiply.receiver;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import net.murinrad.android.timekeeper.ITimeKeeperClient;

import org.murinrad.android.musicmultiply.NotificationProvider;
import org.murinrad.android.musicmultiply.datamodel.device.ClientDeviceFactory;
import org.murinrad.android.musicmultiply.datamodel.device.IDevice;
import org.murinrad.android.musicmultiply.receiver.advertizer.DeviceAdvertizer;
import org.murinrad.android.musicmultiply.receiver.qos.QoSController;
import org.murinrad.android.musicmultiply.receiver.qos.QoSEventReceiver;
import org.murinrad.android.musicmultiply.receiver.qos.TimeKeeperDataLossAnalyzer;
import org.murinrad.android.musicmultiply.tags.Constants;
import org.murinrad.android.musicmultiply.time.NTPSettings;
import org.murinrad.android.musicmultiply.time.TimeKeeperClientService;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by rmurin on 21/03/2015.
 */
public class Receiver extends Service implements SharedPreferences.OnSharedPreferenceChangeListener, QoSEventReceiver {
    public static final String RECEIVER_INTENT_IP_ADDRESS = "IP_ADDRESS";
    public static final String RECEIVER_INTENT_PORT = "IP_PORT";
    public static final String APP_TAG = "Receiver";
    private static final Integer NOTIF_ID = 19890422;
    private static int QUEUE_SIZE = 100;
    ArrayBlockingQueue<DatagramPacket> dataPacketQueue;
    MusicPlayer mp;
    DataReceiveRunner dataRunner;
    DataParser parser;
    Timer advertizerTimer;
    PowerManager.WakeLock wakeLock;
    WifiManager.MulticastLock multicastLock;
    WifiManager.WifiLock wifiLock;
    QoSController qoSController;
    TimeKeeperDataLossAnalyzer dataLossAnalyzer;
    ITimeKeeperClient timeKeeper;
    Timer resetTask;
    ServiceConnection timeKeeperConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(MainActivity.APP_TAG, "TimeKeeper client connected");
            timeKeeper = (ITimeKeeperClient) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(MainActivity.APP_TAG, "TimeKeeper client disconected");
            timeKeeper = null;
        }
    };
    private boolean useTimeKeeper;
    private int maxTimeDifference;

    private void init(String address, int port) {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MainActivity.APP_TAG);
        wakeLock.acquire();
        multicastLock = wm.createMulticastLock(MainActivity.APP_TAG);
        wifiLock = wm.createWifiLock(MainActivity.APP_TAG);
        dataPacketQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        mp = new MusicPlayer();
        dataLossAnalyzer = new TimeKeeperDataLossAnalyzer(500);
        dataLossAnalyzer.registerEventReceiver(this);
        qoSController = new QoSController();
        qoSController.start();
        qoSController.registerEventReceiver(this);
        parser = new DataParser();
        try {
            dataRunner = new DataReceiveRunner(address, port);
        } catch (IOException e) {
            Log.e(MainActivity.APP_TAG, "Receiver service initialization error", e);
            throw new RuntimeException(e);
        }
        enableResetter(SettingsProvider.getBoolean(TweakerActivity.ENABLE_PERIODIC_RESET, this), SettingsProvider.getInteger(TweakerActivity.PERIODIC_RESET_MILLIS, this));
        start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        TweakerActivity.registerSharedPreferenceListener(this);
        reloadSettings();
        Intent timeKeeperLauncher = new Intent(this, TimeKeeperClientService.class);
        bindService(timeKeeperLauncher, timeKeeperConnection, BIND_AUTO_CREATE);
        IDevice dev = ClientDeviceFactory.createLocalDevice(this.getApplicationContext());
        if (dev == null) {
            dev = ClientDeviceFactory.createNewLocalDevice(this.getApplicationContext(), "GENERIC_DEVICE");
        }
        (new InitializeAdvertizer()).execute(dev);
        Notification n = NotificationProvider.buildNotificationCompat(
                getString(R.string.app_notif_message), this, true,
                NOTIF_ID, getString(R.string.app_notif_title), android.R.drawable.stat_sys_headset, null);
        PendingIntent i = PendingIntent.getService(this, 0, new Intent(this, MainActivity.class), 0);
        n.contentIntent = i;
        startForeground(NOTIF_ID, n);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String listeningAddress = intent.getStringExtra(RECEIVER_INTENT_IP_ADDRESS);
            int listeningPort = intent.getIntExtra(RECEIVER_INTENT_PORT, -1);
            init(listeningAddress, listeningPort);
            Log.i(MainActivity.APP_TAG, "Receiver service running...");
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TweakerActivity.deregisterSharedPreferenceListener(this);
        NotificationProvider.dismissAllNotifications(this);
        unbindService(timeKeeperConnection);
        stop();
    }

    public void stop() {
        if (multicastLock != null && multicastLock.isHeld())
            multicastLock.release();
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        parser.stop();
        dataRunner.stop();
        mp.stop();
        qoSController.stop();
        advertizerTimer.cancel();
        qoSController.deregisterEventReceiver(this);
        dataLossAnalyzer.deregisterEventReceiver(this);
        dataLossAnalyzer.stop();
        enableResetter(false, 0);
    }

    private void start() {
        parser.start();
        mp.start();
        dataRunner.start();
    }

    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);
    }


    private void reloadSettings() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        maxTimeDifference = sp.getInt(TweakerActivity.TIME_LIMIT_PREF, 50);
        useTimeKeeper = sp.getBoolean(TweakerActivity.USE_TIMEKEEPER_PREF, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(MainActivity.APP_TAG, "Preference changed in Receiver service: " + key);
        switch (key) {
            case TweakerActivity.USE_TIMEKEEPER_PREF:
                useTimeKeeper = sharedPreferences.getBoolean(key, true);
                break;
            case TweakerActivity.TIME_LIMIT_PREF:
                maxTimeDifference = sharedPreferences.getInt(key, 50);
                break;
            case TweakerActivity.ENABLE_PERIODIC_RESET:
                enableResetter(sharedPreferences.getBoolean(key, false), sharedPreferences.getInt(TweakerActivity.PERIODIC_RESET_MILLIS, 3));
                break;
            case TweakerActivity.PERIODIC_RESET_MILLIS:
                enableResetter(sharedPreferences.getBoolean(TweakerActivity.ENABLE_PERIODIC_RESET, false), sharedPreferences.getInt(key, 5));

        }

    }

    private void enableResetter(boolean value, long period) {
        if (value) {
            TimerTask t = new TimerTask() {
                @Override
                public void run() {
                    if (mp != null)
                        mp.reset();
                }
            };
            resetTask = new Timer(true);
            resetTask.schedule(t, 0, period * 1000);

        } else {
            if (resetTask == null) {
                Log.w(MainActivity.APP_TAG, "Reset task was null and deactivation was asked...");
            } else {
                resetTask.cancel();
                resetTask = null;
            }
        }

    }

    @Override
    public void onPacketLossIncrease() {
        Log.i(MainActivity.APP_TAG, "Deactivating timekeeper due to high packet loss");
        useTimeKeeper = false;
    }

    @Override
    public void onPacketLossRecovery() {
        useTimeKeeper = true && SettingsProvider.getBoolean(SettingsProvider.USE_TIMEKEEPER_BOOL, this);
        Log.i(MainActivity.APP_TAG, "Activating timekeeper, packet loss recovered");

    }

    @Override
    public void onDelayTooLong() {

    }

    private class DataReceiveRunner implements Runnable {
        private static final int TIMEOUT = 500;
        private static final int PACKET_BUFFER_SIZE = 8192;
        InetAddress group = null;
        int port;
        Thread runner;
        DatagramSocket socket;


        private DataReceiveRunner(String group, int port) throws IOException {
            if (group != null) {
                this.group = InetAddress.getByName(group);
            }
            if (port <= 0) {
                throw new IllegalArgumentException("Wrong port number");
            }
            this.port = port;
            runner = new Thread(this);
            initializeNet();
        }

        private void initializeNet() throws IOException {
            if (group != null) {
                MulticastSocket socket = new MulticastSocket(port);
                socket.joinGroup(group);
                socket.setBroadcast(true);
                this.socket = socket;
            } else {
                socket = new DatagramSocket(port);
            }
            socket.setSoTimeout(TIMEOUT);
        }

        private void start() {
            runner.start();
        }

        private void stop() {
            runner.interrupt();

        }


        @Override
        public void run() {
            byte[] buffer;
            DatagramPacket datagram;
            while (!Thread.interrupted()) {
                buffer = new byte[PACKET_BUFFER_SIZE];
                datagram = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(datagram);
                    if (dataPacketQueue.remainingCapacity() > 0) {
                        dataPacketQueue.add(datagram);
                    } else {
                        Log.w(APP_TAG, "Packet dropped, need to code buffer enlargement");
                    }
                } catch (IOException e) {
                    Log.w(APP_TAG, e.toString());
                }

            }
            socket.close();
            Log.i(APP_TAG, "DataReceiveRunner thread stopped");
        }
    }

    private class DataParser implements Runnable {
        Thread runner;
        int lastPacketID = -1;
        int totalPacketsRCVD = 0;
        int totalPacketsLost = 0;
        String lastIp = null;

        DataParser() {
            runner = new Thread(this);
            runner.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Log.e(MainActivity.APP_TAG, "Processor thread exited with exception", ex);
                }
            });
        }


        @Override
        public void run() {
            byte[] data;
            int rcvdID = 0;
            int packetDelta = 0;
            Byte lastTransmissionID = null;
            while (!Thread.interrupted()) {
                try {
                    DatagramPacket packet = dataPacketQueue.take();
                    validatePacket(packet);
                    data = new byte[packet.getLength() - (4 + 8 + 1)];
                    System.arraycopy(packet.getData(), 4 + 8 + 1, data, 0, data.length);
                    rcvdID = new BigInteger(Arrays.copyOfRange(packet.getData(), 0, 4)).intValue();
                    totalPacketsRCVD++;
                    //timestamp validation
                    if (validateTimeStamp(new BigInteger(Arrays.copyOfRange(packet.getData(), 4, 12)).longValue())) {
                        byte transmissionID = packet.getData()[12];
                        Log.v(APP_TAG, String.format("Packet received, packet number: %s, data size: %s, transmission ID: %s", rcvdID, data.length, transmissionID));
                        packetDelta = rcvdID - lastPacketID - 1;
                        //packet delta analysis

                        //if the lastTransmission is different then reset the state of the music player
                        if (lastTransmissionID == null || lastTransmissionID.byteValue() != transmissionID) {
                            Log.i(APP_TAG, "Detected transmission ID change, new song assumed");
                            lastTransmissionID = transmissionID;
                            mp.reset();
                        } else if (packetDelta > 0) {
                            Log.d(APP_TAG, String.format("Packet delta: %s", packetDelta));
                            qoSController.reportPacketLoss(lastPacketID, rcvdID);
                            totalPacketsLost += packetDelta;
                            if (totalPacketsLost % 100 == 0) {
                                Log.d(APP_TAG, String.format("Packet loss: %s", totalPacketsLost));
                            }
                        }
                        process(data, rcvdID);
                    } else {
                        Log.w(APP_TAG, "Packet dropped as time limit expired");
                    }
                    lastPacketID = rcvdID;
                } catch (InterruptedException e) {

                }
            }
            Log.d(APP_TAG, String.format("Receival done: total packets %s, packets loss: %s, percentage lost: %s", totalPacketsRCVD, totalPacketsLost, (totalPacketsLost * 1.0 / (totalPacketsLost + totalPacketsRCVD))));

        }

        private boolean validateTimeStamp(long packetTimestampLong) {
            if (timeKeeper == null) return true;
            try {
                long delta = (System.currentTimeMillis() + timeKeeper.getDelta()) - (packetTimestampLong);
                boolean retVal = Math.abs(delta) < maxTimeDifference;
                qoSController.reportDelay(delta);
                if (!retVal) {
                    Log.i(APP_TAG, "Packet received too late...discarting, delta = " + delta);
                    dataLossAnalyzer.reportDataLoss(1);
                }

                return retVal || !useTimeKeeper;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return true;
        }

        private void validatePacket(DatagramPacket packet) {
            String ip = packet.getAddress().getHostAddress();
            if (lastIp == null || !lastIp.equals(ip)) {
                try {
                    if (timeKeeper != null) {
                        timeKeeper.setServerInfo(ip, NTPSettings.NTP_PORT);
                        lastIp = ip;
                    }
                    qoSController.setServer(ip);
                } catch (RemoteException e) {
                    Log.w(MainActivity.APP_TAG, "Cannot configure TimeKeeper", e);
                }
            }
        }

        public void start() {
            runner.start();
        }

        public void stop() {
            runner.interrupt();
        }


        private void process(byte[] data, int packetID) {
            mp.write(data, packetID);
        }
    }

    private class InitializeAdvertizer extends AsyncTask<IDevice, Void, Void> {


        @Override
        protected Void doInBackground(IDevice... params) {
            try {
                InetAddress broadcastInetAddress = getBroadcastAddress();
                advertizerTimer = new Timer();
                advertizerTimer.schedule(new DeviceAdvertizer(Constants.DEVICE_ADVERTISEMENT_PORT, params[0], broadcastInetAddress), 0, 500);
            } catch (IOException e) {
                Log.e(MainActivity.APP_TAG, "cannot initialize the advert broadcaster");
                e.printStackTrace();
            }
            return null;
        }
    }

}
