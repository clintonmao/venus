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
package com.meidusa.venus.bus.config;

import com.meidusa.venus.io.authenticate.Authenticator;

//TODO 与client统一
public class BusRemoteConfig {
    public final static int DEFAULT_POOL_SIZE = 8;
    private int poolSize = DEFAULT_POOL_SIZE;
    private int loadbalance = 1;
    private String name;
    private BusFactoryConfig factory;
    private Authenticator authenticator;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BusFactoryConfig getFactory() {
        return factory;
    }

    public void setFactory(BusFactoryConfig factory) {
        this.factory = factory;
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

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public static BusRemoteConfig parse(String ipAddressList){
        BusRemoteConfig remoteConfig = new BusRemoteConfig();
        remoteConfig.setFactory(null);
        return remoteConfig;
    }

}
