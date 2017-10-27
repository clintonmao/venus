package com.meidusa.venus.client;

import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.io.serializer.SerializerFactory;
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

    private static boolean isInitedSerializer = false;

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

        //初始化序列化工厂
        initSerializer();
    }

    /**
     * 初始化序列化
     */
    void initSerializer(){
        if(!isInitedSerializer){
            //初始化序列化配置
            SerializerFactory.init();
        }
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
