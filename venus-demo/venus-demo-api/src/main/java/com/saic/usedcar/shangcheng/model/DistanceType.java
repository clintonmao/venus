/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: DistanceType.java
 * Author:   guicailiang
 * Date:     2017年1月12日 上午10:23:53
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

/**
 * 表显里程 <br> 
 * @author guicailiang
 */
public enum DistanceType {

    _1((byte) 1, "1万公里以内"), _2((byte) 2, "1-3万公里"), _3((byte) 3, "3-5万公里"), _4((byte) 4, "5-8万公里"), _5((byte) 5, "8-10万公里"), _6((byte) 6, "10万公里以上");

    DistanceType(byte code, String message) {
        this.code = code;
        this.message = message;
    }

    private byte code;

    private String message;

    public byte getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
    
    /**
     * 
     * 功能描述: 根据里程计算相应的级别<br>
     * @param distance
     * @return
     */
    public static DistanceType convert(long distance) {
        if (distance <= 10000) {
            return _1;
        } else if (distance <= 30000) {
            return _2;
        } else if (distance <= 50000) {
            return _3;
        } else if (distance <= 80000) {
            return _4;
        } else if (distance <= 100000) {
            return _5;
        }
        return _6;
    }
    
    /**
     * 
     * 功能描述: 根据code找名称<br>
     * @param code
     * @return
     */
    public static DistanceType get(byte code){
        if(code < 1 || code > DistanceType.values().length){
            return null;
        }
        return DistanceType.values()[code - 1];
    }

}
