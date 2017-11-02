package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.rule.ConditionRule;
import com.meidusa.venus.client.router.condition.rule.LeftConditionRule;
import com.meidusa.venus.client.router.condition.rule.RightConditionRule;
import com.meidusa.venus.registry.domain.RouterRule;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件路由
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ConditionRouter implements Router {

    @Override
    public List<URL> filte(Invocation invocation, List<URL> urlList) {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        //过滤url
        List<URL> alllowUrlList = new ArrayList<URL>();
        for(URL url: urlList){
            if(!isReject(clientInvocation, url)){
                alllowUrlList.add(url);
            }
        }
        if(CollectionUtils.isEmpty(alllowUrlList)){
            throw new RpcException("with rule filter,no allowed service providers.");
        }
        return alllowUrlList;
    }

    /**
     * 判断是否匹配
     * @param clientInvocation
     * @param  url
     * @return
     */
    boolean isReject(ClientInvocation clientInvocation, URL url){
        //获取服务定义规则列表
        List<ConditionRule> ruleDefList = getRouteRules(url);
        //若规则定义为空，则可访问
        if(CollectionUtils.isEmpty(ruleDefList)){
            return false;
        }

        for (ConditionRule rule : ruleDefList) {
            if (rule.isReject(clientInvocation,url )) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据调用请求获取路由规则
     * @param
     * @return
     */
    List<ConditionRule> getRouteRules(URL url){
        List<ConditionRule> rules = new ArrayList<ConditionRule>();

        VenusServiceDefinitionDO srvDef = (VenusServiceDefinitionDO)url.getServiceDefinition();
        if(srvDef == null){
            return rules;
        }
        List<VenusServiceConfigDO> srvCfgList = srvDef.getServiceConfigs();
        if(CollectionUtils.isEmpty(srvCfgList)){
            return rules;
        }

        for(VenusServiceConfigDO srvCfg:srvCfgList){
            RouterRule jsonRuleDef = srvCfg.getRouterRule();
            if(isValidRule(jsonRuleDef)){
                //将字符串规则转化为领域模型
                rules.add(toConditionRule(jsonRuleDef));
            }
        }

        return rules;
    }

    /**
     * 判断规则是否有效，要有注册中心管理来进行有效性校验
     * @param ruleDef
     * @return
     */
    boolean isValidRule(RouterRule ruleDef){
        if(StringUtils.isEmpty(ruleDef.getExpress())){
            return false;
        }
        String[] arr = ruleDef.getExpress().split("=>");
        if(arr == null || arr.length < 2){
            return false;
        }
        return false;
    }

    /**
     * 将字符串格式转化为可解析的格式
     * @param ruleDef
     * @return
     */
    ConditionRule toConditionRule(RouterRule ruleDef){
        //consumer.host!=192.168.1.1,192.168.1.2 => provider.host=192.168.2.1
        //consumer.host=192.168.1.1&consumer.app=order => provider.version=2.0.0
        String exp = ruleDef.getExpress();
        String[] strDefArr = exp.split("=>");
        //整条规则
        ConditionRule rule = new ConditionRule();
        //左规则
        LeftConditionRule leftRule = new LeftConditionRule();
        String leftDef =strDefArr[0];
        leftRule.setHostExp(ConditionRule.EQ);
        leftRule.setHostValues("10.47.16.40");
        rule.setLeftRule(leftRule);
        //右规则
        RightConditionRule rightRule = new RightConditionRule();
        String rightDef =strDefArr[1];
        rightRule.setHostExp(ConditionRule.EQ);
        rightRule.setHostValues("10.47.16.40");
        rule.setRightRule(rightRule);
        return rule;
    }

    /**
     * 获取所有规则映射表
     * @return
     */
    List<ConditionRule> getTempsRouteRules(URL url){
        //consumer.host=10.47.16.40 => provider.host=10.47.16.40
        //构造rules
        List<ConditionRule> rules = new ArrayList<ConditionRule>();
        //整条规则
        ConditionRule rule = new ConditionRule();
        //左规则
        LeftConditionRule leftRule = new LeftConditionRule();
        leftRule.setHostExp(ConditionRule.EQ);
        leftRule.setHostValues("10.47.16.40");
        rule.setLeftRule(leftRule);
        //右规则
        RightConditionRule rightRule = new RightConditionRule();
        rightRule.setHostExp(ConditionRule.EQ);
        rightRule.setHostValues("10.47.16.40");
        rule.setRightRule(rightRule);
        rules.add(rule);

        return rules;
    }

}
