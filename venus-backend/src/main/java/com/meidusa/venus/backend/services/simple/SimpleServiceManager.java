/**
 * 
 */
package com.meidusa.venus.backend.services.simple;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.backend.services.AbstractServiceManager;
import com.meidusa.venus.backend.services.EndpointItem;
import com.meidusa.venus.backend.services.ServiceObject;
import com.meidusa.venus.exception.ConvertException;
import com.meidusa.venus.exception.ServiceDefinitionException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.metainfo.AnnotationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 基于简单API方式服务管理类
 */
public class SimpleServiceManager extends AbstractServiceManager implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(SimpleServiceManager.class);

    private List<Object> serviceInstances;

    public void afterPropertiesSet() {
        for (Object instance : serviceInstances) {
            ServiceObject[] loadedServices = loadService(instance);

            if (loadedServices == null) {
                continue;
            }

            for (ServiceObject service : loadedServices) {
                serviceMap.put(service.getName(), service);
            }
        }
    }

    /**
     * @return the serviceInstances
     */
    public List<Object> getServiceInstances() {
        return serviceInstances;
    }

    /**
     * @param serviceInstances the serviceInstances to set
     */
    public void setServiceInstances(List<Object> serviceInstances) {
        this.serviceInstances = serviceInstances;
    }

    public Map<String, ServiceObject> getServicesAsMap() {
        return serviceMap;
    }

    /**
     * @throws ConvertException
     * 
     * 
     */
    protected ServiceObject[] loadService(Object instance) throws ServiceDefinitionException, ConvertException {
        Class<?>[] interfaces = instance.getClass().getInterfaces();
        Class<?>[] types = AnnotationUtil.getAnnotatedClasses(interfaces, com.meidusa.venus.annotations.Service.class);

        if (types != null && types.length > 0) {
            ServiceObject[] services = new ServiceObject[types.length];
            for (int i = 0; i < types.length; i++) {
                services[i] = doLoadService(types[i], instance);
            }
            return services;
        }

        return null;
    }

    protected ServiceObject doLoadService(Class<?> type, Object instance) throws ServiceDefinitionException, ConvertException {
        ServiceObject service = new ServiceObject();
        if (logger.isInfoEnabled()) {
            logger.info("Loading From: " + instance.getClass().getCanonicalName());
        }

        // set type

        if (type == null) {
            throw new VenusConfigException(instance.getClass().getCanonicalName());
        }
        if (logger.isInfoEnabled()) {
            logger.info("Load Type: " + type.getCanonicalName());
        }
        service.setType(type);

        // set name
        Service serviceAnnotation = type.getAnnotation(Service.class);
        if (!serviceAnnotation.name().isEmpty()) {
            service.setName(serviceAnnotation.name());
        } else {
            service.setName(type.getCanonicalName());
        }
        if (logger.isInfoEnabled()) {
            logger.info("Use Name: " + service.getName());
        }

        // cache all methods
        Method[] methods = type.getMethods();
        Multimap<String, EndpointItem> endpoints = HashMultimap.create();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Endpoint.class)) {
                EndpointItem ep = initEndpoint(method,null);
                ep.setService(service);
                if (logger.isInfoEnabled()) {
                    logger.info("Add Endpoint: " + ep.getService().getName() + "." + ep.getName());
                }
                endpoints.put(ep.getName(), ep);
            }
        }
        service.setEndpoints(endpoints);

        // inject instance
        service.setInstance(instance);

        return service;
    }

    @Override
    public void destroy() {
    }
}
