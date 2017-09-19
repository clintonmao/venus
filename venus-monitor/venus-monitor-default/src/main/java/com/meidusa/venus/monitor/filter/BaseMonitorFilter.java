package com.meidusa.venus.monitor.filter;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.monitor.reporter.MonitorReporterDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * monitor基类
 * Created by Zhangzhihua on 2017/9/4.
 */
public class BaseMonitorFilter {

    private static Logger logger = LoggerFactory.getLogger(BaseMonitorFilter.class);

    //明细队列 TODO static处理
    static Queue<InvocationDetail> detailQueue = new LinkedBlockingQueue<InvocationDetail>();

    //异常及慢操作明细队列
    static Queue<InvocationDetail> exceptionDetailQueue = new LinkedBlockingQueue<InvocationDetail>();

    //方法调用汇总映射表
    static Map<String,InvocationStatistic> statisticMap = new ConcurrentHashMap<String,InvocationStatistic>();

    static boolean isRunningRunnable = false;

    static Executor processExecutor = Executors.newFixedThreadPool(1);

    static Executor reporterExecutor = Executors.newFixedThreadPool(1);

    MonitorReporterDelegate monitorReporteDelegate = null;

    private AthenaDataService athenaDataService = null;

    public BaseMonitorFilter(){
    }

    /**
     * 添加到明细队列
     * @param invocationDetail
     */
    public void pubInvocationDetailQueue(InvocationDetail invocationDetail){
        //TODO 改初始化时机
        if(!isRunningRunnable){
            processExecutor.execute(new InvocationDetailProcessRunnable());
            reporterExecutor.execute(new InvocationDataReportRunnable());
            isRunningRunnable = true;
        }

        try {
            logger.info("add invocation detail queue:{}.",invocationDetail);
            detailQueue.add(invocationDetail);
        } catch (Exception e) {
            //不处理异常，避免影响主流程
            logger.error("add monitor queue error.",e);
        }
    }

    /**
     * 获取调用方法及调用环境标识路径
     * @return
     */
    String getMethodAndEnvPath(InvocationDetail detail){
        ClientInvocation invocation = null;
        if(detail.getInvocation() instanceof ClientInvocation){
            invocation = (ClientInvocation)detail.getInvocation();
        }
        //请求时间，精确为分钟
        String requestTimeOfMinutes = getTimeOfMinutes(invocation.getRequestTime());
        //方法路径信息
        String methodAndEnvPath = String.format(
                "%s/%s?version=%s&method=%s&startTime=%s",
                invocation.getMethod().getDeclaringClass().getName(),
                invocation.getServiceName(),
                "0.0.0",
                invocation.getMethod().getName(),
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

    /**
     * 判断是否操作异常
     * @param detail
     * @return
     */
    boolean isExceptionOperation(InvocationDetail detail){
        return detail.getException() == null;
    }

    /**
     * 判断是否为慢操作
     * @param detail
     * @return
     */
    boolean isSlowOperation(InvocationDetail detail){
        //TODO 根据配置判断是否为慢操作
        return true;
    }


    /**
     * 明细数据处理，过滤异常/慢操作记录，汇总统计数据
     */
    class InvocationDetailProcessRunnable implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    //TODO 批量处理
                    int fetchNum = 10;
                    if(detailQueue.size() < fetchNum){
                        fetchNum = detailQueue.size();
                    }
                    for(int i=0;i<fetchNum;i++){
                        InvocationDetail detail = detailQueue.poll();
                        //过滤异常、慢操作数据
                        if(isExceptionOperation(detail) || isSlowOperation(detail)){
                            exceptionDetailQueue.add(detail);
                        }

                        //汇总调用统计，查1m内汇总记录，若不存在则新建
                        String methodAndEnvPath = getMethodAndEnvPath(detail);
                        if(statisticMap.get(methodAndEnvPath) == null){
                            statisticMap.put(methodAndEnvPath,new InvocationStatistic(detail));
                        }
                        InvocationStatistic invocationStatistic = statisticMap.get(methodAndEnvPath);
                        invocationStatistic.append(detail);
                    }
                } catch (Exception e) {
                    logger.error("process invocation detail error.",e);
                }

                try {
                    //1s计算一次
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }

    }

    /**
     * 明细、汇总数据上报处理
     */
    class InvocationDataReportRunnable implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    MonitorReporterDelegate reporteDelegate = getMonitorReporteDelegate();
                    if(reporteDelegate == null){
                        logger.error("get reporteDelegate is null.");
                        continue;
                    }

                    //上报异常、慢操作数据
                    logger.info("total exception detail size:{}.",exceptionDetailQueue.size());
                    //TODO 改为批量拿 锁必要性？
                    List<InvocationDetail> exceptionDetailList = new ArrayList<InvocationDetail>();
                    int fetchNum = 50;
                    if(exceptionDetailQueue.size() < fetchNum){
                        fetchNum = exceptionDetailQueue.size();
                    }
                    for(int i=0;i<fetchNum;i++){
                        InvocationDetail exceptionDetail = exceptionDetailQueue.poll();
                        exceptionDetailList.add(exceptionDetail);
                    }
                    try {
                        reporteDelegate.reportExceptionDetailList(exceptionDetailList);
                    } catch (Exception e) {
                        logger.error("report exception detail error.",e);
                    }

                    //上报服务调用汇总数据 TODO 要不要锁？
                    logger.info("total statistic size:{}.",statisticMap.size());
                    Collection<InvocationStatistic> statistics = statisticMap.values();
                    try {
                        reporteDelegate.reportStatisticList(statistics);
                    } catch (Exception e) {
                        logger.error("report statistic error.",e);
                    }
                    //重置统计信息
                    for(Map.Entry<String,InvocationStatistic> entry:statisticMap.entrySet()){
                        entry.getValue().reset();
                    }
                } catch (Exception e) {
                    logger.error("report error.",e);
                }

                try {
                    //1m上报一次
                    Thread.sleep(1000*10);
                } catch (InterruptedException e) {
                }
            }


        }
    }

    /**
     * 获取监控上报代理
     * @return
     */
    MonitorReporterDelegate getMonitorReporteDelegate(){
        if(monitorReporteDelegate == null){
            monitorReporteDelegate = new MonitorReporterDelegate();
            monitorReporteDelegate.setAthenaDataService(this.getAthenaDataService());
        }
        return monitorReporteDelegate;
    }

    public AthenaDataService getAthenaDataService() {
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }
}
