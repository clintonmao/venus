package com.meidusa.venus.backend.services.xml.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meidusa.venus.backend.services.InterceptorMapping;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * venus服务端配置
 */
@XStreamAlias("venus-server")
public class VenusServerConfig {

    @XStreamAlias("services")
    private List<ExportService> exportServices = new ArrayList<ExportService>();

    private Map<String, InterceptorMapping> interceptors = new HashMap<String, InterceptorMapping>();

    private Map<String, InterceptorStackConfig> interceptorStatcks = new HashMap<String, InterceptorStackConfig>();

    public void addService(ExportService service) {
        exportServices.add(service);
    }

    public void addInterceptor(InterceptorMapping mapping) {
        interceptors.put(mapping.getName(), mapping);
    }

    public InterceptorMapping getInterceptor(String name) {
        return interceptors.get(name);
    }

    public void addInterceptorStack(InterceptorStackConfig stack) {
        interceptorStatcks.put(stack.getName(), stack);
    }

    public List<ExportService> getExportServices() {
        return exportServices;
    }

    public Map<String, InterceptorMapping> getInterceptors() {
        return interceptors;
    }

    public Map<String, InterceptorStackConfig> getInterceptorStatcks() {
        return interceptorStatcks;
    }

    public void setExportServices(List<ExportService> exportServices) {
        this.exportServices = exportServices;
    }

    public void setInterceptors(Map<String, InterceptorMapping> interceptors) {
        this.interceptors = interceptors;
    }

    public void setInterceptorStatcks(Map<String, InterceptorStackConfig> interceptorStatcks) {
        this.interceptorStatcks = interceptorStatcks;
    }
}
