package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.router.Router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 条件路由
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ConditionRouter implements Router {

    /**
     * 路由规则列表 TODO 初始化
     */
    Map<URL,List<ConditionRule>> urlConditionRuleMap = new HashMap<URL, List<ConditionRule>>();

    @Override
    public List<URL> filte(List<URL> urlList, Invocation invocation) {
        List<URL> avalibleURLList = new ArrayList<URL>();
        for(URL url: urlList){
            /*
            URL url = getURL(address,invocation);
            List<ConditionRule> conditionRuleList = urlConditionRuleMap.get(url);
            //若不匹配，则过滤掉
            if(CollectionUtils.isNotEmpty(conditionRuleList)){
                for(ConditionRule conditionRule:conditionRuleList){
                    if(!conditionRule.isMatch(url)){
                        break;
                    }
                }
                avalibleAddressList.add(address);
            }
            */
            avalibleURLList.add(url);
        }
        return avalibleURLList;
    }

}
