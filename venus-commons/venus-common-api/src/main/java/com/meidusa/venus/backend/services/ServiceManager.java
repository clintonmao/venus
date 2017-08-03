/**
 * 
 */
package com.meidusa.venus.backend.services;

import java.util.Collection;

import com.meidusa.venus.exception.ServiceNotFoundException;

/**
 * find a service instance from pool
 * 
 * @author Sun Ning
 * @since 2010-3-5
 */
public interface ServiceManager extends EndpointLocator {

    /**
     * 根据服务名称获取服务实例
     * @param serviceName
     * @return
     * @throws ServiceNotFoundException
     */
    Service getService(String serviceName) throws ServiceNotFoundException;

    Collection<Service> getServices();
}
