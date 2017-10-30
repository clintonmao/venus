package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.URL;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RandomLoadbanlance implements Loadbanlance {

    @Override
    public URL select(List<URL> urlList) {
        //加权设置 1~10，待优
        for(URL url:urlList){
            int weight = url.getWeight();
            //改无效设置
            weight = weight < 1?1:weight;
            weight = weight > 10?10:weight;
            if(weight > 1){
                for(int i=0;i<weight;i++){
                    urlList.add(url);
                }
            }
        }
        int index = ThreadLocalRandom.current().nextInt(urlList.size());
        return urlList.get(index);
    }
}
