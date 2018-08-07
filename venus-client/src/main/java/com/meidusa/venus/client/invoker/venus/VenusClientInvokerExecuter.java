package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.BackendConnection;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.*;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.invoker.venus.encode.BaseEncoder;
import com.meidusa.venus.client.invoker.venus.encode.DefaultEncoderFactory;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.packet.AbstractServiceRequestPacket;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * venus协议服务调用实现
 * Created by Zhangzhihua on 2017/7/31.
 */
public class VenusClientInvokerExecuter implements Invoker{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private byte serializeType = PacketConstant.CONTENT_TYPE_JSON;

    /**
     * 远程连接配置，包含ip相关信息
     */
    private ClientRemoteConfig remoteConfig;

    private VenusClientConnectionFactory connectionFactory = VenusClientConnectionFactory.getInstance();

    private BaseEncoder encoder = null;

    public VenusClientInvokerExecuter(){
        init();
    }

    @Override
    public void init() throws RpcException {
        //添加invoker资源
        VenusApplication.addInvoker(this);
    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        VenusThreadContext.set(VenusThreadContext.REQUEST_URL,url);

        //初始化
        init();

        //调用相应协议实现
        Result result = doInvoke(clientInvocation, url);
        return result;
    }


    Result doInvoke(ClientInvocation invocation, URL url) throws RpcException {
        if(!isCallbackInvocation(invocation)){
            return doInvokeWithSync(invocation, url);
        }else{
            return doInvokeWithCallback(invocation, url);
        }
    }

