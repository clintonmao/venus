/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: LevelType.java
 * Author:   guicailiang
 * Date:     2017年1月12日 上午10:50:08
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

/**
 * 功能描述:级别 <br> 
 *
 * @author guicailiang
 */
public enum LevelType {
    
    _1((byte) 1, "紧凑型"), _2((byte) 2, "中型"), _3((byte) 3, "中大型"), _4((byte) 4, "MPV"), _5((byte) 5, "SUV"), _6((byte) 6, "跑车"), _7((byte) 7, "小型车");

    LevelType(byte code, String message) {
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
     * 功能描述: 根据code找名称<br>
     *
     * @param code
     * @return
     */
    public static LevelType get(byte code){
        if(code < 1 || code > LevelType.values().length){
            return null;
        }
        
        return LevelType.values()[code - 1];
    }

}
