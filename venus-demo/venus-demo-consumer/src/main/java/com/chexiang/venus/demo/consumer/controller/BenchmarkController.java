package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.google.common.util.concurrent.AtomicDouble;
import com.meidusa.venus.Result;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ManagerController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/ben")
public class BenchmarkController {

    private static Logger logger = LoggerFactory.getLogger(BenchmarkController.class);

    @Autowired
    HelloService helloService;

    AtomicLong totalCount = new AtomicLong(0);

    AtomicLong currentCount = new AtomicLong(0);

    AtomicLong failCount = new AtomicLong(0);

    AtomicDouble minCostTime = new AtomicDouble(0);

    AtomicDouble maxCostTime = new AtomicDouble(0);

    //开始时间
    long beginTime;

    static boolean isRunning = false;

    @RequestMapping("/start/{threadCount}/{total}/{packet}")
    public Result start(@PathVariable String threadCount,@PathVariable int total,@PathVariable String packet){
        //valid
        if(StringUtils.isEmpty(threadCount) || total==0  || StringUtils.isEmpty(packet)){
            Result result = new Result();
            result.setErrorCode(500);
            result.setErrorMessage("param invalid,please check.");
            return result;
        }

        if(isRunning){
            Result result = new Result();
            result.setErrorCode(500);
            result.setErrorMessage("task is running.");
            return result;
        }
        isRunning =  true;

        //set
        totalCount.set(total);
        currentCount.set(0);
        failCount.set(0);
        minCostTime.set(0);
        maxCostTime.set(0);
        int thrCount = Integer.parseInt(threadCount);

        //测试服务可用
        logger.error("start...");
        try {
            logger.error("test start...");
            helloService.getHello("jack");
            logger.error("test ok.");
        } catch (Exception e) {
            logger.error("test failed.",e);
            Result result = new Result();
            result.setErrorCode(500);
            result.setErrorMessage(e.getLocalizedMessage());
            return result;
        }

        //构造数据大小
        int packetNum = 0;
        if(packet.endsWith("b")){
            String len  = packet.substring(0,packet.length()-1);
            packetNum = Integer.parseInt(len);
        }else if(packet.endsWith("k")){
            String len  = packet.substring(0,packet.length()-1);
            packetNum = Integer.parseInt(len)*1024;
        }else{
            throw new IllegalArgumentException("字节包参数无效。");
        }
        logger.error("build packet,len:{}..",packetNum);
        final byte[] packets = new byte[packetNum];
        for(int i=0;i<packetNum;i++){
            packets[i] = '0';
        }
        logger.error("build packet end.");

        try {
            TimeUnit.MICROSECONDS.sleep(500);
        } catch (InterruptedException e) {}

        //启动测试线程
        beginTime = System.nanoTime();
        for(int i=0;i<thrCount;i++){
            Thread thread = new Thread(new Runnable() {
                final byte[] bb = packets;
                @Override
                public void run() {
                    while(currentCount.get() < totalCount.get()){
                        try {
                            long bTime = System.nanoTime();
                            Hello hello = helloService.getHelloForBench(bb);
                            long eTime = System.nanoTime();
                            long costTime = eTime - bTime;
                            if(costTime < minCostTime.get()){
                                minCostTime.set(costTime);
                            }
                            if(costTime > maxCostTime.get()){
                                maxCostTime.set(costTime);
                            }
                            currentCount.getAndIncrement();
                        } catch (Exception e) {
                            failCount.getAndIncrement();
                            logger.error("occur error.",e);
                        }
                    }
                    isRunning = false;
                    logger.error("complete.");
                }
            });
            thread.setName("beanchmark thread-" + i);
            thread.start();
        }

        //启动计数定时器
        Timer timer = new Timer();
        timer.schedule(new CountTask(),1000,1000);

        Result result = new Result();
        result.setResult("start ok.");
        return result;
    }

    /**
     * 统计task
     */

    class CountTask extends TimerTask {

        //上一秒最后记录数
        long lastCount = 0;

        @Override
        public void run() {
            try {
                long endTime = System.nanoTime();
                long totalCostTime = (endTime - beginTime)/(1000*1000*1000);
                if(currentCount.get() < totalCount.get()){
                    long thisSecTps = currentCount.get() - lastCount;
                    logger.error("current count:{},total time:{},tps:{}.",currentCount.get(),totalCostTime,thisSecTps);
                    lastCount = currentCount.get();
                }else{//总计
                    logger.error("###########complete###########");
                    long totalTps = totalCount.get() / totalCostTime;
                    logger.error("total count:{},fail count:{},total time:{},tps:{}.",totalCount.get(),failCount.get(),totalCostTime,totalTps);

                    double minTime = minCostTime.get()/(1000*1000);
                    double maxTime = maxCostTime.get()/(1000*1000);
                    double avgTime = ((endTime - beginTime)/(1000*1000))/totalCount.get();
                    logger.error("min time:{},max time:{},avg time:{}.",minTime,maxTime,avgTime);

                    this.cancel();
                }
            } catch (Exception e) {
                logger.error("count task error.",e);
            }
        }
    }

}