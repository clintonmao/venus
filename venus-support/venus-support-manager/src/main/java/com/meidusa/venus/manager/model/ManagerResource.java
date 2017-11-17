package com.meidusa.venus.manager.model;

/**
 * 管理资源模型
 * Created by Zhangzhihua on 2017/11/16.
 */
public class ManagerResource {
    //版本号
    private String version;

    //机器资源
    private MachineResource machineResource;

    //venus资源
    private VenusResource venusResource;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public MachineResource getMachineResource() {
        return machineResource;
    }

    public void setMachineResource(MachineResource machineResource) {
        this.machineResource = machineResource;
    }

    public VenusResource getVenusResource() {
        return venusResource;
    }

    public void setVenusResource(VenusResource venusResource) {
        this.venusResource = venusResource;
    }
}
