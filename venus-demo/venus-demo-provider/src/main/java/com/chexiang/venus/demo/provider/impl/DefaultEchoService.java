package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.EchoService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by Zhangzhihua on 2017/10/29.
 */
@Component
public class DefaultEchoService implements EchoService {

    @PostConstruct
    void init(){
        System.out.println("init echo service.");
    }

    @Override
    public void echo(String name) {
        System.out.println("echo:" + name);
    }
}
