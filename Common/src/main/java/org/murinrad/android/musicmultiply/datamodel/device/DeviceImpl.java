package org.murinrad.android.musicmultiply.datamodel.device;

import java.io.InvalidClassException;
import java.net.InetAddress;

/**
 * Created by Radovan Murin on 6.4.2015.
 */
public class DeviceImpl implements IDevice {

    private static final String SERIALIZATION_FORMAT = "%s:%s";
    InetAddress address;
    String deviceName;
    String deviceUUID;

    public DeviceImpl(InetAddress address, String name, String deviceUUID) {
        this.address = address;
        this.deviceName = name;
        this.deviceUUID = deviceUUID;
    }

    public DeviceImpl(String serializedForm, InetAddress address) throws InvalidClassException {
        this.address = address;
        String[] data = serializedForm.split(":");
        if (data.length != 2) {
            throw new InvalidClassException("Bad serial format");
        }
        deviceName = data[0];
        deviceUUID = data[1];
    }

    @Override
    public String getName() {
        return deviceName;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public String getDeviceUUID() {
        return deviceUUID;
    }

    @Override
    public String serialize() {
        return String.format(SERIALIZATION_FORMAT, getName(), getDeviceUUID());
    }

    @Override
    public int hashCode() {
        return getDeviceUUID().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IDevice) {
            return getDeviceUUID().equals(((IDevice) o).getDeviceUUID());
        }
        return super.equals(o);
    }
}
