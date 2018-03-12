package com.chexiang.venus.demo.consumer.direct;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;

/**
 * Created by Zhangzhihua on 2018/1/11.
 */
public class HelloDirectInvoke {

    void invoke(){
        SimpleServiceFactory serviceFactory = new SimpleServiceFactory();
        //serviceFactory.setAddressList("127.0.0.1:16800;127.0.0.2:16800");
        serviceFactory.setAddressList("127.0.0.1:16800");
        HelloService helloService = serviceFactory.getService(HelloService.class);
        Hello hello = helloService.getHello("jack");
        System.out.println("result:" + hello);
    }

    public static void main(String[] args) {
        new HelloDirectInvoke().invoke();
    }
}
