package com.meidusa.venus;

import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Date;

/**
 * client请求对象
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClientInvocation implements Invocation {

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

    private Endpoint endpoint;

    private Method method;

    private EndpointParameter[] params;

    private Object[] args;

    private InvocationListener invocationListener;

    private Type type;

    private Date requestTime;

    //消费方应用名称
    private String consumerApp;

    //消费方ip
    private String consumerIp;

    boolean async;

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

    public String getConsumerApp() {
        return consumerApp;
    }

    public void setConsumerApp(String consumerApp) {
        this.consumerApp = consumerApp;
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

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
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

    @Override
    public String getServiceInterfaceName() {
        return serviceInterface.getName();
    }

    @Override
    public String getServiceName() {
        return service.name();
    }

    @Override
    public String getVersion() {
        return "0.0.0";//TODO 版本号处理
    }

    @Override
    public String getMethodName() {
        return this.getMethod().getName();
    }

}
