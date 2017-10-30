package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.exception.VenusConfigException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 监控上报代理
 * Created by Zhangzhihua on 2017/9/5.
 */
public class VenusMonitorReporter {

    private static Logger logger = LoggerFactory.getLogger(VenusMonitorReporter.class);

    private AthenaDataService athenaDataService;

    /**
     * 上报明细
     * @param detailDOList
     */
    public void reportDetailList(List<MethodCallDetailDO> detailDOList){
        if(CollectionUtils.isEmpty(detailDOList)){
            return;
        }

        if(CollectionUtils.isNotEmpty(detailDOList)){
            if(logger.isInfoEnabled()){
                logger.info("report detail size:{}.",detailDOList.size());
            }
        }else{
            if(logger.isInfoEnabled()){
                logger.info("report detail size:{}.",0);
            }
        }

        if(CollectionUtils.isNotEmpty(detailDOList)){
            getAthenaDataService().reportMethodCallDetail(detailDOList);
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

        if(CollectionUtils.isNotEmpty(staticDOList)){
            if(logger.isInfoEnabled()){
                logger.info("report static size:{}.",staticDOList.size());
            }
        }else{
            if(logger.isInfoEnabled()){
                logger.info("report static size:{}.",0);
            }
        }

        if(CollectionUtils.isNotEmpty(staticDOList)){
            getAthenaDataService().reportMethodStatic(staticDOList);
        }
    }

    public AthenaDataService getAthenaDataService() {
        if(athenaDataService == null){
            throw new VenusConfigException("athenaDataService is not inited.");
        }
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }
}
