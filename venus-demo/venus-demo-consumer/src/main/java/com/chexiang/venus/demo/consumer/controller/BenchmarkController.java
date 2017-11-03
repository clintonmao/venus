package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.chexiang.venus.demo.provider.model.OrderDO;
import com.google.common.util.concurrent.AtomicDouble;
import com.meidusa.venus.Result;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.DecimalFormat;
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

    AtomicLong minCostTime = new AtomicLong(0);

    AtomicLong maxCostTime = new AtomicLong(0);

    //开始时间
    static long beginTime;

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
                            OrderDO hello = helloService.testOrder("name", bb);
                            long eTime = System.nanoTime();
                            long costTime = eTime - bTime;
                            if(minCostTime.get() == 0 && costTime != 0){
                              minCostTime.set(costTime);
                            } else if(costTime < minCostTime.get()){
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
                if(currentCount.get() < totalCount.get()){
                    long endTime = System.nanoTime();
                    long totalCostTime = (endTime - beginTime);
                    long thisSecTps = currentCount.get() - lastCount;
                    logger.error("current count:{},total time:{},tps:{}.",currentCount.get(),totalCostTime/(1000*1000*1000),thisSecTps);
                    lastCount = currentCount.get();
                }else{//总计
                    long endTime = System.nanoTime();
                    long totalCostTime = (endTime - beginTime);
                    logger.error("###########complete###########");
                    long avgTps = totalCount.get() / (totalCostTime/(1000*1000*1000));
                    logger.error("total count:{},fail count:{},total time:{},avg tps:{}.",totalCount.get(),failCount.get(),totalCostTime/(1000*1000*1000),avgTps);

                    float minTime = (float)(minCostTime.get()/(1000));
                    float maxTime = (float)(maxCostTime.get()/(1000));
                    //long avgTimeOfNan = totalCostTime/totalCount.get();
                    //float avgTime = (float)(avgTimeOfNan);
                    logger.error("min time:{}ms,max time:{}ms.",format(minTime),format(maxTime));

                    this.cancel();
                }
            } catch (Exception e) {
                logger.error("count task error.",e);
            }
        }

        String format(float f){
            DecimalFormat df = new DecimalFormat("0.00");//格式化小数
            String s = df.format(f);//返回的是String类型
            return s;
        }
    }

}