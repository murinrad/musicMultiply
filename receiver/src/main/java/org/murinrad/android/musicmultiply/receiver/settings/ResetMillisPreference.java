package org.murinrad.android.musicmultiply.receiver.settings;

import android.content.Context;
import android.util.AttributeSet;

import org.murinrad.android.musicmultiply.IntEditTextPreference;

/**
 * Created by Radovan Murin on 02.08.2015.
 */
public class ResetMillisPreference extends IntEditTextPreference {
    public ResetMillisPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ResetMillisPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResetMillisPreference(Context context) {
        super(context);
    }

}
