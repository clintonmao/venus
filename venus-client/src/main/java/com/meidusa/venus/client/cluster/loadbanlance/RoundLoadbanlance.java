package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.URL;

import java.util.List;

/**
 * 轮询选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RoundLoadbanlance implements Loadbanlance {

    //下标位置
    private int position = 0;

    @Override
    public URL select(List<URL> urlList) {
        //避免地址列表刷新，导致下标越界
        if(position >= urlList.size()){
            position = 0;
        }
        //TODO 加权
        URL url = urlList.get(position);
        if(position < urlList.size()){
            position++;
        }else{
            position = 0;
        }
        return url;
    }
}
