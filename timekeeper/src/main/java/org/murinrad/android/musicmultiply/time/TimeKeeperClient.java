package org.murinrad.android.musicmultiply.time;

import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.murinrad.android.musicmultiply.LibTags;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TimeKeeperClient {

    String ntpServerLocation = "localhost";

    Integer ntpServerPort = 123;
    SynchronizeTime timeSynchonizer;
    long timeDelta = 0;
    private boolean initialized = false;

    public TimeKeeperClient(String URL, Integer port)
            throws UnknownHostException {
        this.ntpServerLocation = URL == null ? ntpServerLocation : URL;
        this.ntpServerPort = port == null ? ntpServerPort : port;
        timeSynchonizer = new SynchronizeTime(ntpServerLocation);
    }

    public void start() {
        Thread t = new Thread(timeSynchonizer);
        t.start();
    }

    public void stop() {
        timeSynchonizer.kill();
    }

    public String getNtpServerLocation() {
        return ntpServerLocation;
    }

    public Integer getNtpServerPort() {
        return ntpServerPort;
    }

    public long getTimeDelta() throws IllegalStateException {
        if (!initialized) throw new IllegalStateException("TimeKeeper not yet initialized");
        return timeDelta;
    }

    public void initialize() {
        while (!initialized) {
            //do nothing
        }
    }

    private class SynchronizeTime implements Runnable {
        private String serverURL;
        private NTPUDPClient client;
        private boolean active = true;

        public SynchronizeTime(String address)
                throws UnknownHostException {
            this.serverURL = address;
        }

        public void run() {
            activate();
            try {
                InetAddress serverURL = null;

                serverURL = InetAddress.getByName(this.serverURL);

                while (active) {
                    try {
                        client = new NTPUDPClient();
                        client.setDefaultTimeout(10000);
                        TimeInfo result = client.getTime(serverURL, getNtpServerPort());
                        result.computeDetails();
                        if (result != null && result.getOffset() != null) {
                            timeDelta = result.getOffset();
                            Log.i("Music Time Keeper", String.format(
                                    "New time delta received, timeD = %s",
                                    result.getOffset() + ""));
                            Log.i("Music Time Keeper", String.format(
                                    "New time delta received, returnTime = %s",
                                    result.getReturnTime() + ""));
                            initialized = true;
                        } else {
                            Log.i("Music Time Keeper", "Time responce received, offset was null");
                            return;
                        }

                    } catch (IOException e) {
                        Log.e("Music Time Keeper", "error  in time synchonisation");
                    } catch (Exception ex) {
                        Log.e("Music Time Keeper", "FATAL ERROR IN TIME SYNCHO");
                        ex.printStackTrace();

                        active = false;
                    }
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException ex) {
                        // do nothing
                    }
                }
            } catch (UnknownHostException e) {
                Log.w(LibTags.APPTAG, "Cannot resolve timekeeper server", e);
            }

        }

        public void kill() {
            active = false;
        }

        private void activate() {
            active = true;
        }

    }

}
