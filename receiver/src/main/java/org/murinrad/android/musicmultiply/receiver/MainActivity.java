package org.murinrad.android.musicmultiply.receiver;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.murinrad.android.musicmultiply.datamodel.device.ClientDeviceFactory;
import org.murinrad.android.musicmultiply.networking.wifi.WifiUtility;
import org.murinrad.android.musicmultiply.org.murinrad.util.GenericCallback;


public class MainActivity extends ActionBarActivity {
    public final static String APP_TAG = "MusicMultiply RCVR";
    public int AUDIO_PORT = 15145;
    public String channel = "224.0.2.0";
    ServiceConnection receiverService;
    Button controllerBTN;

    STATE activeState = STATE.PLAYING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadDefaultSettings();
        setContentView(R.layout.activity_main);
        controllerBTN = (Button) findViewById(R.id.stopMusic);
        controllerBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Receiver.class);
                switch (activeState) {
                    case MUTED:
                        intent.putExtra(Receiver.RECEIVER_INTENT_IP_ADDRESS, channel);
                        intent.putExtra(Receiver.RECEIVER_INTENT_PORT, AUDIO_PORT);
                        startService(intent);
                        activeState = STATE.PLAYING;

                        break;
                    case PLAYING:
                        stopService(intent);
                        activeState = STATE.MUTED;
                }
                controllerBTN.setText(activeState.getButtonTXT(MainActivity.this));

            }
        });
        if (ClientDeviceFactory.createLocalDevice(this) == null) {
            askForADeviceName();
        } else {
            init();
        }


    }

    private void loadDefaultSettings() {
        if(!SettingsProvider.getBoolean(TweakerActivity.DEFAULTS_LOADED,this)) {
            //the above setting might be redundant with the PreferenceManager default built in system
            PreferenceManager.setDefaultValues(this,R.xml.default_preferences,false);
        } else {
            Log.i(APP_TAG,"Defaults already loaded so im not gona load anything");
        }
    }

    private void checkForWifi() {
        if (WifiUtility.isConnectedToWiFi(this)) {
            startService();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.wifi_not_connected)
                    .setTitle(R.string.wifi_not_connected_title)
                    .setPositiveButton(R.string.wifi_connect_auto, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectToWifi();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.wifi_connect_myself, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startService();
                            dialog.dismiss();
                        }
                    });
        }
    }

    private void connectToWifi() {
        ProgressDialog.Builder progressBuilder = new AlertDialog.Builder(this);
        progressBuilder.setMessage(R.string.wifi_please_wait_connecting);
        final AlertDialog progressDialog = progressBuilder.show();
        WifiUtility.connectToAP(this, new GenericCallback() {
            @Override
            public void onCallback() {
                if (getSuccess()) {
                    Log.i(APP_TAG, "Wifi OK");
                } else {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage(R.string.wifi_error_cant_connect)
                                    .show();

                        }
                    });
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startService();
                        progressDialog.dismiss();
                    }
                });

            }
        });
    }

    private void askForADeviceName() {
        DeviceNameAsker asker = (DeviceNameAsker) getLayoutInflater().inflate(R.layout.device_name_asker, null);
        asker.init();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(asker);
        final AlertDialog dialog = builder.create();
        asker.setCallback(new DeviceNameAsker.DeviceNameAskerCallback() {
            @Override
            void onSuccess(String newName) {
                ClientDeviceFactory.createNewLocalDevice(getApplication(), newName);
                dialog.dismiss();
                init();
            }
        });
        dialog.show();
    }

    private void init() {
        checkForWifi();
    }

    private void startService() {
        Intent startMusicReceiverIntent = new Intent(this, Receiver.class);
        startMusicReceiverIntent.putExtra(Receiver.RECEIVER_INTENT_IP_ADDRESS, channel);
        startMusicReceiverIntent.putExtra(Receiver.RECEIVER_INTENT_PORT, AUDIO_PORT);
        startService(startMusicReceiverIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // r.stop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        switch (itemID) {
            case R.id.menu_open_settings:
                Intent i = new Intent(this, TweakerActivity.class);
                startActivity(i);
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    private enum STATE {
        PLAYING {
            @Override
            public int getButtonTXT(Context ctx) {
                return R.string.stop_service_label;
            }
        },
        MUTED {
            @Override
            public int getButtonTXT(Context ctx) {
                return R.string.start_service_label;
            }
        };

        public abstract int getButtonTXT(Context ctx);
    }
}
