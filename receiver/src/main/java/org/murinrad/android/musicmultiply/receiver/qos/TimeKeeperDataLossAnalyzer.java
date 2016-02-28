package org.murinrad.android.musicmultiply.receiver.qos;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Radovan Murin on 7.6.2015.
 */
public class TimeKeeperDataLossAnalyzer {

    public static final int THRESHOLD_FOR_TIMEKEEPER_DEACTIVATION = 5;
    public static final int THREASHOLD_FOR_TIMEKEEPER_ACTIVATION = 1;
    TimerTask task;
    Set<WeakReference<QoSEventReceiver>> listeners = new HashSet<>();
    boolean timeKeeperDeactivated = false;
    Timer runner;
    private int loss = 0;

    public TimeKeeperDataLossAnalyzer(int period) {
        task = new TimerTask() {
            @Override
            public void run() {
                int packetsLostInTimeFrame = loss;
                reset();
                if (packetsLostInTimeFrame >= THRESHOLD_FOR_TIMEKEEPER_DEACTIVATION && !timeKeeperDeactivated) {
                    notifyPacketLossHigh();
                    timeKeeperDeactivated = true;
                    Log.i(this.getClass().getName(), "Detected high packet loss due to timeKeeper, deactivating... loss:" + packetsLostInTimeFrame);
                }
                if (packetsLostInTimeFrame <= THREASHOLD_FOR_TIMEKEEPER_ACTIVATION && timeKeeperDeactivated) {
                    notifyPacketLossRecovery();
                    timeKeeperDeactivated = false;
                    Log.i(this.getClass().getName(), "Detected lower packet loss due to timeKeeper, activating... loss:" + packetsLostInTimeFrame);
                }
                Log.d(this.getClass().getName(), "Packet loss due to timeKeeper: " + packetsLostInTimeFrame);


            }
        };
        runner = new Timer();
        runner.schedule(task, 0, period);

    }

    public void stop() {
        runner.cancel();
    }

    public synchronized void reportDataLoss(int loss) {
        this.loss += loss;

    }

    private synchronized void reset() {
        loss = 0;
    }

    public void registerEventReceiver(QoSEventReceiver receiver) {
        listeners.add(new WeakReference<QoSEventReceiver>(receiver));
    }

    public void deregisterEventReceiver(QoSEventReceiver receiver) {
        Iterator iterator = listeners.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            WeakReference<QoSEventReceiver> item = (WeakReference<QoSEventReceiver>) iterator.next();
            if (item.get() != null) {
                if (item.get() == receiver) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            } else {
                iterator.remove();
            }
        }

        if (!removed) {
            Log.w("QosController", "Attempted to remove a QoSEventReceiver which was not registered.");
        }

    }

    private void notifyPacketLossRecovery() {
        for (WeakReference<QoSEventReceiver> receiver : listeners) {
            if (receiver.get() != null) {
                receiver.get().onPacketLossRecovery();
            }
        }
    }

    private void notifyPacketLossHigh() {
        for (WeakReference<QoSEventReceiver> receiver : listeners) {
            if (receiver.get() != null) {
                receiver.get().onPacketLossIncrease();
            }
        }
    }


}
