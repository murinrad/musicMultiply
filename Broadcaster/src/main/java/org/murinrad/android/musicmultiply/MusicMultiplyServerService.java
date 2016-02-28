package org.murinrad.android.musicmultiply;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import net.murinrad.android.timekeeper.ITimeKeeperServer;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.murinrad.android.musicmultiply.decoder.MusicData;
import org.murinrad.android.musicmultiply.decoder.SoundSystemToPCMDecoder;
import org.murinrad.android.musicmultiply.decoder.events.MusicPlaybackEventDispatcher;
import org.murinrad.android.musicmultiply.decoder.impl.MP3ToPCMSender;
import org.murinrad.android.musicmultiply.devices.management.DeviceAdvertListener;
import org.murinrad.android.musicmultiply.devices.management.IDeviceAdvertListener;
import org.murinrad.android.musicmultiply.networking.PacketLauncher;
import org.murinrad.android.musicmultiply.networking.qos.QoSMessageListener;
import org.murinrad.android.musicmultiply.tags.Constants;
import org.murinrad.android.musicmultiply.time.TimeKeeperServerService;
import org.murinrad.android.musicmultiply.ui.notification.NotificationProvider;
import org.murinrad.android.musicmultiply.upnp.AVTransportService;
import org.murinrad.android.musicmultiply.upnp.ConnectionManager;
import org.murinrad.android.musicmultiply.upnp.RenderingControl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import sandbox.murinrad.org.sandbox.R;

public class MusicMultiplyServerService extends Service {

