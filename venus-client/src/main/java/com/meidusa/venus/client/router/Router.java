package com.meidusa.venus.client.router;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.URL;

import java.util.List;

/**
 * 路由接口
 * Created by Zhangzhihua on 2017/7/31.
 */
public interface Router {

    /**
     * 根据路由规则过滤地址列表 TODO invocation参数定义?
     * @param invocation  @return
     * @param urlList
     */
    List<URL> filte(Invocation invocation, List<URL> urlList);
}
