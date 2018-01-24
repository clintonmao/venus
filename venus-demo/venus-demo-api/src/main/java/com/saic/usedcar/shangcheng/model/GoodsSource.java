/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: GoodsStatus.java
 * Author:   guicailiang
 * Date:     2017年1月10日 下午5:58:30
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

/**
 * 功能描述：商品来源 <br>
 * @author guicailiang
 */
public enum GoodsSource {

    SX((byte) 1, "协议"), SELF((byte) 2, "自建")
    ;

    GoodsSource(byte code, String message) {
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
    public static GoodsSource get(byte code){
        if(code < 1 || code > GoodsSource.values().length){
            return null;
        }
        
        return GoodsSource.values()[code - 1];
    }

}
