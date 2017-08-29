package com.meidusa.venus.client.router.condition;

/**
 * Host规则判断逻辑
 * Created by Zhangzhihua on 2017/8/29.
 */
public class HostRule {

    public static boolean isReject(String hostExp,String sourceHost,String targetHosts){
        hostExp = hostExp.trim();
        if(hostExp.equalsIgnoreCase(ConditionRule.EQ)){//EQ/白名单
            //允许HOST名单
            String allowHosts = targetHosts;
            return !allowHosts.contains(sourceHost);
        }else if(hostExp.equalsIgnoreCase(ConditionRule.NEQ)){//NEQ/黑名单
            //拒绝HOST名单
            String refuseHosts = targetHosts;
            return refuseHosts.contains(sourceHost);
        }
        return false;
    }

}
