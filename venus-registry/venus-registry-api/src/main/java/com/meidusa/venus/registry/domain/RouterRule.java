package com.meidusa.venus.registry.domain;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * 如有规则
 * 
 * @author tangmin
 *
 */
public class RouterRule implements Serializable {

	private static final long serialVersionUID = 6862542808793923987L;

	private String method; // 端点（方法）

	private String express; // 表达式

	private String desc; // 规则描述

	private String position; // 对象（立场：consumer、provider）

	private int priority; // 优先级

	private boolean active; // 是否开启（0 - 禁止， 1 - 开启）

	public RouterRule() {
		super();
	}

	public RouterRule(String method, String express, String desc, String position, int priority, boolean active) {
		super();
		this.method = method;
		this.express = express;
		this.desc = desc;
		this.position = position;
		this.priority = priority;
		this.active = active;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getExpress() {
		return express;
	}

	public void setExpress(String express) {
		this.express = express;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
