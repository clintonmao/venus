package com.meidusa.venus.backend.services.xml.support;

import com.meidusa.venus.VenusMetaInfo;
import com.meidusa.venus.service.monitor.MonitorRuntime;
import com.meidusa.venus.service.monitor.MonitorService;
import com.meidusa.venus.service.monitor.ServerStatus;
import com.meidusa.venus.service.monitor.ServiceBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 监听service
 * Created by Zhangzhihua on 2017/8/3.
 */
public class VenusMonitorService implements MonitorService{

    @Override
    public List<ServiceBean> getSerivces() {
        List<ServiceBean> list = new ArrayList<ServiceBean>();
        list.addAll(MonitorRuntime.getInstance().getServiceMap().values());
        return list;
    }

    @Override
    public ServerStatus getServerStatus() {
        ServerStatus status = new ServerStatus();
        status.setUptime(MonitorRuntime.getInstance().getUptime());

        return status;
    }

    @Override
    public String getVersion() {
        return VenusMetaInfo.VENUS_VERSION;
    }

}
