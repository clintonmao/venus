package com.meidusa.venus.bus;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * BusApplication
 * Created by Zhangzhihua on 2017/8/15.
 */
public class BusApplication {

    //TODO 完善main app
    void run(){
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:service-bus-container.xml");
        context.containsBean("authenticator");
        try {
            Thread.sleep(1000*60*60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        new BusApplication().run();
    }
}