    public static final String INTENT_URI_TAG = "INTENT_BUNDLE_URI";
    public static final String INTENT_PLAY = "PLAY";
    public static final String INTENT_STOP = "STOP";
    public static final String INTENT_PAUSE = "PAUSE";
    public static final String INTENT_SEEK = "SEEK";
    public static final String INTENT_SEEK_TIME_TAG = "INTENT_SEEK_TIME_TAG";
    public static final String INTENT_NEXT = "NEXT";
    public static final String INTENT_PREVIOUS = "PREVIOUS";
    public static final String INTENT_CLOSE = "CLOSE";
    AVTransportService avTransportService;
    // AVTransport service
    LocalDevice service;
    ITimeKeeperServer timeKeeperServer;
    ServiceConnection timeKeeperSC = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(MainActivity.APP_TAG, "Timekeeper connected");
            timeKeeperServer = (ITimeKeeperServer) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(MainActivity.APP_TAG, "Timekeeper disconnected");
            timeKeeperServer = null;

        }
    };
    private PacketLauncher packetLauncher;
    private IDeviceAdvertListener advertListener;
    private SoundSystemToPCMDecoder activeDecoder;
    private QoSMessageListener qosSubsystem;
    private NotificationProvider notificationProvider;
    private AndroidUpnpService androidUpnpService;
    ServiceConnection upnpServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(MainActivity.APP_TAG, "upnpService connected");
            androidUpnpService = (AndroidUpnpService) service;
            try {
                initializeUPNP();
            } catch (IOException | ValidationException e) {
                Log.e(MainActivity.APP_TAG, "Error on upnp initialization", e);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(MainActivity.APP_TAG, "upnpService disconnected");
            androidUpnpService = null;
        }
    };

    private Queue<String> pastSongs = new LinkedList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int retVal = START_STICKY;
        if (intent != null) {
            String action = intent.getAction();
            if (action == null) action = "";
            switch (action) {
                case INTENT_NEXT:
                    pastSongs.add(intent.getStringExtra(INTENT_URI_TAG));
                    playSong(intent.getStringExtra(INTENT_URI_TAG));
                    break;
                case INTENT_PAUSE:
                    pause();
                    break;
                case INTENT_PLAY:
                    pastSongs.add(intent.getStringExtra(INTENT_URI_TAG));
                    playSong(intent.getStringExtra(INTENT_URI_TAG));
                    break;
                case INTENT_PREVIOUS:
                    previous();
                    break;
                case INTENT_STOP:
                    stop();
                    break;
                case INTENT_CLOSE:
                    onDestroy();
                    retVal = START_NOT_STICKY;
                    break;
                default:
                    break;
            }
        }
        return retVal;
    }

    private void playSong(String stringExtra) {
        try {
            if (activeDecoder != null) {
                qosSubsystem.removeHandler(activeDecoder);
                activeDecoder.stop();
            }
            activeDecoder = new MP3ToPCMSender(stringExtra,this);
            qosSubsystem.addHandler(activeDecoder);
            activeDecoder.registerOnDataSentListener(packetLauncher);
            activeDecoder.play();
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(getContentResolver()
                    .openAssetFileDescriptor(Uri.parse(stringExtra),"r")
                    .getFileDescriptor());
            MusicData.Builder blrd = new MusicData.Builder().withName(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                    .withArtist(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
            try {
                blrd.withLength(Integer.parseInt(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
            } catch (NumberFormatException ex) {
                Log.w(MainActivity.APP_TAG,"Could not parse song length",ex);
            }
            MusicPlaybackEventDispatcher.notifyMusicDatachange(blrd.build());

        } catch (IOException e) {
            Log.e(MainActivity.APP_TAG, "Could not initialize MP3Decoder", e);
        }
    }

    private void pause() {
        if (activeDecoder != null) {
            activeDecoder.performPause();
        }

    }

    private void stop() {
        if (activeDecoder != null) {
            activeDecoder.stop();
        }
        qosSubsystem.stop();
        advertListener.stop();


    }

    private void previous() {
        if(!pastSongs.isEmpty()) {
            String uri = pastSongs.remove();
            playSong(uri);
        } else {
            Toast.makeText(this,getText(R.string.no_more_previous),Toast.LENGTH_LONG).show();
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(MainActivity.APP_TAG, "MusicMultiplyServer service is starting up...");
        Intent upnpStartIntent = new Intent(this, AndroidUpnpServiceImpl.class);
        bindService(upnpStartIntent, upnpServiceConnection, BIND_AUTO_CREATE);
        Intent timeKeeperStartIntent = new Intent(this, TimeKeeperServerService.class);
        bindService(timeKeeperStartIntent, timeKeeperSC, BIND_AUTO_CREATE);
        try {
            advertListener = new DeviceAdvertListener(Constants.DEVICE_ADVERTISEMENT_PORT, this);
            packetLauncher = new PacketLauncher(MainActivity.AUDIO_PORT, this.getApplicationContext());
            advertListener.addDeviceListener(packetLauncher);
            packetLauncher.start();
            qosSubsystem = new QoSMessageListener();
            qosSubsystem.addHandler(packetLauncher);
            advertListener.addDeviceListener(qosSubsystem);
            qosSubsystem.start();
        } catch (IOException ex) {
            //rethrow
            throw new RuntimeException(ex);
        }


        notificationProvider = new NotificationProvider(this);
        startForeground(notificationProvider.NOTIFICATION_ID, notificationProvider.buildNotification("Running..."));
    }

    public void initializeUPNP() throws IOException, ValidationException {
        service = createDevice();
        androidUpnpService.getRegistry().addDevice(service);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        stopServices();
        notificationProvider.dismissNotification();
        Log.i(MainActivity.APP_TAG, "onDestroy called");
    }

    private void stopServices() {
        Intent upnpIntent = new Intent(this, AndroidUpnpServiceImpl.class);
        stopService(upnpIntent);
        Intent timeKeeperIntent = new Intent(this, TimeKeeperServerService.class);
        stopService(timeKeeperIntent);
    }

    private LocalDevice createDevice()
            throws ValidationException, LocalServiceBindingException, IOException {
        UUID uuid = UUIDUtility.retrieveUUID(getApplicationContext());
        DeviceIdentity identity =
                new DeviceIdentity(new UDN(uuid));

        DeviceType type =
                new UDADeviceType("MediaRenderer", 1);

        DeviceDetails details =
                new DeviceDetails(
                        "Music Multiply",
                        new ManufacturerDetails("RMURIN"),
                        new ModelDetails(
                                "NOTHING SPECIAL",
                                "A server for broadcasting music",
                                "v1"
                        )
                );

        LocalService<AVTransportService> avTransportService =
                new AnnotationLocalServiceBinder().read(AVTransportService.class);
        LocalService<RenderingControl> renderingControlLocalService = new AnnotationLocalServiceBinder().read(RenderingControl.class);
        avTransportService.setManager(
                new DefaultServiceManager(avTransportService, AVTransportService.class)
        );
        LocalService<ConnectionManager> connectionManager = new AnnotationLocalServiceBinder().read(ConnectionManager.class);
        connectionManager.setManager(new DefaultServiceManager<>(connectionManager, ConnectionManager.class));

        renderingControlLocalService.setManager(new DefaultServiceManager<>(renderingControlLocalService, RenderingControl.class));
        avTransportService.getManager().getImplementation().setContext(this.getApplicationContext());
        this.avTransportService = avTransportService.getManager().getImplementation();
        LocalDevice retVal = new LocalDevice(identity, type, details, new LocalService[]{avTransportService, renderingControlLocalService, connectionManager});


        return retVal;
    }


}
