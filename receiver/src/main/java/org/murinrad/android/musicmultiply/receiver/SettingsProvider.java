package org.murinrad.android.musicmultiply.receiver;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Rado on 24.5.2015.
 */
public class SettingsProvider {

    public static final String USE_TIMEKEEPER_BOOL = "USE_TIMEKEEPER_BOOL";
    public static final String MAX_TIME_DIFFERENCE = "MAX_TIME_DIFFERENCE";
    private static final String SETTINGS_TAG = "MM_CLIENT_SETTINGS";
    private static Set<WeakReference<SettingsProviderNotifiable>> notifiables = new HashSet<WeakReference<SettingsProviderNotifiable>>();

    private SettingsProvider() {
    }

    public static void registerNotifiable(SettingsProviderNotifiable notifiable) {
        notifiables.add(new WeakReference<SettingsProviderNotifiable>(notifiable));
    }


    public static boolean getBoolean(String key, Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SETTINGS_TAG, Context.MODE_APPEND);
        return sp.getBoolean(key, false);
    }

    public static int getInteger(String key, Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SETTINGS_TAG, Context.MODE_APPEND);
        return sp.getInt(key, 0);
    }

    public static void setBoolean(String key, boolean value, Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SETTINGS_TAG, Context.MODE_APPEND);
        SharedPreferences.Editor e = sp.edit();
        e.putBoolean(key, value);
        e.commit();
        nofityAllListeners();
    }

    public static void setInteger(String key, int value, Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SETTINGS_TAG, Context.MODE_APPEND);
        SharedPreferences.Editor e = sp.edit();
        e.putInt(key, value);
        e.commit();
        nofityAllListeners();
    }

    private static void nofityAllListeners() {
        for (WeakReference<SettingsProviderNotifiable> n : notifiables) {
            if (n.get() != null) {
                n.get().notifySettingChanged();
            }
        }
    }


    public static interface SettingsProviderNotifiable {

        void notifySettingChanged();

    }
}
