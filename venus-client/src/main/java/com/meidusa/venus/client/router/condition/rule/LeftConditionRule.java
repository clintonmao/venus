package com.meidusa.venus.client.router.condition.rule;

import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.router.condition.RuleDef;
import com.meidusa.venus.client.router.condition.determin.AppRuleDetermin;
import com.meidusa.venus.client.router.condition.determin.HostRuleDetermin;
import org.apache.commons.lang.StringUtils;

/**
 * 左侧规则定义，也即消费者规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class LeftConditionRule implements RuleDef {

    private String appExp;

    private String appValues;

    private String hostExp;

    private String hostValues;

    /**
     * 判断是否允许访问
     * @return
     */
    public boolean isReject(ClientInvocation invocation, URL url){
        if(StringUtils.isNotBlank(appExp) && StringUtils.isNotBlank(appValues)){
            //获取消费者APP
            String consumerApp = invocation.getConsumerApp();
            boolean isReject = AppRuleDetermin.isReject(appExp,consumerApp,appValues);
            if(isReject){
                return true;
            }
        }

        if(StringUtils.isNotBlank(hostExp) && StringUtils.isNotBlank(hostValues)){
            //获取消费者HOST
            String consumerHost = invocation.getConsumerIp();
            boolean isReject = HostRuleDetermin.isReject(hostExp,consumerHost,hostValues);
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
}
