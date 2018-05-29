/*
 * Copyright (C), 2013-2014, 上海汽车集团股份有限公司
 * FileName: SaleLeadsResponse.java
 * Author:   zhaomeng
 * Date:     2014-4-28 14:24:29
 * Description: 官网销售线索返回实体类   
 */
package com.chexiang.venus.demo.provider.model;

import java.io.Serializable;

/**
 * 官网销售线索返回实体类
 * @author zhaomeng
 *
 */
public class SgmSaleLeadsResponse implements Serializable{

	private static final long serialVersionUID = 1L;

	private int resultCode;
	
	private String resultDesc;
	
	private String resultDetail;

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

	public String getResultDetail() {
		return resultDetail;
	}

	public void setResultDetail(String resultDetail) {
		this.resultDetail = resultDetail;
	}
	
	/**
     * 覆盖父类toString方法
     */    
    @Override
    public String toString() {
        return super.toString();
    }
}
