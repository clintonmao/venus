package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.backend.serializer.JSONSerializer;
import com.meidusa.venus.util.UUIDUtil;
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
public abstract class AbstractMonitorReporter {

    private static Logger logger = LoggerFactory.getLogger(AbstractMonitorReporter.class);

    private AthenaDataService athenaDataService;

    //TODO 将序列化传输协议无关移到common包中
    private JSONSerializer jsonSerializer = new JSONSerializer();

    private boolean isEnableReporte = true;

    /**
     * 上报异常明细 TODO 上报放到reporter模块，可选择依赖
     * @param exceptionDetailList
     */
    public void reportExceptionDetailList(Collection<InvocationDetail> exceptionDetailList){
        AthenaDataService athenaDataService = getAthenaDataService();
        if(logger.isDebugEnabled()){
            logger.debug("report exception detail size:{}.",exceptionDetailList.size());
        }
        if(CollectionUtils.isEmpty(exceptionDetailList)){
            return;
        }

        List<MethodCallDetailDO> detailDOList = new ArrayList<MethodCallDetailDO>();
        for(InvocationDetail detail:exceptionDetailList){
            MethodCallDetailDO detailDO = convertDetail(detail);
            detailDOList.add(detailDO);
        }

        if(isEnableReporte){
            athenaDataService.reportMethodCallDetail(detailDOList);
        }
    }

    /**
     * 明细转换，由各端上报类实现
     * @param detail
     * @return
     */
    abstract MethodCallDetailDO convertDetail(InvocationDetail detail);


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
    public void reportStatisticList(Collection<InvocationStatistic> statisticList){
        if(CollectionUtils.isEmpty(statisticList)){
            return;
        }
        AthenaDataService athenaDataService = getAthenaDataService();

        List<MethodStaticDO> staticDOList = new ArrayList<MethodStaticDO>();
        for(InvocationStatistic statistic:statisticList){
            if(statistic.getTotalNum().intValue() < 1){
                continue;
            }
            MethodStaticDO staticDO = convertStatistic(statistic);
            staticDOList.add(staticDO);
        }
        if(logger.isDebugEnabled()){
            logger.debug("report statistic size:{}.",staticDOList.size());
        }
        try {
            String statisticDOListOfJson = new JSONSerializer().serialize(staticDOList);
            if(logger.isDebugEnabled()){
                logger.debug("report statistic json:{}.",statisticDOListOfJson);
            }
        } catch (Exception e) {}

        if(isEnableReporte){
            athenaDataService.reportMethodStatic(staticDOList);
        }
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
