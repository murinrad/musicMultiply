package org.murinrad.android.musicmultiply;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Created by Rado on 6.4.2015.
 */
public class UUIDUtility {
    public static final String SETTINGS_PREF_TAG = "MUSIC_MULTIPLY_PREFS";

    private static final String SETTINGS_UUID_TAG = "SETTING_UUID";


    public static UUID retrieveUUID(Context applicationContext) {
        SharedPreferences settings = applicationContext.getSharedPreferences(SETTINGS_PREF_TAG, 0);
        String retVal = settings.getString(SETTINGS_UUID_TAG, null);
        if (retVal == null) {
            UUID newUUID = UUID.randomUUID();
            SharedPreferences.Editor e = settings.edit();
            e.putString(SETTINGS_UUID_TAG, newUUID.toString());
            e.commit();
            return newUUID;
        }
        return UUID.fromString(retVal);
    }
}
