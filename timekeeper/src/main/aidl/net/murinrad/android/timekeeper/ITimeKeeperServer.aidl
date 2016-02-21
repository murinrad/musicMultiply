// ITimeKeeperServer.aidl
package net.murinrad.android.timekeeper;

// Declare any non-default types here with import statements

interface ITimeKeeperServer {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void setPort(int portNumber);

    void start();

    void stop();
}
