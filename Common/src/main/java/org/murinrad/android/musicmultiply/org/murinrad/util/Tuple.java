package org.murinrad.android.musicmultiply.org.murinrad.util;

public class Tuple<T, E> {
    private final T first;
    private final E second;

    public Tuple(T value, E value2) {
        first = value;
        second = value2;
    }

    public T getFirst() {
        return first;
    }

    public E getSecond() {
        return second;
    }
}
