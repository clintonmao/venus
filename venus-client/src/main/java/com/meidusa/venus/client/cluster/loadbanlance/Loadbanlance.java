package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.URL;

import java.util.List;

/**
 * loadbanlance接口
 * Created by Zhangzhihua on 2017/8/1.
 */
public interface Loadbanlance {

    /**
     * 选择目标地址
     * @return
     */
    URL select(List<URL> urlList);
}
