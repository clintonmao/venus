package com.chexiang.venus.demo.consumer;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.notify.InvocationListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    //@Autowired
    //AthenaDataService athenaDataService;

    //@Test
    public void testSayHello(){
        logger.info("testSayHello begin...");
        helloService.sayHello("jack");
        logger.info("testSayHello end...");

        /*
        try {
            Thread.sleep(1000*60*60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */
    }

    //@Test
    public void testSayHelloWithCallback(){
        helloService.sayHello("jack", new InvocationListener<Hello>() {

            @Override
            public void callback(Hello object) {
                System.out.println("Hello:" + object);
            }

            @Override
            public void onException(Exception e) {
                System.out.println(e);
            }
        });

        try {
            Thread.sleep(1000*60*60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testGetHello(){
        while(true){
            //System.out.println("athenaDataService:" + athenaDataService);
            System.out.println("testGetHello begin...");
            Hello hello = helloService.getHello("jack");
            System.out.println("testGetHello end,result:" + hello);

            try {
                Thread.sleep(1000*5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
