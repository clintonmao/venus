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
     * 上报明细及统计数据
     * @param detailDOList
     * @param staticDOList
     */
    public void reportDetailAndStatic(List<MethodCallDetailDO> detailDOList,List<MethodStaticDO> staticDOList){
        if(CollectionUtils.isEmpty(detailDOList) && CollectionUtils.isEmpty(staticDOList)){
            return;
        }
        if(CollectionUtils.isEmpty(detailDOList)){
            if(logger.isDebugEnabled()){
                logger.debug("report detail size:{}.",detailDOList.size());
            }
        }
        if(CollectionUtils.isEmpty(staticDOList)){
            if(logger.isDebugEnabled()){
                logger.debug("report static size:{}.",staticDOList.size());
            }
        }

        AthenaDataService athenaDataService = getAthenaDataService();
        if(athenaDataService != null){
            athenaDataService.reportDetailAndStatic(detailDOList,staticDOList);
        }

    }

    public AthenaDataService getAthenaDataService() {
        if(VenusMonitorFactory.getInstance() == null){
            return null;
        }
        return VenusMonitorFactory.getInstance().getAthenaDataService();
    }

}
