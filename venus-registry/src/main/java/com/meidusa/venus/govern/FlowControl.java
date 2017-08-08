package com.meidusa.venus.govern;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * 流量控制
 * @author tangmin
 *
 */
public class FlowControl {
	
	private String method; //端点（方法）
	
	private String position ; //对象（立场：consumer、provider）
	
	private String fcType; //流控类型（并发数-CON、TPS）
	
	private int threshold; //流控阈值
	
	private String processGrade; //处理级别(限流-limit、告警-alarm)
	
	private boolean active; //是否开启（0 - 禁止， 1 - 开启）
	
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public String getFcType() {
		return fcType;
	}
	public void setFcType(String fcType) {
		this.fcType = fcType;
	}
	public int getThreshold() {
		return threshold;
	}
	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}
	public String getProcessGrade() {
		return processGrade;
	}
	public void setProcessGrade(String processGrade) {
		this.processGrade = processGrade;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	public FlowControl() {
		super();
	}
	public FlowControl(String method, String position, String fcType,
			int threshold, String processGrade, boolean active) {
		super();
		this.method = method;
		this.position = position;
		this.fcType = fcType;
		this.threshold = threshold;
		this.processGrade = processGrade;
		this.active = active;
	}
	
}
