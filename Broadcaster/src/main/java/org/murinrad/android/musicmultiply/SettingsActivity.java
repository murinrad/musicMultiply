package org.murinrad.android.musicmultiply;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

import org.murinrad.android.musicmultiply.datamodel.device.IDevice;
import org.murinrad.android.musicmultiply.devices.management.DeviceAdvertListener;
import org.murinrad.android.musicmultiply.devices.management.security.SecurityProvider;
import org.murinrad.android.musicmultiply.ui.settings.DeviceSettingItem;
import org.murinrad.android.musicmultiply.ui.settings.IDeviceStateCallback;

import sandbox.murinrad.org.sandbox.R;

/**
 * Created by Rado on 19.4.2015.
 */
public class SettingsActivity extends Activity {

    SecurityProvider security;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        security = new SecurityProvider(this);


    }

    @Override
    protected void onStart() {
        super.onStart();
        LinearLayout layout = (LinearLayout) findViewById(R.id.settings_root);
        IDevice[] devices = DeviceAdvertListener.getAllKnownDevices(this);
        for (IDevice device : devices) {
            DeviceSettingItem deviceView = (DeviceSettingItem) getLayoutInflater().inflate(R.layout.device_settings, null);
            DeviceBusiness db = new DeviceBusiness(deviceView, device);
            layout.addView(db.getView());
        }
    }

    private class DeviceBusiness {
        DeviceSettingItem view;
        IDeviceStateCallback callback;
        IDevice dev;

        private DeviceBusiness(final DeviceSettingItem view, final IDevice dev) {
            this.view = view;
            this.dev = dev;
            callback = new IDeviceStateCallback() {
                @Override
                public void onButtonClick(DeviceSettingItem.DEVICE_STATE state) {
                    switch (state) {
                        case TEMPORARILY_SUSPENDED:
                        case NOT_ALLOWED:
                            view.setDeviceState(DeviceSettingItem.DEVICE_STATE.ALLOWED);
                            security.setPermission(dev, true);
                            break;
                        case ALLOWED:
                            view.setDeviceState(DeviceSettingItem.DEVICE_STATE.NOT_ALLOWED);
                            security.setPermission(dev, false);
                            break;
                    }

                }
            };
            this.view.registerCallBack(callback);
            this.view.setDeviceName(dev.getName());
            this.view.setDeviceState(getState(dev));
        }

        private DeviceSettingItem.DEVICE_STATE getState(IDevice dev) {
            return security.isAllowed(dev) ? DeviceSettingItem.DEVICE_STATE.ALLOWED : DeviceSettingItem.DEVICE_STATE.NOT_ALLOWED;
        }

        public DeviceSettingItem getView() {
            return view;
        }
    }
}
