package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    AtomicInteger totalCount = new AtomicInteger(0);

    AtomicInteger currentCount = new AtomicInteger(0);

    AtomicInteger failCount = new AtomicInteger(0);

    long bTime;

    static boolean isRunning = false;

    @RequestMapping("/start/{threadCount}/{total}")
    public Result start(@PathVariable String threadCount,@PathVariable int total){
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
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {}

        //启动测试线程
        bTime = System.currentTimeMillis();
        for(int i=0;i<thrCount;i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(currentCount.get() < totalCount.get()){
                        try {
                            Hello hello = helloService.getHello("jack");
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
        int lastSecCount = 0;

        @Override
        public void run() {
            try {
                long totalCostTime = (System.currentTimeMillis() - bTime)/1000;
                if(currentCount.get() < totalCount.get()){
                    long thisSecTps = currentCount.get() - lastSecCount;
                    logger.error("current count:{},total time:{},tps:{}.",currentCount.get(),totalCostTime,thisSecTps);
                    lastSecCount = currentCount.get();
                }else{//总计
                    logger.error("###########complete###########");
                    long totalTps = totalCount.get() / totalCostTime;
                    logger.error("total count:{},total time:{},fail count:{},tps:{}.",totalCount.get(),totalCostTime,failCount.get(),totalTps);
                    this.cancel();
                }
            } catch (Exception e) {
                logger.error("count task error.",e);
            }
        }
    }

}
