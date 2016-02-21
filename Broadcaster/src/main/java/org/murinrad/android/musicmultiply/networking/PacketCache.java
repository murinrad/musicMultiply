package org.murinrad.android.musicmultiply.networking;

import org.murinrad.android.musicmultiply.org.murinrad.util.Tuple;

import java.util.HashMap;

/**
 * Created by Rado on 10.5.2015.
 */
public class PacketCache {

    private int size;
    private HashMap<Integer, Tuple<Integer, byte[]>> cache;

    public PacketCache(int size) {
        this.size = size;
        cache = new HashMap<>();
    }

    public void put(int key, byte[] data) {
        Tuple<Integer, byte[]> tuple = new Tuple<>(key, data);
        cache.put(calculateHash(key), tuple);

    }

    public byte[] retrieve(int key) throws CacheMissException {
        Tuple<Integer, byte[]> tuple = cache.get(calculateHash(key));
        if (tuple != null && tuple.getFirst() == key)
            return tuple.getSecond();
        throw new CacheMissException();
    }

    private int calculateHash(int data) {
        return data % size;
    }


    public class CacheMissException extends Exception {

    }
}
