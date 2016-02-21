package org.murinrad.android.musicmultiply;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.murinrad.android.musicmultiply.networking.wifi.WifiUtility;
import org.murinrad.android.musicmultiply.org.murinrad.util.GenericCallback;

import sandbox.murinrad.org.sandbox.R;


public class MainActivity extends Activity {
    public static final String APP_TAG = "MusicMultiply SERVER";
    public static int AUDIO_PORT = 15145;
    public static String channel = "224.0.2.0";
    Button closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MusicMultiplyServerService.class);
                intent.setAction(MusicMultiplyServerService.INTENT_STOP);
                startService(intent);
                //  finish();
            }
        });
        performInitializationChecks();


    }

    private void performInitializationChecks() {
        if (WifiUtility.isConnectedToWiFi(this)) {
            startMusicService();
        } else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.not_connected_warning_title)
                    .setMessage(R.string.not_connected_warning)
                    .setPositiveButton(R.string.activate_ap, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            initWifi();

                        }
                    }).setNegativeButton(R.string.ignore_wifi_warning, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startMusicService();
                    dialog.dismiss();
                }
            });
            dialogBuilder.show();

        }
    }

    private void initWifi() {
        ProgressDialog.Builder progress = new AlertDialog.Builder(this);
        progress.setMessage(R.string.starting_wifi);
        progress.setCancelable(false);
        final AlertDialog progressDialog = progress.show();
        GenericCallback callback = new GenericCallback() {
            @Override
            public void onCallback() {
                if (getSuccess()) {
                    progressDialog.dismiss();

                } else {
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this).setMessage(R.string.wifi_init_failure).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    progressDialog.dismiss();
                                }
                            }).show();
                        }
                    };
                    MainActivity.this.runOnUiThread(r);

                }
                startMusicService();
            }
        };
        WifiUtility.setupAP(this, callback);
    }

    private void startMusicService() {
        Intent intent = new Intent(this, MusicMultiplyServerService.class);
        startService(intent);
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsStartIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsStartIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
