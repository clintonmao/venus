package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.monitor.VenusMonitorFactory;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import java.util.List;

/**
 * 监控上报代理
 * Created by Zhangzhihua on 2017/9/5.
 */
public class VenusMonitorReporter {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * 上报明细
     * @param detailDOList
     */
    public void reportDetailList(List<MethodCallDetailDO> detailDOList){
        if(CollectionUtils.isEmpty(detailDOList)){
            return;
        }
        if(logger.isInfoEnabled()){
            logger.info("report detail size:{}.",detailDOList.size());
        }

        AthenaDataService athenaDataService = getAthenaDataService();
        if(athenaDataService != null){
            if(logger.isInfoEnabled()){
                logger.info("do report detail size:{}.",detailDOList.size());
            }
            athenaDataService.reportMethodCallDetail(detailDOList);
        }
    }

    /**
     * 上报统计数据
     * @param staticDOList
     */
    public void reportStatisticList(List<MethodStaticDO> staticDOList){
        if(CollectionUtils.isEmpty(staticDOList)){
            return;
        }
        if(logger.isInfoEnabled()){
            logger.info("report static size:{}.",staticDOList.size());
        }

        AthenaDataService athenaDataService = getAthenaDataService();
        if(athenaDataService != null){
            if(logger.isInfoEnabled()){
                logger.info("do report static size:{}.",staticDOList.size());
            }
            athenaDataService.reportMethodStatic(staticDOList);
        }
    }

    public AthenaDataService getAthenaDataService() {
        return VenusMonitorFactory.getInstance().getAthenaDataService();
    }

}
