// 
// Decompiled by Procyon v0.5.30
// 

package org.murinrad.android.musicmultiply.receiver.utils;

import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.Collection;
import java.util.Arrays;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PriorityCircularBlockingQueue<T extends Comparable> implements BlockingQueue<T>
{
    private int capacity;
    private PriorityBlockingQueue<T> delegate;

    public PriorityCircularBlockingQueue(final int capacity) {
        this.capacity = capacity;
        this.delegate = new PriorityBlockingQueue<T>(capacity + 1);
    }

    private void deleteElementIfNeeded() {
        if (this.delegate.size() == this.capacity + 1) {
            final Object[] array = this.toArray();
            Arrays.sort(array);
            final boolean remove = this.delegate.remove(array[this.capacity]);
            assert remove;
        }
    }

    @Override
    public boolean add(final T t) {
        synchronized (this) {
            final boolean add = this.delegate.add(t);
            this.deleteElementIfNeeded();
            return add;
        }
    }

    @Override
    public boolean addAll(final Collection<? extends T> collection) {
        final Iterator<? extends T> iterator = collection.iterator();
        while (iterator.hasNext()) {
            this.offer((T)iterator.next());
        }
        return true;
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public boolean contains(final Object o) {
        return this.delegate.contains(o);
    }

    @Override
    public boolean containsAll(final Collection<?> collection) {
        return this.delegate.containsAll(collection);
    }

    @Override
    public int drainTo(final Collection<? super T> collection) {
        return this.delegate.drainTo(collection);
    }

    @Override
    public int drainTo(final Collection<? super T> collection, final int n) {
        return this.delegate.drainTo(collection, n);
    }

    @Override
    public T element() {
        return this.delegate.element();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return this.delegate.iterator();
    }

    @Override
    public boolean offer(final T t) {
        synchronized (this) {
            final boolean offer = this.delegate.offer(t);
            this.deleteElementIfNeeded();
            return offer;
        }
    }

    @Override
    public boolean offer(final T t, final long n, final TimeUnit timeUnit) throws InterruptedException {
        synchronized (this) {
            return this.offer(t);
        }
    }

    @Override
    public T peek() {
        return this.delegate.peek();
    }

    @Override
    public T poll() {
        return this.delegate.poll();
    }

    @Override
    public T poll(final long n, final TimeUnit timeUnit) throws InterruptedException {
        return this.delegate.poll();
    }

    @Override
    public void put(final T t) throws InterruptedException {
        this.delegate.put(t);
        this.deleteElementIfNeeded();
    }

    @Override
    public int remainingCapacity() {
        return this.delegate.remainingCapacity();
    }

    @Override
    public T remove() {
        return this.delegate.remove();
    }

    @Override
    public boolean remove(final Object o) {
        return this.delegate.remove(o);
    }

    @Override
    public boolean removeAll(final Collection<?> collection) {
        return this.delegate.removeAll(collection);
    }

    @Override
    public boolean retainAll(final Collection<?> collection) {
        return this.delegate.retainAll(collection);
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public T take() throws InterruptedException {
        return this.delegate.take();
    }

    @Override
    public Object[] toArray() {
        return this.delegate.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] array) {
        return this.delegate.toArray(array);
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }
}
