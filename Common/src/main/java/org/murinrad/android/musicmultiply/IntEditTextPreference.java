package org.murinrad.android.musicmultiply;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Created by Rado on 25.5.2015.
 */
public class IntEditTextPreference extends EditTextPreference {

    private boolean wasValidatorCalled = false;

    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public IntEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IntEditTextPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setOnPreferenceChangeListener(new InputValidator());
    }

    @Override
    public String getText() {
        return super.getText();
    }

    @Override
    public void setText(String text) {
        super.setText(text);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return super.getPersistedInt(-1) + "";
    }

    @Override
    protected boolean persistString(String value) {
        return super.persistInt(Integer.parseInt(value));
    }

    protected class InputValidator implements OnPreferenceChangeListener {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String value = (String) newValue;
            try {
                Integer.parseInt(value);
                wasValidatorCalled = true;
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
    }

}
