package org.murinrad.android.musicmultiply.upnp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.fourthline.cling.binding.annotations.UpnpInputArgument;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.AVTransportException;
import org.fourthline.cling.support.avtransport.AbstractAVTransportService;
import org.fourthline.cling.support.model.DeviceCapabilities;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PlayMode;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.StorageMedium;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportSettings;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.model.TransportStatus;
import org.murinrad.android.musicmultiply.MainActivity;
import org.murinrad.android.musicmultiply.MusicMultiplyServerService;
import org.murinrad.android.musicmultiply.decoder.MusicData;
import org.murinrad.android.musicmultiply.decoder.events.MusicPlaybackEventDispatcher;
import org.murinrad.android.musicmultiply.decoder.events.OnMusicPlaybackListener;

import java.lang.ref.WeakReference;
import java.util.Random;

/**
 * Created by rmurin on 22/03/2015.
 */
public class AVTransportService extends AbstractAVTransportService implements OnMusicPlaybackListener {
    WeakReference<Context> ctx;

    String AV_URI = "";

    String NEXT_AV_URI = "";

    StorageMedium[] PLAY_CAPABILITIES = new StorageMedium[]{StorageMedium.NETWORK};

    TransportInfo currentStatus;


    long instanceID;

    public AVTransportService() {
        Random r = new Random();
        instanceID = r.nextLong();
        currentStatus = new TransportInfo(TransportState.NO_MEDIA_PRESENT, TransportStatus.OK);
        MusicPlaybackEventDispatcher.registerListener(this);
    }

    public void setContext(Context ctx) {
        this.ctx = new WeakReference<Context>(ctx);
    }


    @Override
    public void setAVTransportURI(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId, @UpnpInputArgument(name = "CurrentURI", stateVariable = "AVTransportURI") String currentURI, @UpnpInputArgument(name = "CurrentURIMetaData", stateVariable = "AVTransportURIMetaData") String currentURIMetaData) throws AVTransportException {

        Log.i(MainActivity.APP_TAG, "setAVTransportURI");
        if (currentURI != null && !"".equals(currentURI)) {
            AV_URI = currentURI;
            if (currentStatus.getCurrentTransportState().equals(TransportState.NO_MEDIA_PRESENT)) {
                currentStatus = new TransportInfo(TransportState.STOPPED);
            }
        }
    }

