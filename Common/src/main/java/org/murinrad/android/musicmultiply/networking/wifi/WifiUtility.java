package org.murinrad.android.musicmultiply.networking.wifi;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.whitebyte.wifihotspotutils.WifiApManager;

import org.murinrad.android.musicmultiply.org.murinrad.util.GenericCallback;

/**
 * Created by Radovan Murin on 14.6.2015.
 */
public class WifiUtility {

    private static String WIFI_NAME = "MusicMMInternalWifi";

    private static String WIFI_PASSW = "BolaBabkaMalaCapka";

    public static boolean isConnectedToWiFi(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();

    }

    public static void setupAP(final Context ctx, final GenericCallback callback) {
        AsyncTask<Void, Void, Void> connectToAPTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    WifiApManager apMgr = new WifiApManager(ctx);
                    WifiConfiguration wifiConfiguration = new WifiConfiguration();
                    wifiConfiguration.hiddenSSID = true;
                    wifiConfiguration.SSID = String.format("\"%s\"", WIFI_NAME);
                    wifiConfiguration.preSharedKey = String.format("\"%s\"", WIFI_PASSW);
                    apMgr.setWifiApEnabled(wifiConfiguration, true);
                    callback.setSuccess(true);
                } catch (Exception ex) {
                    Log.e(WifiUtility.class.getCanonicalName(), "There was a problem while connecting to the wifi", ex);
                    callback.setSuccess(false);
                }
                callback.onCallback();
                return null;
            }
        };
        connectToAPTask.execute(null, null, null);
    }

    public static void connectToAP(final Context ctx, final GenericCallback callback) {
        AsyncTask<Void, Void, Void> connectToAPTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    WifiConfiguration wifiConfig = new WifiConfiguration();
                    wifiConfig.SSID = String.format("\"%s\"", WIFI_NAME);
                    wifiConfig.preSharedKey = String.format("\"%s\"", WIFI_PASSW);

                    WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
                    //remember id
                    int netId = wifiManager.addNetwork(wifiConfig);
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();
                    callback.setSuccess(true);
                } catch (Exception ex) {
                    Log.e(WifiUtility.class.getCanonicalName(), "There was a problem while connecting to the wiif", ex);
                    callback.setSuccess(false);
                }
                callback.onCallback();
                return null;
            }
        };
        connectToAPTask.execute(null, null, null);

    }

    public static void disableAP(final Context ctx, final GenericCallback callback) {
        AsyncTask<Void, Void, Void> connectToAPTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    WifiApManager apMgr = new WifiApManager(ctx);
                    apMgr.setWifiApEnabled(null, false);
                } catch (Exception ex) {
                    Log.e(WifiUtility.class.getCanonicalName(), "There was a problem while connecting to the wiif", ex);
                    if (callback != null)
                        callback.setSuccess(false);
                }
                if (callback != null)
                    callback.onCallback();
                return null;
            }
        };
        connectToAPTask.execute(null, null, null);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void deregisterWifiListener(ConnectivityManager.NetworkCallback networkChangeBroadcastListener, Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        connManager.unregisterNetworkCallback(networkChangeBroadcastListener);


    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void registerNetworkListener(ConnectivityManager.NetworkCallback networkChangeBroadcastListener, Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        connManager.registerNetworkCallback(new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), networkChangeBroadcastListener);

    }
}
