package org.murinrad.android.musicmultiply.receiver;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Radovan Murin on 24.5.2015.
 */
public class TweakerActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String USE_TIMEKEEPER_PREF = "pref_key_use_timekeeper";

    public static final String TIME_LIMIT_PREF = "pref_key_time_limit";
    public static final String ENABLE_PERIODIC_RESET = "pref_key_enable_reset";
    public static final String PERIODIC_RESET_MILLIS = "pref_key_reset_millis";
    public static final String DEFAULTS_LOADED = "pref_key_defaults_loaded";

    private static Set<WeakReference<SharedPreferences.OnSharedPreferenceChangeListener>> listeners = new HashSet<>();

    public static void registerSharedPreferenceListener(SharedPreferences.OnSharedPreferenceChangeListener l) {
        listeners.add(new WeakReference<SharedPreferences.OnSharedPreferenceChangeListener>(l));
    }

    public static void deregisterSharedPreferenceListener(SharedPreferences.OnSharedPreferenceChangeListener l) {
        WeakReference<SharedPreferences.OnSharedPreferenceChangeListener> toRemove = null;
        for (WeakReference<SharedPreferences.OnSharedPreferenceChangeListener> listener : listeners) {
            if (listener.get() != null && listener.get() == l) {
                toRemove = listener;
            }
        }
        if (toRemove != null) {
            listeners.remove(toRemove);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        deregisterListeners();

    }

    private void registerListeners() {
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        for (WeakReference<SharedPreferences.OnSharedPreferenceChangeListener> listener : listeners) {
            if (listener.get() != null) {
                getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener.get());
            }
        }
    }

    private void deregisterListeners() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        for (WeakReference<SharedPreferences.OnSharedPreferenceChangeListener> listener : listeners) {
            if (listener.get() != null) {
                getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener.get());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i("TweakerActivity", "Preference changed " + key);
    }
}
