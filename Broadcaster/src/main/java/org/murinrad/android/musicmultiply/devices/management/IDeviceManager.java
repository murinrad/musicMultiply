package org.murinrad.android.musicmultiply.devices.management;

import org.murinrad.android.musicmultiply.datamodel.device.IDevice;

/**
 * Created by Rado on 3/29/2015.
 */
public interface IDeviceManager {

    void addDevice(IDevice dev);

    void removeDevice(IDevice dev);

    void removeDevice(String UUID);

    void refreshDevices(IDevice[] devs);
}
