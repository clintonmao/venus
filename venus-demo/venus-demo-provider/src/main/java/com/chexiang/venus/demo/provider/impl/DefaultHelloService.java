package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.notify.InvocationListener;
import org.springframework.stereotype.Component;

/**
 * Created by Zhangzhihua on 2017/8/15.
 */
public class DefaultHelloService implements HelloService {

    @Override
    public void sayHello(String name, InvocationListener invocationListener) {
        System.out.println("name:" + name);
    }

    @Override
    public void sayHello(String name) {
        System.out.println("name:" + name);
    }

    @Override
    public Hello getHello(String name) {
        return new Hello("zhangzh","jack");
    }
}
