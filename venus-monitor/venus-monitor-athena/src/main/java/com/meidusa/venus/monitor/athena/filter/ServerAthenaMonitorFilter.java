package com.meidusa.venus.monitor.athena.filter;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.*;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.Service;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.monitor.athena.reporter.AthenaReporterDelegate;
import com.meidusa.venus.monitor.athena.AthenaTransactionId;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.Stack;

/**
 * server athena监控filter
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ServerAthenaMonitorFilter implements Filter {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    static boolean isRunning = false;

    public ServerAthenaMonitorFilter(){
        synchronized (this){
            if(!isRunning){
                init();
                isRunning = true;
            }
        }
    }

    @Override
    public void init() throws RpcException {
        AthenaReporterDelegate.getInstance().init();
    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        try {
            ServerInvocation serverInvocation = (ServerInvocation)invocation;
            Endpoint endpoint = serverInvocation.getEndpointDef();
            if(endpoint == null){
                return null;
            }
            Service service = endpoint.getService();
            if (service == null || !service.getAthenaFlag()) {
                return null;
            }

            if(serverInvocation.getAthenaId() == null){
                AthenaTransactionId transactionId = AthenaReporterDelegate.getInstance().newTransaction();
                if(transactionId != null && transactionId.getRootId() != null){
                    serverInvocation.setAthenaId(transactionId.getRootId().getBytes());
                    serverInvocation.setParentId(transactionId.getParentId().getBytes());
                    serverInvocation.setMessageId(transactionId.getMessageId().getBytes());
                }
            }

            if(serverInvocation.getAthenaId() == null){
                if(logger.isWarnEnabled()){
                    logger.warn("athena rootId/parnetId/messageId is null,skip report.");
                    return null;
                }
            }

            //调用服务
            Tuple<Long, byte[]> data = serverInvocation.getData();
            SerializeServiceRequestPacket request = serverInvocation.getRequest();
            String apiName = request.apiName;
            long startTime = TimeUtil.currentTimeMillis();
            VenusThreadContext.set(VenusThreadContext.SERVER_BEGIN_TIME,Long.valueOf(startTime));
            AthenaReporterDelegate.getInstance().metric(apiName + ".handleRequest");
            AthenaTransactionId transactionId = new AthenaTransactionId();
            transactionId.setRootId(new String(serverInvocation.getAthenaId()));
            transactionId.setParentId(new String(serverInvocation.getParentId()));
            transactionId.setMessageId(new String(serverInvocation.getMessageId()));
            AthenaReporterDelegate.getInstance().startServerTransaction(transactionId, apiName);
            AthenaReporterDelegate.getInstance().setServerInputSize(data.right.length);
            return null;
        }catch(Throwable e){
            //只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ServerAthenaMonitorFilter.beforeInvoke error.",e);
            }
            return null;
        }
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        try {
            ServerInvocation serverInvocation = (ServerInvocation)invocation;
            Endpoint endpoint = serverInvocation.getEndpointDef();
            if(endpoint == null){
                return null;
            }
            Service service = endpoint.getService();
            if (service == null || !service.getAthenaFlag()) {
                return null;
            }

            if(serverInvocation.getAthenaId() == null){
                if(logger.isWarnEnabled()){
                    logger.warn("athena rootId/parnetId/messageId is null,skip report.");
                    return null;
                }
            }

            SerializeServiceRequestPacket request = serverInvocation.getRequest();
            String apiName = request.apiName;
            AthenaReporterDelegate.getInstance().metric(apiName + ".error");
            AthenaReporterDelegate.getInstance().problem(e.getMessage(), e);
            //VenusMonitorDelegate.getInstance().reportError(e.getMessage(), e);
            return null;
        } catch (Throwable ex) {
            //只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ServerAthenaMonitorFilter.beforeInvoke error.",ex);
            }
            return null;
        }
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        try {
            ServerInvocation serverInvocation = (ServerInvocation)invocation;
            Endpoint endpoint = serverInvocation.getEndpointDef();
            if(endpoint == null){
                return null;
            }
            Service service = endpoint.getService();
            if (service == null || !service.getAthenaFlag()) {
                return null;
            }

            if(serverInvocation.getAthenaId() == null){
                if(logger.isWarnEnabled()){
                    logger.warn("athena rootId/parnetId/messageId is null,skip report.");
                    return null;
                }
            }

            Tuple<Long, byte[]> data = serverInvocation.getData();
            SerializeServiceRequestPacket request = serverInvocation.getRequest();
            String apiName = request.apiName;
            boolean athenaFlag = endpoint.getService().getAthenaFlag();
            AthenaReporterDelegate.getInstance().metric(apiName + ".complete");
            Long startTime = (Long) VenusThreadContext.get(VenusThreadContext.SERVER_BEGIN_TIME);
            long endRunTime = TimeUtil.currentTimeMillis();
            long queuedTime = startTime.longValue() - data.left;
            long executeTime = endRunTime - startTime.longValue();
            if ((endpoint.getTimeWait() < (queuedTime + executeTime)) && athenaFlag) {
                AthenaReporterDelegate.getInstance().metric(apiName + ".timeout");
            }

            //保存输出报文长度
            //VenusThreadContext.set(VenusThreadContext.SERVER_OUTPUT_SIZE,Integer.valueOf(byteBuffer.limit()));

            Integer serverOutputSize = (Integer) VenusThreadContext.get(VenusThreadContext.SERVER_OUTPUT_SIZE);
            if(serverOutputSize != null){
                AthenaReporterDelegate.getInstance().setServerOutputSize(serverOutputSize.intValue());
            }

            AthenaReporterDelegate.getInstance().completeServerTransaction();
            return null;
        }catch (Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ServerAthenaMonitorFilter.afterInvoke error.",e);
            }
            return null;
        }
    }

    @Override
    public void destroy() throws RpcException {

    }
}
