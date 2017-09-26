package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.notify.InvocationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by Zhangzhihua on 2017/8/15.
 */
public class DefaultHelloService implements HelloService {

    private static Logger logger = LoggerFactory.getLogger(DefaultHelloService.class);

    @Override
    public void sayHello(String name, InvocationListener<Hello> invocationListener) {
        logger.info("invoke sayHello with listener:" + name);
//        if("jack".equalsIgnoreCase(name)){
//            throw new RpcException("callback exception.");
//        }
        Hello hello = new Hello();
        hello.setName("zhangzh");
        hello.setNick("jack");
        if(invocationListener != null){
            invocationListener.callback(hello);
        }
//        if(invocationListener != null){
//            invocationListener.onException(new RpcException("callback exception."));
//        }
    }

    @Override
    public void sayHello(String name) {
        logger.info("invoke sayHello:" + name);
        //throw new RuntimeException("test throw ex.");
    }

    @Override
    public Hello getHello(String name) {
        logger.info("invoke getHello.");
        return new Hello(name,name);
    }
}
