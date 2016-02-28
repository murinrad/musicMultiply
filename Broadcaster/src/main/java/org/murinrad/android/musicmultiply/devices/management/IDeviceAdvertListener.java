package org.murinrad.android.musicmultiply.devices.management;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.datamodel.device.DeviceImpl;
import org.murinrad.android.musicmultiply.datamodel.device.IDevice;
import org.murinrad.android.musicmultiply.devices.management.security.SecurityProvider;

import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Radovan Murin on 6.4.2015.
 */
public abstract class IDeviceAdvertListener implements IDeviceManager, SecurityProvider.ISecurityProviderListener {


    private static final String KNOWN_DEV_STORE = "KNOWN_DEVICES";
    List<IDeviceManager> deviceManagerList = new ArrayList<>();
    Set<IDevice> deviceSet;
    SharedPreferences knownDevicesStore;

    IDeviceAdvertListener(Context ctx) {
        deviceSet = new DeviceSet(new SecurityProvider(ctx));
        knownDevicesStore = ctx.getSharedPreferences(KNOWN_DEV_STORE, 0);
        SecurityProvider.addSecurityProviderListener(this);
    }

    public static IDevice[] getAllKnownDevices(Context ctx) {
        SharedPreferences prefx = ctx.getSharedPreferences(KNOWN_DEV_STORE, 0);
        Map<String, Object> allDevices = (Map<String, Object>) prefx.getAll();
        List<IDevice> devices = new ArrayList<>();
        for (Object o : allDevices.values()) {
            String serialized = (String) o;

            try {
                IDevice dev = new DeviceImpl(serialized, null);
                devices.add(dev);
            } catch (InvalidClassException e) {
                Log.w(MainActivity.APP_TAG, "Unknown string in device store", e);
            }

        }
        return devices.toArray(new IDevice[]{});
    }

    synchronized void notifyAdd(IDevice device) {
        for (IDeviceManager devMgr : deviceManagerList) {
            devMgr.addDevice(device);
        }
    }

    synchronized void notifyRemove(IDevice device) {
        for (IDeviceManager devMgr : deviceManagerList) {
            devMgr.removeDevice(device);
        }
    }

    synchronized void notifyRemove(String device) {
        for (IDeviceManager devMgr : deviceManagerList) {
            devMgr.removeDevice(device);
        }
    }

    synchronized void notifyListeners() {
        for (IDeviceManager devMgr : deviceManagerList) {
            devMgr.refreshDevices(getAllDevices());
        }

    }

    public synchronized void addDeviceListener(IDeviceManager deviceManager) {
        deviceManagerList.add(deviceManager);
        deviceManager.refreshDevices(getAllDevices());
    }

    public synchronized void removeDeviceManager(IDeviceManager deviceManager) {
        Object o = deviceManagerList.remove(deviceManager);
        if (o == null)
            Log.w(MainActivity.APP_TAG, "Device advert listener was asked to remove a listener which was not registered");
    }

    synchronized IDevice[] getAllDevices() {
        return deviceSet.toArray(new IDevice[]{});
    }

    @Override
    public synchronized void addDevice(IDevice dev) {

        if (deviceSet.add(dev)) {
            Log.i(MainActivity.APP_TAG, "New device added:" + dev.getName());
            notifyAdd(dev);
        }

    }

    @Override
    public synchronized void removeDevice(IDevice dev) {
        deviceSet.remove(dev);
        Log.i(MainActivity.APP_TAG, "Device removed:" + dev.getName());
        notifyRemove(dev);

    }

    @Override
    public synchronized void removeDevice(String UUID) {
        Iterator<IDevice> iterator = deviceSet.iterator();
        while (iterator.hasNext()) {
            IDevice d = iterator.next();
            if (d.getDeviceUUID().equals(UUID)) {
                iterator.remove();
                notifyRemove(d);
                break;

            }
        }
    }

    @Override
    public void onSecurityChange() {
        removeAllDevices();

    }

    private void removeAllDevices() {
        for (IDevice dev : getAllDevices()) {
            removeDevice(dev);
        }
    }

    @Override
    public synchronized void refreshDevices(IDevice[] devs) {
        notifyListeners();
    }


    public abstract void stop();

    protected class DeviceSet extends HashSet<IDevice> {
        SecurityProvider security;

        DeviceSet(SecurityProvider provider) {
            this.security = provider;
        }

        @Override
        public boolean add(final IDevice object) {
            if (contains(object)) return false;
            SharedPreferences.Editor e = knownDevicesStore.edit();
            e.putString(object.getDeviceUUID(), object.serialize());
            e.commit();
            final AtomicBoolean retVal = new AtomicBoolean(false);
            final AtomicBoolean waiter = new AtomicBoolean(true);
            security.isAuthorisedAsync(object, new SecurityProvider.SecurityProviderCallback() {
                @Override
                public void onAuthSuccess() {
                    retVal.set(true);
                    DeviceSet.super.add(object);
                    waiter.set(false);
                }

                @Override
                public void onAuthFailure() {
                    waiter.set(false);

                }
            });
            while (waiter.get()) {
                //wait for auth
            }
            return retVal.get();
        }

    }
}
