package com.meidusa.venus.client.cluster.loadbalance;

import com.meidusa.venus.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 轮询选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RoundLoadbalance implements Loadbalance {

    private static Logger lbLogger = LoggerFactory.getLogger("venus.lb");

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
            //lbLogger.info("thread:{},position:{},address:{}",Thread.currentThread(),position,url.getHost() +":" +  url.getPort());
            position++;
            return url;
        }
    }
}
