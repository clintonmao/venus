package com.meidusa.venus.monitor.filter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Zhangzhihua on 2017/9/6.
 */
public class DateTesting {

    public static void main(String[] args){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.SECOND,0);
        SimpleDateFormat format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
        String sTime = format.format(calendar.getTime());
        System.out.println("sTime:" + sTime);
    }
}
