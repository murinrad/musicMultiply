package org.murinrad.android.musicmultiply.receiver;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by Radovan Murin on 11.4.2015.
 */
public class DeviceNameAsker extends LinearLayout {
    EditText text;
    Button okButton;
    DeviceNameAsker m_this;
    DeviceNameAskerCallback callback;


    public DeviceNameAsker(Context context) {
        super(context);
        // init();
    }

    public DeviceNameAsker(Context context, AttributeSet attrs) {
        super(context, attrs);
        // init();
    }


    void init() {
        text = (EditText) findViewById(R.id.editText);
        okButton = (Button) findViewById(R.id.button);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = text.getText().toString();
                if (validateName(newName)) {
                    if (callback != null) {
                        callback.successInternal(newName);
                    }
                } else {
                    text.setError(getResources().getString(R.string.device_name_error));
                }

            }
        });
        m_this = this;
    }

    public void setCallback(DeviceNameAskerCallback callback) {
        this.callback = callback;
    }

    private boolean validateName(String text) {
        if (text == null || text.length() <= 4) {
            return false;
        }
        return true;
    }

    public static abstract class DeviceNameAskerCallback {

        void successInternal(String newName) {
            onSuccess(newName);
        }

        abstract void onSuccess(String newName);

    }
}
