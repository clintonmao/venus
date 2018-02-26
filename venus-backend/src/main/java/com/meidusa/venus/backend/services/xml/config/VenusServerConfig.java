package com.meidusa.venus.backend.services.xml.config;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * venus服务端配置
 */
@XStreamAlias("venus-server")
public class VenusServerConfig {

    @XStreamAlias("services")
    private List<ExportService> exportServices = new ArrayList<ExportService>();

    @XStreamAlias("interceptors")
    private List<InterceptorDef> interceptorDefList = new ArrayList<InterceptorDef>();

    public List<ExportService> getExportServices() {
        return exportServices;
    }

    public void setExportServices(List<ExportService> exportServices) {
        this.exportServices = exportServices;
    }

    public List<InterceptorDef> getInterceptorDefList() {
        return interceptorDefList;
    }

    public void setInterceptorDefList(List<InterceptorDef> interceptorDefList) {
        this.interceptorDefList = interceptorDefList;
    }
}
