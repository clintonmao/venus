package com.meidusa.venus.monitor.config;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * athena配置信息委托
 * Created by Zhangzhihua on 2017/10/9.
 */
public interface ClientConfigManagerDelegate {


    /**
     * 初始化配置信息管理服务
     * @param appName
     * @param enabled
     * @return
     */
    Object initConfigManager(String appName, boolean enabled);

    /**
     * 注册配置信息服务实例
     * @param beanFactory
     * @param clientConfigManager
     */
    void registeConfigManager(ConfigurableListableBeanFactory beanFactory,Object clientConfigManager);
}
