package org.murinrad.android.musicmultiply.devices.management;

import android.content.Context;
import android.util.Log;

import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.datamodel.device.DeviceImpl;
import org.murinrad.android.musicmultiply.datamodel.device.IDevice;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by Rado on 6.4.2015.
 */
public class MockAdvertListener extends IDeviceAdvertListener {

    public MockAdvertListener(Context ctx) {
        super(ctx);
        Thread initThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    IDevice device = new DeviceImpl(InetAddress.getByName("192.168.1.3"), "fakeLocal", "F-F-F");
                    addDevice(device);
                    device = new DeviceImpl(InetAddress.getByName("192.168.1.44"), "fakeLocal", "F-F-G");
                    addDevice(device);
                } catch (IOException ex) {
                    Log.e(MainActivity.APP_TAG, "Error on init of fake device", ex);
                }
            }
        });
        initThread.start();
    }

    @Override
    public void stop() {
        //do nothing
    }
}
