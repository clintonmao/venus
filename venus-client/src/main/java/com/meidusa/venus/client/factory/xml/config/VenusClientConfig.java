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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * venus客户端配置
 */
@XStreamAlias("venus-client")
public class VenusClientConfig {

    //@XStreamImplicit(itemFieldName = "services")
    @XStreamAlias("services")
    private List<ReferenceService> referenceServices = new ArrayList<ReferenceService>();

    private Map<String, ClientRemoteConfig> remoteConfigMap = new HashMap<String, ClientRemoteConfig>();

    public void addService(ReferenceService config) {
        referenceServices.add(config);
    }

    public void addRemote(ClientRemoteConfig remoteConfig) {
        remoteConfigMap.put(remoteConfig.getName(), remoteConfig);
    }

    public Map<String, ClientRemoteConfig> getRemoteConfigMap() {
        return remoteConfigMap;
    }

    public List<ReferenceService> getReferenceServices() {
        return referenceServices;
    }

}
