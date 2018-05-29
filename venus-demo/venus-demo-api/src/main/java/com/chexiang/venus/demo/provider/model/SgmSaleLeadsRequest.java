/*
 * Copyright (C), 2013-2014, 上海汽车集团股份有限公司
 * FileName: SaleLeadsRequest.java
 * Author:   zhaomeng
 * Date:     2014-4-28 14:24:29
 * Description: 官网销售线索请求类
 */
package com.chexiang.venus.demo.provider.model;

import java.io.Serializable;
import java.util.List;

import com.meidusa.fastjson.annotation.JSONField;
import com.meidusa.fastmark.Serialize;

/**
 * 官网销售线索请求类
 * @author zhaomeng
 *
 */
public class SgmSaleLeadsRequest implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * url
	 */
	private String url;
	/**
	 * 传输记录数
	 */
	private int recordNumber;
	
	/**
	 * 销售线索
	 */
	@Serialize(name = "registredData")
	//@JSONField(name = "registredData")
	private List<SgmSaleLeadsDto> saleLeadsList;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<SgmSaleLeadsDto> getSaleLeadsList() {
		return saleLeadsList;
	}

	public void setSaleLeadsList(List<SgmSaleLeadsDto> saleLeadsList) {
		this.saleLeadsList = saleLeadsList;
	}

	public int getRecordNumber() {
		return recordNumber;
	}

	public void setRecordNumber(int recordNumber) {
		this.recordNumber = recordNumber;
	}
}
