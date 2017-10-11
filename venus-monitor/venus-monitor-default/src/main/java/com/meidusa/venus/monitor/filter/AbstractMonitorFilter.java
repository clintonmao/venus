package com.meidusa.venus.monitor.filter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.monitor.reporter.VenusMonitorReporter;
import com.meidusa.venus.util.JSONUtil;
import org.apache.commons.collections.CollectionUtils;
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
public abstract class AbstractMonitorFilter {

    private static Logger logger = LoggerFactory.getLogger(AbstractMonitorFilter.class);

    //明细队列
    Queue<InvocationDetail> detailQueue = new LinkedBlockingQueue<InvocationDetail>();

    //异常及慢操作明细队列
    Queue<InvocationDetail> exceptionDetailQueue = new LinkedBlockingQueue<InvocationDetail>();

    //方法调用汇总映射表
    Map<String,InvocationStatistic> statisticMap = new ConcurrentHashMap<String,InvocationStatistic>();

    boolean isRunningRunnable = false;

    Executor processExecutor = Executors.newFixedThreadPool(1);

    Executor reporterExecutor = Executors.newFixedThreadPool(1);

    VenusMonitorReporter monitorReporter = null;

    AthenaDataService athenaDataService = null;

    //Athena接口名称定义
    public static final String ATHENA_INTERFACE_SIMPLE_NAME = "AthenaDataService";
    public static final String ATHENA_INTERFACE_FULL_NAME = "com.athena.service.api.AthenaDataService";


    public AbstractMonitorFilter(){
    }

    /**
     * 起动数据计算及上报线程
     */
    void startProcessAndReporterTread(){
        if(!isRunningRunnable){
            processExecutor.execute(new InvocationDataProcessRunnable());
            reporterExecutor.execute(new InvocationDataReportRunnable());
            isRunningRunnable = true;
        }
    }

    /**
     * 添加到明细队列
     * @param invocationDetail
     */
    public void putInvocationDetailQueue(InvocationDetail invocationDetail){
        try {
            if(logger.isDebugEnabled()){
                logger.debug("add invocation detail queue:{}.",invocationDetail);
            }
            detailQueue.add(invocationDetail);
        } catch (Exception e) {
            //不处理异常，避免影响主流程
            logger.error("add monitor queue error.",e);
        }
    }

    /**
     * 获取调用方法及调用环境标识路径
     * @param detail
     * @return
     */
    abstract String getMethodAndEnvPath(InvocationDetail detail);

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
     * 调用数据处理，过滤异常/慢操作记录，汇总统计数据
     */
    class InvocationDataProcessRunnable implements Runnable{
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
     * 调用数据上报处理
     */
    class InvocationDataReportRunnable implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    VenusMonitorReporter monitorReporter = getMonitorReporter();
                    if(monitorReporter == null){
                        logger.error("get monitorReporter is null.");
                        continue;
                    }

                    //上报异常、慢操作数据
                    logger.info("monitor detail queue size:{}.",exceptionDetailQueue.size());
                    //TODO 改为批量拿 锁必要性？
                    List<InvocationDetail> detailList = new ArrayList<InvocationDetail>();
                    int fetchNum = 50;
                    if(exceptionDetailQueue.size() < fetchNum){
                        fetchNum = exceptionDetailQueue.size();
                    }
                    for(int i=0;i<fetchNum;i++){
                        InvocationDetail exceptionDetail = exceptionDetailQueue.poll();
                        detailList.add(exceptionDetail);
                    }
                    try {
                        if(CollectionUtils.isNotEmpty(detailList)){
                            monitorReporter.reportDetailList(toDetailDOList(detailList));
                        }
                    } catch (Exception e) {
                        logger.error("report exception detail error.",e);
                    }

                    //上报服务调用汇总数据 TODO 要不要锁？
                    logger.info("monitor statistic queue size:{}.",statisticMap.size());
                    Collection<InvocationStatistic> statisticCollection = statisticMap.values();
                    try {
                        if(CollectionUtils.isNotEmpty(statisticCollection)){
                            monitorReporter.reportStatisticList(toStaticDOList(statisticCollection));
                        }
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
                    Thread.sleep(1000*60);
                } catch (InterruptedException e) {
                }
            }


        }
    }

    /**
     * 转化detailDOList
     * @param detailList
     * @return
     */
    List<MethodCallDetailDO> toDetailDOList(List<InvocationDetail> detailList){
        List<MethodCallDetailDO> detailDOList = new ArrayList<MethodCallDetailDO>();
        for(InvocationDetail detail:detailList){
            MethodCallDetailDO detailDO = convertDetail(detail);
            detailDOList.add(detailDO);
        }
        return detailDOList;
    }

    /**
     * 明细转换，由各端上报类实现
     * @param detail
     * @return
     */
    abstract MethodCallDetailDO convertDetail(InvocationDetail detail);

    /**
     * 转化staticDOList
     * @param statisticList
     * @return
     */
    List<MethodStaticDO> toStaticDOList(Collection<InvocationStatistic> statisticList){
        List<MethodStaticDO> staticDOList = new ArrayList<MethodStaticDO>();
        for(InvocationStatistic statistic:statisticList){
            if(statistic.getTotalNum().intValue() < 1){
                continue;
            }
            MethodStaticDO staticDO = convertStatistic(statistic);
            staticDOList.add(staticDO);
        }
        return staticDOList;
    }

    /**
     * 转换为statisticDo
     * @param statistic
     * @return
     */
    MethodStaticDO convertStatistic(InvocationStatistic statistic){
        MethodStaticDO staticDO = new MethodStaticDO();
        staticDO.setInterfaceName(statistic.getServiceInterfaceName());
        staticDO.setServiceName(statistic.getServiceName());
        staticDO.setVersion(statistic.getVersion());
        staticDO.setMethodName(statistic.getMethod());
        staticDO.setTotalCount((statistic.getTotalNum().intValue()));
        staticDO.setFailCount(statistic.getFailNum().intValue());
        staticDO.setSlowCount(statistic.getSlowNum().intValue());
        staticDO.setAvgDuration(statistic.getAvgCostTime().intValue());
        staticDO.setMaxDuration(statistic.getMaxCostTime().intValue());

        staticDO.setDomain(statistic.getApplication());
        staticDO.setSourceIp(statistic.getHost());
        staticDO.setStartTime(statistic.getBeginTime());
        staticDO.setEndTime(statistic.getEndTime());
        return staticDO;
    }

    VenusMonitorReporter getMonitorReporter(){
        if(monitorReporter == null){
            monitorReporter = new VenusMonitorReporter();
            monitorReporter.setAthenaDataService(this.getAthenaDataService());
        }
        return monitorReporter;
    }

    public AthenaDataService getAthenaDataService() {
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }

    /**
     * 判断是否athena接口
     * @param invocation
     * @return
     */
    boolean isAthenaInterface(Invocation invocation){
        String serviceInterfaceName = invocation.getServiceInterfaceName();
        return ATHENA_INTERFACE_SIMPLE_NAME.equalsIgnoreCase(serviceInterfaceName) || ATHENA_INTERFACE_FULL_NAME.equalsIgnoreCase(serviceInterfaceName);
    }

    String serialize(Object object){
        return JSONUtil.toJSONString(object);
    }

}
