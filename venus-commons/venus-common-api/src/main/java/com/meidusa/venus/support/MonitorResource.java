package com.meidusa.venus.support;

import java.util.Map;

/**
 * 资源监听接口
 * Created by Zhangzhihua on 2017/11/16.
 */
public interface MonitorResource {

    /**
     * 初始化资源监控
     */
    void init();

    /**
     * 添加属性项
     * @param key
     * @param object
     */
    void addProperty(String key,Object object);

    /**
     * 获取属性项
     * @param key
     * @return
     */
    Object getProperty(String key);

    /**
     * 获取所有属性项
     * @return
     */
    Map<String,Object> getAllProperties();


}
