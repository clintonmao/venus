package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.EchoService;
import com.meidusa.venus.support.Echo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by Zhangzhihua on 2017/10/29.
 */
@Component
public class DefaultEchoService implements EchoService {

    private static Logger logger = LoggerFactory.getLogger(DefaultEchoService.class);

    @PostConstruct
    void init(){
        logger.info("init echo service.");
    }

    @Override
    public void echo(String name) {
        logger.info("echo:" + name);
    }

    @Override
    public Echo getEcho(String name) {
        return new Echo("hi",name);
    }
}
