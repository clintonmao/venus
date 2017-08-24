package com.meidusa.venus.service.monitor;

import java.util.List;

import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;

@Service(name = "venus.MonitorService", version = 1, singleton = true)
public interface MonitorService extends SystemService {

    @Endpoint(name = "getSerivces")
    List<ServiceBean> getSerivces();

    @Endpoint(name = "getServerStatus")
    ServerStatus getServerStatus();

    @Endpoint(name = "getVersion")
    String getVersion();

}
