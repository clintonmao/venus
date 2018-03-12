package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.EchoService;
import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.HelloValidException;
import com.chexiang.venus.demo.provider.InvalidParamException;
import com.chexiang.venus.demo.provider.model.Hello;
import com.chexiang.venus.demo.provider.model.HelloEx;
import com.meidusa.fastjson.JSONArray;
import com.meidusa.fastjson.JSONObject;
import com.meidusa.venus.VenusApplication;
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

    VenusApplication venusApplication;

    VenusProtocol venusProtocol;

    @Autowired
    EchoService echoService;

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
        //logger.info("invoke getHello,param:" + name);
        int ran = ThreadLocalRandom.current().nextInt(100);
        if( ran > 80){
            throw new IllegalArgumentException("param invalid.");
        }else if(ran > 50){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }
//        try {
//        } catch (Exception e) {
//            logger.error("getHello failed.",e);
//        }
        return new Hello(name,name);
    }

    @Override
    public HelloEx getHelloForBench(String name, byte[] params) {
        HelloEx o=new HelloEx();
        o.setName(name);
        o.setBytes(params);
        return o;
    }

    @Override
    public int cal(int param)  throws HelloValidException,InvalidParamException {
        if(param > 10000){
            throw new InvalidParamException();
        }
        if(param > 1000){
            throw new HelloValidException(200001,"param is invalid.");
        }


        try{
            int ret=100/param;
            logger.info("ret:{}",ret);
            return ret;
        }catch(Exception e){
            throw e;
        }
    }

    public VenusApplication getVenusApplication() {
        return venusApplication;
    }

    public void setVenusApplication(VenusApplication venusApplication) {
        this.venusApplication = venusApplication;
    }

    public VenusProtocol getVenusProtocol() {
        return venusProtocol;
    }

    public void setVenusProtocol(VenusProtocol venusProtocol) {
        this.venusProtocol = venusProtocol;
    }

}
