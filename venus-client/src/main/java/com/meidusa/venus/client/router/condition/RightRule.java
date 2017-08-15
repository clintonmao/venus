package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.URL;

/**
 * 右侧规则定义，也即提供者规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class RightRule {

    private String appExp;

    private String appValue;

    private String hostExp;

    private String[] hostValues;

    private String versionExp;

    private String[] versionValues;

    public boolean isMatch(URL url){
        //TODO 解析url提供者信息，若匹配则返回true
        return false;
    }

    public String getAppExp() {
        return appExp;
    }

    public void setAppExp(String appExp) {
        this.appExp = appExp;
    }

    public String getAppValue() {
        return appValue;
    }

    public void setAppValue(String appValue) {
        this.appValue = appValue;
    }

    public String getHostExp() {
        return hostExp;
    }

    public void setHostExp(String hostExp) {
        this.hostExp = hostExp;
    }

    public String[] getHostValues() {
        return hostValues;
    }

    public void setHostValues(String[] hostValues) {
        this.hostValues = hostValues;
    }

    public String getVersionExp() {
        return versionExp;
    }

    public void setVersionExp(String versionExp) {
        this.versionExp = versionExp;
    }

    public String[] getVersionValues() {
        return versionValues;
    }

    public void setVersionValues(String[] versionValues) {
        this.versionValues = versionValues;
    }
}
