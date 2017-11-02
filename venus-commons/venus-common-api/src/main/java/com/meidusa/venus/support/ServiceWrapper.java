package com.meidusa.venus.support;

import com.meidusa.venus.annotations.Service;

/**
 * 原service注解包装扩展，因原service注解类在vm destroy处理时为NULL
 * Created by Zhangzhihua on 2017/10/13.
 */
public class ServiceWrapper {
    /*
    String name() default "";
    boolean singleton() default true;
    int version() default 0;
    String implement() default "";
    String description() default "";
    boolean athenaFlag() default true;
    */
    private String name = "";
    private boolean singleton = true;
    private int version = 0;
    private String versionx = "";
    private String implement = "";
    private String description = "";
    private boolean athenaFlag = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getImplement() {
        return implement;
    }

    public void setImplement(String implement) {
        this.implement = implement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAthenaFlag() {
        return athenaFlag;
    }

    public void setAthenaFlag(boolean athenaFlag) {
        this.athenaFlag = athenaFlag;
    }

    public String getVersionx() {
        return versionx;
    }

    public void setVersionx(String versionx) {
        this.versionx = versionx;
    }

    public static ServiceWrapper wrapper(Service service){
        ServiceWrapper sw = new ServiceWrapper();
        sw.setName(service.name());
        sw.setVersion(service.version());
        sw.setImplement(service.implement());
        sw.setAthenaFlag(service.athenaFlag());
        sw.setDescription(service.description());
        return sw;
    }
}
