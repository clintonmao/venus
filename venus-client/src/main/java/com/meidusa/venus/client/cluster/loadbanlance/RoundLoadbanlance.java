package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.URL;

import java.util.List;

/**
 * 轮询选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RoundLoadbanlance implements Loadbanlance {

    private int index = 0;

    @Override
    public URL select(List<URL> urlList) {
        //TODO 地址记忆
        //TODO 权重因子
        URL url = urlList.get(index);
        if(index < urlList.size()){
            index++;
        }else{
            index = 0;
        }
        return url;
    }
}
