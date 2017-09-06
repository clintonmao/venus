package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.backend.serializer.JSONSerializer;
import com.meidusa.venus.monitor.filter.client.ClientInvocationDetail;
import com.meidusa.venus.monitor.filter.client.ClientInvocationStatistic;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 监控上报代理，目的让接入方避免直接依赖athena-api
 * Created by Zhangzhihua on 2017/9/5.
 */
public class ClientMonitorReporterDelegate {

    private static Logger logger = LoggerFactory.getLogger(ClientMonitorReporterDelegate.class);

    private AthenaDataService athenaDataService;

    //TODO 将序列化传输协议无关移到common包中
    private JSONSerializer jsonSerializer = new JSONSerializer();

    /**
     * 上报异常明细 TODO 上报放到reporter模块，可选择依赖
     * @param exceptionDetailList
     */
    public void reportExceptionDetailList(Collection<ClientInvocationDetail> exceptionDetailList){
        AthenaDataService athenaDataService = getAthenaDataService();
        logger.info("report exceptionDetailList size:{}.",exceptionDetailList.size());
        if(CollectionUtils.isEmpty(exceptionDetailList)){
            return;
        }

        List<MethodCallDetailDO> detailDOList = new ArrayList<MethodCallDetailDO>();
        for(ClientInvocationDetail detail:exceptionDetailList){
            MethodCallDetailDO detailDO = convertDetail(detail);
            detailDOList.add(detailDO);
        }
        athenaDataService.reportMethodCallDetail(detailDOList);
    }

    /**
     * 转化为detailDo
     * @param detail
     * @return
     */
    MethodCallDetailDO convertDetail(ClientInvocationDetail detail){
        Invocation invocation = detail.getInvocation();
        URL url = detail.getUrl();
        Result result = detail.getResult();
        Throwable exception = detail.getException();

        MethodCallDetailDO detailDO = new MethodCallDetailDO();
        //基本信息
        detailDO.setId(invocation.getId());
        detailDO.setRpcId(invocation.getRpcId());
        detailDO.setTraceId(invocation.getAthenaId());
        detailDO.setSourceType(detail.getFrom());
        //请求信息
        detailDO.setServiceName(invocation.getService().name());
        detailDO.setInterfaceName(invocation.getServiceInterface().getName());
        detailDO.setMethodName(invocation.getEndpoint().name());
        if(invocation.getArgs() != null){
            detailDO.setRequestJson(serialize(invocation.getArgs()));
        }
        detailDO.setRequestTime(invocation.getRequestTime());
        detailDO.setConsumerIp(invocation.getConsumerIp());
        detailDO.setProviderIp(url.getHost());
        //响应信息
        detailDO.setResponseTime(detail.getResponseTime());
        //响应结果
        if(result != null){
            detailDO.setReponseJson(serialize(result));
            detailDO.setStatus(1);
        } else{
            //响应异常
            detailDO.setErrorInfo(serialize(exception));
            detailDO.setStatus(0);
        }
        //TODO 响应地址
        //状态相关
        return detailDO;
    }

    /**
     * 序列化对象
     * @param object
     * @return
     */
    String serialize(Object object){
        try {
            return jsonSerializer.serialize(object);
        } catch (Exception e) {
            logger.error("serialize error.",e);
            return "";
        }
    }

    /**
     * 上报统计数据 TODO 上报放到reporter模块，可选择依赖
     * @param statisticList
     */
    public void reportStatisticList(Collection<ClientInvocationStatistic> statisticList){
        AthenaDataService athenaDataService = getAthenaDataService();
        logger.info("report statisticList size:{}.",statisticList.size());
        if(CollectionUtils.isEmpty(statisticList)){
            return;
        }

        List<MethodStaticDO> staticDOList = new ArrayList<MethodStaticDO>();
        for(ClientInvocationStatistic statistic:statisticList){
            MethodStaticDO staticDO = convertStatistic(statistic);
            staticDOList.add(staticDO);
        }
        try {
            String statisticDOListOfJson = new JSONSerializer().serialize(staticDOList);
            logger.info("statisticDOListOfJson:{}.",statisticDOListOfJson);
        } catch (Exception e) {}
        athenaDataService.reportMethodStatic(staticDOList);
    }

    /**
     * 转换为statisticDo
     * @param statistic
     * @return
     */
    MethodStaticDO convertStatistic(ClientInvocationStatistic statistic){
        MethodStaticDO staticDO = new MethodStaticDO();
        staticDO.setInterfaceName(statistic.getServiceInterfaceName());
        staticDO.setServiceName(statistic.getServiceName());
        staticDO.setVersion(statistic.getVersion());
        staticDO.setMethodName(statistic.getMethod());
        staticDO.setSuccessCount((statistic.getTotalNum().intValue()-statistic.getFailNum().intValue()));
        staticDO.setFailCount(statistic.getFailNum().intValue());
        staticDO.setAvgDurationMillisecond(statistic.getAvgCostTime().intValue());
        staticDO.setMaxDurationMillisecond(statistic.getMaxCostTime().intValue());
        staticDO.setMaxTps(statistic.getTotalNum().intValue());

        staticDO.setDomain(statistic.getApplication());
        staticDO.setSourceIp(statistic.getHost());
        staticDO.setStartTime(statistic.getBeginTime());
        staticDO.setEndTime(statistic.getEndTime());
        return staticDO;
    }

    public AthenaDataService getAthenaDataService() {
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }
}
