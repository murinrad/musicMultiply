package org.murinrad.android.musicmultiply.devices.management.security;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.murinrad.android.musicmultiply.NotificationProvider;
import org.murinrad.android.musicmultiply.SettingsActivity;
import org.murinrad.android.musicmultiply.datamodel.device.IDevice;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import sandbox.murinrad.org.sandbox.R;

/**
 * Created by Radovan Murin on 19.4.2015.
 */
public class SecurityProvider {

    private static final String SECURITY_PROVIDER_STORE = "SEC_PROVIDER_STORE";
    private static Set<WeakReference<ISecurityProviderListener>> listeners = new HashSet<>();
    private static Integer notificationId;
    Context ctx;
    SharedPreferences prefs;
    Set<IDevice> temporarySupressed = new HashSet<>();

    public SecurityProvider(Context ctx) {
        this.ctx = ctx;
        this.prefs = ctx.getSharedPreferences(SECURITY_PROVIDER_STORE, 0);
    }

    public static void addSecurityProviderListener(ISecurityProviderListener listener) {
        WeakReference<ISecurityProviderListener> listenerWeakReference = new WeakReference<ISecurityProviderListener>(listener);
        listeners.add(listenerWeakReference);
    }

    private void launchNotifications() {
        Iterator<WeakReference<ISecurityProviderListener>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<ISecurityProviderListener> listener = iterator.next();
            if (listener.get() != null) {
                listener.get().onSecurityChange();
            } else {
                iterator.remove();
            }
        }
    }

    public void isAuthorisedAsync(final IDevice device, final SecurityProviderCallback callback) {
        if (!prefs.contains(device.getDeviceUUID()) && !temporarySupressed.contains(device)) {
            Intent settingLauncher = new Intent(ctx, SettingsActivity.class);
            notificationId = NotificationProvider.postNotification(
                    ctx.getString(R.string.new_device_message), null, settingLauncher, notificationId, ctx,
                    ctx.getString(R.string.app_title)
            );
            callback.onAuthFailure();
        } else if (prefs.getBoolean(device.getDeviceUUID(), false)) {
            callback.onAuthSuccess();
        } else {
            callback.onAuthFailure();
        }

    }

    public boolean isAllowed(IDevice device) {
        return prefs.getBoolean(device.getDeviceUUID(), false);
    }

    public void setPermission(IDevice device, boolean isAllowed) {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean(device.getDeviceUUID(), isAllowed);
        e.commit();
        launchNotifications();
    }

    public static interface ISecurityProviderListener {

        public void onSecurityChange();
    }

    public static abstract class SecurityProviderCallback {

        public abstract void onAuthSuccess();

        public abstract void onAuthFailure();
    }
}
