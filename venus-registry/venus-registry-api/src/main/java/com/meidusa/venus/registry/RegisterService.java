package com.meidusa.venus.registry;

import com.meidusa.venus.URL;
import com.meidusa.venus.service.registry.ServiceDefinition;

/**
 * Created by Zhangzhihua on 2017/8/16.
 */
public interface RegisterService {

    /**
     * 服务注册
     * @param url
     * @throws VenusRegisteException
     */
    void registe(URL url) throws VenusRegisteException;

    /**
     * 服务反注册
     * @param url
     * @return TODO
     * @throws VenusRegisteException
     */
    boolean unregiste(URL url) throws VenusRegisteException;

    /**
     * 服务订阅
     * @param url
     * @throws VenusRegisteException
     */
    void subscrible(URL url) throws VenusRegisteException;

    /**
     * 服务反订阅
     * @param url
     * @return TODO
     * @throws VenusRegisteException
     */
    boolean unsubscrible(URL url) throws VenusRegisteException;

    /**
     * 提供方、消费方心跳检测
     * @throws VenusRegisteException
     */
    void heartbeat() throws VenusRegisteException;

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
    
    ServiceDefinition urlToServiceDefine(URL url);
    
    void heartbeatRegister(URL url);
    
    void heartbeatSubcribe(URL url);
    
    void clearInvalidService(String currentDateTime);
}
