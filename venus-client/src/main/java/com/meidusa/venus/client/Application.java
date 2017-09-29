package com.meidusa.venus.client;

import com.meidusa.venus.VenusContext;
import com.meidusa.venus.exception.VenusConfigException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Venus应用定义
 * Created by Zhangzhihua on 2017/9/15.
 */
public class Application implements InitializingBean,BeanFactoryPostProcessor {

    /**
     * 应用名称
     */
    private String name;

    @Override
    public void afterPropertiesSet() throws Exception {
        //校验
        valid();

        //初始化
        init();
    }

    /**
     * 验证名称有效性
     */
    void valid(){
        if(StringUtils.isEmpty(name)){
            throw new VenusConfigException("name not allow empty.");
        }
    }

    /**
     * 初始化
     */
    void init(){
        VenusContext.getInstance().setApplication(name);

        //初始化venus协议，protcol不再单独配置，由application内置默认初始化
        //TODO 通信端口，相关连接配置等，需要单独外部配置
        VenusSerializerFactory.init();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
