package com.meidusa.venus.client.cluster.loadbalance;

import com.meidusa.venus.URL;

import java.util.List;

/**
 * loadbalance接口
 * Created by Zhangzhihua on 2017/8/1.
 */
public interface Loadbalance {

    /**
     * 选择目标地址
     * @return
     */
    URL select(List<URL> urlList);
}