    /**
     * 判断是否callback异步调用
     * @param invocation
     * @return
     */
    boolean isCallbackInvocation(ClientInvocation invocation){
        EndpointParameter[] params = invocation.getEndpointParameters();
        if (params != null) {
            Object[] args = invocation.getArgs();
            for (int i = 0; i < params.length; i++) {
                if (args[i] instanceof InvocationListener) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * sync同步调用
     * @param invocation
     * @param url
     * @return
     * @throws Exception
     */
    public Result doInvokeWithSync(ClientInvocation invocation, URL url) throws RpcException {
        Result result = null;
        int timeout = invocation.getTimeout();

        //构造请求消息
        AbstractServiceRequestPacket request = buildRequest(invocation);

        //添加rpcId -> reqResp映射表
        String rpcId = invocation.getRpcId();
        VenusReqRespWrapper reqRespWrapper = new VenusReqRespWrapper(invocation);
        connectionFactory.getServiceReqRespMap().put(rpcId,reqRespWrapper);

        //发送消息
        sendRequest(invocation, request, url,reqRespWrapper);

        //latch阻塞等待
        boolean isAwaitException = false;
        try {
            reqRespWrapper.getReqRespLatch().await(timeout,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            isAwaitException = true;
            throw new RpcException(e);
        }finally {
            if(isAwaitException){
                if(connectionFactory.getServiceReqRespMap().get(rpcId) != null){
                    connectionFactory.getServiceReqRespMap().remove(rpcId);
                }
            }
        }

        //处理响应
        result = fetchResponse(rpcId);
        if(result == null){
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION,String.format("invoke api:%s,service:%s timeout,timeout:%dms",invocation.getApiName(),url.getPath(),timeout));
        }
        return result;
    }

    /**
     * callback异步调用
     * @param invocation
     * @param url
     * @return
     * @throws Exception
     */
    public Result doInvokeWithCallback(ClientInvocation invocation, URL url) throws RpcException {
        //构造请求消息
        AbstractServiceRequestPacket request = buildRequest(invocation);

        //添加rpcId-> reqResp映射表
        connectionFactory.getServiceReqCallbackMap().put(invocation.getRpcId(),invocation);

        //发送消息
        sendRequest(invocation, request, url,null);

        //立即返回，响应由invocationListener处理
        return new Result(null);
    }



    /**
     * 构造请求消息
     * @param invocation
     * @return
     */
    AbstractServiceRequestPacket buildRequest(ClientInvocation invocation){
        Serializer serializer = SerializerFactory.getSerializer(serializeType);
        AbstractServiceRequestPacket serviceRequestPacket = getEncoder().encode(invocation,serializer);
        return serviceRequestPacket;
    }

    /**
     * 发送远程调用消息
     * @param invocation
     * @param serviceRequestPacket
     * @param url 目标地址
     * @return
     * @throws Exception
     */
    void sendRequest(ClientInvocation invocation, AbstractServiceRequestPacket serviceRequestPacket, URL url,VenusReqRespWrapper reqRespWrapper) throws RpcException{
        long start = TimeUtil.currentTimeMillis();
        long borrowed = start;
        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        String rpcId = invocation.getRpcId();
        Throwable exception = null;

        //获取连接
        try {
            VenusClientConnectionFactory.BackendConnectionWrapper connectionWrapper = connectionFactory.getConnection(url);
            nioConnPool = connectionWrapper.getBackendConnectionPool();
            conn = connectionWrapper.getBackendConnection();
            borrowed = System.currentTimeMillis();
            if(reqRespWrapper != null){
                reqRespWrapper.setBackendConnection(conn);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (conn != null && nioConnPool != null) {
                nioConnPool.returnObject(conn);
            }
        }

        //发送请求消息，响应由handler类处理
        try {
            ByteBuffer buffer = serviceRequestPacket.toByteBuffer();
            VenusThreadContext.set(VenusThreadContext.CLIENT_OUTPUT_SIZE,Integer.valueOf(buffer.limit()));
            conn.write(buffer);
        } catch (RpcException e){
            exception = e;
            throw e;
        }catch (Throwable e){
            exception = e;
            throw new RpcException(e);
        }finally {
            //返连接
            if (conn != null && nioConnPool != null) {
                nioConnPool.returnObject(conn);
            }

            //打印trace logger
            long connTime = borrowed - start;
            long totalTime = System.currentTimeMillis() - start;
            //athena调用输出到default
            Logger trLogger = tracerLogger;
            if(VenusUtil.isAthenaInterface(invocation)){
                trLogger = logger;
            }

            if(exception != null){
                //输出异常日志
                if (trLogger.isErrorEnabled()) {
                    String tpl = "[C] [failed,{}] send request failed,rpcId:{},api:{},method:{},targetIp:{},exception:{}.";
                    Object[] arguments = new Object[]{
                            totalTime + "ms," + connTime+"ms",
                            rpcId,
                            invocation.getApiName(),
                            invocation.getMethodPath(),
                            url.getHost(),
                            exception
                    };
                    trLogger.error(tpl,arguments);
                    //错误日志
                    exceptionLogger.error(tpl,arguments);
                }
            }else{
                if(trLogger.isInfoEnabled()){
                    String tpl = "[C] [{}] send request,rpcId:{},api:{},method:{},targetIp:{}.";
                    Object[] arguments = new Object[]{
                            totalTime + "ms," + connTime+"ms",
                            rpcId,
                            invocation.getApiName(),
                            invocation.getMethodPath(),
                            url.getHost(),
                    };
                    trLogger.info(tpl,arguments);
                }
            }
        }
    }

    /**
     * 获取对应请求的响应结果
     * @param rpcId
     * @return
     */
    Result fetchResponse(String rpcId){
        VenusReqRespWrapper reqRespWrapper = connectionFactory.getServiceReqRespMap().get(rpcId);
        if(reqRespWrapper == null){
            return null;
        }

        Result result = reqRespWrapper.getResult();
        if(result == null){
            connectionFactory.getServiceReqRespMap().remove(rpcId);
            return null;
        }else {
            //删除映射数据
            connectionFactory.getServiceReqRespMap().remove(rpcId);
            return result;
        }
    }

    public short getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(byte serializeType) {
        this.serializeType = serializeType;
    }

    public ClientRemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(ClientRemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    @Override
    public void destroy() throws RpcException{
        if(logger.isInfoEnabled()){
            logger.info("destroy invoker:{}.",this);
        }

        connectionFactory.destroy();
    }

    public BaseEncoder getEncoder() {
        if(encoder == null){
            encoder = DefaultEncoderFactory.newInstance(VenusContext.getInstance().getEncodeType());
        }
        return encoder;
    }
}
