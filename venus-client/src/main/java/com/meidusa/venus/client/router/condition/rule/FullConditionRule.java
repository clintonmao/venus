package com.meidusa.venus.client.router.condition.rule;

import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.URL;

/**
 * 条件路由规则
 * consumer.host!=192.168.1.1,192.168.1.2 => provider.host=192.168.2.1
 * consumer.host=192.168.1.1&consumer.app=order => provider.version=2.0.0
 * Created by Zhangzhihua on 2017/8/15.
 */
public class FullConditionRule implements ConditionRule {

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
    private LeftConditionRule leftRule;

    /**
     * 右侧规则定义，即提供者规则
     */
    private RightConditionRule rightRule;


    /**
     * 判断是否允许访问
     * @return
     */
    public boolean isReject(ClientInvocation invocation, URL url){
        return leftRule.isReject(invocation, url) || rightRule.isReject(invocation, url);
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

    public LeftConditionRule getLeftRule() {
        return leftRule;
    }

    public void setLeftRule(LeftConditionRule leftRule) {
        this.leftRule = leftRule;
    }

    public RightConditionRule getRightRule() {
        return rightRule;
    }

    public void setRightRule(RightConditionRule rightRule) {
        this.rightRule = rightRule;
    }
}
