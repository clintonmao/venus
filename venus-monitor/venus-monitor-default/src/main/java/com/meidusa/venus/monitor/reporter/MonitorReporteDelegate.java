package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.monitor.filter.BaseMonitorFilter;
import com.meidusa.venus.monitor.filter.support.InvocationDetail;
import com.meidusa.venus.monitor.filter.support.InvocationStatistic;
import com.meidusa.venus.util.UUID;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 监控上报代理
 * Created by Zhangzhihua on 2017/9/5.
 */
public class MonitorReporteDelegate {

    private static Logger logger = LoggerFactory.getLogger(MonitorReporteDelegate.class);

    private AthenaDataService athenaDataService;

    /**
     * 上报异常明细 TODO 上报放到reporter模块，可选择依赖
     * @param exceptionDetailList
     */
    public void reportExceptionDetailList(Collection<InvocationDetail> exceptionDetailList){
        AthenaDataService athenaDataService = getAthenaDataService();
        logger.info("report exceptionDetailList size:{}.",exceptionDetailList.size());
        if(CollectionUtils.isEmpty(exceptionDetailList)){
            return;
        }

        List<MethodCallDetailDO> detailDOList = new ArrayList<MethodCallDetailDO>();
        for(InvocationDetail detail:exceptionDetailList){
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
    MethodCallDetailDO convertDetail(InvocationDetail detail){
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
        detailDO.setRequestTime(detail.getRequestTime());
        detailDO.setServiceName(invocation.getService().name());
        detailDO.setInterfaceName(invocation.getService().name());
        detailDO.setMethodName(invocation.getEndpoint().name());
        detailDO.setConsumerIp(url.getHost());
        //响应信息
        detailDO.setResponseTime(detail.getResponseTime());
        //状态相关

        return detailDO;
    }

    /**
     * 上报统计数据 TODO 上报放到reporter模块，可选择依赖
     * @param statisticList
     */
    public void reportStatisticList(Collection<InvocationStatistic> statisticList){
        AthenaDataService athenaDataService = getAthenaDataService();
        logger.info("report statisticList size:{}.",statisticList.size());
        if(CollectionUtils.isEmpty(statisticList)){
            return;
        }

        List<MethodStaticDO> staticDOList = new ArrayList<MethodStaticDO>();
        for(InvocationStatistic statistic:statisticList){
            MethodStaticDO staticDO = convertStatistic(statistic);
            staticDOList.add(staticDO);
        }
        athenaDataService.reportMethodStatic(staticDOList);
    }

    /**
     * 转换为statisticDo
     * @param statistic
     * @return
     */
    MethodStaticDO convertStatistic(InvocationStatistic statistic){
        MethodStaticDO staticDO = new MethodStaticDO();
        return staticDO;
    }

    public AthenaDataService getAthenaDataService() {
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }
}
