package com.meidusa.venus.monitor.filter;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.monitor.filter.support.InvocationDetail;
import com.meidusa.venus.monitor.filter.support.InvocationStatistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * monitor基类
 * Created by Zhangzhihua on 2017/9/4.
 */
public class BaseMonitorFilter {

    private static Logger logger = LoggerFactory.getLogger(BaseMonitorFilter.class);

    //明细队列
    Queue<InvocationDetail> detailQueue = new LinkedBlockingQueue<InvocationDetail>();

    //异常及慢操作明细队列
    Queue<InvocationDetail> exceptionDetailQueue = new LinkedBlockingQueue<InvocationDetail>();

    //方法调用汇总映射表
    Map<String,InvocationStatistic> statisticMap = new ConcurrentHashMap<String,InvocationStatistic>();

    static boolean isRunningRunnable = false;

    public BaseMonitorFilter(){
        init();
    }

    /**
     * 初始化
     */
    void init(){
        if(!isRunningRunnable){
            new Thread(new InvocationDetailProcessRunnable()).start();
            new Thread(new InvocationDataReportRunnable()).start();
        }
    }
    /**
     * 添加到明细队列
     * @param invocationDetail
     */
    public void pubInvocationDetailQueue(InvocationDetail invocationDetail){
        try {
            detailQueue.add(invocationDetail);
        } catch (Exception e) {
            //不处理异常，避免影响主流程
            logger.error("add monitor queue error.",e);
        }
    }

    /**
     * 获取方法标识路径
     * @return
     */
    String getMethodPath(InvocationDetail detail){
        Invocation invocation = detail.getInvocation();
        String methodPath = String.format(
                "%s/%s?version=%s&method=%s",
                invocation.getMethod().getDeclaringClass().getName(),
                invocation.getService().name(),
                "0.0.0",
                invocation.getMethod().getName()
        );
        logger.info("methodPath:{}.", methodPath);
        return methodPath;
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
     * 上报异常明细
     * @param exceptionDetailList
     */
    void reportExceptionDetailList(Collection<InvocationDetail> exceptionDetailList){
        logger.info("report exceptionDetailList:{}.",exceptionDetailList);

        //TODO 上报异常明细
    }

    /**
     * 上报统计数据
     * @param statisticList
     */
    void reportStatisticList(Collection<InvocationStatistic> statisticList){
        logger.info("report statisticList:{}.",statisticList);

        //TODO 上报方法调用统计
    }

    /**
     * 明细处理，统计异常明细、统计服务调用数据
     */
    class InvocationDetailProcessRunnable implements Runnable{
        @Override
        public void run() {
            while(!detailQueue.isEmpty()){
                //处理异常、慢操作数据
                InvocationDetail detail = detailQueue.poll();
                if(isExceptionOperation(detail)){
                    exceptionDetailQueue.add(detail);
                }
                if(isSlowOperation(detail)){
                    exceptionDetailQueue.add(detail);
                }

                //统计服务调用汇总数据
                String methodPath = getMethodPath(detail);
                if(statisticMap.get(methodPath) == null){
                    statisticMap.put(methodPath,new InvocationStatistic());
                }
                InvocationStatistic invocationStatistic = statisticMap.get(methodPath);
                //++ TODO

                //TODO 改调度方式
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }

    }

    /**
     * 明细、汇总数据上报任务
     */
    class InvocationDataReportRunnable implements Runnable{
        @Override
        public void run() {
            while(true){
                //上报异常、慢操作数据 TODO 改为批量拿
                List<InvocationDetail> exceptionDetailList = new ArrayList<InvocationDetail>();
                InvocationDetail exceptionDetail = exceptionDetailQueue.poll();
                if(exceptionDetail != null){
                    exceptionDetailList.add(exceptionDetail);
                }
                reportExceptionDetailList(exceptionDetailList);

                //上报服务调用汇总数据 TODO
                Collection<InvocationStatistic> statistics = statisticMap.values();
                reportStatisticList(statistics);
                //重置计数
                for(Map.Entry<String,InvocationStatistic> entry:statisticMap.entrySet()){
                    entry.getValue().reset();
                }

                //TODO 改调度方式
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }


        }
    }

}
