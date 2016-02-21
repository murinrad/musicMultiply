// ITimeKeeperClient.aidl
package net.murinrad.android.timekeeper;

// Declare any non-default types here with import statements

interface ITimeKeeperClient {
    void setServerInfo(String server, int port);

    void start();

    void stop();

    long getDelta();


}
