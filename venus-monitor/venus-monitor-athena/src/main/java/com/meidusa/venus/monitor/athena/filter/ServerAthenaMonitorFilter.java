package com.meidusa.venus.monitor.athena.filter;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.*;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.monitor.athena.reporter.AthenaExtensionResolver;
import com.meidusa.venus.monitor.athena.reporter.AthenaReporterDelegate;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionDelegate;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionId;

/**
 * server athena监控filter
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ServerAthenaMonitorFilter implements Filter {

    static boolean isRunning = false;

    boolean isFindderBug = false;

    public ServerAthenaMonitorFilter(){
        if(!isRunning){
            init();
            isRunning = true;
        }
    }

    @Override
    public void init() throws RpcException {
        AthenaExtensionResolver.getInstance().resolver();
    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        ServerInvocation rpcInvocation = (ServerInvocation)invocation;
        Tuple<Long, byte[]> data = rpcInvocation.getData();
        SerializeServiceRequestPacket request = rpcInvocation.getRequest();
        String apiName = request.apiName;
        Endpoint endpoint = rpcInvocation.getEndpointDef();
        long startTime = TimeUtil.currentTimeMillis();
        VenusThreadContext.set(VenusThreadContext.SERVER_BEGIN_TIME,Long.valueOf(startTime));

        //调用服务
        boolean athenaFlag = endpoint.getService().getAthenaFlag();
        if (athenaFlag) {
            AthenaReporterDelegate.getDelegate().metric(apiName + ".handleRequest");
            AthenaTransactionId transactionId = new AthenaTransactionId();
            transactionId.setRootId(new String(rpcInvocation.getAthenaId()));
            transactionId.setParentId(new String(rpcInvocation.getParentId()));
            transactionId.setMessageId(new String(rpcInvocation.getMessageId()));
            AthenaTransactionDelegate.getDelegate().startServerTransaction(transactionId, apiName);
            AthenaTransactionDelegate.getDelegate().setServerInputSize(data.right.length);
        }
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        ServerInvocation rpcInvocation = (ServerInvocation)invocation;
        SerializeServiceRequestPacket request = rpcInvocation.getRequest();
        String apiName = request.apiName;
        Endpoint endpoint = rpcInvocation.getEndpointDef();
        boolean athenaFlag = endpoint.getService().getAthenaFlag();
        if (athenaFlag) {
            AthenaReporterDelegate.getDelegate().metric(apiName + ".error");
            AthenaReporterDelegate.getDelegate().problem(e.getMessage(), e);
            //VenusMonitorDelegate.getInstance().reportError(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        ServerInvocation rpcInvocation = (ServerInvocation)invocation;
        Endpoint endpoint = rpcInvocation.getEndpointDef();
        if(isFindderBug){
            throw new NoSuchMethodError("test NoSuchMethodError error logic.");
        }
        Tuple<Long, byte[]> data = rpcInvocation.getData();
        SerializeServiceRequestPacket request = rpcInvocation.getRequest();
        String apiName = request.apiName;
        boolean athenaFlag = endpoint.getService().getAthenaFlag();
        if (athenaFlag) {
            AthenaReporterDelegate.getDelegate().metric(apiName + ".complete");
            Long startTime = (Long) VenusThreadContext.get(VenusThreadContext.SERVER_BEGIN_TIME);
            long endRunTime = TimeUtil.currentTimeMillis();
            long queuedTime = startTime.longValue() - data.left;
            long executeTime = endRunTime - startTime.longValue();
            //TODO 超时要这样处理吗？
            if ((endpoint.getTimeWait() < (queuedTime + executeTime)) && athenaFlag) {
                AthenaReporterDelegate.getDelegate().metric(apiName + ".timeout");
            }

            //保存输出报文长度
            //VenusThreadContext.set(VenusThreadContext.SERVER_OUTPUT_SIZE,Integer.valueOf(byteBuffer.limit()));

            //TODO 处理未执行到finally语句中情况，参见VenusServerInvokerTask.handle方法
            Integer serverOutputSize = (Integer) VenusThreadContext.get(VenusThreadContext.SERVER_OUTPUT_SIZE);
            if(serverOutputSize != null){
                AthenaTransactionDelegate.getDelegate().setServerOutputSize(serverOutputSize.intValue());
            }

            AthenaTransactionDelegate.getDelegate().completeServerTransaction();
        }
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }
}
