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

import com.meidusa.venus.RemoteConfig;
import com.meidusa.venus.io.authenticate.Authenticator;

/**
 * client静态地址配置
 */
public class ClientRemoteConfig implements RemoteConfig {

    private String name;

    private FactoryConfig factory;

    /**
     * 连接池配置
     */
    private PoolConfig pool;

    private boolean share = true;

    private int loadbalance = 1;

    private boolean enabled =true;

    private Authenticator authenticator;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FactoryConfig getFactory() {
        return factory;
    }

    public void setFactory(FactoryConfig factory) {
        this.factory = factory;
    }

    public PoolConfig getPool() {
        return pool;
    }

    public void setPool(PoolConfig pool) {
        this.pool = pool;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public int getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(int loadbalance) {
        this.loadbalance = loadbalance;
    }

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isShare() {
		return share;
	}

	public void setShare(boolean share) {
		this.share = share;
	}

    /**
     * 根据ip地址段构造缺省的远程配置
     * @param ipAddressList
     * @return
     */
	public static ClientRemoteConfig newInstace(String ipAddressList){
        FactoryConfig factoryConfig = new FactoryConfig();
        factoryConfig.setIpAddressList(ipAddressList);
        ClientRemoteConfig remoteConfig = new ClientRemoteConfig();
        remoteConfig.setFactory(factoryConfig);
        return remoteConfig;
    }

}
