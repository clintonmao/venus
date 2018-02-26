package com.meidusa.venus.monitor.filter;

import com.athena.venus.domain.VenusMethodCallDetailDO;
import com.athena.venus.domain.VenusMethodStaticDO;
import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.monitor.MonitorDataConvert;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;
import com.meidusa.venus.monitor.support.VenusMonitorConstants;
import com.meidusa.venus.monitor.task.VenusMonitorProcessTask;
import com.meidusa.venus.monitor.task.VenusMonitorReportTask;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.UUIDUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.Date;

/**
 * server监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ServerVenusMonitorFilter extends AbstractMonitorFilter implements Filter,MonitorDataConvert {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private boolean isInited = false;

    public ServerVenusMonitorFilter(){
    }

    @Override
    public void init() throws RpcException {
        synchronized (this){
            if(!isInited){
                //启动上报线程
                startProcessAndReporterTread();
                isInited = true;
            }
        }
    }

    /**
     * 起动数据计算及上报线程
     */
    void startProcessAndReporterTread(){
        if(!isRunning){
            Thread processThread = new Thread(new VenusMonitorProcessTask(detailQueue, reportDetailQueue, statisticMap, this));
            processThread.setName("provider monitor process");
            processThread.start();

            Thread reporterThread = new Thread(new VenusMonitorReportTask(detailQueue, reportDetailQueue, statisticMap, this));
            reporterThread.setName("provider monitor report");
            reporterThread.start();
            isRunning = true;
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
            //若不需要上报，则跳过
            if(!isNeedReport(serverInvocation)){
                return null;
            }

            Result result = (Result) VenusThreadContext.get(VenusThreadContext.RESPONSE_RESULT);
            Throwable e = (Throwable)VenusThreadContext.get(VenusThreadContext.RESPONSE_EXCEPTION);

            InvocationDetail invocationDetail = new InvocationDetail();
            invocationDetail.setFrom(InvocationDetail.FROM_SERVER);
            invocationDetail.setInvocation(serverInvocation);
            invocationDetail.setResponseTime(new Date());
            invocationDetail.setResult(result);
            invocationDetail.setException(e);

            //添加到明细队列
            putInvocationDetailQueue(invocationDetail);
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
     * 判断是否需要监控上报
     * @param invocation
     * @return
     */
    boolean isNeedReport(ServerInvocationOperation invocation){
        //走注册中心才上报
        /*
        if(clientInvocation.getLookupType() == 0){
            return false;
        }
        */
        return !VenusUtil.isAthenaInterface(invocation);
    }

    /**
     * 获取调用方法及调用环境标识路径
     * @return
     */
    String getMethodAndEnvPath(InvocationDetail detail){
        /*
        Invocation invocation = detail.getInvocation();
        //请求时间，精确为分钟
        ServerInvocation serverInvocation = (ServerInvocation)invocation;
        String requestTimeOfMinutes = getTimeOfMinutes(serverInvocation.getRequestTime());

        //方法路径信息
        String methodAndEnvPath = String.format(
                "%s/%s?version=%s&method=%s&startTime=%s",
                invocation.getServiceInterfaceName(),
                invocation.getServiceName(),
                invocation.getVersion(),
                invocation.getMethodName(),
                requestTimeOfMinutes
        );
        if(logger.isDebugEnabled()){
            logger.debug("methodAndEnvPath:{}.", methodAndEnvPath);
        }
        return methodAndEnvPath;
        */
        //服务端不做统计上报
        return null;
    }


    @Override
    public int getRole() {
        return VenusMonitorConstants.ROLE_PROVIDER;
    }

    /**
     * 转化为detailDo
     * @param detail
     * @return
     */
    public VenusMethodCallDetailDO convertDetail(InvocationDetail detail){
        ServerInvocationOperation serverInvocation = (ServerInvocationOperation)detail.getInvocation();
        Result result = detail.getResult();
        Throwable exception = detail.getException();

        VenusMethodCallDetailDO detailDO = new VenusMethodCallDetailDO();
        //基本信息
        detailDO.setId(UUIDUtil.create().toString());
        detailDO.setRpcId(serverInvocation.getRpcId());
        if(serverInvocation.getAthenaId() != null){
            detailDO.setTraceId(new String(serverInvocation.getAthenaId()));
        }
        if(serverInvocation.getMessageId() != null){
            detailDO.setMessageId(new String(serverInvocation.getMessageId()));
        }
        detailDO.setSourceType(detail.getFrom());
        //请求信息
        detailDO.setServiceName(serverInvocation.getServiceName());
        if(serverInvocation.getServiceInterface() != null){
            detailDO.setInterfaceName(serverInvocation.getServiceInterface().getName());
        }
        if(serverInvocation.getEndpoint() != null){
            detailDO.setMethodName(serverInvocation.getEndpoint().getName());
        }else if(serverInvocation.getMethod() != null){
            detailDO.setMethodName(serverInvocation.getMethod().getName());
        }
        if(serverInvocation.getArgs() != null){
            String requestJson = serialize(serverInvocation.getArgs());
            detailDO.setRequestJson(requestJson);
        }
        detailDO.setRequestTime(serverInvocation.getRequestTime());
        detailDO.setProviderDomain(serverInvocation.getProviderApp());
        detailDO.setProviderIp(serverInvocation.getProviderIp());
        detailDO.setConsumerDomain(serverInvocation.getConsumerApp());
        detailDO.setConsumerIp(serverInvocation.getConsumerIp());
        //响应信息
        detailDO.setResponseTime(detail.getResponseTime());

        if(result != null){//响应结果
            if(result.getErrorCode() == 0){
                String responseJson = serialize(result.getResult());
                detailDO.setReponseJson(responseJson);
                detailDO.setStatus(1);
            }else{
                String responseJson = String.format("%s-%s",result.getErrorCode(),result.getErrorMessage());
                detailDO.setReponseJson(responseJson);
                detailDO.setStatus(0);
            }
        } else if(exception != null){//响应异常
            String responseJsonForException = serialize(exception);
            detailDO.setErrorInfo(responseJsonForException);
            detailDO.setStatus(0);
        }
        //耗时
        long costTime = detail.getResponseTime().getTime()-serverInvocation.getRequestTime().getTime();
        detailDO.setDurationMillisecond(Integer.parseInt(String.valueOf(costTime)));
        //状态相关
        return detailDO;
    }

    public VenusMethodStaticDO convertStatistic(InvocationStatistic statistic){
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }

}
