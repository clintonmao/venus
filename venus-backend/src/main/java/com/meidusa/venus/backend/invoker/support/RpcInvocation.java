package com.meidusa.venus.backend.invoker.support;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.Invocation;

/**
 * rpc调用对象，TODO 统一invocation
 * Created by Zhangzhihua on 2017/8/2.
 */
public class RpcInvocation extends Invocation{

    VenusFrontendConnection conn;

    Tuple<Long, byte[]> data;

    byte[] message;

    byte serializeType;

    byte packetSerializeType;

    String finalSourceIp;

    long waitTime;

    VenusRouterPacket routerPacket;

    SerializeServiceRequestPacket serviceRequestPacket;

    Endpoint ep;

    //extra

    String localHost;

    String host;

    int clientId;

    EndpointInvocation.ResultType resultType;

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

    public SerializeServiceRequestPacket getServiceRequestPacket() {
        return serviceRequestPacket;
    }

    public void setServiceRequestPacket(SerializeServiceRequestPacket serviceRequestPacket) {
        this.serviceRequestPacket = serviceRequestPacket;
    }

    public Endpoint getEp() {
        return ep;
    }

    public void setEp(Endpoint ep) {
        this.ep = ep;
    }

    public String getLocalHost() {
        return localHost;
    }

    public void setLocalHost(String localHost) {
        this.localHost = localHost;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public EndpointInvocation.ResultType getResultType() {
        return resultType;
    }

    public void setResultType(EndpointInvocation.ResultType resultType) {
        this.resultType = resultType;
    }
}
