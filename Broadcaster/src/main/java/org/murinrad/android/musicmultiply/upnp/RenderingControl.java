package org.murinrad.android.musicmultiply.upnp;

import android.util.Log;

import org.fourthline.cling.binding.annotations.UpnpInputArgument;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.renderingcontrol.AbstractAudioRenderingControl;
import org.fourthline.cling.support.renderingcontrol.RenderingControlException;
import org.murinrad.android.musicmultiply.MainActivity;

/**
 * Created by Radovan Murin on 1.4.2015.
 */
public class RenderingControl extends AbstractAudioRenderingControl {
    @Override
    public boolean getMute(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes unsignedIntegerFourBytes, @UpnpInputArgument(name = "Channel") String s) throws RenderingControlException {
        Log.i(MainActivity.APP_TAG, "getMute");
        return false;
    }

    @Override
    public void setMute(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes unsignedIntegerFourBytes, @UpnpInputArgument(name = "Channel") String s, @UpnpInputArgument(name = "DesiredMute", stateVariable = "Mute") boolean b) throws RenderingControlException {
        Log.i(MainActivity.APP_TAG, "setMute");
    }

    @Override
    public UnsignedIntegerTwoBytes getVolume(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes unsignedIntegerFourBytes, @UpnpInputArgument(name = "Channel") String s) throws RenderingControlException {
        Log.i(MainActivity.APP_TAG, "getVolume");
        UnsignedIntegerTwoBytes bytes = new UnsignedIntegerTwoBytes(50);
        return bytes;
    }

    @Override
    public void setVolume(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes unsignedIntegerFourBytes, @UpnpInputArgument(name = "Channel") String s, @UpnpInputArgument(name = "DesiredVolume", stateVariable = "Volume") UnsignedIntegerTwoBytes unsignedIntegerTwoBytes) throws RenderingControlException {
        Log.i(MainActivity.APP_TAG, "setVolume");
    }

    @Override
    protected Channel[] getCurrentChannels() {
        Log.i(MainActivity.APP_TAG, "getCurrentChannels");
        return new Channel[0];
    }

    @Override
    public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
        Log.i(MainActivity.APP_TAG, "getCurrentInstanceIds");
        return new UnsignedIntegerFourBytes[0];
    }
}
