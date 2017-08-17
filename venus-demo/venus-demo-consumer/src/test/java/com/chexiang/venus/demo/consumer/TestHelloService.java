package com.chexiang.venus.demo.consumer;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static Logger logger = LoggerFactory.getLogger(TestHelloService.class);

    @Autowired
    HelloService helloService;

    @Test
    public void testSayHello(){
        logger.info("testSayHello begin...");
        helloService.sayHello("jack");
        logger.info("testSayHello end...");
    }

    //@Test
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


    //@Test
    public void testGetHello(){
        try {
            Hello hello = helloService.getHello("jack");
            System.out.println("hello:" + hello);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
