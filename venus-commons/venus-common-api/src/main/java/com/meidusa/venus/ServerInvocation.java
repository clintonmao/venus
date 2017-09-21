package com.meidusa.venus;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.services.RequestContext;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Date;

/**
 * server调用对象，由于协议定义不一致，暂无法统一client/server/bus invocation
 * Created by Zhangzhihua on 2017/8/2.
 */
public class ServerInvocation implements Invocation {

    private int clientId;

    private long clientRequestId;

    //venus相关id
    private String rpcId;

    private byte[] traceID;

    //athena相关id
    private byte[] athenaId;

    private byte[] parentId;

    private byte[] messageId;

    private Class<?> serviceInterface;

    private Service service;

    private com.meidusa.venus.annotations.Endpoint endpoint;

    private Method method;

    private EndpointParameter[] params;

    private Object[] args;

    private InvocationListener invocationListener;

    private Type type;

    private Date requestTime;

    //consumerIp
    private String consumerIp;

    boolean async;

    //-----------------------ext--------------------------

    VenusFrontendConnection conn;

    SerializeServiceRequestPacket request;

    VenusRouterPacket routerPacket;

    Tuple<Long, byte[]> data;

    byte[] message;

    byte serializeType;

    byte packetSerializeType;

    String finalSourceIp;

    long waitTime;

    /**
     * 服务端端点配置，非注释配置 TODO 统一
     */
    Endpoint endpointEx;

    //extra

    String localHost;

    String host;

    EndpointInvocation.ResultType resultType;

    RequestContext requestContext;

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public long getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(long clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public String getRpcId() {
        return rpcId;
    }

    public void setRpcId(String rpcId) {
        this.rpcId = rpcId;
    }


    public String getConsumerIp() {
        return consumerIp;
    }

    public void setConsumerIp(String consumerIp) {
        this.consumerIp = consumerIp;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public com.meidusa.venus.annotations.Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(com.meidusa.venus.annotations.Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public EndpointParameter[] getParams() {
        return params;
    }

    public void setParams(EndpointParameter[] params) {
        this.params = params;
    }

    public byte[] getTraceID() {
        return traceID;
    }

    public void setTraceID(byte[] traceID) {
        this.traceID = traceID;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public byte[] getMessageId() {
        return messageId;
    }

    public void setMessageId(byte[] messageId) {
        this.messageId = messageId;
    }

    public InvocationListener getInvocationListener() {
        return invocationListener;
    }

    public void setInvocationListener(InvocationListener invocationListener) {
        this.invocationListener = invocationListener;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Date getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }

    public byte[] getAthenaId() {
        return athenaId;
    }

    public void setAthenaId(byte[] athenaId) {
        this.athenaId = athenaId;
    }

    public byte[] getParentId() {
        return parentId;
    }

    public void setParentId(byte[] parentId) {
        this.parentId = parentId;
    }

    public String getServiceName() {
        if(service != null){
            return service.name();
        }else if(this instanceof ServerInvocation){
            ServerInvocation rpcInvocation = this;
            if(rpcInvocation.getEndpointEx() != null && rpcInvocation.getEndpointEx().getService() != null){
                return rpcInvocation.getEndpointEx().getService().getName();
            }else{
                throw new RpcException("get serviceName error.");
            }
        }else{
            throw new RpcException("get serviceName error.");
        }
    }

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
