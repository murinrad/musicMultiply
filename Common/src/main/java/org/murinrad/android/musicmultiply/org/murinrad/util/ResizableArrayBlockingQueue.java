package org.murinrad.android.musicmultiply.org.murinrad.util;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Rado on 26.4.2015.
 */
public class ResizableArrayBlockingQueue<E> implements BlockingQueue<E> {
    private static final int RESIZE_FACTOR = 2;
    ArrayBlockingQueue<E> dataHolder;

    public ResizableArrayBlockingQueue(int initialSize) {
        dataHolder = new ArrayBlockingQueue<E>(initialSize);
    }

    @Override
    public synchronized boolean add(E e) {
        if (dataHolder.remainingCapacity() == 0) {
            resize();
        }
        return dataHolder.add(e);
    }

    private void resize() {
        ArrayBlockingQueue<E> temp = dataHolder;
        dataHolder = new ArrayBlockingQueue<E>(dataHolder.size() * RESIZE_FACTOR);
        temp.drainTo(dataHolder);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> collection) {
        while (dataHolder.remainingCapacity() < collection.size()) {
            resize();
        }
        return dataHolder.addAll(collection);
    }

    @Override
    public synchronized void clear() {
        dataHolder.clear();

    }

    @Override
    public synchronized boolean offer(E e) {
        return add(e);
    }

    @Override
    public synchronized E remove() {
        return dataHolder.remove();
    }

    @Override
    public synchronized E poll() {
        return dataHolder.poll();
    }

    @Override
    public synchronized E element() {
        return dataHolder.element();
    }

    @Override
    public synchronized E peek() {
        return null;
    }

    @Override
    public synchronized void put(E e) throws InterruptedException {
        add(e);

    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public synchronized E take() throws InterruptedException {
        return dataHolder.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public int remainingCapacity() {
        return 1;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @NonNull
    @Override
    public <T> T[] toArray(T[] array) {
        return null;
    }

    @Override
    public boolean contains(Object o) {
        return dataHolder.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return dataHolder.containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return dataHolder.isEmpty();
    }

    @NonNull
    @Override
    public synchronized Iterator<E> iterator() {
        return dataHolder.iterator();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return 0;
    }


}
