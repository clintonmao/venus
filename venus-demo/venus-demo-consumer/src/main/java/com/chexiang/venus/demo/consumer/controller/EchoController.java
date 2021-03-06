package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.service.EchoService;
import com.meidusa.venus.Result;
import com.chexiang.venus.demo.provider.model.Echo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HelloController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/echo")
public class EchoController {

    private static Logger logger = LoggerFactory.getLogger(EchoController.class);

    @Autowired
    EchoService echoService;

    @RequestMapping("/echo")
    public Result echo(){
        echoService.echo("jack");
        return new Result("ok");
    }

    @RequestMapping("/getEcho/{name}")
    public Result getEcho(@PathVariable String name){
        Echo echo = null;
        try {
            logger.info("getEcho begin...");
            echo = echoService.getEcho(name);
            logger.info("getEcho end,result:" + echo);
        } catch (Exception e) {
            logger.error("e:{}.",e);
            return new Result(e);
        }
        return new Result(echo);
    }

}
