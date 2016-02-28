package org.murinrad.android.musicmultiply;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.apache.mina.proxy.utils.StringUtilities;
import org.eclipse.jetty.util.StringUtil;
import org.murinrad.android.musicmultiply.decoder.MusicData;
import org.murinrad.android.musicmultiply.decoder.events.MusicPlaybackEventDispatcher;
import org.murinrad.android.musicmultiply.decoder.events.OnMusicPlaybackListener;
import org.murinrad.android.musicmultiply.networking.wifi.WifiUtility;
import org.murinrad.android.musicmultiply.org.murinrad.util.GenericCallback;

import sandbox.murinrad.org.sandbox.R;


public class MainActivity extends Activity {
    public static final String APP_TAG = "MusicMultiply SERVER";
    private static final int SONG_PICKER = 1;
    public static int AUDIO_PORT = 15145;
    public static String channel = "224.0.2.0";
    Button closeButton, selectSongButton;
    ImageButton playButton, prevButton, nextButton;
    PlaybackListener playbackListener;
    String lastURI = null;
    TextView songName,songDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        selectSongButton = (Button) findViewById(R.id.selectSong);
        closeButton = (Button) findViewById(R.id.closeButton);
        playButton = (ImageButton) findViewById(R.id.playButton);
        nextButton = (ImageButton) findViewById(R.id.nextBTN);
        prevButton = (ImageButton) findViewById(R.id.previousBTN);
        songName = (TextView) findViewById(R.id.songName);

        initializeButtonActions();

        performInitializationChecks();


    }



    private void initializeButtonActions() {
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MusicMultiplyServerService.class);
                intent.setAction(MusicMultiplyServerService.INTENT_STOP);
                startService(intent);
                //  finish();
            }
        });
        selectSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMusicSelector();
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastURI == null || "".equals(lastURI)) {
                    launchMusicSelector();
                } else {
                    Intent intent = new Intent(getApplicationContext(), MusicMultiplyServerService.class);
                    intent.setAction(MusicMultiplyServerService.INTENT_PAUSE);
                    startService(intent);
                }
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), MusicMultiplyServerService.class);
                intent.setAction(MusicMultiplyServerService.INTENT_PREVIOUS);
                startService(intent);
            }
        });


    }

    @Override
    protected void onStop() {
        super.onStop();
        MusicPlaybackEventDispatcher.deregisterListener(playbackListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        playbackListener = new PlaybackListener();
        MusicPlaybackEventDispatcher.registerListener(playbackListener);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SONG_PICKER:
                if (resultCode == RESULT_OK) {
                    parsePickerResult(data);
                }
                break;
            default:
                Log.i(APP_TAG, "Main activity received result with an unknown request code");

        }
    }

    private void parsePickerResult(Intent data) {
        String uri = data.getData().toString();
        Log.i(APP_TAG, "Main activity received new music URI : " + uri);
        this.lastURI = uri;
        startSong(uri);
    }

    private void startSong(String uri) {
        Intent intent = new Intent(this, MusicMultiplyServerService.class);
        intent.setAction(MusicMultiplyServerService.INTENT_PLAY);
        intent.putExtra(MusicMultiplyServerService.INTENT_URI_TAG, uri);
        startService(intent);
    }

    private void launchMusicSelector() {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_PICK);
        i.setType(MediaStore.Audio.Media.CONTENT_TYPE);
        i.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(i, "Song"), SONG_PICKER);
    }

    private class PlaybackListener implements OnMusicPlaybackListener {

        @Override
        public void onStop() {
            if (playButton != null) {
                playButton.setImageResource((android.R.drawable.ic_media_play));

            }


        }

        @Override
        public void onPause() {
            if (playButton != null) {
                playButton.setImageResource(android.R.drawable.ic_media_play);
            }

        }

        @Override
        public void onStart() {
            if (playButton != null) {
                playButton.setImageResource(android.R.drawable.ic_media_pause);
            }

        }

        @Override
        public void onMusicInfoChange(MusicData data) {
            if(songName!=null) {
                songName.setText(data.getSongName());



            }

        }
    }
}
