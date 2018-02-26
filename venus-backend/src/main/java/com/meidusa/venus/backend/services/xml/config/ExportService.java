package com.meidusa.venus.backend.services.xml.config;

import com.meidusa.toolkit.common.util.StringUtil;
import com.meidusa.venus.backend.services.Interceptor;
import com.meidusa.venus.util.ArrayRange;
import com.meidusa.venus.util.BetweenRange;
import com.meidusa.venus.util.Range;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发布服务
 */
@XStreamAlias("service")
public class ExportService {

    //接口名称
    @XStreamAsAttribute
    private String type;

    //引用spring实例名称
    @XStreamAsAttribute
    private String ref;

    //兼容版本
    @XStreamAsAttribute
    private String supportVersion;

    //拦截器名称
    @XStreamAsAttribute
    private String interceptors;

    private List<Interceptor> interceptorList = new ArrayList<>();

    private String serviceName;

    private Class<?> serviceInterface;

    private Object instance;

    private Range supportVersionRange;

    private int version;

    private boolean athenaFlag;

    private String description;

    //venus协议默认线程数
    private int coreThreads;

    private boolean active = true;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isAthenaFlag() {
        return athenaFlag;
    }

    public void setAthenaFlag(boolean athenaFlag) {
        this.athenaFlag = athenaFlag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Range getSupportVersionRange() {
        if (supportVersionRange != null) {
            return supportVersionRange;
        }
        if (!StringUtil.isEmpty(supportVersion)) {
            supportVersion = supportVersion.trim();
            String[] tmps = StringUtils.split(supportVersion, "{}[], ");
            int[] rages = new int[tmps.length];
            for (int i = 0; i < tmps.length; i++) {
                rages[i] = Integer.valueOf(tmps[i]);
            }

            if (supportVersion.startsWith("[")) {
                supportVersionRange = new BetweenRange(rages);
            } else {
                supportVersionRange = new ArrayRange(rages);
            }
            return supportVersionRange;
        } else {
            return null;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public int getCoreThreads() {
        return coreThreads;
    }

    public void setCoreThreads(int coreThreads) {
        this.coreThreads = coreThreads;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSupportVersion() {
        return supportVersion;
    }

    public void setSupportVersion(String supportVersion) {
        this.supportVersion = supportVersion;
    }

    public void setSupportVersionRange(Range supportVersionRange) {
        this.supportVersionRange = supportVersionRange;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(String interceptors) {
        this.interceptors = interceptors;
    }

    public List<Interceptor> getInterceptorList() {
        return interceptorList;
    }

    public void setInterceptorList(List<Interceptor> interceptorList) {
        this.interceptorList = interceptorList;
    }
}
