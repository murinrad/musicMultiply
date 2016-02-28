package org.murinrad.android.musicmultiply.receiver.qos;

/**
 * Created by Radovan Murin on 6.6.2015.
 */
public interface QoSEventReceiver {

    void onPacketLossIncrease();

    void onPacketLossRecovery();

    void onDelayTooLong();
}
