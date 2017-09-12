package com.meidusa.venus;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.services.RequestContext;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;

/**
 * rpc调用对象，TODO 统一invocation
 * Created by Zhangzhihua on 2017/8/2.
 */
public class RpcInvocation extends Invocation{

    VenusFrontendConnection conn;

    VenusRouterPacket routerPacket;

    Tuple<Long, byte[]> data;

    byte[] message;

    byte serializeType;

    byte packetSerializeType;

    String finalSourceIp;

    long waitTime;

    SerializeServiceRequestPacket request;

    /**
     * 服务端端点配置，非注释配置 TODO 统一
     */
    Endpoint endpointEx;

    //extra

    String localHost;

    String host;

    EndpointInvocation.ResultType resultType;

    RequestContext requestContext;

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

    public SerializeServiceRequestPacket getRequest() {
        return request;
    }

    public void setRequest(SerializeServiceRequestPacket request) {
        this.request = request;
    }

    public Endpoint getEndpointEx() {
        return endpointEx;
    }

    public void setEndpointEx(Endpoint endpointEx) {
        this.endpointEx = endpointEx;
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

    public EndpointInvocation.ResultType getResultType() {
        return resultType;
    }

    public void setResultType(EndpointInvocation.ResultType resultType) {
        this.resultType = resultType;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }
}
