package com.meidusa.venus.cache.trans;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public class BsonTransCoder implements Transcoder<Object> {

    public Object decode(CachedData arg0) {
        return null;
    }

    public CachedData encode(Object arg0) {
        return null;
    }

    public boolean isPackZeros() {
        return false;
    }

    public boolean isPrimitiveAsString() {
        return false;
    }

    public void setCompressionThreshold(int arg0) {

    }

    public void setPackZeros(boolean arg0) {

    }

    public void setPrimitiveAsString(boolean arg0) {

    }

}
