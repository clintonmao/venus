package com.meidusa.venus.backend.support;

import com.meidusa.venus.Result;
import com.meidusa.venus.backend.ServerInvocation;
import com.meidusa.venus.backend.services.EndpointItem;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.ServiceAPIPacket;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.notify.InvocationListener;

/**
 * 响应包装处理对象
 * Created by Zhangzhihua on 2017/8/23.
 */
public class ServerResponseWrapper {
    VenusFrontendConnection conn;
    ServiceAPIPacket apiPacket;
    VenusRouterPacket routerPacket;
    EndpointItem endpoint;
    SerializeServiceRequestPacket request;
    InvocationListener invocationListener;
    short serializeType;
    Result result;
    boolean athenaFlag;

    public ServerResponseWrapper(){
    }

    public static ServerResponseWrapper parse(ServerInvocation invocation, Result result, boolean athenaFlag){
        ServerResponseWrapper wrapper = new ServerResponseWrapper();
        wrapper.setConn(invocation.getConn());
        wrapper.setRouterPacket(invocation.getRouterPacket());
        wrapper.setApiPacket(invocation.getApiPacket());
        wrapper.setRequest(invocation.getServiceRequestPacket());
        wrapper.setEndpoint(invocation.getEndpointDef());
        wrapper.setInvocationListener(invocation.getInvocationListener());
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

    public EndpointItem getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndpointItem endpoint) {
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

    public InvocationListener getInvocationListener() {
        return invocationListener;
    }

    public void setInvocationListener(InvocationListener invocationListener) {
        this.invocationListener = invocationListener;
    }

    public ServiceAPIPacket getApiPacket() {
        return apiPacket;
    }

    public void setApiPacket(ServiceAPIPacket apiPacket) {
        this.apiPacket = apiPacket;
    }
}
