package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.EchoService;
import com.meidusa.venus.support.Echo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zhangzhihua on 2017/10/29.
 */
@Component("echoService")
public class DefaultEchoService implements EchoService {

    private static Logger logger = LoggerFactory.getLogger(DefaultEchoService.class);

    boolean isBuildDataModel = true;

    @Override
    public void echo(String name) {
        logger.info("invoke echo,param:" + name);
    }

    @Override
    public Echo getEcho(String name) {
        logger.info("invoke getEcho,param:" + name);
        if(isBuildDataModel){
            //构造慢操作
            if(ThreadLocalRandom.current().nextInt(100) > 95){
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                }
            }
            //构造异常操作
            if(ThreadLocalRandom.current().nextInt(100) > 99){
                if("A".equals("A")){
                    throw new IllegalArgumentException("param invalid.");
                }
            }
        }
        return new Echo("hi",name);
    }
}
