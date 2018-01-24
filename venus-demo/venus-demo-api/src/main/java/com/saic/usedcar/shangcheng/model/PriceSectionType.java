/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: PriceSectionType.java
 * Author:   guicailiang
 * Date:     2017年1月12日 上午10:39:40
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

/**
 * 功能描述:商城价格区间 <br> 
 * @author guicailiang
 */
public enum PriceSectionType {

    _1((byte) 1, "3万以下"), _2((byte) 2, "3-5万"), _3((byte) 3, "5-8万"), _4((byte) 4, "8-10万"), _5((byte) 5, "10-15万"), _6((byte) 6, "15-20万"), _7((byte) 7,
            "20-30万"), _8((byte) 8, "30-50万"), _9((byte) 9, "50万以上");

    PriceSectionType(byte code, String message) {
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
     *
     * @param distance
     * @return
     */
    public static PriceSectionType convert(int price) {
        if (price <= 30000) {
            return _1;
        } else if (price <= 50000) {
            return _2;
        } else if (price <= 80000) {
            return _3;
        } else if (price <= 100000) {
            return _4;
        } else if (price <= 150000) {
            return _5;
        } else if (price <= 200000) {
            return _6;
        } else if (price <= 300000) {
            return _7;
        } else if (price <= 500000) {
            return _8;
        }
        
        return _9;
    }
    
    /**
     * 
     * 功能描述: 根据code找名称<br>
     *
     * @param code
     * @return
     */
    public static PriceSectionType get(byte code){
        if(code < 1 || code > PriceSectionType.values().length){
            return null;
        }
        
        return PriceSectionType.values()[code - 1];
    }

}
