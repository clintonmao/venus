package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.Address;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.Invocation;
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

    /**
     * 路由规则列表 TODO 初始化
     */
    Map<URL,List<ConditionRule>> urlConditionRuleMap = new HashMap<URL, List<ConditionRule>>();

    @Override
    public List<Address> filte(List<Address> addressList, Invocation invocation) {
        List<Address> avalibleAddressList = new ArrayList<Address>();
        for(Address address:addressList){
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
        }
        return avalibleAddressList;
    }

    /**
     * 获取url服务调用路径
     * @param address
     * @param invocation
     * @return
     */
    URL getURL(Address address, Invocation invocation){
        return null;
    }
}
