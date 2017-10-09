package com.meidusa.venus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 反射工具类
 * Created by Zhangzhihua on 2017/10/9.
 */
public class ReftorUtil {

    private static Logger logger = LoggerFactory.getLogger(ReftorUtil.class);

    public static <T> T newInstance(String className){
        if (className == null) {
            return null;
        }

        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.error("load class error " + className, e);
            return null;
        }

        try {
            return (T) (clazz.newInstance());
        } catch (InstantiationException e) {
            logger.error("instantiate class error " + className, e);
            return null;
        } catch (IllegalAccessException e) {
            logger.error("class cannot be access error " + className, e);
            return null;
        }
    }
}
