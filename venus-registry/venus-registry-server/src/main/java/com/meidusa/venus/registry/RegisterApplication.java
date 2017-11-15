package com.meidusa.venus.registry;

import com.meidusa.venus.registry.service.RegisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;

/**
 * Created by Zhangzhihua on 2017/9/21.
 */
@SpringBootApplication(exclude= {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
        })
@ImportResource("classpath:conf/application-venus-server.xml")
public class RegisterApplication extends SpringBootServletInitializer {

    public static final Logger logger = LoggerFactory.getLogger(RegisterApplication.class);

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(RegisterApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(RegisterApplication.class, args);
    }

}
