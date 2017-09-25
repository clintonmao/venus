package com.chexiang.venus.demo.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateExceptionTranslator;

/**
 * Created by songcl on 2017/4/29.
 */
@Configuration
public class TomcatCustomerConfig {

    @Bean
    public HibernateExceptionTranslator hibernateExceptionTranslator(){
        return new HibernateExceptionTranslator();
    }

    /*
    @Bean
    public FilterRegistrationBean filterRegistrationBean(OpenSessionInViewFilter openSessionInViewFilter){
        FilterRegistrationBean  filterRegistrationBean = new FilterRegistrationBean ();
        filterRegistrationBean.setFilter(openSessionInViewFilter);
        filterRegistrationBean.setEnabled(true);
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }
    */

}
