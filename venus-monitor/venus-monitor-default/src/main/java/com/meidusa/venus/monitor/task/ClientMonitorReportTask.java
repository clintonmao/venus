package com.meidusa.venus.monitor.task;

/**
 * Created by Zhangzhihua on 2017/11/30.
 */

import com.athena.venus.domain.VenusMethodCallDetailDO;
import com.athena.venus.domain.VenusMethodStaticDO;
import com.meidusa.venus.ClientInvocationOperation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.monitor.reporter.VenusMonitorReporter;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;
import com.meidusa.venus.monitor.support.VenusMonitorConstants;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.UUIDUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;

import java.util.*;

/**
 * 1分钟数据上报任务
 */
public class ClientMonitorReportTask implements Runnable{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //明细队列
    private Queue<InvocationDetail> detailQueue = null;
    //待上报明细队列
    private Queue<InvocationDetail> reportDetailQueue = null;
    //方法调用汇总映射表
    private Map<String,InvocationStatistic> statisticMap = null;

    //venus上报reporter
    private VenusMonitorReporter monitorReporter = new VenusMonitorReporter();

    public ClientMonitorReportTask(Queue<InvocationDetail> detailQueue, Queue<InvocationDetail> reportDetailQueue, Map<String,InvocationStatistic> statisticMap){
        this.detailQueue = detailQueue;
        this.reportDetailQueue = reportDetailQueue;
        this.statisticMap = statisticMap;
    }

    @Override
    public void run() {
        while(true){
            try {
                //1、构造明细上报数据
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
            VenusMethodCallDetailDO detailDO = convertDetail(detail);
            detailDOList.add(detailDO);
        }
        return detailDOList;
    }

    /**
     * 转化为detailDo
     * @param detail
     * @return
     */
    VenusMethodCallDetailDO convertDetail(InvocationDetail detail){
        ClientInvocationOperation clientInvocation = (ClientInvocationOperation)detail.getInvocation();
        URL url = detail.getUrl();
        Result result = detail.getResult();
        Throwable exception = detail.getException();

        VenusMethodCallDetailDO detailDO = new VenusMethodCallDetailDO();
        //基本信息
        detailDO.setId(UUIDUtil.create().toString());
        detailDO.setRpcId(clientInvocation.getRpcId());
        if(clientInvocation.getAthenaId() != null){
            detailDO.setTraceId(new String(clientInvocation.getAthenaId()));
        }
        if(clientInvocation.getMessageId() != null){
            detailDO.setMessageId(new String(clientInvocation.getMessageId()));
        }
        detailDO.setSourceType(detail.getFrom());

        //请求信息
        if(clientInvocation.getServiceInterface() != null){
            detailDO.setInterfaceName(clientInvocation.getServiceInterface().getName());
        }
        detailDO.setServiceName(clientInvocation.getServiceName());
        if(clientInvocation.getEndpoint() != null){
            detailDO.setMethodName(clientInvocation.getEndpoint().getName());
        }else if(clientInvocation.getMethod() != null){
            detailDO.setMethodName(clientInvocation.getMethod().getName());
        }
        if(clientInvocation.getArgs() != null){
            String requestJson = serialize(clientInvocation.getArgs());
            detailDO.setRequestJson(requestJson);
        }
        detailDO.setRequestTime(clientInvocation.getRequestTime());
        detailDO.setProviderDomain(url.getApplication());
        detailDO.setProviderIp(url.getHost());
        detailDO.setConsumerDomain(clientInvocation.getConsumerApp());
        detailDO.setConsumerIp(clientInvocation.getConsumerIp());

        //响应信息
        detailDO.setResponseTime(detail.getResponseTime());
        if(result != null){ //响应结果
            if(result.getErrorCode() == 0){//成功
                String responseJson = serialize(result.getResult());
                detailDO.setReponseJson(responseJson);
                detailDO.setStatus(1);
            }else{//失败
                String responseJson = String.format("%s-%s",result.getErrorCode(),result.getErrorMessage());
                detailDO.setReponseJson(responseJson);
                detailDO.setStatus(0);
            }
        } else if(exception != null){//响应异常
            String responseJsonForException = serialize(exception);
            detailDO.setErrorInfo(responseJsonForException);
            detailDO.setStatus(0);
        }
        //耗时
        long costTime = detail.getResponseTime().getTime()-clientInvocation.getRequestTime().getTime();
        detailDO.setDurationMillisecond(Integer.parseInt(String.valueOf(costTime)));
        //状态相关

        return detailDO;
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
            VenusMethodStaticDO staticDO = convertStatistic(statistic);
            staticDOList.add(staticDO);
        }
        return staticDOList;
    }

    /**
     * 转换为statisticDo
     * @param statistic
     * @return
     */
    VenusMethodStaticDO convertStatistic(InvocationStatistic statistic){
        VenusMethodStaticDO staticDO = new VenusMethodStaticDO();
        staticDO.setInterfaceName(statistic.getServiceInterfaceName());
        staticDO.setServiceName(statistic.getServiceName());
        staticDO.setVersion(statistic.getVersion());
        staticDO.setMethodName(statistic.getMethod());
        staticDO.setTotalCount((statistic.getTotalNum().intValue()));
        staticDO.setFailCount(statistic.getFailNum().intValue());
        staticDO.setSlowCount(statistic.getSlowNum().intValue());
        staticDO.setAvgDuration(statistic.getAvgCostTime().intValue());
        staticDO.setMaxDuration(statistic.getMaxCostTime().intValue());

        staticDO.setDomain(statistic.getProviderApp());
        staticDO.setSourceIp(statistic.getProviderIp());
        staticDO.setStartTime(statistic.getBeginTime());
        staticDO.setEndTime(statistic.getEndTime());
        return staticDO;
    }

    String serialize(Object object){
        return JSONUtil.toJSONString(object);
    }

}
