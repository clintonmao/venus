package com.meidusa.venus.monitor.filter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
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
 * client监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ClientVenusMonitorFilter extends AbstractMonitorFilter implements Filter,MonitorDataConvert {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private boolean isInited = false;

    public ClientVenusMonitorFilter(){
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
            processThread.setName("consumer monitor process");
            processThread.start();

            Thread reporterThread = new Thread(new VenusMonitorReportTask(detailQueue, reportDetailQueue, statisticMap, this));
            reporterThread.setName("consumer monitor report");
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
            ClientInvocation clientInvocation = (ClientInvocation)invocation;
            //若不走注册中心，则跳过
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
            putInvocationDetailQueue(invocationDetail);
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
    boolean isNeedReport(ClientInvocation clientInvocation){
        //走注册中心才上报
        if(clientInvocation.getLookupType() == 0){
            return false;
        }
        return !VenusUtil.isAthenaInterface(clientInvocation);
    }

    @Override
    public int getRole() {
        return VenusMonitorConstants.ROLE_CONSUMER;
    }

    /**
     * 转化为detailDo
     * @param detail
     * @return
     */
    public MethodCallDetailDO convertDetail(InvocationDetail detail){
        ClientInvocation clientInvocation = (ClientInvocation)detail.getInvocation();
        URL url = detail.getUrl();
        Result result = detail.getResult();
        Throwable exception = detail.getException();

        MethodCallDetailDO detailDO = new MethodCallDetailDO();
        //基本信息
        detailDO.setId(UUIDUtil.create().toString());
        detailDO.setRpcId(clientInvocation.getRpcId());
        if(clientInvocation.getAthenaId() != null){
            detailDO.setTraceId(new String(clientInvocation.getAthenaId()));
        }
        if(clientInvocation.getMessageId() != null){
            detailDO.setMessageId(new String(clientInvocation.getMessageId()));
        }
        detailDO.setSourceType(detail.getFrom());

        //请求信息
        if(clientInvocation.getServiceInterface() != null){
            detailDO.setInterfaceName(clientInvocation.getServiceInterface().getName());
        }
        detailDO.setServiceName(clientInvocation.getServiceName());
        if(clientInvocation.getEndpoint() != null){
            detailDO.setMethodName(clientInvocation.getEndpoint().getName());
        }else if(clientInvocation.getMethod() != null){
            detailDO.setMethodName(clientInvocation.getMethod().getName());
        }
        if(clientInvocation.getArgs() != null){
            String requestJson = serialize(clientInvocation.getArgs());
            detailDO.setRequestJson(requestJson);
        }
        detailDO.setRequestTime(clientInvocation.getRequestTime());
        detailDO.setProviderDomain(url.getApplication());
        detailDO.setProviderIp(url.getHost());
        detailDO.setConsumerDomain(clientInvocation.getConsumerApp());
        detailDO.setConsumerIp(clientInvocation.getConsumerIp());

        //响应信息
        detailDO.setResponseTime(detail.getResponseTime());
        if(result != null){ //响应结果
            if(result.getErrorCode() == 0){//成功
                String responseJson = serialize(result.getResult());
                detailDO.setReponseJson(responseJson);
                detailDO.setStatus(1);
            }else{//失败
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
        long costTime = detail.getResponseTime().getTime()-clientInvocation.getRequestTime().getTime();
        detailDO.setDurationMillisecond(Integer.parseInt(String.valueOf(costTime)));
        //状态相关

        return detailDO;
    }

    /**
     * 转换为statisticDo
     * @param statistic
     * @return
     */
    public MethodStaticDO convertStatistic(InvocationStatistic statistic){
        MethodStaticDO staticDO = new MethodStaticDO();
        staticDO.setInterfaceName(statistic.getServiceInterfaceName());
        staticDO.setServiceName(statistic.getServiceName());
        staticDO.setVersion(statistic.getVersion());
        staticDO.setMethodName(statistic.getMethod());
        staticDO.setTotalCount((statistic.getTotalNum().intValue()));
        staticDO.setFailCount(statistic.getFailNum().intValue());
        staticDO.setSlowCount(statistic.getSlowNum().intValue());
        staticDO.setAvgDuration(statistic.getAvgCostTime().intValue());
        staticDO.setMaxDuration(statistic.getMaxCostTime().intValue());

        staticDO.setDomain(statistic.getProviderApp());
        staticDO.setSourceIp(statistic.getProviderIp());
        staticDO.setStartTime(statistic.getBeginTime());
        staticDO.setEndTime(statistic.getEndTime());
        return staticDO;
    }

    @Override
    public void destroy() throws RpcException {

    }

}
