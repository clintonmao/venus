package com.meidusa.venus.registry.service;

import java.util.List;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;

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
     * 根据URL对象返回服务定义对象
     * @param url
     * @return
     */
    VenusServiceDefinitionDO urlToServiceDefine(URL url);
    
    /**
     * 根据接口名和服务名查询返回服务定义列表
     * @param interfaceName
     * @param serviceName
     * @return
     * @throws VenusRegisteException
     */
    List<VenusServiceDefinitionDO> getServiceDefines(String interfaceName, String serviceName) throws VenusRegisteException;
    
    /**
     * 根据URL更新注册接口的心跳时间
     * @param url
     */
    void heartbeatRegister(URL url);
    
    /**
     * 根据URL更新订阅接口的心跳时间
     * @param url
     */
    void heartbeatSubcribe(URL url);
    
    /**
     * 清理无效的服务映射关系
     * @param currentDateTime
     * @param updateTime
     */
    void clearInvalidService(String currentDateTime,String updateTime);

}
