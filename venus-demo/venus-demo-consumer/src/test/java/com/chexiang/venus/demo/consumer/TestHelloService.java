package com.chexiang.venus.demo.consumer;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.notify.InvocationListener;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by Zhangzhihua on 2017/8/15.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/applicationContext-consumer.xml")
public class TestHelloService {

    @Autowired
    HelloService helloService;

    @Test
    public void testSayHello(){
        helloService.sayHello("jack");
    }

    @Test
    public void testSayHelloWithCallback(){
        helloService.sayHello("jack", new InvocationListener() {
            @Override
            public void callback(Object object) {
                System.out.println(object);
            }

            @Override
            public void onException(Exception e) {
                System.out.println(e);
            }
        });
    }


    @Test
    public void testGetHello(){
        helloService.getHello("jack");
    }


}
