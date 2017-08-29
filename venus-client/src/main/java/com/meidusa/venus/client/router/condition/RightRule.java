package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.URL;
import org.apache.commons.lang.StringUtils;

/**
 * 右侧规则定义，也即提供者规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class RightRule {

    private String appExp;

    private String appValues;

    private String hostExp;

    private String hostValues;

    private String versionExp;

    private String versionValues;

    public boolean isReject(URL url){
        if(StringUtils.isNotBlank(appExp) && StringUtils.isNotBlank(appValues)){
            //获取提供者APP TODO
            String providerApp = "TODO";
            boolean isReject = AppRule.isReject(appExp,providerApp,appValues);
            if(isReject){
                return true;
            }
        }

        if(StringUtils.isNotBlank(hostExp) && StringUtils.isNotBlank(hostValues)){
            //获取提供者HOST TODO
            String providerHost = "10.47.16.40";
            boolean isReject = HostRule.isReject(hostExp,providerHost,hostValues);
            if(isReject){
                return true;
            }
        }

        if(StringUtils.isNotBlank(versionExp) && StringUtils.isNotBlank(versionValues)){
            //获取提供者版本号 TODO
            String providerVersion = "0.0.0";
            boolean isReject = VersionRule.isReject(versionExp,providerVersion,versionValues);
            if(isReject){
                return true;
            }
        }
        return false;
    }

    public String getAppExp() {
        return appExp;
    }

    public void setAppExp(String appExp) {
        this.appExp = appExp;
    }

    public String getAppValues() {
        return appValues;
    }

    public void setAppValues(String appValues) {
        this.appValues = appValues;
    }

    public String getHostExp() {
        return hostExp;
    }

    public void setHostExp(String hostExp) {
        this.hostExp = hostExp;
    }

    public String getHostValues() {
        return hostValues;
    }

    public void setHostValues(String hostValues) {
        this.hostValues = hostValues;
    }

    public String getVersionExp() {
        return versionExp;
    }

    public void setVersionExp(String versionExp) {
        this.versionExp = versionExp;
    }

    public String getVersionValues() {
        return versionValues;
    }

    public void setVersionValues(String versionValues) {
        this.versionValues = versionValues;
    }
}
