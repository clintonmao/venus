package com.meidusa.venus.client.router.condition.determin;

import com.meidusa.venus.client.router.condition.ConditionRuleConstants;
import com.meidusa.venus.client.router.condition.rule.FullConditionRule;

/**
 * Host语义规则判断逻辑
 * Created by Zhangzhihua on 2017/8/29.
 */
public class HostRuleDetermin {

    public static boolean isReject(String hostExp,String sourceHost,String targetHosts){
        hostExp = hostExp.trim();
        if(hostExp.equalsIgnoreCase(ConditionRuleConstants.EQ)){//EQ/白名单
            //允许HOST名单
            String allowHosts = targetHosts;
            return !allowHosts.contains(sourceHost);
        }else if(hostExp.equalsIgnoreCase(ConditionRuleConstants.NEQ)){//NEQ/黑名单
            //拒绝HOST名单
            String refuseHosts = targetHosts;
            return refuseHosts.contains(sourceHost);
        }
        return false;
    }

}
