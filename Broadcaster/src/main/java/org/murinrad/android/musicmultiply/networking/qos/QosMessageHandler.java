package org.murinrad.android.musicmultiply.networking.qos;

import java.net.InetAddress;

/**
 * Created by Rado on 19.4.2015.
 */
public interface QosMessageHandler {

    void onSlowDownRequest();

    void onRequestRetransmission(int startPacketID);

    void onDelayRequest(int delayMs);

    void onPacketResentRequest(InetAddress address, int packetID);


}
