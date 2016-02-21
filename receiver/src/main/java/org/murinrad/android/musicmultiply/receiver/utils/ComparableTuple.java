package org.murinrad.android.musicmultiply.receiver.utils;

import org.murinrad.android.musicmultiply.org.murinrad.util.Tuple;

/**
 * Created by Rado on 10.5.2015.
 */
public class ComparableTuple extends Tuple<Integer, byte[]> implements Comparable {

    public ComparableTuple(Integer value, byte[] value2) {
        super(value, value2);
    }

    @Override
    public int compareTo(Object another) {
        if (another instanceof ComparableTuple) {
            return getFirst() - ((ComparableTuple) another).getFirst();
        }
        return -1;
    }
}
