package com.meidusa.venus.monitor.reporter;

import com.alibaba.fastjson.JSON;
import com.athena.venus.domain.VenusMethodCallDetailDO;
import com.athena.venus.domain.VenusMethodStaticDO;
import com.athena.venus.domain.VenusReportDO;
import com.meidusa.venus.monitor.VenusMonitorFactory;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;

import java.util.Date;
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
    public void reportDetailAndStatic(List<VenusMethodCallDetailDO> detailDOList, List<VenusMethodStaticDO> staticDOList){
        if(CollectionUtils.isEmpty(detailDOList) && CollectionUtils.isEmpty(staticDOList)){
            return;
        }
        int detailNum = 0;
        if(CollectionUtils.isNotEmpty(detailDOList)){
            detailNum = detailDOList.size();
        }
        int staticNum = 0;
        if(CollectionUtils.isNotEmpty(staticDOList)){
            staticNum = staticDOList.size();
        }

        VenusReportDO reportDO = new VenusReportDO();
        reportDO.setMethodCallDetailDOs(detailDOList);
        reportDO.setMethodStaticDOs(staticDOList);
        Producer<String,String> kafkaProducer = getKafkaProducer();
        if(kafkaProducer != null){
            String reportMsg = JSON.toJSONString(reportDO);
            //logger.info("reportMsg:{}",reportMsg);
            logger.info("report venus monitor info,static size:{},detail size:{}.",staticNum,detailNum);
            kafkaProducer.send(new ProducerRecord<String, String>(
                    getTopic(),
                    String.valueOf(new Date().getTime()),
                    reportMsg)
            );
            kafkaProducer.flush();
        }

    }

    public Producer<String,String> getKafkaProducer() {
        if(VenusMonitorFactory.getInstance() == null){
            return null;
        }
        return VenusMonitorFactory.getInstance().getKafkaProducer();
    }

    String getTopic(){
        return VenusMonitorFactory.getInstance().getTopic();
    }

}
