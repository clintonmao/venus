package com.chexiang.venus.demo.provider.impl;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zhangzhihua on 2017/11/7.
 */
public class RandomBuildUtil {

    static Random random = new Random();

    public static void randomSleepOrThrow(boolean isBuildDataModel){
        if(ThreadLocalRandom.current().nextInt(100) > 80){//构造异常操作
            if("A".equals("A")){
                throw new IllegalArgumentException("param invalid.");
            }
        }else if(ThreadLocalRandom.current().nextInt(100) > 50){//构造慢操作
            try {
                Thread.sleep(random.nextInt(3000));
            } catch (InterruptedException e) {}
        }
    }
}
