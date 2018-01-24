/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: CarUsedTimeType.java
 * Author:   guicailiang
 * Date:     2017年1月12日 上午10:35:25
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

/**
 * 功能描述:车龄 <br>
 * @author guicailiang
 */
public enum CarUsedTimeType {

    _1((byte) 1, "3年以内"), _2((byte) 2, "3-5年"), _3((byte) 3, "5-8年"), _4((byte) 4, "8-10年"), _5((byte) 5, "10年以上");

    CarUsedTimeType(byte code, String message) {
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
    public static CarUsedTimeType convert(int carUsedTime) {
        if (carUsedTime <= 3) {
            return _1;
        } else if (carUsedTime <= 5) {
            return _2;
        } else if (carUsedTime <= 8) {
            return _3;
        } else if (carUsedTime <= 10) {
            return _4;
        }
        return _5;
    }
    
    /**
     * 
     * 功能描述: 根据code找名称<br>
     *
     * @param code
     * @return
     */
    public static CarUsedTimeType get(byte code){
        if(code < 1 || code > CarUsedTimeType.values().length){
            return null;
        }
        return CarUsedTimeType.values()[code - 1];
    }

}
