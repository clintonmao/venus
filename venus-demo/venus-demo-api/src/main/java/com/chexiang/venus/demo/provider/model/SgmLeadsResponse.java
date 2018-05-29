package com.chexiang.venus.demo.provider.model;

import java.io.Serializable;

/**
 * 
 * 官网销售线索校验结果返回实体类
 * 
 *
 * @author weibinbin
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class SgmLeadsResponse implements Serializable {

    /**
     */
    private static final long serialVersionUID = 1L;
    
    private String recordId; //官网销售线索ID

    private int resultCode; //验证代码

    private String resultDesc; //验证描述

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public void setResultDesc(String resultDesc) {
        this.resultDesc = resultDesc;
    }
}
