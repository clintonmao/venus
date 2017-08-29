package com.meidusa.venus.client.router.condition;

/**
 * Version规则判断逻辑
 * Created by Zhangzhihua on 2017/8/29.
 */
public class VersionRule {

    public static boolean isReject(String versionExp,String sourceVersion,String targetVersions){
        versionExp = versionExp.trim();
        if(versionExp.equalsIgnoreCase(ConditionRule.EQ)){//EQ/白名单
            //允许Version名单
            String allowVersions = targetVersions;
            return !allowVersions.contains(sourceVersion);
        }else if(versionExp.equalsIgnoreCase(ConditionRule.NEQ)){//NEQ/黑名单
            //拒绝Version名单
            String refuseVersions = targetVersions;
            return targetVersions.contains(sourceVersion);
        }
        return false;
    }

}
