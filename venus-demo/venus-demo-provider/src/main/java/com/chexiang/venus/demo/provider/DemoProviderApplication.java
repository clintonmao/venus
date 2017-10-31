package com.chexiang.venus.demo.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

/**
 * Created by Zhangzhihua on 2017/9/21.
 */

//@SpringBootApplication
@SpringBootApplication(exclude= {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
        })
//@ComponentScan(basePackages = {"com.chexiang"})
@ImportResource("classpath:conf/applicationContext-provider.xml")
public class DemoProviderApplication {

    public static final Logger logger = LoggerFactory.getLogger(DemoProviderApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoProviderApplication.class, args);
    }

}
