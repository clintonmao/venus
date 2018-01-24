/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: LocationType.java
 * Author:   guicailiang
 * Date:     2017年1月12日 上午10:52:53
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

/**
 * 功能描述:地区 <br> 
 * @author guicailiang
 */
public enum LocationType {
	//增加所在地天津、苏州、宁波、常州、合肥、重庆
    _1((byte) 1, "上海", "block"), _2((byte) 2, "北京", "block"), _3((byte) 3, "成都", "block"), _4((byte) 4, "杭州", "block"), _5((byte) 5, "南京", "block"), 
    _6((byte) 6, "天津", "block"), _7((byte) 7, "石家庄", "none"), _8((byte) 8, "广州", "none"), _9((byte) 9, "深圳", "block"), _10((byte) 10, "郑州", "none"), 
    _11((byte) 11, "苏州", "block"), _12((byte) 12, "宁波", "block"), _13((byte) 13, "常州", "block"), _14((byte) 14, "合肥", "block"), _15((byte) 15, "重庆", "block");
    
    LocationType(byte code, String message, String display) {
        this.code = code;
        this.message = message;
        this.display = display;
    }

    private byte code;

    private String message;
    
    private String display;

    public byte getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
    
    public String getDisplay() {
        return display;
    }

    /**
     * 
     * 功能描述: 根据code找名称<br>
     *
     * @param code
     * @return
     */
    public static LocationType get(byte code){
        if(code < 1 || code > LocationType.values().length){
            return null;
        }
        
        return LocationType.values()[code - 1];
    }
}
