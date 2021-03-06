package com.meidusa.venus.monitor.task;

/**
 * Created by Zhangzhihua on 2017/11/30.
 */

import com.meidusa.venus.ClientInvocationOperation;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;
import com.meidusa.venus.monitor.support.VenusMonitorConstants;
import com.meidusa.venus.monitor.support.VenusMonitorUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Queue;

/**
 * 1秒钟数据汇总任务，过滤异常/慢操作记录，汇总统计
 */
public class ClientMonitorProcessTask implements Runnable{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //明细队列
    private Queue<InvocationDetail> detailQueue = null;
    //待上报明细队列
    private Queue<InvocationDetail> reportDetailQueue = null;
    //方法调用汇总映射表
    private Map<String,InvocationStatistic> statisticMap = null;

    public ClientMonitorProcessTask(Queue<InvocationDetail> detailQueue, Queue<InvocationDetail> reportDetailQueue, Map<String,InvocationStatistic> statisticMap){
        this.detailQueue = detailQueue;
        this.reportDetailQueue = reportDetailQueue;
        this.statisticMap = statisticMap;
    }

    @Override
    public void run() {
        while(true){
            try {
                int fetchNum = VenusMonitorConstants.FETCH_NUM;
                if(detailQueue.size() < fetchNum){
                    fetchNum = detailQueue.size();
                }
                for(int i=0;i<fetchNum;i++){
                    InvocationDetail detail = detailQueue.poll();
                    //1、过滤明细，异常或慢操作
                    if(detail.isExceptionOperation() || detail.isSlowOperation()){
                        if(reportDetailQueue.size() < VenusMonitorConstants.QUEU_MAX_SIZE){
                            reportDetailQueue.add(detail);
                        }
                    }

                    //2、汇总统计，查1m内汇总记录，若不存在则新建
                    String methodAndEnvPath = getMethodAndEnvPath(detail);
                    if(statisticMap.get(methodAndEnvPath) == null){
                        InvocationStatistic statistic = new InvocationStatistic();
                        statistic.init(detail);
                        statisticMap.put(methodAndEnvPath,statistic);
                    }
                    InvocationStatistic invocationStatistic = statisticMap.get(methodAndEnvPath);
                    invocationStatistic.append(detail);
                }
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("process monitor detail error.",e);
                }
            }

            try {
                //1s计算一次
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
        }
    }

    int getRole() {
        return VenusMonitorConstants.ROLE_CONSUMER;
    }

    /**
     * 获取调用方法及调用环境标识路径
     * @return
     */
    String getMethodAndEnvPath(InvocationDetail detail){
        ClientInvocationOperation clientInvocation = (ClientInvocationOperation) detail.getInvocation();
        //请求时间，精确为分钟
        String requestTimeOfMinutes = getTimeOfMinutes(clientInvocation.getRequestTime());

        //方法路径信息
        String methodAndEnvPath = String.format(
                "%s/%s?version=%s&method=%s&target=%s&startTime=%s",
                clientInvocation.getServiceInterfaceName(),
                clientInvocation.getServiceName(),
                clientInvocation.getVersion(),
                clientInvocation.getMethodName(),
                detail.getUrl().getHost(),
                requestTimeOfMinutes
        );
        if(logger.isDebugEnabled()){
            logger.debug("methodAndEnvPath:{}.", methodAndEnvPath);
        }
        return methodAndEnvPath;
    }

    /**
     * 获取时间，精确到分钟
     * @param date
     * @return
     */
    String getTimeOfMinutes(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.SECOND,0);
        SimpleDateFormat format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
        String sTime = format.format(calendar.getTime());
        return sTime;
    }



}