    @Override
    public void setNextAVTransportURI(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId, @UpnpInputArgument(name = "NextURI", stateVariable = "AVTransportURI") String nextURI, @UpnpInputArgument(name = "NextURIMetaData", stateVariable = "AVTransportURIMetaData") String nextURIMetaData) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "setNextAVTransportURI");
        if (nextURI != null && !"".equals(nextURI)) {
            NEXT_AV_URI = nextURI;
        }
    }

    @Override
    public MediaInfo getMediaInfo(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "getMediaInfo");
        MediaInfo mediaInfo = new MediaInfo(AV_URI, "");
        return mediaInfo;
    }

    @Override
    public TransportInfo getTransportInfo(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "getTransportInfo");
        return currentStatus;
    }

    @Override
    public PositionInfo getPositionInfo(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "getPositionInfo");
        return new PositionInfo();
    }

    @Override
    public DeviceCapabilities getDeviceCapabilities(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "getDeviceCapabilities");
        DeviceCapabilities retVal = new DeviceCapabilities(PLAY_CAPABILITIES);
        return retVal;
    }

    @Override
    public TransportSettings getTransportSettings(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "getTransportSettings");
        TransportSettings transportSettings = new TransportSettings(PlayMode.DIRECT_1);
        return transportSettings;
    }

    @Override
    public void stop(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "stop");
        if (ctx.get() == null) return;
        Intent playIntent = new Intent(ctx.get(), MusicMultiplyServerService.class);
        playIntent.setAction(MusicMultiplyServerService.INTENT_STOP);
        ctx.get().startService(playIntent);

    }

    @Override
    public void play(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId, @UpnpInputArgument(name = "Speed", stateVariable = "TransportPlaySpeed") String speed) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "play");
        if (ctx.get() == null) return;
        Intent playIntent = new Intent(ctx.get(), MusicMultiplyServerService.class);
        playIntent.putExtra(MusicMultiplyServerService.INTENT_URI_TAG, AV_URI);
        playIntent.setAction(MusicMultiplyServerService.INTENT_PLAY);
        ctx.get().startService(playIntent);
    }

    @Override
    public void pause(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "performPause");
        if (ctx.get() == null) return;
        Intent playIntent = new Intent(ctx.get(), MusicMultiplyServerService.class);
        playIntent.setAction(MusicMultiplyServerService.INTENT_PAUSE);
        ctx.get().startService(playIntent);

    }

    @Override
    public void record(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "record");
    }

    @Override
    public void seek(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId, @UpnpInputArgument(name = "Unit", stateVariable = "A_ARG_TYPE_SeekMode") String unit, @UpnpInputArgument(name = "Target", stateVariable = "A_ARG_TYPE_SeekTarget") String target) throws AVTransportException {
        SeekMode seek = SeekMode.valueOrExceptionOf(unit);
        switch (seek) {
            case TRACK_NR:
                break;
            case REL_COUNT:
                break;
            case REL_TIME:
                break;
            case ABS_COUNT:
                break;
            case ABS_TIME:
                break;
            default:
                break;

        }
        Log.i(MainActivity.APP_TAG, "seek");
    }

    @Override
    public void next(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "next");
        if (ctx.get() == null) return;
        Intent playIntent = new Intent(ctx.get(), MusicMultiplyServerService.class);
        playIntent.setAction(MusicMultiplyServerService.INTENT_NEXT);
        ctx.get().startService(playIntent);
    }

    @Override
    public void previous(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "previous");
        if (ctx.get() == null) return;
        Intent playIntent = new Intent(ctx.get(), MusicMultiplyServerService.class);
        playIntent.setAction(MusicMultiplyServerService.INTENT_PREVIOUS);
        ctx.get().startService(playIntent);
    }

    @Override
    public void setPlayMode(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId, @UpnpInputArgument(name = "NewPlayMode", stateVariable = "CurrentPlayMode") String newPlayMode) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "setPlayMode");

    }

    @Override
    public void setRecordQualityMode(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId, @UpnpInputArgument(name = "NewRecordQualityMode", stateVariable = "CurrentRecordQualityMode") String newRecordQualityMode) throws AVTransportException {
        Log.i(MainActivity.APP_TAG, "setRecordQualityMode");
    }

    @Override
    protected TransportAction[] getCurrentTransportActions(UnsignedIntegerFourBytes instanceId) throws Exception {
        Log.i(MainActivity.APP_TAG, "getCurrentTransportActions");
        return getTransportActionsPossible();
    }

    @Override
    public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
        Log.i(MainActivity.APP_TAG, "getCurrentInstanceIds");
        UnsignedIntegerFourBytes retVal = new UnsignedIntegerFourBytes(instanceID);
        return new UnsignedIntegerFourBytes[]{retVal};
    }

    @Override
    public void onStop() {
        currentStatus = new TransportInfo(TransportState.STOPPED);
    }

    @Override
    public void onPause() {
        currentStatus = new TransportInfo(TransportState.PAUSED_PLAYBACK);
    }

    @Override
    public void onStart() {
        currentStatus = new TransportInfo(TransportState.PLAYING);
    }

    @Override
    public void onMusicInfoChange(MusicData data) {
        //do nothing this is handeld by the receiver anyways
    }

    private TransportAction[] getTransportActionsPossible() {
        TransportAction[] retVal;
        switch (currentStatus.getCurrentTransportState()) {
            case NO_MEDIA_PRESENT:
                retVal = new TransportAction[]{};
                break;
            case PAUSED_PLAYBACK:
                retVal = new TransportAction[]{TransportAction.Stop, TransportAction.Play, TransportAction.Seek};
                break;
            case PLAYING:
                retVal = new TransportAction[]{TransportAction.Stop, TransportAction.Pause, TransportAction.Seek};
                break;
            case STOPPED:
                retVal = new TransportAction[]{TransportAction.Play};
                break;
            case TRANSITIONING:
                retVal = new TransportAction[]{};
                break;
            default:
                retVal = new TransportAction[]{};
        }
        return retVal;
    }
}
