package com.meidusa.venus.client.invoker.venus.encode;

public class DefaultEncoderFactory {

    public static BaseEncoder newInstance(String type){
        if("venus".equalsIgnoreCase(type)){
            return new VenusEncoder();
        }else if("json".equalsIgnoreCase(type)){
            return new HttpEncoder();
        }else {
            throw new RuntimeException("unspport encode type:" + type);
        }
    }
}
