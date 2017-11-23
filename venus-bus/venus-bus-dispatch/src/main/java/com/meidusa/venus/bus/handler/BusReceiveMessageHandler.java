package com.meidusa.venus.bus.handler;

import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.venus.Result;
import com.meidusa.venus.ServerInvocation;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.backend.support.ServerResponseHandler;
import com.meidusa.venus.backend.support.ServerResponseWrapper;
import com.meidusa.venus.bus.dispatch.BusDispatcher;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.io.handler.VenusServerMessageHandler;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.support.VenusConstants;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * bus消息接收处理
 * @author structchen
 */
public class BusReceiveMessageHandler extends VenusServerMessageHandler implements MessageHandler<VenusFrontendConnection, Tuple<Long, byte[]>>, Initialisable,InitializingBean{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

    private VenusRegistryFactory venusRegistryFactory;

    //bus请求分发
    private BusDispatcher busDispatcher = new BusDispatcher();

    private Map<String, VenusFrontendConnection> reqFrontConnMap = new ConcurrentHashMap<String, VenusFrontendConnection>();

    private ServerResponseHandler responseHandler = new ServerResponseHandler();

    @Override
    public void afterPropertiesSet() throws Exception {
        if(venusRegistryFactory == null){
            throw new VenusConfigException("venusRegistryFactory not config.");
        }
        Register register = venusRegistryFactory.getRegister();
        if(register == null){
            throw new VenusConfigException("register init failed.");
        }

        busDispatcher.setRegister(register);
        busDispatcher.getMessageHandler().setReqFrontConnMap(reqFrontConnMap);
    }

    @Override
    public void init() throws InitialisationException {
    }


   public void handle(VenusFrontendConnection conn, Tuple<Long, byte[]> data) {
        byte[] message = data.right;
        int type = AbstractServicePacket.getType(message);
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            VenusRouterPacket routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            type = AbstractServicePacket.getType(routerPacket.data);
        }

