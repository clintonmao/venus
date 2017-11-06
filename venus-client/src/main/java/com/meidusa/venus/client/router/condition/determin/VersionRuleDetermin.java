package com.meidusa.venus.client.router.condition.determin;

import com.meidusa.venus.client.router.condition.ConditionRuleConstants;
import com.meidusa.venus.client.router.condition.rule.FullConditionRule;

/**
 * Version语义规则判断逻辑
 * Created by Zhangzhihua on 2017/8/29.
 */
@Deprecated
//服务有独立的版本号，只能控制app/host语义
public class VersionRuleDetermin {

    public static boolean isReject(String versionExp,String sourceVersion,String targetVersions){
        versionExp = versionExp.trim();
        if(versionExp.equalsIgnoreCase(ConditionRuleConstants.EQ)){//EQ/白名单
            //允许Version名单
            String allowVersions = targetVersions;
            return !allowVersions.contains(sourceVersion);
        }else if(versionExp.equalsIgnoreCase(ConditionRuleConstants.NEQ)){//NEQ/黑名单
            //拒绝Version名单
            String refuseVersions = targetVersions;
            return targetVersions.contains(sourceVersion);
        }
        return false;
    }

}
