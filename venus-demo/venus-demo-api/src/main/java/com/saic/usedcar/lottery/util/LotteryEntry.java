/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: LotteryEntry.java
 * Author:   guicailiang
 * Date:     2017年11月18日 下午6:41:45
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.lottery.util;

/**
 * 功能描述 <br> 
 *
 * @author guicailiang
 */
public enum LotteryEntry {

    /**
     * 用户对象为null
     */
    USER_NULL(501, "未传用户对象"),
    
    /**
     * 用户未登录
     */
    USER_NOT_LOGIN(502, "用户未登录"),

    /**
     * 用户未抽奖
     */
    USER_NOT_USE(503, "用户未抽奖"),
    
    /**
     * 未指定活动
     */
    ACTIVITY_UNKNOW(511, "未指定活动"),
    
    /**
     * 活动未开始或已过期
     */
    ACTIVITY_INVALID(512, "活动不在有效期"),
    
    /**
     * 已抽奖
     */
    AWARD_USED(521, "已抽奖"),
    
    /**
     * 抽奖但是未抽中
     */
    AWARD_UNSELECTED(522, "谢谢参与"),
    
    /**
     * 抽中奖品
     */
    AWARD_SELECTED(523, "抽中奖品")
    ;
    
    @Override
    public String toString() {
        return "{\"status\":"+this.status+",\"message\":\""+message+"\"}";
    }

    LotteryEntry(int status, String message){
        this.status = status;
        this.message = message;
    }
    
    private int status;
    private String message;
    
    public int getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
    
}
