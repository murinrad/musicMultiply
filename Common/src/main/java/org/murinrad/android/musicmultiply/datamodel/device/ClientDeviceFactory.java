package org.murinrad.android.musicmultiply.datamodel.device;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Created by Rado on 11.4.2015.
 */
public class ClientDeviceFactory {
    private static final String UUID_STORE = "UUID_STORE";
    private static final String NAME_STORE = "NAME_STORE";
    private static final String STORE_NAME = "MM_CLIENT_SETTINGS_STORE";

    public static IDevice createLocalDevice(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(STORE_NAME, 0);
        String uuid = prefs.getString(UUID_STORE, null);
        String name = prefs.getString(NAME_STORE, null);
        if (uuid == null) return null;
        IDevice retVal = new DeviceImpl(null, name, uuid);
        return retVal;
    }

    public static IDevice createNewLocalDevice(Context ctx, String deviceName) {
        UUID deviceID = UUID.randomUUID();
        IDevice retVal = new DeviceImpl(null, deviceName, deviceID.toString());
        SharedPreferences.Editor e = ctx.getSharedPreferences(STORE_NAME, 0).edit();
        e.putString(UUID_STORE, retVal.getDeviceUUID());
        e.putString(NAME_STORE, retVal.getName());
        e.commit();
        return retVal;
    }
}
