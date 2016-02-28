package org.murinrad.android.musicmultiply.receiver.qos;

import android.util.Log;

import org.murinrad.android.musicmultiply.receiver.MainActivity;
import org.murinrad.android.musicmultiply.receiver.Receiver;
import org.murinrad.android.musicmultiply.tags.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Created by Radovan Murin on 26.4.2015.
 */
public class QosMessenger {
    String serverAddress;

    public QosMessenger(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public void sendQoSMessage(int averageDelay, int packetLoss) {
        if (serverAddress == null) return;

        try {
            byte[] dataToSend = new byte[9];
            dataToSend[0] = Constants.QoS_PACKET_TYPE.STATISTICS_REPORT.getByteRepresentation();
            System.arraycopy(ByteBuffer.allocate(4).putInt(averageDelay).array(), 0, dataToSend, 1, 4);
            System.arraycopy(ByteBuffer.allocate(4).putInt(packetLoss).array(), 0, dataToSend, 5, 4);
            sendData(dataToSend);
            Log.i(MainActivity.APP_TAG, String.format("Qos: Qos report sent to server. Average delay: %s, " +
                    "packet loss: %s", averageDelay, packetLoss));
        } catch (SocketException | UnknownHostException e) {
            Log.w(Receiver.APP_TAG, "Qos: Problem sending QoS message", e);
        } catch (IOException e) {
            Log.w(Receiver.APP_TAG, "Qos: Problem sending QoS message", e);
        }
    }

    private synchronized void sendData(byte[] dataToSend) throws IOException {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length);
            InetAddress targetServer = InetAddress.getByName(serverAddress);
            packet.setAddress(targetServer);
            packet.setPort(Constants.QoS_PORT);
            socket.send(packet);
        } finally {
            if (socket != null)
                socket.close();
        }
    }

    public void requestPacketResend(int startPacketID, int endPacketID) {
        Log.i(Receiver.APP_TAG, String.format("Qos: Requesting packet resend from %s to %s", startPacketID, endPacketID));
        for (int i = startPacketID; i <= endPacketID; i++) {
            byte[] data = new byte[5];
            data[0] = Constants.QoS_PACKET_TYPE.PACKET_RESEND_REQUEST.getByteRepresentation();
            System.arraycopy(ByteBuffer.allocate(4).putInt(i).array(), 0, data, 1, 4);
            try {
                sendData(data);
            } catch (IOException e) {
                Log.w(Receiver.APP_TAG, "Qos: Problem sending packet request", e);
            }
        }
    }
}
