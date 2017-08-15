package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.URL;

/**
 * 条件路由规则
 * Created by Zhangzhihua on 2017/8/15.
 */
public class ConditionRule {

    /**
     * 服务接口名称
     */
    private String serviceInterfaceName;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 版本号
     */
    private String version;

    /**
     * 规则字符串
     */
    private String rule;

    /**
     * 左侧规则定义，目前暂支持一个消费者定义
     */
    private LeftRule leftRule;

    /**
     * 右侧规则定义，目前暂支持一个提供者定义
     */
    private RightRule rightRule;


    public boolean isMatch(URL url){
        return leftRule.isMatch(url) && rightRule.isMatch(url);
    }

    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    public void setServiceInterfaceName(String serviceInterfaceName) {
        this.serviceInterfaceName = serviceInterfaceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public LeftRule getLeftRule() {
        return leftRule;
    }

    public void setLeftRule(LeftRule leftRule) {
        this.leftRule = leftRule;
    }

    public RightRule getRightRule() {
        return rightRule;
    }

    public void setRightRule(RightRule rightRule) {
        this.rightRule = rightRule;
    }
}
