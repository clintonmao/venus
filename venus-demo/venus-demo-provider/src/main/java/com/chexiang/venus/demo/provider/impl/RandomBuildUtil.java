package com.chexiang.venus.demo.provider.impl;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zhangzhihua on 2017/11/7.
 */
public class RandomBuildUtil {

    static boolean isBuildDataModel = false;

    static Random random = new Random();

    public static void randomSleepOrThrow(){
        if(isBuildDataModel){
            if(ThreadLocalRandom.current().nextInt(100) > 50){//构造慢操作
                try {
                    Thread.sleep(random.nextInt(500));
                } catch (InterruptedException e) {
                }
            }else if(ThreadLocalRandom.current().nextInt(100) > 60){//构造异常操作
                if("A".equals("A")){
                    throw new IllegalArgumentException("param invalid.");
                }
            }
        }
    }
}
