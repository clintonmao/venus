package com.meidusa.venus.client.rpcclient;

import com.meidusa.fastjson.JSON;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.client.invoker.ClientInvokerProxy;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterContext;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class JsonRpc {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    private ClientInvokerProxy clientInvokerProxy = new ClientInvokerProxy();

    private static AtomicLong sequenceId = new AtomicLong(1);

    private String appName;

    private Register register;

    private static Map<String,String> serviceLoadMapping = new ConcurrentHashMap<>();

    private JsonRpc(){}

    public JsonRpc(String appName,Register register){
        this.appName = appName;
        if(StringUtils.isBlank(this.appName)){
            this.appName = VenusContext.getInstance().getApplication();
        }
        if(StringUtils.isBlank(this.appName)){
            throw new IllegalArgumentException("appName not allow blank.");
        }
        this.register = register;
        if(this.register == null){
            this.register = RegisterContext.getInstance().getRegister();
        }
        if(this.register == null){
            throw new IllegalArgumentException("registry not allow blank.");
        }
    }

    public String invoke(String serviceName,String endpointName,Map<String,Object> parameterMap){
        return invoke(serviceName,endpointName,null,parameterMap);
    }

    public String invoke(String serviceName,String endpointName,String version,Map<String,Object> parameterMap){
        long bTime = System.currentTimeMillis();
        ClientInvocation invocation = null;
        String ret = "";
        String exception = "";
        try {
            //加载服务定义
            loadServiceDef(serviceName,serviceName,version);

            //构造请求
            invocation = buildInvocation(serviceName,endpointName,version,parameterMap);

            Result result = doInvoke(invocation);
            if(result.getErrorCode() == 0){
                Object object = result.getResult();
                ret = JSON.toJSONString(object);
                return ret;
            }else{
                //TODO 错误处理优化
                Map<String,String> error = new HashMap<>();
                error.put("errorCode",String.valueOf(result.getErrorCode()));
                error.put("errorMsg",result.getErrorMessage());
                exception = JSON.toJSONString(error);
                return exception;
            }
        } catch (Throwable ex) {
            //TODO 错误处理优化
            Map<String,String> error = new HashMap<>();
            error.put("errorCode","500");
            error.put("errorMsg",ex.getMessage());
            exception = JSON.toJSONString(error);
            return exception;
        } finally {
            printTracerLogger(invocation,parameterMap,ret,exception,bTime);
        }
    }

    Result doInvoke(ClientInvocation invocation) {
        //通过代理调用服务
        Result result = getClientInvokerProxy().invoke(invocation,null);
        return result;
    }

    /**
     * 加载服务定义
     * @param serviceInterfaceName
     * @param serviceName
     * @param version
     */
    void loadServiceDef(String serviceInterfaceName,String serviceName,String version){
        String api = new StringBuffer().append("/").append(serviceInterfaceName).append("/").append(serviceName)
                .toString();
        if(serviceLoadMapping.get(api) != null){
            return;
        }

        try{
            StringBuffer buf = new StringBuffer();
            buf.append("/").append(serviceName);
            buf.append("/").append(serviceName);
            buf.append("?application=").append(appName);
            buf.append("&host=").append(NetUtil.getLocalIp());
            String subscribleUrl = buf.toString();
            URL url = URL.parse(subscribleUrl);

            register.subscrible(url);
            register.load();

            serviceLoadMapping.put(api,api);
        }catch (Throwable ex){
            exceptionLogger.error("subscrible service error.",ex);
        }

    }

    /**
     * 构造请求
     * @param serviceName
     * @param endpointName
     * @param version
     * @param parameterMap
     * @return
     */
    ClientInvocation buildInvocation(String serviceName,String endpointName,String version,Map<String,Object> parameterMap){
        ClientInvocation invocation = new ClientInvocation();
        invocation.setServiceName(serviceName);
        invocation.setEndpointName(endpointName);
        invocation.setApiName(serviceName + "." + endpointName);
        if(StringUtils.isBlank(version)){
            version = "0";
        }
        invocation.setVersion(version);
        invocation.setParameterMap(parameterMap);
        //其它设置
        invocation.setRequestTime(new Date());
        String consumerApp = this.appName;
        invocation.setConsumerApp(consumerApp);
        invocation.setConsumerIp(NetUtil.getLocalIp(true));
        invocation.setAsync(false);
        //clientId
        invocation.setClientId(PacketConstant.VENUS_CLIENT_ID);
        invocation.setClientRequestId(sequenceId.getAndIncrement());
        //设置rpcId
        invocation.setRpcId(RpcIdUtil.getRpcId(invocation.getClientId(),invocation.getClientRequestId()));
        //设置traceId
        byte[] traceID = VenusTracerUtil.getTracerID();
        if (traceID == null) {
            traceID = VenusTracerUtil.randomTracerID();
        }
        invocation.setTraceID(traceID);
        invocation.setLookupType(1);

        return invocation;
    }

    /**
     * 获取client调用代理
     * @return
     */
    public ClientInvokerProxy getClientInvokerProxy() {
        VenusContext.getInstance().setEncodeType("json");
        clientInvokerProxy.setRegister(RegisterContext.getInstance().getRegister());
        return clientInvokerProxy;
    }

    /**
     * 输出tracer日志
     * @param invocation
     * @param parameterMap
     * @param result
     * @param exception
     * @param bTime
     */
    void printTracerLogger(ClientInvocation invocation,Map<String,Object> parameterMap, String result, String exception, long bTime){
        long usedTime = System.currentTimeMillis() - bTime;
        String rpcId = invocation.getRpcId();
        String methodPath = invocation.getMethodPath();
        String param = "";
        //参数
        if(parameterMap != null){
            param = JSON.toJSONString(parameterMap);
        }
        //结果
        if(StringUtils.isBlank(result)){
            result = "";
        }
        //异常
        if(StringUtils.isBlank(exception)){
            exception = "";
        }
        //状态
        String status = "";
        boolean hasException = false;
        if(StringUtils.isNotBlank(exception)){
            hasException = true;
        }
        if(hasException){
            status = "failed";
        }else if(usedTime > 1000){
            status = ">1000ms";
        }else if(usedTime > 500){
            status = ">500ms";
        }else if(usedTime > 200){
            status = ">200ms";
        }else{
            status = "<200ms";
        }

        //输出日志
        Logger trLogger = tracerLogger;
        if(VenusUtil.isAthenaInterface(invocation)){
            trLogger = logger;
        }
        if(hasException){
            String tpl = "[C] [{},{}],consumer invoke,rpcId:{},method:{},param:{},error:{}";
            Object[] arguments = new Object[]{
                    status,
                    usedTime + "ms",
                    rpcId,
                    methodPath,
                    param,
                    exception
            };

            if(trLogger.isErrorEnabled()){
                trLogger.error(tpl,arguments);
            }
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error(tpl,arguments);
            }
        }else{
            String tpl = "[C] [{},{}],consumer invoke,rpcId:{},method:{},param:{},result:{}.";
            Object[] arguments = new Object[]{
                    status,
                    usedTime + "ms",
                    rpcId,
                    methodPath,
                    param,
                    result
            };
            if(trLogger.isInfoEnabled()){
                trLogger.info(tpl,arguments);
            }
        }

    }


}
