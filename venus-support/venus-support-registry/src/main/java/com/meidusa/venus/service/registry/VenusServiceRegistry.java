package com.meidusa.venus.service.registry;

import com.meidusa.venus.backend.services.Service;

import java.util.*;

/**
 * venus服务注册类
 * Created by Zhangzhihua on 2017/8/3.
 */
public class VenusServiceRegistry implements ServiceRegistry{

    Map<String, Service> services = new HashMap<String, Service>();

    public VenusServiceRegistry(Map<String, Service> services){
        this.services = services;
    }

    @Override
    public List<ServiceDefinition> getServiceDefinitions() {
        List<ServiceDefinition> sdList = new ArrayList<ServiceDefinition>();
        Collection<Service> list = getServices().values();
        for (Service service : list) {
            ServiceDefinition definition = new ServiceDefinition();
            definition.setActive(service.isActive());
            definition.setName(service.getName());
            definition.setDescription(service.getDescription());
            if (service.getVersionRange() != null) {
                definition.setVersionRange(service.getVersionRange().toString());
            }
            sdList.add(definition);
        }
        return sdList;
    }

    @Override
    public ServiceDefinition getServiceDefinition(String name, int version) {
        Service service = getServices().get(name);
        if (service.getVersionRange().contains(version)) {
            ServiceDefinition definition = new ServiceDefinition();
            definition.setActive(service.isActive());
            definition.setName(service.getName());
            definition.setDescription(service.getDescription());
            if (service.getVersionRange() != null) {
                definition.setVersionRange(service.getVersionRange().toString());
            }
            return definition;
        }

        return null;
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public void setServices(Map<String, Service> services) {
        this.services = services;
    }
}
