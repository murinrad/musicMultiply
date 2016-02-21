package org.murinrad.android.musicmultiply.time;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import net.murinrad.android.timekeeper.ITimeKeeperClient;

import java.net.UnknownHostException;

public class TimeKeeperClientService extends Service implements ITimeKeeperClientService {
    private TimeKeeperClient timeKeeper;
    private boolean isErrorState = false;
    private final ITimeKeeperClient.Stub timeBinder = new ITimeKeeperClient.Stub() {
        public long getDelta() throws RemoteException {
            long retVal = 0;
            if (isErrorState) {
                throw new RemoteException();
            }
            if (timeKeeper != null) {
                try {
                    retVal = timeKeeper.getTimeDelta();
                } catch (IllegalStateException ex) {
                    Log.e("TimeKeeper", "Timekeeper is in an illegal state.", ex);
                    throw new RemoteException();
                }
            }
            return retVal;
        }

        @Override
        public void setServerInfo(String URL, int port) throws RemoteException {
            createTimeKeeper(URL, port);

        }

        @Override
        public void start() throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void stop() throws RemoteException {
            // TODO Auto-generated method stub

        }
    };


    public IBinder onBind(Intent arg0) {
        String timeKeeperServerURL = arg0
                .getStringExtra(TIME_KEEPER_SERVER_URL);
        Integer timeKeeperServerPort = arg0.getIntExtra(
                TIME_KEEPER_SERVER_PORT, 123);
        createTimeKeeper(timeKeeperServerURL, timeKeeperServerPort);

        return timeBinder;
    }

    private void createTimeKeeper(String timeKeeperServerURL, Integer timeKeeperServerPort) {
        if (timeKeeper != null) {
            timeKeeper.stop();
        }
        try {
            timeKeeper = new TimeKeeperClient(timeKeeperServerURL, timeKeeperServerPort);
            timeKeeper.start();
            isErrorState = false;
        } catch (UnknownHostException e) {
            isErrorState = true;
        }
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (timeKeeper != null)
            timeKeeper.stop();
    }

}
