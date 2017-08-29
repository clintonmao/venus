package com.meidusa.venus.client.router.condition;

/**
 * App规则判断逻辑
 * Created by Zhangzhihua on 2017/8/29.
 */
public class AppRule {

    /**
     *
     * @param appExp 表达式类型，等于(= EQ)或不等于(!= NEQ)
     * @param sourceApp 源APP名称
     * @param targetApps 目标匹配APP名称
     * @return
     */
    public static boolean isReject(String appExp,String sourceApp,String targetApps){
        appExp = appExp.trim();
        if(appExp.equalsIgnoreCase(ConditionRule.EQ)){//EQ/白名单
            //允许APP名单
            String allowApps = targetApps;
            return !allowApps.contains(sourceApp);
        }else if(appExp.equalsIgnoreCase(ConditionRule.NEQ)){//NEQ/黑名单
            //拒绝APP名单
            String refuseApps = targetApps;
            return refuseApps.contains(sourceApp);
        }
        return false;
    }

}
