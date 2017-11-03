package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.client.router.condition.rule.FullConditionRule;
import com.meidusa.venus.client.router.condition.rule.LeftConditionRule;
import com.meidusa.venus.client.router.condition.rule.RightConditionRule;
import com.meidusa.venus.registry.domain.RouterRule;
import org.apache.commons.collections.CollectionUtils;
import org.dom4j.rule.Rule;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件路由规则解析
 * Created by Zhangzhihua on 2017/11/3.
 */
public class ConditionRuleParser {

    /**
     * 将字符串格式转化为可解析的格式
     * @param ruleDef
     * @return
     */
    public FullConditionRule parse(RouterRule ruleDef){
        //consumer.host!=192.168.1.1,192.168.1.2 => provider.host=192.168.2.1
        //consumer.host=192.168.1.1&consumer.app=order => *
        String exp = ruleDef.getExpress();
        String[] strDefArr = exp.split(ConditionRuleConstants.RULE_SPLIT);
        //整条规则
        FullConditionRule fullRule = new FullConditionRule();
        //左规则
        String leftDef =strDefArr[0];
        LeftConditionRule leftRule = this.parseLeftRule(leftDef);
        fullRule.setLeftRule(leftRule);
        //右规则
        String rightDef =strDefArr[1];
        RightConditionRule rightRule = this.parseRightRule(rightDef);
        fullRule.setRightRule(rightRule);
        return fullRule;
    }

    /**
     * 解析左规则
     * @param leftExp
     * @return
     */
    LeftConditionRule parseLeftRule(String leftExp){
        LeftConditionRule leftRule = new LeftConditionRule();
        List<RuleExpDef> defList = this.toRuleExpDefList(leftExp);
        if(CollectionUtils.isEmpty(defList)){
            return leftRule;
        }
        //解析consumer.app或consumer.host
        for(RuleExpDef def:defList){
            String name = def.getName();
            if("consumer.app".equalsIgnoreCase(name)){
                leftRule.setAppExp(def.getExp());
                leftRule.setAppValues(def.getValues());
            }else if("consumer.host".equalsIgnoreCase(name)){
                leftRule.setHostExp(def.getExp());
                leftRule.setHostValues(def.getValues());
            }
        }
        return leftRule;
    }

    /**
     * 解析右规则
     * @param rightExp
     * @return
     */
    RightConditionRule parseRightRule(String rightExp){
        RightConditionRule rightRule = new RightConditionRule();
        List<RuleExpDef> defList = this.toRuleExpDefList(rightExp);
        if(CollectionUtils.isEmpty(defList)){
            return rightRule;
        }
        for(RuleExpDef def:defList){
            String name = def.getName();
            //解析provider.host
            if("provider.host".equalsIgnoreCase(name)){
                rightRule.setHostExp(def.getExp());
                rightRule.setHostValues(def.getValues());
            }
        }
        return rightRule;
    }

    /**
     * 按规则将字符串定义转化语义记录
     * @param strExp
     * @return
     */
    List<RuleExpDef> toRuleExpDefList(String strExp){
        //consumer.host!=192.168.1.1,192.168.1.2 => provider.host=192.168.2.1
        strExp = strExp.trim();
        List<RuleExpDef> defList = new ArrayList<>();
        if("*".equalsIgnoreCase(strExp)){
            return defList;
        }
        String[] defArr = strExp.split("&");
        if(defArr != null && defArr.length > 0){
            for(String def:defArr){
                String[] sDef = def.split("=");
                if(sDef != null && sDef.length > 0){
                    RuleExpDef expDef = new RuleExpDef();
                    expDef.setName(sDef[0]);
                    expDef.setExp(ConditionRuleConstants.EQ);
                    expDef.setValues(sDef[1]);
                    defList.add(expDef);
                }
                sDef = def.split("=>");
                if(sDef != null && sDef.length > 0){
                    RuleExpDef expDef = new RuleExpDef();
                    expDef.setName(sDef[0]);
                    expDef.setExp(ConditionRuleConstants.NEQ);
                    expDef.setValues(sDef[1]);
                    defList.add(expDef);
                }
            }
        }
        return defList;
    }

    class RuleExpDef{
        //名称，如consumer.host、consumer.app
        private String name;
        //匹配符，如=、!=
        private String exp;
        //值，如192.168.1.1,192.168.1.2
        private String values;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getExp() {
            return exp;
        }

        public void setExp(String exp) {
            this.exp = exp;
        }

        public String getValues() {
            return values;
        }

        public void setValues(String values) {
            this.values = values;
        }
    }
}
