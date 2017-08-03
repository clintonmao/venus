package com.meidusa.venus.backend.invoker.support;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.VenusRouterPacket;

/**
 * Created by Zhangzhihua on 2017/8/2.
 */
public class ProviderInvocation {

    VenusFrontendConnection conn;

    Tuple<Long, byte[]> data;

    byte[] message;

    byte serializeType;

    byte packetSerializeType;

    String finalSourceIp;

    long waitTime;

    VenusRouterPacket routerPacket;

    public VenusFrontendConnection getConn() {
        return conn;
    }

    public void setConn(VenusFrontendConnection conn) {
        this.conn = conn;
    }

    public Tuple<Long, byte[]> getData() {
        return data;
    }

    public void setData(Tuple<Long, byte[]> data) {
        this.data = data;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public byte getPacketSerializeType() {
        return packetSerializeType;
    }

    public void setPacketSerializeType(byte packetSerializeType) {
        this.packetSerializeType = packetSerializeType;
    }

    public String getFinalSourceIp() {
        return finalSourceIp;
    }

    public void setFinalSourceIp(String finalSourceIp) {
        this.finalSourceIp = finalSourceIp;
    }

    public VenusRouterPacket getRouterPacket() {
        return routerPacket;
    }

    public void setRouterPacket(VenusRouterPacket routerPacket) {
        this.routerPacket = routerPacket;
    }

    public byte getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(byte serializeType) {
        this.serializeType = serializeType;
    }
}
