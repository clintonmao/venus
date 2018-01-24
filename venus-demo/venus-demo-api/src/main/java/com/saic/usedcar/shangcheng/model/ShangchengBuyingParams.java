package com.saic.usedcar.shangcheng.model;

import java.io.Serializable;
import java.util.List;

/**
 * 抢购参数类
 * @author tangxiangquan
 */
public class ShangchengBuyingParams implements Serializable{

	private static final long serialVersionUID = 1L;
	
	/** 组织ID  */
    private Long orgId;
    
    /** 地区  */
    private String location;
    
    /** 商品编号  */
    private String mdseCode;
    
    private String startTimeS; //抢购开始时间
    private String startTimeE;  
    
    private String endTimeS; //抢购结束
    private String endTimeE;
    
    private int capitalHave=-1; //是否有留资
    private int capitalStatus=-1;//留资状态
    
    private int sellHave=-1; //是否售出
    
    private int rule=-1; //规则
    
    private String saleTimeS; //销售收款时间
    private String saleTimeE;
    
    /** 车牌  */
    private String licence;
    
    /** 交易号  */
    private String transactionCode;
    
    /** 商品状态  */
    private int status;
    private List<Long> orgIds;
    
	public int getCapitalHave() {
		return capitalHave;
	}
	public void setCapitalHave(int capitalHave) {
		this.capitalHave = capitalHave;
	}
	public int getCapitalStatus() {
		return capitalStatus;
	}
	public void setCapitalStatus(int capitalStatus) {
		this.capitalStatus = capitalStatus;
	}
	public int getSellHave() {
		return sellHave;
	}
	public void setSellHave(int sellHave) {
		this.sellHave = sellHave;
	}
	public int getRule() {
		return rule;
	}
	public void setRule(int rule) {
		this.rule = rule;
	}
	public String getSaleTimeS() {
		return saleTimeS;
	}
	public void setSaleTimeS(String saleTimeS) {
		this.saleTimeS = saleTimeS;
	}
	public String getSaleTimeE() {
		return saleTimeE;
	}
	public void setSaleTimeE(String saleTimeE) {
		this.saleTimeE = saleTimeE;
	}
	public String getEndTimeS() {
		return endTimeS;
	}
	public void setEndTimeS(String endTimeS) {
		this.endTimeS = endTimeS;
	}
	public String getEndTimeE() {
		return endTimeE;
	}
	public void setEndTimeE(String endTimeE) {
		this.endTimeE = endTimeE;
	}
	public List<Long> getOrgIds() {
		return orgIds;
	}
	public void setOrgIds(List<Long> orgIds) {
		this.orgIds = orgIds;
	}
	public Long getOrgId() {
		return orgId;
	}
	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getMdseCode() {
		return mdseCode;
	}
	public void setMdseCode(String mdseCode) {
		this.mdseCode = mdseCode;
	}
	public String getStartTimeS() {
		return startTimeS;
	}
	public void setStartTimeS(String startTimeS) {
		this.startTimeS = startTimeS;
	}
	public String getStartTimeE() {
		return startTimeE;
	}
	public void setStartTimeE(String startTimeE) {
		this.startTimeE = startTimeE;
	}
	public String getLicence() {
		return licence;
	}
	public void setLicence(String licence) {
		this.licence = licence;
	}
	public String getTransactionCode() {
		return transactionCode;
	}
	public void setTransactionCode(String transactionCode) {
		this.transactionCode = transactionCode;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
}
