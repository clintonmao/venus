package com.meidusa.venus.backend;

import com.athena.service.api.AthenaDataService;

/**
 * venus协议上下文信息
 * Created by Zhangzhihua on 2017/8/28.
 */
public class VenusProtocolContext {

    private static VenusProtocolContext venusProtocolContext;

    private VenusProtocol venusProtocol;

    public static VenusProtocolContext getInstance(){
        if(venusProtocolContext == null){
            venusProtocolContext = new VenusProtocolContext();
        }
        return venusProtocolContext;
    }

    public VenusProtocol getVenusProtocol() {
        return venusProtocol;
    }

    public void setVenusProtocol(VenusProtocol venusProtocol) {
        this.venusProtocol = venusProtocol;
    }
}
