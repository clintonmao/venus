package com.meidusa.venus.bus.registry;

import java.util.List;

import com.meidusa.venus.bus.registry.xml.config.BusRemoteConfig;

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
    List<BusRemoteConfig> lookup(String serviceName);

}
