package org.murinrad.android.musicmultiply.receiver.qos;

import android.util.Log;

import org.murinrad.android.musicmultiply.org.murinrad.util.Tuple;
import org.murinrad.android.musicmultiply.receiver.MainActivity;
import org.murinrad.android.musicmultiply.receiver.Receiver;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Rado on 26.4.2015.
 */
public class QoSController {

    public static final int THRESHOLD_FOR_PACKET_RESENDING = 3;
    private static final int QoS_SCHEDULE = 5000;
    BlockingQueue<Long> delayReportQueue;
    BlockingQueue<Tuple<Integer, Integer>> packetLossQueue;
    Thread delayProcessor, packetLossProcessor;
    Timer sendReportsTask;
    QosMessenger messenger;
    boolean isThereNewData = false;
    Thread.UncaughtExceptionHandler handlerOfThreadExceptions = new QoSControllerExceptionHandler();
    Set<WeakReference<QoSEventReceiver>> eventReceivers = new HashSet<>();
    private int averageDelay = 0;
    private int packetLoss = 0;


    public QoSController() {
        delayReportQueue = new ArrayBlockingQueue<Long>(150);
        packetLossQueue = new ArrayBlockingQueue<>(150);
        delayProcessor = new Thread(new DelayAnalyser());
        delayProcessor.setUncaughtExceptionHandler(handlerOfThreadExceptions);
        packetLossProcessor = new Thread(new PacketLossAnalyser());
        packetLossProcessor.setUncaughtExceptionHandler(handlerOfThreadExceptions);
        //  readLock = new ReentrantLock();
        messenger = new QosMessenger(null);
        //register a logger to the events
        eventReceivers.add(new WeakReference<QoSEventReceiver>(new LoggerForQosEvents()));

    }

    public void setServer(String server) {
        messenger = new QosMessenger(server);
    }

    public void reportDelay(long delay) {
        delayReportQueue.offer(delay);
    }

    public void reportPacketLoss(int lastReceived, int nowHave) {
        Tuple<Integer, Integer> tuple = new Tuple<>(lastReceived, nowHave);
        packetLossQueue.offer(tuple);
    }

    public void start() {
        delayProcessor.start();
        packetLossProcessor.start();
        sendReportsTask = new Timer(true);
        sendReportsTask.schedule(new QosSender(), 0, QoS_SCHEDULE);

    }

    public void stop() {
        delayProcessor.interrupt();
        packetLossProcessor.interrupt();
        sendReportsTask.cancel();
    }

    public void registerEventReceiver(QoSEventReceiver receiver) {
        eventReceivers.add(new WeakReference<QoSEventReceiver>(receiver));
    }

    public void deregisterEventReceiver(QoSEventReceiver receiver) {
        Iterator iterator = eventReceivers.iterator();
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

    private class PacketLossAnalyser implements Runnable {
        private static final int WINDOWS_SIZE = 150;
        boolean timeKeeperDeactivated = false;
        private Queue<Integer> data = new LinkedList<>();

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Tuple<Integer, Integer> tuple = packetLossQueue.take();
                    int lostPackets = tuple.getSecond() - tuple.getFirst();
                    data.add(lostPackets);
                    //  boolean release = readLock.tryLock();
                    isThereNewData = true;
                    packetLoss += lostPackets;
                    if (data.size() >= WINDOWS_SIZE) {
                        packetLoss -= data.remove();
                    }
                    if (lostPackets >= THRESHOLD_FOR_PACKET_RESENDING) {
                        messenger.requestPacketResend(tuple.getFirst(), tuple.getSecond());
                    }
                    //    if (release)
                    //        readLock.unlock();


                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    private class DelayAnalyser implements Runnable {
        private int delayReports = 0;
        private long totalDelay = 0;

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    long delay = delayReportQueue.take();
                    delayReports++;
                    totalDelay += delay;
                    // boolean release = readLock.tryLock();
                    isThereNewData = true;
                    averageDelay = Math.round((float) totalDelay / (float) delayReports);
                    // if (release) readLock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class QosSender extends TimerTask {

        @Override
        public void run() {
            if (!isThereNewData) return;
            //  readLock.lock();
            try {
                isThereNewData = false;
                messenger.sendQoSMessage(averageDelay, packetLoss);
            } catch (Exception ex) {
                Log.w(MainActivity.APP_TAG, "QosSender failure", ex);
            }
            // readLock.unlock();
        }
    }

    private class QoSControllerExceptionHandler implements Thread.UncaughtExceptionHandler {


        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e(Receiver.APP_TAG, "Uncaught exception in thread", ex);
        }
    }


    private class LoggerForQosEvents implements QoSEventReceiver {

        @Override
        public void onPacketLossIncrease() {
            Log.i("LoggerForQosEvents", "onPacketLossIncrease called");
        }

        @Override
        public void onPacketLossRecovery() {
            Log.i("LoggerForQosEvents", "onPacketLossIncrease called");

        }

        @Override
        public void onDelayTooLong() {
            Log.i("LoggerForQosEvents", "onPacketLossIncrease called");

        }
    }


}
