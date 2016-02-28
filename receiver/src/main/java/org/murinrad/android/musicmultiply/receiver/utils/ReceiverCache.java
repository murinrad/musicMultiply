package org.murinrad.android.musicmultiply.receiver.utils;

import org.murinrad.android.musicmultiply.receiver.utils.PriorityCircularBlockingQueue;

import java.util.concurrent.TimeUnit;

/**
 * Created by Radovan Murin on 10.5.2015.
 */
public class ReceiverCache extends PriorityCircularBlockingQueue<ComparableTuple> {

    int lastPacketPopped = -1;

    public ReceiverCache(int size) {
        super(size);
    }

    @Override
    public boolean add(ComparableTuple comparableTuple) {
        if (comparableTuple.getFirst() <= lastPacketPopped) return false;
        return super.add(comparableTuple);
    }

    @Override
    public boolean offer(ComparableTuple comparableTuple) {
        if (comparableTuple.getFirst() <= lastPacketPopped) return false;
        return super.offer(comparableTuple);
    }

    @Override
    public void put(ComparableTuple comparableTuple) throws InterruptedException {
        if (comparableTuple.getFirst() <= lastPacketPopped) return;
        super.put(comparableTuple);
    }

    @Override
    public boolean offer(ComparableTuple comparableTuple, long timeout, TimeUnit unit) throws InterruptedException {
        if (comparableTuple.getFirst() <= lastPacketPopped) return false;
        return super.offer(comparableTuple, timeout, unit);
    }

    @Override
    public ComparableTuple take() throws InterruptedException {
        ComparableTuple retVal = super.take();
        lastPacketPopped = retVal.getFirst();
        return retVal;
    }

    @Override
    public ComparableTuple poll() {
        ComparableTuple retVal = super.poll();
        lastPacketPopped = retVal.getFirst();
        return retVal;
    }

    @Override
    public ComparableTuple poll(long timeout, TimeUnit unit) throws InterruptedException {
        ComparableTuple retVal = super.poll(timeout, unit);
        lastPacketPopped = retVal.getFirst();
        return retVal;
    }

    public void reset() {
        clear();
        lastPacketPopped = -1;
    }
}
