package com.chexiang.venus.demo.provider;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by Zhangzhihua on 2017/8/15.
 */
public class DemoProviderApplication {

    void run(){
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:/conf/applicationContext-provider.xml");
        context.containsBean("helloService");
        try {
            Thread.sleep(1000*60*60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        new DemoProviderApplication().run();
    }
}
