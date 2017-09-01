package com.meidusa.venus.bus.service;

import java.util.List;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.venus.URL;
import com.meidusa.venus.util.Range;

/**
 * Service Manager Interface
 * 
 * @author structchen
 * 
 */
public interface ServiceRemoteManager {

    List<Tuple<Range, BackendConnectionPool>> getRemoteList(String serviceName);

    /**
     * 查找服务地址
     * @param serviceName
     * @return
     */
    List<URL> lookup(String serviceName);

}
