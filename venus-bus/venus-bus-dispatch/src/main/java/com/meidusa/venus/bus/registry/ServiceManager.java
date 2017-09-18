package com.meidusa.venus.bus.registry;

import java.util.List;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.venus.bus.config.BusRemoteConfig;
import com.meidusa.venus.util.Range;

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
