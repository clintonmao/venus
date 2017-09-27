package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.backend.serializer.JSONSerializer;
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

    //Athena接口名称定义
    public static final String ATHENA_INTERFACE_SIMPLE_NAME = "AthenaDataService";
    public static final String ATHENA_INTERFACE_FULL_NAME = "com.athena.service.api.AthenaDataService";

    private AthenaDataService athenaDataService;

    //TODO 将序列化传输协议无关移到common包中
    private JSONSerializer jsonSerializer = new JSONSerializer();

    private boolean isEnableReporte = true;

    /**
     * 上报明细 TODO 上报放到reporter模块，可选择依赖
     * @param exceptionDetailList
     */
    public void reportDetailList(Collection<InvocationDetail> exceptionDetailList){
        AthenaDataService athenaDataService = getAthenaDataService();
        if(CollectionUtils.isEmpty(exceptionDetailList)){
            return;
        }

        List<MethodCallDetailDO> detailDOList = new ArrayList<MethodCallDetailDO>();
        for(InvocationDetail detail:exceptionDetailList){
            MethodCallDetailDO detailDO = convertDetail(detail);
            detailDOList.add(detailDO);
        }

        if(CollectionUtils.isNotEmpty(detailDOList)){
            /*
            try {
                String detailDoListOfJson = serialize(detailDOList);
                logger.info("report detailDOList json:{}.",detailDoListOfJson);
            } catch (Exception e) {}
            */
            logger.info("report detail size:{}.",detailDOList.size());
        }else{
            logger.info("report detail size:{}.",0);
        }

        if(isEnableReporte){
            if(CollectionUtils.isNotEmpty(detailDOList)){
                athenaDataService.reportMethodCallDetail(detailDOList);
            }
        }
    }

    /**
     * 明细转换，由各端上报类实现
     * @param detail
     * @return
     */
    abstract MethodCallDetailDO convertDetail(InvocationDetail detail);

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

        if(CollectionUtils.isNotEmpty(staticDOList)){
            /*
            try {
                String statisticDOListOfJson = serialize(staticDOList);
                logger.info("report staticDOList json:{}.",statisticDOListOfJson);
            } catch (Exception e) {}
            */
            logger.info("report static size:{}.",staticDOList.size());
        }else{
            logger.info("report static size:{}.",0);
        }

        if(isEnableReporte){
            if(CollectionUtils.isNotEmpty(staticDOList)){
                athenaDataService.reportMethodStatic(staticDOList);
            }
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

    /**
     * 判断是否athena接口
     * @param invocation
     * @return
     */
    boolean isAthenaInterface(Invocation invocation){
        String serviceInterfaceName = invocation.getServiceInterfaceName();
        return ATHENA_INTERFACE_SIMPLE_NAME.equalsIgnoreCase(serviceInterfaceName) || ATHENA_INTERFACE_FULL_NAME.equalsIgnoreCase(serviceInterfaceName);
    }


    /**
     * 序列化对象
     * @param object
     * @return
     */
    String serialize(Object object){
        try {
            String json = jsonSerializer.serialize(object);
            return json;
        } catch (Exception e) {
            logger.error("serialize error.",e);
            return "";
        }
    }

    public AthenaDataService getAthenaDataService() {
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }
}
