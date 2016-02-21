package org.murinrad.android.musicmultiply.time;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import net.murinrad.android.timekeeper.ITimeKeeperServer;

import org.murinrad.android.musicmultiply.LibTags;

import java.io.IOException;

public class TimeKeeperServerService extends Service {
    NtpServer ntpServer = null;

    ITimeKeeperServer binder = new ITimeKeeperServer.Stub() {

        @Override
        public void stop() throws RemoteException {
            stopServer();
            // TODO Auto-generated method stub

        }

        @Override
        public void setPort(int portNumber) throws RemoteException {

        }

        @Override
        public void start() throws RemoteException {
            if (ntpServer == null || !ntpServer.isStarted()) {
                activateServer();
            }

        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return (IBinder) binder;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        if (noServicePresentOnLAN()) {
            activateServer();
        }
    }

    private void activateServer() {
        stopServer();
        ntpServer = new NtpServer(NTPSettings.NTP_PORT);
        try {

            ntpServer.start();
        } catch (IOException e) {
            Log.w(LibTags.APPTAG, "Error in time server startup");
            e.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private boolean noServicePresentOnLAN() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        stopServer();
    }

    private void stopServer() {
        if (ntpServer != null) {
            ntpServer.stop();
        }
    }

}
