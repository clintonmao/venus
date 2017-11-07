package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.EchoService;
import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.chexiang.venus.demo.provider.model.HelloEx;
import com.meidusa.venus.Application;
import com.meidusa.venus.backend.VenusProtocol;
import com.meidusa.venus.notify.InvocationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zhangzhihua on 2017/8/15.
 */
public class DefaultHelloService implements HelloService {

    private static Logger logger = LoggerFactory.getLogger(DefaultHelloService.class);

    Application application;

    VenusProtocol venusProtocol;

    @Autowired
    EchoService echoService;

    boolean isBuildDataModel = true;

    @Override
    public void sayHello(String name, InvocationListener<Hello> invocationListener) {
        logger.info("invoke sayHello with listener:" + name);
        /*
        if("jack".equalsIgnoreCase(name)){
            throw new RpcException("callback exception.");
        }
        */
        Hello hello = new Hello();
        hello.setName("zhangzh");
        hello.setNick("jack");
        if(invocationListener != null){
            invocationListener.callback(hello);
        }
        /*
        if(invocationListener != null){
            invocationListener.onException(new RpcException("callback exception."));
        }
        */
    }

    @Override
    public void sayHello(String name) {
        logger.info("invoke sayHello:" + name);
        //throw new RuntimeException("test throw ex.");
    }

    @Override
    public Hello getHello(String name) {
        //logger.info("invoke getHello.");
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
        return new Hello(name,name);
    }

    @Override
    public HelloEx getHelloForBench(String name, byte[] params) {
        HelloEx o=new HelloEx();
        o.setName(name);
        o.setBytes(params);
        return o;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public VenusProtocol getVenusProtocol() {
        return venusProtocol;
    }

    public void setVenusProtocol(VenusProtocol venusProtocol) {
        this.venusProtocol = venusProtocol;
    }



}
