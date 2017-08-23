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
    public void sayHello(String name, InvocationListener<Hello> invocationListener) {
        System.out.println("invoke sayHello with listener:" + name);
        Hello hello = new Hello();
        hello.setName("zhangzh");
        hello.setNick("jack");
        if(invocationListener != null){
            invocationListener.callback(hello);
        }
    }

    @Override
    public void sayHello(String name) {
        System.out.println("invoke sayHello:" + name);
        //throw new RuntimeException("test throw ex.");
    }

    @Override
    public Hello getHello(String name) {
        System.out.println("invoke getHello.");
        return new Hello("zhangzh","jack");
    }
}
