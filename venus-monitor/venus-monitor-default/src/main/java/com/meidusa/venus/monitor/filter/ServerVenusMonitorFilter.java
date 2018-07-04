package com.meidusa.venus.monitor.filter;

import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.VenusMonitorUtil;
import com.meidusa.venus.monitor.task.ServerMonitorProcessTask;
import com.meidusa.venus.monitor.task.ServerMonitorReportTask;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.Date;

/**
 * server监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ServerVenusMonitorFilter extends AbstractMonitorFilter implements Filter{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private boolean isInited = false;

    public ServerVenusMonitorFilter(){
    }

    @Override
    public void init() throws RpcException {
        synchronized (this){
            if(!isInited){
                //启动计算、上报线程
                Thread processThread = new Thread(new ServerMonitorProcessTask(detailQueue, reportDetailQueue, statisticMap));
                processThread.setName("provider monitor process");
                processThread.start();

                Thread reporterThread = new Thread(new ServerMonitorReportTask(detailQueue, reportDetailQueue, statisticMap));
                reporterThread.setName("provider monitor report");
                reporterThread.start();
                isInited = true;
            }
        }
    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        try {
            ServerInvocationOperation serverInvocation = (ServerInvocationOperation)invocation;
            //athena不上报
            if(!isNeedReport(serverInvocation)){
                return null;
            }

            Result result = (Result) VenusThreadContext.get(VenusThreadContext.RESPONSE_RESULT);
            Throwable e = (Throwable)VenusThreadContext.get(VenusThreadContext.RESPONSE_EXCEPTION);

            InvocationDetail detail = new InvocationDetail();
            detail.setFrom(InvocationDetail.FROM_SERVER);
            detail.setInvocation(serverInvocation);
            detail.setResponseTime(new Date());
            detail.setResult(cloneResult(result));
            detail.setException(e);
            if(VenusMonitorUtil.isExceptionOperation(detail)){
                detail.setExceptionOperation(true);
            }else{
                if(VenusMonitorUtil.isSlowOperation(detail)){
                    detail.setSlowOperation(true);
                }
            }

            //添加到明细队列
            putDetail2Queue(detail);
            return null;
        }catch(Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ServerVenusMonitorFilter.afterInvoke error.",e);
            }
            return null;
        }
    }

    /**
     * 复制Result，去除result.object返回结果
     * @param result
     * @return
     */
    Result cloneResult(Result result){
        if(result == null){
            return null;
        }
        Result ret = new Result();
        ret.setException(result.getException());
        ret.setErrorCode(result.getErrorCode());
        ret.setErrorMessage(result.getErrorMessage());
        return ret;
    }

    /**
     * 判断是否需要监控上报
     * @param invocation
     * @return
     */
    boolean isNeedReport(ServerInvocationOperation invocation){
        return !VenusUtil.isAthenaInterface(invocation);
    }

    @Override
    public void destroy() throws RpcException {

    }

}
