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
public class ServiceObject {

    private Class<?> type;

    private String name;

    //当前版本号
    private int version;

    //当前版本向下支持版本范围
    private Range supportVersionRange;

    private Multimap<String, EndpointItem> endpoints;

    private String description;

    private boolean athenaFlag;

    private boolean active = true;

    private Object instance;

    //是否打印输入参数
    private String printParam;

    //是否打印输出结果
    private String printResult;

    /**
     * @return the instance
     */
    public Object getInstance() {
        return instance;
    }

    /**
     * @param instance the instance to set
     */
    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Range getSupportVersionRange() {
        return supportVersionRange;
    }

    public void setSupportVersionRange(Range supportVersionRange) {
        this.supportVersionRange = supportVersionRange;
    }

    public void setAthenaFlag(boolean athenaFlag) {
        this.athenaFlag = athenaFlag;
    }

    public boolean getAthenaFlag(){
        return this.athenaFlag;
    }

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
    public Multimap<String, EndpointItem> getEndpoints() {
        return endpoints;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoints(Multimap<String, EndpointItem> endpoint) {
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getPrintParam() {
        return printParam;
    }

    public void setPrintParam(String printParam) {
        this.printParam = printParam;
    }

    public String getPrintResult() {
        return printResult;
    }

    public void setPrintResult(String printResult) {
        this.printResult = printResult;
    }
}
