package com.meidusa.venus.bus;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.util.Range;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * bus调用对象
 * Created by Zhangzhihua on 2017/8/2.
 */
public class BusInvocation implements Invocation {

    private BusFrontendConnection srcConn;

    private byte[] message;

    private byte serializeType;

    private long clientId;

    private long clientRequestId;

    //venus相关id
    private String rpcId;

    private byte[] traceID;

    //athena相关id
    private byte[] athenaId;

    private byte[] parentId;

    private byte[] messageId;

    private String apiName;

    private String serviceInterfaceName;

    private String serviceName ;

    private String version;

    private String methodName;

    private String consumerIp;

    private String providerIp;

    private Date requestTime;

    private Date dispatchTime;

    private Date responseTime;

    public BusFrontendConnection getSrcConn() {
        return srcConn;
    }

    public void setSrcConn(BusFrontendConnection srcConn) {
        this.srcConn = srcConn;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
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

    public byte[] getTraceID() {
        return traceID;
    }

    public void setTraceID(byte[] traceID) {
        this.traceID = traceID;
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

    public byte[] getMessageId() {
        return messageId;
    }

    public void setMessageId(byte[] messageId) {
        this.messageId = messageId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    @Override
    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    public void setServiceInterfaceName(String serviceInterfaceName) {
        this.serviceInterfaceName = serviceInterfaceName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConsumerIp() {
        return consumerIp;
    }

    public void setConsumerIp(String consumerIp) {
        this.consumerIp = consumerIp;
    }

    public String getProviderIp() {
        return providerIp;
    }

    public void setProviderIp(String providerIp) {
        this.providerIp = providerIp;
    }

    public Date getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }

    public Date getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(Date dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public Date getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Date responseTime) {
        this.responseTime = responseTime;
    }

    @Override
    public String getMethodName() {
        return this.methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public byte getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(byte serializeType) {
        this.serializeType = serializeType;
    }
}
