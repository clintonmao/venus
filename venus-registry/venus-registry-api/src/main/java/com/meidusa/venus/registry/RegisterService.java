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

    ServiceDefinition urlToServiceDefine(URL url);
    
    void heartbeatRegister(URL url);
    
    void heartbeatSubcribe(URL url);
    
    void clearInvalidService(String currentDateTime,String updateTime);
}
