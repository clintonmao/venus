package com.meidusa.venus.monitor.filter;

import com.athena.venus.domain.VenusMethodCallDetailDO;
import com.athena.venus.domain.VenusMethodStaticDO;
import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;
import com.meidusa.venus.monitor.support.VenusMonitorConstants;
import com.meidusa.venus.monitor.task.ClientMonitorProcessTask;
import com.meidusa.venus.monitor.task.ClientMonitorReportTask;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.UUIDUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.Date;

/**
 * client监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ClientVenusMonitorFilter extends AbstractMonitorFilter implements Filter {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private boolean isInited = false;

    public ClientVenusMonitorFilter(){
    }

    @Override
    public void init() throws RpcException {
        synchronized (this){
            if(!isInited){
                //启动计算、上报线程
                Thread processThread = new Thread(new ClientMonitorProcessTask(detailQueue, reportDetailQueue, statisticMap));
                processThread.setName("consumer monitor process");
                processThread.start();

                Thread reporterThread = new Thread(new ClientMonitorReportTask(detailQueue, reportDetailQueue, statisticMap));
                reporterThread.setName("consumer monitor report");
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
            ClientInvocationOperation clientInvocation = (ClientInvocationOperation)invocation;
            //athena不上报
            if(!isNeedReport(clientInvocation)){
                return null;
            }

            //请求url
            url = (URL) VenusThreadContext.get(VenusThreadContext.REQUEST_URL);
            //若寻址失败，则不上报
            if(url == null){
                return null;
            }
            //响应结果
            Result result = (Result) VenusThreadContext.get(VenusThreadContext.RESPONSE_RESULT);
            //响应异常
            Throwable e = (Throwable)VenusThreadContext.get(VenusThreadContext.RESPONSE_EXCEPTION);

            InvocationDetail invocationDetail = new InvocationDetail();
            invocationDetail.setFrom(InvocationDetail.FROM_CLIENT);
            invocationDetail.setInvocation(clientInvocation);
            invocationDetail.setUrl(url);
            invocationDetail.setResponseTime(new Date());
            invocationDetail.setResult(result);
            invocationDetail.setException(e);

            //添加到明细队列
            putDetail2Queue(invocationDetail);
            return null;
        }catch(Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ClientVenusMonitorFilter.afterInvoke error.",e);
            }
            return null;
        }
    }

    /**
     * 判断是否需要监控上报
     * @param clientInvocation
     * @return
     */
    boolean isNeedReport(ClientInvocationOperation clientInvocation){
        return !VenusUtil.isAthenaInterface(clientInvocation);
    }

    @Override
    public void destroy() throws RpcException {

    }

}
