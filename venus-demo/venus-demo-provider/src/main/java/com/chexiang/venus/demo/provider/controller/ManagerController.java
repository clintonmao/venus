package com.chexiang.venus.demo.provider.controller;

import com.chexiang.venus.demo.provider.EchoService;
import com.chexiang.venus.demo.provider.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

/**
 * ManagerController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/mgr")
public class ManagerController {

    private static Logger logger = LoggerFactory.getLogger(ManagerController.class);

    @Autowired
    HelloService helloService;

    @Autowired
    EchoService echoService;

    @RequestMapping("/exit")
    public void exit(){
        logger.info("exit...");
        System.exit(0);
    }

    @RequestMapping("/anno")
    public void testAnno(){
        if(helloService != null){
            helloService.getHello("jack");
        }
        if(echoService != null){
            echoService.echo("@hi.");
        }
    }

}
