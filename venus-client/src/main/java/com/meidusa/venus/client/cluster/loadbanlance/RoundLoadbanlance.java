package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.URL;

import java.util.List;

/**
 * 轮询选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RoundLoadbanlance implements Loadbanlance {

    //下标位置
    private Integer position = 0;

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

        //reset
        synchronized (position){
            if(position >= urlList.size()){
                position = 0;
            }
            URL url = urlList.get(position);
            position++;
            return url;
        }
    }
}
