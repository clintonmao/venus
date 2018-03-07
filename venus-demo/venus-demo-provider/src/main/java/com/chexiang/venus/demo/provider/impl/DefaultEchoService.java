package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.EchoService;
import com.chexiang.venus.demo.provider.model.Echo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zhangzhihua on 2017/10/29.
 */
@Component("echoService")
public class DefaultEchoService implements EchoService {

    private static Logger logger = LoggerFactory.getLogger(DefaultEchoService.class);

    boolean isBuildDataModel = true;

    Random random = new Random();

    @Override
    public void echo(String name) {
        logger.info("invoke echo,param:" + name);
    }

    @Override
    public Echo getEcho(String name) {
        logger.info("invoke getEcho,param:" + name);
        if(ThreadLocalRandom.current().nextInt(100) > 20){//构造异常操作
            if("A".equals("B")){
                throw new IllegalArgumentException("param invalid.");
            }
        }
        return new Echo("hi",name);
    }
}
