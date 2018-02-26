package com.meidusa.venus.registry.domain;

import com.meidusa.venus.URL;

import java.io.Serializable;
import java.util.List;

/**
 * 查询服务定义结果DO
 * Created by Zhangzhihua on 2018/2/24.
 */
public class QueryServiceDefinitionResultDO implements Serializable {

    private static final long serialVersionUID = -4662871174906585750L;

    private URL url;

    private List<VenusServiceDefinitionDO> serviceDefinitionList;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public List<VenusServiceDefinitionDO> getServiceDefinitionList() {
        return serviceDefinitionList;
    }

    public void setServiceDefinitionList(List<VenusServiceDefinitionDO> serviceDefinitionList) {
        this.serviceDefinitionList = serviceDefinitionList;
    }
}
