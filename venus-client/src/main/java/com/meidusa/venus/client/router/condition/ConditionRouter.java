package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.router.Router;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 条件路由
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ConditionRouter implements Router {

    @Override
    public List<URL> filte(Invocation invocation, List<URL> urlList) {
        List<URL> alllowUrlList = new ArrayList<URL>();
        for(URL url: urlList){
            List<ConditionRule> ruleList = getRouteRules(url);
            if(CollectionUtils.isEmpty(ruleList)){
                //若路由规则为空，则添加
                alllowUrlList.add(url);
            }else{
                //若规则不空，则判断是否匹配
                if(isReject(url, ruleList)){
                    continue;
                }else{
                    alllowUrlList.add(url);
                }
            }
        }
        if(CollectionUtils.isEmpty(alllowUrlList)){
            throw new RpcException("not found avalid providers or not allowed access.");
        }
        return alllowUrlList;
    }

    /**
     * 判断是否匹配
     * @param url
     * @param ruleList
     * @return
     */
    boolean isReject(URL url, List<ConditionRule> ruleList){
        if(CollectionUtils.isNotEmpty(ruleList)) {
            for (ConditionRule rule : ruleList) {
                if (rule.isReject(url)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据url获取路由规则
     * @param url
     * @return
     */
    List<ConditionRule> getRouteRules(URL url){
        //TODO 测试构造用，根据请求url解析
        String serviceUrl = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0";
        return getAllRouterRuleMappings().get(URL.parse(serviceUrl));
    }

    /**
     * 获取所有规则映射表 TODO 临时构造测试用，从注册中心加载
     * @return
     */
    Map<URL,List<ConditionRule>> getAllRouterRuleMappings(){
        Map<URL,List<ConditionRule>> urlRulesMap = new HashMap<URL, List<ConditionRule>>();

        //构造Url
        String serviceUrl = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0";
        URL url = URL.parse(serviceUrl);

        //构造rules
        List<ConditionRule> rules = new ArrayList<ConditionRule>();
        //rule1[consumer.host=10.47.16.40 => provider.host=10.47.16.40]
        ConditionRule rule = new ConditionRule();
        LeftRule leftRule = new LeftRule();
        leftRule.setHostExp(ConditionRule.EQ);
        leftRule.setHostValues("10.47.16.40");
        rule.setLeftRule(leftRule);
        RightRule rightRule = new RightRule();
        rightRule.setHostExp(ConditionRule.EQ);
        rightRule.setHostValues("10.47.16.40");
        rule.setRightRule(rightRule);
        rules.add(rule);

        urlRulesMap.put(url,rules);
        return urlRulesMap;
    }

}
