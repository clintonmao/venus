/**
 * 
 */
package com.meidusa.venus.backend.services;

import com.google.common.collect.Multimap;
import com.meidusa.venus.util.Range;

/**
 * class describes a service (contains a set of endpoints)
 * 
 * @author Sun Ning
 * @since 2010-3-4
 */
public abstract class Service {

    private Class<?> type;

    private String name;

    private String versionx;

    private Range versionRange;

    private Multimap<String, Endpoint> endpoints;

    private String description;

    private boolean athenaFlag;

    private boolean active = true;

    public Range getVersionRange() {
        return versionRange;
    }

    public void setVersionRange(Range versionRange) {
        this.versionRange = versionRange;
    }

    public void setAthenaFlag(boolean athenaFlag) {
        this.athenaFlag = athenaFlag;
    }

    public boolean getAthenaFlag(){
        return this.athenaFlag;
    }

    /**
     * 
     * @return
     */
    public abstract Object getInstance();

    /**
     * @return the interfaceName
     */
    public String getName() {
        return name;
    }

    /**
     * @param interfaceName the interfaceName to set
     */
    public void setName(String interfaceName) {
        this.name = interfaceName;
    }

    /**
     * @return the interfaceType
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @param interfaceType the interfaceType to set
     */
    public void setType(Class<?> interfaceType) {
        this.type = interfaceType;
    }

    /**
     * @return the endpoint
     */
    public Multimap<String, Endpoint> getEndpoints() {
        return endpoints;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoints(Multimap<String, Endpoint> endpoint) {
        this.endpoints = endpoint;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersionx() {
        return versionx;
    }

    public void setVersionx(String versionx) {
        this.versionx = versionx;
    }
}
