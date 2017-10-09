package com.meidusa.venus.monitor;

import com.athena.service.api.AthenaDataService;

/**
 * athena应用上下文信息
 * Created by Zhangzhihua on 2017/8/28.
 */
public class AthenaContext {

    private static AthenaContext athenaContext;

    private AthenaDataService athenaDataService;

    public static AthenaContext getInstance(){
        if(athenaContext == null){
            athenaContext = new AthenaContext();
        }
        return athenaContext;
    }

    public AthenaDataService getAthenaDataService() {
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }
}
