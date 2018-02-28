package com.meidusa.venus.monitor.task;

/**
 * Created by Zhangzhihua on 2017/11/30.
 */

import com.athena.venus.domain.VenusMethodCallDetailDO;
import com.athena.venus.domain.VenusMethodStaticDO;
import com.meidusa.venus.monitor.MonitorDataConvert;
import com.meidusa.venus.monitor.reporter.VenusMonitorReporter;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;
import com.meidusa.venus.monitor.support.VenusMonitorConstants;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;

import java.util.*;

/**
 * 1分钟数据上报任务
 */
public class VenusMonitorReportTask implements Runnable{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //明细队列
    private Queue<InvocationDetail> detailQueue = null;
    //待上报明细队列
    private Queue<InvocationDetail> reportDetailQueue = null;
    //方法调用汇总映射表
    private Map<String,InvocationStatistic> statisticMap = null;
    //监控上报操作对象
    private MonitorDataConvert monitorOperation = null;

    //venus上报reporter
    private VenusMonitorReporter monitorReporter = new VenusMonitorReporter();

    public VenusMonitorReportTask(Queue<InvocationDetail> detailQueue, Queue<InvocationDetail> reportDetailQueue, Map<String,InvocationStatistic> statisticMap, MonitorDataConvert monitorOperation){
        this.detailQueue = detailQueue;
        this.reportDetailQueue = reportDetailQueue;
        this.statisticMap = statisticMap;
        this.monitorOperation = monitorOperation;
    }

    @Override
    public void run() {
        while(true){
            try {
                //1、构造明细上报数据
                if(reportDetailQueue != null && reportDetailQueue.size() > 0){
                    if(logger.isDebugEnabled()){
                        logger.debug("current detail report queue size:{}.", reportDetailQueue.size());
                    }
                }
                List<InvocationDetail> reportDetailList = new ArrayList<InvocationDetail>();
                int fetchNum = VenusMonitorConstants.perDetailReportNum;
                if(reportDetailQueue.size() < fetchNum){
                    fetchNum = reportDetailQueue.size();
                }
                for(int i=0;i<fetchNum;i++){
                    InvocationDetail exceptionDetail = reportDetailQueue.poll();
                    reportDetailList.add(exceptionDetail);
                }

                //2、构造汇总上报数据
                List<InvocationStatistic> reportStatisticList = new ArrayList<>();
                List<String> deleteKeys = new ArrayList<>();
                for(Map.Entry<String,InvocationStatistic> entry:statisticMap.entrySet()){
                    String key = entry.getKey();
                    InvocationStatistic statistic = entry.getValue();
                    //过滤currentTime > statistic.endTime的记录，添加到统计上报列表，上报完要删除
                    if(new Date().after(statistic.getEndTime())){
                        reportStatisticList.add(statistic);
                        deleteKeys.add(key);
                    }
                }
                if(reportStatisticList != null && reportStatisticList.size() > 0){
                    if(logger.isDebugEnabled()){
                        logger.debug("current statistic report list size:{}.",reportStatisticList.size());
                    }
                }

                //3、上报统计及明细数据
                if(CollectionUtils.isNotEmpty(reportDetailList) || CollectionUtils.isNotEmpty(reportStatisticList)){
                    try {
                        List<VenusMethodCallDetailDO> detailDOList = toDetailDOList(reportDetailList);
                        List<VenusMethodStaticDO> staticDOList = toStaticDOList(reportStatisticList);
                        monitorReporter.reportDetailAndStatic(detailDOList,staticDOList);
                    } catch (Throwable e) {
                        if(exceptionLogger.isErrorEnabled()){
                            exceptionLogger.error("report detail and static error.",e);
                        }
                    }
                }

                //4、清空统计信息
                if(MapUtils.isNotEmpty(statisticMap) && CollectionUtils.isNotEmpty(deleteKeys)){
                    for(String delKey:deleteKeys){
                        if(statisticMap.containsKey(delKey)){
                            statisticMap.remove(delKey);
                        }
                    }
                }
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("report error.",e);
                }
            }

            try {
                //1m上报一次
                Thread.sleep(1000*60);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * 转化明细列表
     * @param detailList
     * @return
     */
    List<VenusMethodCallDetailDO> toDetailDOList(Collection<InvocationDetail> detailList){
        List<VenusMethodCallDetailDO> detailDOList = new ArrayList<VenusMethodCallDetailDO>();
        if(CollectionUtils.isEmpty(detailList)){
            return detailDOList;
        }
        for(InvocationDetail detail:detailList){
            VenusMethodCallDetailDO detailDO = monitorOperation.convertDetail(detail);
            detailDOList.add(detailDO);
        }
        return detailDOList;
    }

    /**
     * 转化staticDOList
     * @param statisticList
     * @return
     */
    List<VenusMethodStaticDO> toStaticDOList(Collection<InvocationStatistic> statisticList){
        List<VenusMethodStaticDO> staticDOList = new ArrayList<VenusMethodStaticDO>();
        for(InvocationStatistic statistic:statisticList){
            if(statistic.getTotalNum().intValue() < 1){
                continue;
            }
            VenusMethodStaticDO staticDO = monitorOperation.convertStatistic(statistic);
            staticDOList.add(staticDO);
        }
        return staticDOList;
    }




}
