package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.EchoService;
import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.Result;
import com.meidusa.venus.notify.InvocationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

/**
 * HelloController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/build")
public class BuildDataController {

    private static Logger logger = LoggerFactory.getLogger(BuildDataController.class);

    @Autowired
    HelloService helloService;

    @Autowired
    EchoService echoService;

    @RequestMapping("/start/{total}")
    public Result start(@PathVariable int total){
        final int count = total;
        if(total < 1){
            return new Result("param invalid.");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<count;i++){
                    if(ThreadLocalRandom.current().nextInt(100) < 50){
                        helloService.sayHello("jack" + i);
                    }else{
                        echoService.getEcho("jack" + i);
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {}
                    logger.info("current index:{}.",i);
                }
                logger.info("end.");
            }
        }).start();

        return new Result("start ok");
    }

}
