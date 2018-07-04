package com.meidusa.venus.backend;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.ServerInvocationOperation;
import com.meidusa.venus.backend.services.EndpointItem;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.context.RequestContext;
import com.meidusa.venus.backend.services.ServiceObject;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.ServiceAPIPacket;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusUtil;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Date;

/**
 * server调用对象，由于协议定义不一致，暂无法统一client/server/bus invocation
 * Created by Zhangzhihua on 2017/8/2.
 */
public class ServerInvocation implements ServerInvocationOperation {

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

    private ServiceWrapper service;

    private EndpointWrapper endpoint;

    private Method method;

    private EndpointParameter[] params;

    private Object[] args;

    private InvocationListener invocationListener;

    private Type type;

    private Date requestTime;

    private String consumerApp;

    private String consumerIp;

    private String providerApp;

    private String providerIp;

    boolean async;

    //是否打印输入参数，默认true
    private boolean printParam = true;

    //是否打印输出结果，默认false
    private boolean printResult = false;

    //-----------------------ext--------------------------

    VenusFrontendConnection conn;

    /**
     * 服务端端点配置，非注释配置
     */
    EndpointItem endpointDef;

    SerializeServiceRequestPacket serviceRequestPacket;

    VenusRouterPacket routerPacket;

    ServiceAPIPacket apiPacket;

    ServicePacketBuffer packetBuffer;

    Tuple<Long, byte[]> data;

    byte[] message;

    int messageType;

    byte serializeType;

    byte packetSerializeType;

    String sourceIp;

    String routeIp;

    long waitTime;

    String localHost;

    String host;

    EndpointInvocation.ResultType resultType;

    RequestContext requestContext;

    //-------------bus分发使用---------

    String serviceInterfaceName;

    String serviceName;

    String methodName;

    String version;

    String apiName;

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

    public ServiceWrapper getService() {
        return service;
    }

    public void setService(ServiceWrapper service) {
        this.service = service;
    }

    public EndpointWrapper getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndpointWrapper endpoint) {
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

    public String getRouteIp() {
        return routeIp;
    }

    public void setRouteIp(String routeIp) {
        this.routeIp = routeIp;
    }

    public byte getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(byte serializeType) {
        this.serializeType = serializeType;
    }

    public EndpointItem getEndpointDef() {
        return endpointDef;
    }

    public void setEndpointDef(EndpointItem endpointDef) {
        this.endpointDef = endpointDef;
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

    @Override
    public String getServiceInterfaceName() {
        if(this.serviceInterfaceName != null){
            return this.serviceInterfaceName;
        }else if(endpointDef != null){
            ServiceObject service = endpointDef.getService();
            return service.getType().getName();
        }else{
            return "null";
        }
    }

    @Override
    public String getServiceName() {
        if(this.serviceName != null){
            return this.serviceName;
        }else if(endpointDef != null){
            return endpointDef.getService().getName();
        }else{
            return "null";
        }
    }

    @Override
    public String getMethodName() {
        if(this.getMethod() != null){
            return this.getMethod().getName();
        }else if(this.methodName != null){
            return this.methodName;
        }else{
            return null;
        }
    }

    public String getConsumerApp() {
        return consumerApp;
    }

    public void setConsumerApp(String consumerApp) {
        this.consumerApp = consumerApp;
    }

    public String getProviderApp() {
        return providerApp;
    }

    public void setProviderApp(String providerApp) {
        this.providerApp = providerApp;
    }

    public String getProviderIp() {
        return providerIp;
    }

    public void setProviderIp(String providerIp) {
        this.providerIp = providerIp;
    }

    public String getInvokeModel(){
        return "sync";
    }

    public String getServicePath(){
        return VenusUtil.getServicePath(this);
    }

    public String getMethodPath(){
        return VenusUtil.getMethodPath(this);
    }

    public void setServiceInterfaceName(String serviceInterfaceName) {
        this.serviceInterfaceName = serviceInterfaceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public SerializeServiceRequestPacket getServiceRequestPacket() {
        return serviceRequestPacket;
    }

    public void setServiceRequestPacket(SerializeServiceRequestPacket serviceRequestPacket) {
        this.serviceRequestPacket = serviceRequestPacket;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public ServiceAPIPacket getApiPacket() {
        return apiPacket;
    }

    public void setApiPacket(ServiceAPIPacket apiPacket) {
        this.apiPacket = apiPacket;
    }

    public ServicePacketBuffer getPacketBuffer() {
        return packetBuffer;
    }

    public void setPacketBuffer(ServicePacketBuffer packetBuffer) {
        this.packetBuffer = packetBuffer;
    }

    @Override
    public String getVersion() {
        if(StringUtils.isNotEmpty(version)){
            return this.version;
        }else if(this.endpointDef != null && this.endpointDef.getService() != null){
            return String.valueOf(this.endpointDef.getService().getVersion());
        }else{
            return null;
        }
    }

    public boolean isPrintParam() {
        return printParam;
    }

    public void setPrintParam(boolean printParam) {
        this.printParam = printParam;
    }

    public boolean isPrintResult() {
        return printResult;
    }

    public void setPrintResult(boolean printResult) {
        this.printResult = printResult;
    }
}
