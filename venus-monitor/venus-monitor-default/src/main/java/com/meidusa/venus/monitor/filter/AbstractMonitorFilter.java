package com.meidusa.venus.monitor.filter;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.monitor.reporter.VenusMonitorReporter;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

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

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //明细队列
    private Queue<InvocationDetail> detailQueue = new LinkedBlockingQueue<InvocationDetail>();

    //待上报明细队列
    private Queue<InvocationDetail> reportDetailQueue = new LinkedBlockingQueue<InvocationDetail>();

    //方法调用汇总映射表
    private Map<String,InvocationStatistic> statisticMap = new ConcurrentHashMap<String,InvocationStatistic>();

    //计算线程
    private Executor processExecutor = Executors.newFixedThreadPool(1);

    //上报线程
    private Executor reporterExecutor = Executors.newFixedThreadPool(1);

    private boolean isRunning = false;

    private VenusMonitorReporter monitorReporter = new VenusMonitorReporter();

    //consumer
    protected static int ROLE_CONSUMER = 0;
    //provider
    protected static int ROLE_PROVIDER = 2;
    //一次处理记录条数
    private static int perDetailProcessNum = 100;
    //一次上报记录条数
    private static int perDetailReportNum = 100;
    //慢操作耗时
    private static int SLOW_COST_TIME = 200;
    //支持最大队列长度
    private static int QUEU_MAX_SIZE = 50000;


    public AbstractMonitorFilter(){
    }

    /**
     * 起动数据计算及上报线程
     */
    public void startProcessAndReporterTread(){
        synchronized (AbstractMonitorFilter.class){
            if(!isRunning){
                processExecutor.execute(new InvocationDataProcessRunnable());
                reporterExecutor.execute(new InvocationDataReportRunnable());
                isRunning = true;
            }
        }
    }

    /**
     * 添加到明细队列
     * @param invocationDetail
     */
    public void putInvocationDetailQueue(InvocationDetail invocationDetail){
        try {
            if(detailQueue.size() > QUEU_MAX_SIZE){
                return;
            }
            detailQueue.add(invocationDetail);
        } catch (Exception e) {
            //不处理异常，避免影响主流程
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("add monitor queue error.",e);
            }
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
        if(detail.getException() != null){
            return true;
        }
        return detail.getResult() != null && detail.getResult().getErrorCode() != 0;
    }


    /**
     * 判断是否为慢操作
     * @param detail
     * @return
     */
    boolean isSlowOperation(InvocationDetail detail){
        if(detail.getResponseTime() == null){
            return true;
        }
        long costTime = detail.getResponseTime().getTime() - detail.getInvocation().getRequestTime().getTime();
        return costTime > SLOW_COST_TIME;
    }


    /**
     * 调用数据处理，过滤异常/慢操作记录，汇总统计数据
     */
    class InvocationDataProcessRunnable implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    int fetchNum = perDetailProcessNum;
                    if(detailQueue.size() < fetchNum){
                        fetchNum = detailQueue.size();
                    }
                    for(int i=0;i<fetchNum;i++){
                        InvocationDetail detail = detailQueue.poll();
                        //1、过滤明细，异常或慢操作
                        if(isExceptionOperation(detail) || isSlowOperation(detail)){
                            if(reportDetailQueue.size() < QUEU_MAX_SIZE){
                                reportDetailQueue.add(detail);
                            }
                        }

                        //2、汇总统计，查1m内汇总记录，若不存在则新建
                        if(getRole() == ROLE_CONSUMER){//只consumer处理汇总统计
                            String methodAndEnvPath = getMethodAndEnvPath(detail);
                            if(StringUtils.isNotEmpty(methodAndEnvPath)){
                                if(statisticMap.get(methodAndEnvPath) == null){
                                    InvocationStatistic statistic = new InvocationStatistic();
                                    statistic.init(detail);
                                    statisticMap.put(methodAndEnvPath,statistic);
                                }
                                InvocationStatistic invocationStatistic = statisticMap.get(methodAndEnvPath);
                                invocationStatistic.append(detail);
                            }
                        }
                    }
                } catch (Exception e) {
                    if(exceptionLogger.isErrorEnabled()){
                        exceptionLogger.error("process monitor detail error.",e);
                    }
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
     * 队列数据上报
     */
    class InvocationDataReportRunnable implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    //1、构造明细上报数据
                    if(logger.isDebugEnabled()){
                        logger.debug("current detail report queue size:{}.", reportDetailQueue.size());
                    }
                    List<InvocationDetail> detailList = new ArrayList<InvocationDetail>();
                    int fetchNum = perDetailReportNum;
                    if(reportDetailQueue.size() < fetchNum){
                        fetchNum = reportDetailQueue.size();
                    }
                    for(int i=0;i<fetchNum;i++){
                        InvocationDetail exceptionDetail = reportDetailQueue.poll();
                        detailList.add(exceptionDetail);
                    }

                    //2、构造汇总上报数据
                    if(logger.isDebugEnabled()){
                        logger.debug("current statistic report map size:{}.",statisticMap.size());
                    }
                    Collection<InvocationStatistic> statisticList = statisticMap.values();

                    //3、上报统计及明细数据
                    if(CollectionUtils.isNotEmpty(detailList) || CollectionUtils.isNotEmpty(statisticList)){
                        try {
                            List<MethodCallDetailDO> detailDOList = toDetailDOList(detailList);
                            List<MethodStaticDO> staticDOList = toStaticDOList(statisticList);
                            monitorReporter.reportDetailAndStatic(detailDOList,staticDOList);
                        } catch (Exception e) {
                            if(exceptionLogger.isErrorEnabled()){
                                exceptionLogger.error("report detail and static error.",e);
                            }
                        }
                    }

                    //4、重置统计信息
                    for(Map.Entry<String,InvocationStatistic> entry:statisticMap.entrySet()){
                        entry.getValue().reset();
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
            if(detailDO != null){
                detailDOList.add(detailDO);
                if(logger.isDebugEnabled()){
                    logger.debug("report detailDO:{}.",JSONUtil.toJSONString(detailDO));
                }
            }
        }
        return detailDOList;
    }

    /**
     * 明细DO转换，client/server转换
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
            if(logger.isDebugEnabled()){
                logger.debug("report staticDO:{}.",JSONUtil.toJSONString(staticDO));
            }
            staticDOList.add(staticDO);
        }
        return staticDOList;
    }

    /**
     * 统计DO转换，client转换
     * @param statistic
     * @return
     */
    abstract MethodStaticDO convertStatistic(InvocationStatistic statistic);

    /**
     * 获取角色
     * @return
     */
    abstract int getRole();

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    String serialize(Object object){
        return JSONUtil.toJSONString(object);


    }

}