        switch (type) {
            case PacketConstant.PACKET_TYPE_PING:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_PONG:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_VENUS_STATUS_REQUEST:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_REQUEST:
                //处理服务调用消息
                doHandle(conn, data);
                break;
            default:
                super.handle(conn, data);
        }

    }

    /**
     * 处理远程调用请求
     * @param conn
     * @param data
     */
    void doHandle(VenusFrontendConnection conn, Tuple<Long, byte[]> data) {
        long bTime = System.currentTimeMillis();
        ServerInvocation invocation = null;
        Result result = null;
        String rpcId = null;
        int sourcePacketType = -1;
        try {
            //解析请求对象
            invocation = parseInvocation(conn, data);
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info("recv request,rpcId:{},message len:{}.",rpcId,data.right.length);
            }

            rpcId = invocation.getRpcId();
            if(StringUtils.isNotEmpty(rpcId) && conn != null){
                reqFrontConnMap.put(rpcId,conn);
            }

            //分发调用
            busDispatcher.dispatch(invocation);
        } catch (Throwable t) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("dispatch error,rpcId:" + rpcId,t);
            }
            result = buildResult(t);
            //若分发异常，则删除连接关系记录
            if(reqFrontConnMap.containsKey(rpcId)){
                reqFrontConnMap.remove(rpcId);
            }
        }finally {
            try {
                //输出tracer日志
                printTracerLogger(invocation,result,bTime);
            } catch (Exception e) {}
        }

        //若分发成功，则直接返回
        if(result == null){
            return;
        }
        //若分发失败，如服务不存在或无权限访问等，则直接输出错误信息
        try {
            ServerResponseWrapper responseEntityWrapper = ServerResponseWrapper.parse(invocation,result,false);

            if (invocation.getResultType() == EndpointInvocation.ResultType.RESPONSE) {
                if(tracerLogger.isInfoEnabled()){
                    tracerLogger.info("write normal response,rpcId:{},used time:{}ms.",rpcId,System.currentTimeMillis()-bTime);
                }
                responseHandler.writeResponseForResponse(responseEntityWrapper,sourcePacketType);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.OK) {
                if(tracerLogger.isInfoEnabled()){
                    tracerLogger.info("write normal response,rpcId:{},used time:{}ms.",rpcId,System.currentTimeMillis()-bTime);
                }
                responseHandler.writeResponseForOk(responseEntityWrapper,sourcePacketType);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.NOTIFY) {
                //callback回调异常情况
                if(result.getErrorCode() != 0){
                    if(tracerLogger.isInfoEnabled()){
                        tracerLogger.info("write notify response,rpcId:{},used time:{}ms.",rpcId,System.currentTimeMillis()-bTime);
                    }
                    responseHandler.writeResponseForNotify(responseEntityWrapper,sourcePacketType);
                }
            }
        } catch (Throwable t) {
            if(logger.isErrorEnabled()){
                logger.error("write response error.",t);
            }
        }
    }

    /**
     * 输出tracer日志
     * @param invocation
     * @param result
     * @param bTime
     */
    void printTracerLogger(ServerInvocation invocation,Result result,long bTime){
        //构造参数
        boolean hasException = false;
        long usedTime = System.currentTimeMillis() - bTime;
        String invokeModel = invocation.getInvokeModel();
        String rpcId = invocation.getRpcId();
        String methodPath = invocation.getMethodPath();
        String param = "";
        if(invocation.isEnablePrintParam() && invocation.getArgs() != null){
            param = JSONUtil.toJSONString(invocation.getArgs());
        }
        Object output = "";
        if(invocation.isEnablePrintResult()){
            if(result.getErrorCode() == 0 && result.getException() == null){
                output = JSONUtil.toJSONString(result.getResult());
            }else if(result.getException() != null){
                hasException = true;
                output = result.getException();
            }else if(result.getErrorCode() != 0){
                hasException = true;
                output = String.format("%s-%s",result.getErrorCode(),result.getErrorMessage());
            }
        }
        String status = "";
        if(hasException){
            status = "failed";
        }else if(usedTime > 1000){
            status = "slow>1000ms";
        }else if(usedTime > 500){
            status = "slow>500ms";
        }else if(usedTime > 200){
            status = "slow>200ms";
        }else{
            status = "success";
        }

        //打印结果
        String tpl = "{} handle,rpcId:{},methodPath:{},status:{},used time:{}ms,param:{},result:{}.";
        Object[] arguments = new Object[]{
                invokeModel,
                rpcId,
                methodPath,
                status,
                usedTime,
                param,
                output
        };
        if(hasException){
            //输出错误日志
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error(tpl,arguments);
            }
            if(tracerLogger.isErrorEnabled()){
                tracerLogger.error(tpl,arguments);
            }
        }else if(usedTime > 200){
            if(tracerLogger.isWarnEnabled()){
                tracerLogger.warn(tpl,arguments);
            }
        }else{
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info(tpl,arguments);
            }
        }
    }

    /**
     * 解析并构造请求对象
     * @return
     */
    ServerInvocation parseInvocation(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        byte[] message = data.right;
        ServerInvocation invocation = new ServerInvocation();
        invocation.setConn(conn);
        invocation.setData(data);
        invocation.setMessage(message);
        invocation.setClientId(conn.getClientId());
        invocation.setHost(conn.getHost());
        invocation.setLocalHost(conn.getLocalHost());
        invocation.setRequestTime(new Date());
        String providerApp = VenusContext.getInstance().getApplication();
        String providerIp = NetUtil.getLocalIp();
        String consumerIp = conn.getHost();
        invocation.setProviderApp(providerApp);
        invocation.setProviderIp(providerIp);
        invocation.setConsumerIp(consumerIp);
        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
        ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
        apiPacket.init(packetBuffer);
        String apiName = apiPacket.apiName;
        String[] arr = apiName.split("\\.");
        invocation.setServiceName(arr[0]);
        invocation.setMethodName(arr[1]);
        int version = apiPacket.serviceVersion;
        if(version != 0){
            invocation.setVersion(String.valueOf(version));
        }else{
            invocation.setVersion(String.valueOf(VenusConstants.VERSION_DEFAULT));
        }
        invocation.setRpcId(RpcIdUtil.getRpcId(apiPacket));
        return invocation;
    }


    /**
     * 将异常转化为result
     * @param t
     * @return
     */
    Result buildResult(Throwable t){
        Result result = new Result();
        //将rpcException包装异常转化为v3内置异常
        Throwable ex = restoreException(t);

        int errorCode = XmlVenusExceptionFactory.getInstance().getErrorCode(ex.getClass());
        if(errorCode != 0){//自定义异常
            result.setErrorCode(errorCode);
            result.setErrorMessage(ex.getMessage());
            result.setException(ex);
        }else{//内置异常
            //包装为venus默认异常，以便到client能还原异常信息
            DefaultVenusException dex = new DefaultVenusException(VenusExceptionCodeConstant.UNKNOW_EXCEPTION,ex.getMessage(),ex);
            result.setErrorCode(dex.getErrorCode());
            result.setErrorMessage(dex.getMessage());
            result.setException(dex);
        }
        return result;
    }

    /**
     * 将rpcExcpetion包装异常转换为V3版本已内置异常，为不兼容升级
     * @param t
     * @return
     */
    Throwable restoreException(Throwable t){
        if(t instanceof RpcException){
            RpcException rex = (RpcException)t;
            if(rex.getCause() != null){
                return rex.getCause();
            }else{
                DefaultVenusException dex = new DefaultVenusException(rex.getErrorCode(),rex.getMessage());
                return dex;
            }
        }else{
            return t;
        }
    }

    public VenusRegistryFactory getVenusRegistryFactory() {
        return venusRegistryFactory;
    }

    public void setVenusRegistryFactory(VenusRegistryFactory venusRegistryFactory) {
        this.venusRegistryFactory = venusRegistryFactory;
    }
}
