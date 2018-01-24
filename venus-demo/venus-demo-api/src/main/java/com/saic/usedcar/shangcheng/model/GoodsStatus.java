/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: GoodsStatus.java
 * Author:   guicailiang
 * Date:     2017年1月10日 下午5:58:30
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

/**
 * 功能描述 ：商品状态<br>
 *
 * @author guicailiang
 */
public enum GoodsStatus {

    WAIT_UP((byte) 0, "待上架"), WAIT_AUDIT((byte) 1, "待审核"), UP((byte) 2, "已上架"), DOWN((byte) 3, "已下架"), REJECT((byte) 4, "已驳回"),
    ;

    GoodsStatus(byte code, String message) {
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
    public static GoodsStatus get(byte code){
        if(code < 0 || code + 1 > GoodsStatus.values().length){
            return null;
        }
        
        return GoodsStatus.values()[code];
    }

}
