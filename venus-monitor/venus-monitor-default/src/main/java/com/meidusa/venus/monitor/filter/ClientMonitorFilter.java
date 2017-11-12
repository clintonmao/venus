package com.meidusa.venus.monitor.filter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.util.UUIDUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * client监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ClientMonitorFilter extends AbstractMonitorFilter implements Filter {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();


    public ClientMonitorFilter(){
    }

    public ClientMonitorFilter(AthenaDataService athenaDataService){
        this.setAthenaDataService(athenaDataService);
        startProcessAndReporterTread();
    }

    @Override
    public void init() throws RpcException {
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
            //响应结果
            Result result = (Result) VenusThreadContext.get(VenusThreadContext.RESPONSE_RESULT);
            //响应异常
            Throwable e = (Throwable)VenusThreadContext.get(VenusThreadContext.RESPONSE_EXCEPTION);

            //组装并添加到明细队列
            InvocationDetail invocationDetail = new InvocationDetail();
            invocationDetail.setFrom(InvocationDetail.FROM_CLIENT);
            invocationDetail.setInvocation(clientInvocation);
            invocationDetail.setUrl(url);
            invocationDetail.setResponseTime(new Date());
            invocationDetail.setResult(result);
            invocationDetail.setException(e);

            putInvocationDetailQueue(invocationDetail);
            return null;
        } catch (RpcException e) {
            throw e;
        }catch(Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ClientMonitorFilter.afterInvoke error.",e);
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
        return clientInvocation.getLookupType() != 0;
    }

    /**
     * 获取调用方法及调用环境标识路径
     * @return
     */
    String getMethodAndEnvPath(InvocationDetail detail){
        Invocation invocation = detail.getInvocation();
        URL url = detail.getUrl();
        //请求时间，精确为分钟
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        String requestTimeOfMinutes = getTimeOfMinutes(clientInvocation.getRequestTime());

        //方法路径信息
        String methodAndEnvPath = String.format(
                "%s/%s?version=%s&method=%s&target=%s&startTime=%s",
                invocation.getServiceInterfaceName(),
                invocation.getServiceName(),
                invocation.getVersion(),
                invocation.getMethodName(),
                url.getHost(),
                requestTimeOfMinutes
        );
        if(logger.isDebugEnabled()){
            logger.debug("methodAndEnvPath:{}.", methodAndEnvPath);
        }
        return methodAndEnvPath;
    }

    /**
     * 转化为detailDo
     * @param detail
     * @return
     */
    MethodCallDetailDO convertDetail(InvocationDetail detail){
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
            //Athena上报接口不记输入、输出信息，存在递归拼接问题
            if(!isAthenaInterface(clientInvocation)){
                String requestJson = serialize(clientInvocation.getArgs());
                detailDO.setRequestJson(requestJson);
            }
        }
        detailDO.setRequestTime(clientInvocation.getRequestTime());
        detailDO.setProviderDomain(url.getApplication());
        detailDO.setProviderIp(url.getHost());
        detailDO.setConsumerDomain(clientInvocation.getConsumerApp());
        detailDO.setConsumerIp(clientInvocation.getConsumerIp());

        //响应信息
        detailDO.setResponseTime(detail.getResponseTime());
        //响应结果
        if(result != null){
            if(result.getErrorCode() == 0){
                //Athena上报接口不记输入、输出信息，存在递归拼接问题
                if(!isAthenaInterface(clientInvocation)){
                    String responseJson = serialize(result.getResult());
                    detailDO.setReponseJson(responseJson);
                }
                detailDO.setStatus(1);
            }else{
                //Athena上报接口不记输入、输出信息，存在递归拼接问题
                if(!isAthenaInterface(clientInvocation)){
                    String responseJson = serialize(result.getErrorCode());
                    detailDO.setReponseJson(responseJson);
                }
                detailDO.setStatus(1);
            }
        } else{
            //响应异常
            //Athena上报接口不记输入、输出信息，存在递归拼接问题
            if(!isAthenaInterface(clientInvocation)){
                String responseJsonForException = serialize(exception);
                detailDO.setErrorInfo(responseJsonForException);
            }
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
    MethodStaticDO convertStatistic(InvocationStatistic statistic){
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
    int getRole() {
        return ROLE_CONSUMER;
    }

    @Override
    public void destroy() throws RpcException {

    }

}
