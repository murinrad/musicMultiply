package org.murinrad.android.musicmultiply.receiver.advertizer;

import android.util.Log;

import org.murinrad.android.musicmultiply.datamodel.device.IDevice;
import org.murinrad.android.musicmultiply.receiver.MainActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.TimerTask;

/**
 * Created by Rado on 11.4.2015.
 */
public class DeviceAdvertizer extends TimerTask {
    DatagramSocket socket;
    DatagramPacket packet;


    public DeviceAdvertizer(int port, IDevice device, InetAddress broadcastAddress) {
        String advert = device.serialize();
        packet = new DatagramPacket(advert.getBytes(), advert.getBytes().length);
        packet.setPort(port);
        packet.setAddress(broadcastAddress);

    }


    @Override
    public void run() {
        if (socket == null) {
            //init
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                Log.e(MainActivity.APP_TAG, "Error in advertizer init", e);
            }
        }
        try {
            socket.send(packet);
        } catch (IOException e) {
            Log.e(MainActivity.APP_TAG, "Error in advertizer sending", e);
        }

    }
}
