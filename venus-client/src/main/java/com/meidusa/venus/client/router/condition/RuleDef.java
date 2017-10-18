package com.meidusa.venus.client.router.condition;

import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.URL;

/**
 * 规则定义接口
 * Created by Zhangzhihua on 2017/10/18.
 */
public interface RuleDef {

    /**
     * 判断请求是否符合规则
     * @param invocation
     * @param url
     * @return
     */
    boolean isReject(ClientInvocation invocation, URL url);

}
