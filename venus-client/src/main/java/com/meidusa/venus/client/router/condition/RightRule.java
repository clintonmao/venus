package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.URL;

/**
 * 右侧规则定义，也即提供者规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class RightRule {

    private String app;

    private String[] hosts;

    private String[] versions;

    public boolean isMatch(URL url){
        //TODO 解析url提供者信息，若匹配则返回true
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

    public String[] getVersions() {
        return versions;
    }

    public void setVersions(String[] versions) {
        this.versions = versions;
    }
}
