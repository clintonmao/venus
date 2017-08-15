package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.URL;

/**
 * 左侧规则定义，也即消费者规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class LeftRule {

    private String appExp;

    private String appValue;

    private String hostExp;

    private String[] hostValues;

    public boolean isMatch(URL url){
        //TODO 解析url消费者信息，若匹配则返回true
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
}
