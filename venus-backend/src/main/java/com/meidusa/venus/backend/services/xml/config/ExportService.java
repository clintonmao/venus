package com.meidusa.venus.backend.services.xml.config;

import com.meidusa.toolkit.common.util.StringUtil;
import com.meidusa.venus.util.ArrayRange;
import com.meidusa.venus.util.BetweenRange;
import com.meidusa.venus.util.Range;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 发布服务
 */
@XStreamAlias("service")
public class ExportService {

    //接口名称
    @XStreamAsAttribute
    private String type;

    private Class<?> interfaceType;

    private String name;

    //引用spring实例名称
    @XStreamAsAttribute
    private String ref;

    private Object instance;

    //版本号，如1.0.0
    private String version;

    //兼容版本
    @XStreamAsAttribute
    private String supportVersion;

    private Range supportVersionRange;

    private Map<String, ExportServiceConfig> endpointConfigMap = new HashMap<String, ExportServiceConfig>();

    private String interceptorStack;

    //venus协议默认线程数
    private int coreThreads;

    private boolean active = true;

    public String getVersion() {
        return version;
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

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInterceptorStack() {
        return interceptorStack;
    }

    public void setInterceptorStack(String interceptorStack) {
        this.interceptorStack = interceptorStack;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Class<?> getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(Class<?> interfaceType) {
        this.interfaceType = interfaceType;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public ExportServiceConfig getEndpointConfig(String name) {
        return endpointConfigMap.get(name);
    }

    public void addEndpointConfig(ExportServiceConfig config) {
        endpointConfigMap.put(config.getName(), config);
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

    public Map<String, ExportServiceConfig> getEndpointConfigMap() {
        return endpointConfigMap;
    }

    public void setEndpointConfigMap(Map<String, ExportServiceConfig> endpointConfigMap) {
        this.endpointConfigMap = endpointConfigMap;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }
}
