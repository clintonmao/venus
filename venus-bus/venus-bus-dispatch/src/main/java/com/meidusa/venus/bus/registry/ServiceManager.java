package com.meidusa.venus.bus.registry;

import java.util.List;

import com.meidusa.venus.bus.registry.xml.config.RemoteConfig;

/**
 * Service Manager Interface
 * 
 * @author structchen
 * 
 */
public interface ServiceManager {

    /**
     * 查找服务地址
     * @param serviceName
     * @return
     */
    List<RemoteConfig> lookup(String serviceName);

}
