/*
 * Copyright 2008-2108 amoeba.meidusa.com 
 * 
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.venus.client.factory.xml.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务引用配置
 */
@XStreamAlias("service")
public class ReferenceService {

    //spring实例名称
    @XStreamAsAttribute
    private String name;

    //服务名称
    private String serviceName;

    @XStreamAsAttribute
    private String type;

    private Class<?> serviceInterface;

    private Object instance;

    private int version;

    //远程配置名称
    private String remote;

    //ip地址列表
    @XStreamAsAttribute
    private String ipAddressList;

    //超时时间
    @XStreamAsAttribute
    private String timeout;

    private int timeoutCfg;

    //重试次数
    @XStreamAsAttribute
    private String retries;

    private int retriesCfg;

    //默认连接数
    @XStreamAsAttribute
    private int coreConnections;

    //集群容错策略
    @XStreamAsAttribute
    private String cluster;

    //负载均衡策略
    @XStreamAsAttribute
    private String loadbalance;

    @XStreamImplicit
    private List<ReferenceMethod> methodList = new ArrayList<ReferenceMethod>();

    private int timeWait;

    private boolean enabled = true;
    
    public int getTimeWait() {
        return timeWait;
    }

    public void setTimeWait(int soTimeout) {
        this.timeWait = soTimeout;
    }

    public String getIpAddressList() {
        return ipAddressList;
    }

    public void setIpAddressList(String ipAddressList) {
        this.ipAddressList = ipAddressList;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object implement) {
        this.instance = implement;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public int getRetriesCfg() {
        return retriesCfg;
    }

    public void setRetriesCfg(int retriesCfg) {
        this.retriesCfg = retriesCfg;
    }

    public int getTimeoutCfg() {
        return timeoutCfg;
    }

    public void setTimeoutCfg(int timeoutCfg) {
        this.timeoutCfg = timeoutCfg;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getRetries() {
        return retries;
    }

    public void setRetries(String retries) {
        this.retries = retries;
    }

    public int getCoreConnections() {
        return coreConnections;
    }

    public void setCoreConnections(int coreConnections) {
        this.coreConnections = coreConnections;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<ReferenceMethod> getMethodList() {
        return methodList;
    }

    public void setMethodList(List<ReferenceMethod> methodList) {
        this.methodList = methodList;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
