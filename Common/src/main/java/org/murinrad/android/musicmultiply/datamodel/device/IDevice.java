package org.murinrad.android.musicmultiply.datamodel.device;

import java.net.InetAddress;

/**
 * Created by Radovan Murin on 3/29/2015.
 */
public interface IDevice {

    String getName();

    InetAddress getAddress();

    String getDeviceUUID();

    String serialize();

}
