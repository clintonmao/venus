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

        VenusReportDO reportDO = new VenusReportDO();
        reportDO.setMethodCallDetailDOs(detailDOList);
        reportDO.setMethodStaticDOs(staticDOList);
        Producer<String,String> kafkaProducer = getKafkaProducer();
        if(kafkaProducer != null){
            logger.info("##########send message with MQ.");
            kafkaProducer.send(new ProducerRecord<String, String>(
                    getTopic(),
                    String.valueOf(new Date().getTime()),
                    JSON.toJSONString(reportDO))
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
