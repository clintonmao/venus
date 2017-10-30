package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.EchoService;
import com.chexiang.venus.demo.provider.KakaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by Zhangzhihua on 2017/10/29.
 */
@Component
public class DefaultEchoService implements EchoService {

    @Autowired(required=true)
    KakaService kakaService;

    @Override
    public void echo(String name) {
        System.out.println("echo:" + name);
    }
}
