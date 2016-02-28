package org.murinrad.android.musicmultiply.receiver.settings;

import android.content.Context;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import org.murinrad.android.musicmultiply.IntEditTextPreference;
import org.murinrad.android.musicmultiply.receiver.R;

/**
 * Created by Radovan Murin on 25.5.2015.
 */
public class MaxDelayPreference extends IntEditTextPreference {
    public MaxDelayPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public MaxDelayPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public MaxDelayPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateValue(s.toString());
            }
        });
        setOnPreferenceChangeListener(new MaxDelayValidator());
    }

    private boolean validateValue(String value) {
        try {
            int newValue = Integer.parseInt(value);
            if (newValue < 9 || newValue > 1000) {
                getEditText().setError(getContext().getString(R.string.delay_limit_info));
                return false;
            }
            getEditText().setError(null);
            return true;
        } catch (NumberFormatException ex) {
            getEditText().setError(getContext().getString(R.string.must_be_number_error));
            return false;
        }
    }

    protected class MaxDelayValidator extends InputValidator {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (super.onPreferenceChange(preference, newValue)) {
                return validateValue((String) newValue);
            }
            return false;

        }
    }


}
