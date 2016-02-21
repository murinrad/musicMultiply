package org.murinrad.android.musicmultiply.upnp;

import org.fourthline.cling.binding.annotations.UpnpInputArgument;
import org.fourthline.cling.model.action.ActionException;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.model.types.csv.CSV;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.model.ConnectionInfo;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;

/**
 * Created by Rado on 6.4.2015.
 */
public class ConnectionManager extends ConnectionManagerService {

    @Override
    public synchronized ConnectionInfo getCurrentConnectionInfo(@UpnpInputArgument(name = "ConnectionID") int connectionId) throws ActionException {
        return super.getCurrentConnectionInfo(connectionId);
    }

    @Override
    public synchronized CSV<UnsignedIntegerFourBytes> getCurrentConnectionIDs() {
        return super.getCurrentConnectionIDs();
    }

    @Override
    public synchronized void getProtocolInfo() throws ActionException {

    }

    @Override
    public synchronized ProtocolInfos getSinkProtocolInfo() {
        ProtocolInfo infoMp3 = new ProtocolInfo(Protocol.HTTP_GET, "*", "audio/mp3", "");
        ProtocolInfo infoMpeg3 = new ProtocolInfo(Protocol.HTTP_GET, "*", "audio/mpeg3", "");
        ProtocolInfo infoMpeg = new ProtocolInfo(Protocol.HTTP_GET, "*", "audio/mpeg", "");


        ProtocolInfos retVal = new ProtocolInfos(infoMp3, infoMpeg3, infoMpeg);
        return retVal;
    }
}
