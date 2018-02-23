package com.meidusa.venus.client.router.condition.rule;

import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.router.condition.determin.AppRuleDetermin;
import com.meidusa.venus.client.router.condition.determin.HostRuleDetermin;
import org.apache.commons.lang.StringUtils;

/**
 * 右侧规则定义，也即提供者规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class RightConditionRule implements ConditionRule {

    private String appExp;

    private String appValues;

    private String hostExp;

    private String hostValues;

    //private String versionExp;
    //private String versionValues;

    public boolean isReject(ClientInvocation invocation, URL url){
        if(StringUtils.isNotBlank(appExp) && StringUtils.isNotBlank(appValues)){
            //获取提供者APP
            String providerApp = url.getApplication();
            boolean isReject = AppRuleDetermin.isReject(appExp,providerApp,appValues);
            if(isReject){
                return true;
            }
        }

        if(StringUtils.isNotBlank(hostExp) && StringUtils.isNotBlank(hostValues)){
            //获取提供者HOST
            String providerHost = url.getHost();
            boolean isReject = HostRuleDetermin.isReject(hostExp,providerHost,hostValues);
            if(isReject){
                return true;
            }
        }

        /*
        if(StringUtils.isNotBlank(versionExp) && StringUtils.isNotBlank(versionValues)){
            String providerVersion = url.getVersion();
            boolean isReject = VersionRuleDetermin.isReject(versionExp,providerVersion,versionValues);
            if(isReject){
                return true;
            }
        }
        */
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

}
