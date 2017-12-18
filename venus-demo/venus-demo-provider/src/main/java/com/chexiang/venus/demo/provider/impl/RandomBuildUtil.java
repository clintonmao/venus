package com.chexiang.venus.demo.provider.impl;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zhangzhihua on 2017/11/7.
 */
public class RandomBuildUtil {

    public static void randomSleepOrThrow(boolean isBuildDataModel){
        if(!isBuildDataModel){
            return;
        }
        if(ThreadLocalRandom.current().nextInt(100) > 90){//构造异常操作
            if("A".equals("A")){
                throw new IllegalArgumentException("param invalid.");
            }
        }else if(ThreadLocalRandom.current().nextInt(100) > 80){//构造慢操作
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000));
            } catch (InterruptedException e) {}
        }
    }
}
