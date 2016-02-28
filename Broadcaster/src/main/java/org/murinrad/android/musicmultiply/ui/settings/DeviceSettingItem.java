package org.murinrad.android.musicmultiply.ui.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import sandbox.murinrad.org.sandbox.R;

/**
 * Created by Radovan Murin on 19.4.2015.
 */
public class DeviceSettingItem extends LinearLayout {

    Button action;
    TextView deviceName;
    TextView deviceState;
    IDeviceStateCallback callback;
    private DEVICE_STATE state;
    private DeviceSettingItem _this;


    public DeviceSettingItem(Context context) {
        super(context);
    }

    public DeviceSettingItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceSettingItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void registerCallBack(IDeviceStateCallback callback) {
        this.callback = callback;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        _this = this;
        action = (Button) findViewById(R.id.actionBTN);
        deviceState = (TextView) findViewById(R.id.deviceState);
        deviceName = (TextView) findViewById(R.id.deviceName);
        action.setOnClickListener(new OnButtonPressedCallback());
    }

    public void setDeviceName(String name) {
        deviceName.setText(name);
    }

    public void setDeviceState(DEVICE_STATE state) {
        this.state = state;
        deviceState.setText(this.state.getStatusText());
        action.setText(state.getButtonActionText());

    }

    public enum DEVICE_STATE {
        ALLOWED {
            @Override
            int getStatusText() {
                return R.string.device_allowed;
            }

            @Override
            int getButtonActionText() {
                return R.string.disallow_device;
            }
        }, TEMPORARILY_SUSPENDED {
            @Override
            int getStatusText() {
                return R.string.device_suspended;
            }

            @Override
            int getButtonActionText() {
                return R.string.allow_device;
            }
        }, NOT_ALLOWED {
            @Override
            int getStatusText() {
                return R.string.device_not_allowed;
            }

            @Override
            int getButtonActionText() {
                return R.string.allow_device;
            }
        };

        abstract int getStatusText();

        abstract int getButtonActionText();
    }

    public class OnButtonPressedCallback implements OnClickListener {

        @Override
        public void onClick(View v) {
            callback.onButtonClick(state);
        }
    }
}
