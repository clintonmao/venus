package com.meidusa.venus.registry.domain;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用
 * 
 * @author longhaisheng
 *
 */
public class VenusApplicationDO implements Serializable {

	private static final long serialVersionUID = 3177280460657245748L;

	private Integer id;

	/** 应用名 */
	private String appCode;

	/** 提供方 */
	private Boolean provider;

	/** 订阅方 */
	private Boolean consumer;

	private String createName;

	private String updateName;

	private Date createTime;

	private Date updateTime;
	
	private Boolean newApp;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAppCode() {
		return appCode;
	}

	public void setAppCode(String appCode) {
		this.appCode = appCode;
	}

	public String getCreateName() {
		return createName;
	}

	public void setCreateName(String createName) {
		this.createName = createName;
	}

	public String getUpdateName() {
		return updateName;
	}

	public void setUpdateName(String updateName) {
		this.updateName = updateName;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Boolean isProvider() {
		return provider;
	}

	public void setProvider(Boolean provider) {
		this.provider = provider;
	}

	public Boolean isConsumer() {
		return consumer;
	}

	public void setConsumer(Boolean consumer) {
		this.consumer = consumer;
	}

	public Boolean getNewApp() {
		return newApp;
	}

	public void setNewApp(Boolean newApp) {
		this.newApp = newApp;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
