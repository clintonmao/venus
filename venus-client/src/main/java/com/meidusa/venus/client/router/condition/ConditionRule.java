package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.URL;

/**
 * 条件路由规则
 * consumer.host!=192.168.1.1,192.168.1.2 => provider.host=192.168.2.1
 * consumer.host=192.168.1.1&consumer.app=order => provider.version=2.0.0
 * Created by Zhangzhihua on 2017/8/15.
 */
public class ConditionRule {

    //等于，白名单
    public static final String EQ = "=";
    //不等于，黑名单
    public static final String NEQ = "!=";

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务接口名称
     */
    private String serviceInterfaceName;

    /**
     * 版本号
     */
    private String version;

    /**
     * 规则字符串
     */
    private String rule;

    /**
     * 左侧规则定义，即消费者规则
     */
    private LeftRule leftRule;

    /**
     * 右侧规则定义，即提供者规则
     */
    private RightRule rightRule;


    /**
     * 判断是否允许访问
     * @param url
     * @return
     */
    public boolean isReject(URL url){
        return leftRule.isReject(url) || rightRule.isReject(url);
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
