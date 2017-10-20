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

    static AtomicInteger count = new AtomicInteger(0);

    static long bTime;

    static boolean isEnd = false;

    @RequestMapping("/start/{threadCount}")
    public Result start(@PathVariable String threadCount){
        logger.info("start...");
        int thrCount = Integer.parseInt(threadCount);
        bTime = System.currentTimeMillis();
        for(int i=0;i<thrCount;i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(!isEnd){

                        try {
                            Hello hello = helloService.getHello("jack");
                            if(logger.isDebugEnabled()){
                                logger.debug("result:{}.",hello);
                            }
                            count.getAndIncrement();
                            int r = new Random().nextInt(1000);
                            if(r > 990){
                                long costTime = (System.currentTimeMillis() - bTime)/1000;
                                long tps = count.get() / costTime;
                                System.out.println(String.format("current count:%s,costTime:%s,tps:%s.",count.get(),costTime,tps));
                            }
                        } catch (Exception e) {
                            //logger.error("occur error.",e);
                        }
                    }
                }
            });
            thread.setName("beanchmark thread.");
            thread.start();
        }
        return new Result("ok");
    }

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


}
