package com.meidusa.venus.client;

import com.meidusa.venus.ClientInvocationOperation;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusConstants;
import com.meidusa.venus.support.VenusUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

/**
 * client请求对象
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClientInvocation implements ClientInvocationOperation {

    //------------共通参数----------------
    private int clientId;

    private long clientRequestId;

    //venus相关id
    private String rpcId;

    private byte[] traceID;

    //athena相关id
    private byte[] athenaId;

    private byte[] parentId;

    private byte[] messageId;

    //接口名称
    private String serviceInterfaceName;

    //服务名称
    private String serviceName;

    //版本号
    private String version;

    //endpoint名称
    private String endpointName;

    //api(serviceName+"."+endpointName)
    private String apiName;

    //--------------rpc调用相关参数------------------
    //服务接口
    private Class<?> serviceInterface;

    private ServiceWrapper service;

    private EndpointWrapper endpoint;

    private EndpointParameter[] endpointParameters;

    private Method method;

    private Object[] args;

    //callback回调listener
    private InvocationListener invocationListener;

    private Type paramType;

    //------------http调用相关参数------------------
    private Map<String, Object> parameterMap = null;

    //------------其它共通相关参数-------------------
    private Date requestTime;

    //消费方应用名称
    private String consumerApp;

    //消费方ip
    private String consumerIp;

    //是否异步
    boolean async;

    //寻址方式，默认本地，0:本地;1:注册中心
    private int lookupType = 0;

    //超时时间，默认30000ms
    private int timeout = VenusConstants.TIMEOUT_DEFAULT;

    //重试次数，若retries不为空，则cluster默认开启failover
    private int retries = VenusConstants.RETRIES_DEFAULT;

    //连接数，默认8（一个服务）
    private int coreConnections = VenusConstants.CONNECTION_DEFAULT_COUNT;

    //集群容错策略，默认fastfail
    private String cluster = VenusConstants.CLUSTER_DEFAULT;

    //负载均衡策略,默认random
    private String loadbalance = VenusConstants.LOADBALANCE_DEFAULT;

    private boolean isAthenaInvoker = false;

    //是否打印输入参数，默认true
    private boolean printParam = true;

    //是否打印输出结果，默认true
    private boolean printResult = true;

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

    public EndpointParameter[] getEndpointParameters() {
        return endpointParameters;
    }

    public void setEndpointParameters(EndpointParameter[] endpointParameters) {
        this.endpointParameters = endpointParameters;
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

    public Type getParamType() {
        return paramType;
    }

    public void setParamType(Type paramType) {
        this.paramType = paramType;
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
        if(serviceInterface != null){
            return serviceInterface.getName();
        }else{
            return serviceName;
        }
    }

    @Override
    public String getMethodName() {
        if(getMethod() != null){
            return this.getMethod().getName();
        }{
            return endpointName;
        }
    }

    public int getLookupType() {
        return lookupType;
    }

    public void setLookupType(int lookupType) {
        this.lookupType = lookupType;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    /**
     * 获取服务路径
     * @return
     */
    public String getServicePath(){
        return VenusUtil.getServicePath(this);
    }

    /**
     * 获取方法路径
     * @return
     */
    public String getMethodPath(){
        return VenusUtil.getMethodPath(this);
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getCoreConnections() {
        return coreConnections;
    }

    public void setCoreConnections(int coreConnections) {
        this.coreConnections = coreConnections;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInvokeModel(){
        return "sync";
    }

    public String getApiName() {
        //VenusUtil.getApiName(method,service,endpoint);
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public boolean isAthenaInvoker() {
        return isAthenaInvoker;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
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

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map<String, Object> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public void setServiceInterfaceName(String serviceInterfaceName) {
        this.serviceInterfaceName = serviceInterfaceName;
    }
}
