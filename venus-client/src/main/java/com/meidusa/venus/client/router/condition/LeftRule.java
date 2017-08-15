package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.URL;

/**
 * 左侧规则定义，也即消费者规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class LeftRule {

    private String app;

    private String[] hosts;

    public boolean isMatch(URL url){
        //TODO 解析url消费者信息，若匹配则返回true
        return false;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String[] getHosts() {
        return hosts;
    }

    public void setHosts(String[] hosts) {
        this.hosts = hosts;
    }
}
