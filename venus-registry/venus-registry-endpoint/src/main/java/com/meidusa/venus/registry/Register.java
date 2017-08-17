package com.meidusa.venus.registry;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.service.registry.ServiceDefinition;

/**
 * 服务注册接口
 * Created by Zhangzhihua on 2017/7/27.
 */
public interface Register {

    /**
     * 服务注册
     * @param url
     * @throws VenusRegisteException
     */
    void registe(URL url) throws VenusRegisteException;

    /**
     * 服务反注册
     * @param url
     * @throws VenusRegisteException
     */
    void unregiste(URL url) throws VenusRegisteException;

    /**
     * 服务订阅
     * @param url
     * @throws VenusRegisteException
     */
    void subscrible(URL url) throws VenusRegisteException;

    /**
     * 服务反订阅
     * @param url
     * @throws VenusRegisteException
     */
    void unsubscrible(URL url) throws VenusRegisteException;

    /**
     * 提供方、消费方心跳检测
     * @throws VenusRegisteException
     */
    void heartbeat() throws VenusRegisteException;

    /**
     * 清理注册中心服务提供方、消费方无效地址列表
     * @throws VenusRegisteException
     */
    void clearInvalid() throws VenusRegisteException;

    /**
     * 从本地查找服务定义
     * @param url
     * @return
     * @throws VenusRegisteException
     */
    ServiceDefinition lookup(URL url) throws VenusRegisteException;

    /**
     * 加载服务定义到本地
     * @throws VenusRegisteException
     */
    void load() throws VenusRegisteException;

    /**
     * 销毁，清理、释放相关资源
     * @throws VenusRegisteException
     */
    void destroy() throws VenusRegisteException;


}