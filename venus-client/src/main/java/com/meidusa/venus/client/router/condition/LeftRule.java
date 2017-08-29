package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.URL;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 左侧规则定义，也即消费者规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class LeftRule {

    private String appExp;

    private String appValues;

    private String hostExp;

    private String hostValues;

    /**
     * 判断是否允许访问
     * @param url
     * @return
     */
    public boolean isReject(URL url){
        if(StringUtils.isNotBlank(appExp) && StringUtils.isNotBlank(appValues)){
            //获取消费者APP TODO
            String consumerApp = "TODO";
            boolean isReject = AppRule.isReject(appExp,consumerApp,appValues);
            if(isReject){
                return true;
            }
        }

        if(StringUtils.isNotBlank(hostExp) && StringUtils.isNotBlank(hostValues)){
            //获取消费者HOST TODO
            String consumerHost = "10.47.16.40";
            boolean isReject = HostRule.isReject(hostExp,consumerHost,hostValues);
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
