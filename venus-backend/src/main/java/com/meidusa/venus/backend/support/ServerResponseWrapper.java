package com.meidusa.venus.backend.support;

import com.meidusa.venus.Result;
import com.meidusa.venus.ServerInvocation;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;

/**
 * 响应包装处理对象
 * Created by Zhangzhihua on 2017/8/23.
 */
public class ServerResponseWrapper {
    VenusFrontendConnection conn;
    VenusRouterPacket routerPacket;
    Endpoint endpoint;
    SerializeServiceRequestPacket request;
    short serializeType;
    Result result;
    boolean athenaFlag;

    public ServerResponseWrapper(){
    }

    public static ServerResponseWrapper parse(ServerInvocation invocation, Result result, boolean athenaFlag){
        ServerResponseWrapper wrapper = new ServerResponseWrapper();
        wrapper.setConn(invocation.getConn());
        wrapper.setRouterPacket(invocation.getRouterPacket());
        wrapper.setEndpoint(invocation.getEndpointDef());
        wrapper.setRequest(invocation.getRequest());
        wrapper.setSerializeType(invocation.getSerializeType());
        wrapper.setResult(result);
        wrapper.setAthenaFlag(athenaFlag);
        return wrapper;
    }

    public VenusFrontendConnection getConn() {
        return conn;
    }

    public void setConn(VenusFrontendConnection conn) {
        this.conn = conn;
    }

    public VenusRouterPacket getRouterPacket() {
        return routerPacket;
    }

    public void setRouterPacket(VenusRouterPacket routerPacket) {
        this.routerPacket = routerPacket;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public SerializeServiceRequestPacket getRequest() {
        return request;
    }

    public void setRequest(SerializeServiceRequestPacket request) {
        this.request = request;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public short getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(short serializeType) {
        this.serializeType = serializeType;
    }

    public boolean isAthenaFlag() {
        return athenaFlag;
    }

    public void setAthenaFlag(boolean athenaFlag) {
        this.athenaFlag = athenaFlag;
    }
}
