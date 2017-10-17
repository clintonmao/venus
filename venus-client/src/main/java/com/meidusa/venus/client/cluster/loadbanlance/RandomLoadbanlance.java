package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.URL;

import java.util.List;
import java.util.Random;

/**
 * 随机选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RandomLoadbanlance implements Loadbanlance {

    private final Random random = new Random();

    @Override
    public URL select(List<URL> urlList) {
        //TODO 地址记忆
        //TODO 实现及权重因子
        int index = random.nextInt(urlList.size());
        return urlList.get(index);
    }
}
