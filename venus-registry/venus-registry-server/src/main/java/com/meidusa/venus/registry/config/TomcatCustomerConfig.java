package com.meidusa.venus.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateExceptionTranslator;

/**
 * Created by zhangzhihua on 2017/4/29.
 */
//@Configuration
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
