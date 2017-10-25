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

import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executor;
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

    long bTime;

    boolean isEnd = false;

    Random  random = new Random();

    @RequestMapping("/start/{threadCount}/{total}")
    public Result start(@PathVariable String threadCount,@PathVariable int total){
        totalCount.set(total);
        currentCount.set(0);
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
            Thread.sleep(1000*3);
        } catch (InterruptedException e) {}

        int thrCount = Integer.parseInt(threadCount);
        bTime = System.currentTimeMillis();
        for(int i=0;i<thrCount;i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(currentCount.get() < totalCount.get()){
                        try {
                            long tbTime = System.currentTimeMillis();
                            Hello hello = helloService.getHello("jack");
                            currentCount.getAndIncrement();
                            int r = random.nextInt(50000);
                            long teTime = System.currentTimeMillis();
                            if(r > 49980){
                                logger.error("current cost time:{}.",teTime - tbTime);
                                long costTime = (System.currentTimeMillis() - bTime)/1000;
                                long tps = currentCount.get() / costTime;
                                logger.error("#######current count:{},total time:{},tps:{}.",currentCount.get(),costTime,tps);
                            }
                        } catch (Exception e) {
                            logger.error("occur error.",e);
                        }
                    }
                    logger.error("complete.");
                }
            });
            thread.setName("beanchmark thread-" + i);
            thread.start();
        }
        Result result = new Result();
        result.setResult("start ok.");
        return result;
    }

    /*
    @RequestMapping("/end")
    public Result end(){
        long eTime = System.currentTimeMillis();
        long costTime = (eTime - bTime)/1000;
        long tps = count.get() / costTime;
        logger.warn("costTime:{},count:{},tps:{}.",costTime,count,tps);
        String result = String.format("costTime:%s,count:%s,tps:%s.",costTime,count,tps);
        count.set(0);
        bTime = -1;
        isEnd = true;
        return new Result(result);
    }
    */


}
