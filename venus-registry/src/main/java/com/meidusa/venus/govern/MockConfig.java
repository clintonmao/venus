package com.meidusa.venus.govern;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * 降级配置
 * @author tangmin
 *
 */
public class MockConfig {
	
	private String method; //端点（方法）
	
	private String position ; //对象（立场：consumer、provider）
	
	private String mode; //降级方式（return、throw、callback）
	
	private String policy; //处理策略
	
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
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getPolicy() {
		return policy;
	}
	public void setPolicy(String policy) {
		this.policy = policy;
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
	public MockConfig() {
		super();
	}
	public MockConfig(String method, String position, String mode,
			String policy, boolean active) {
		super();
		this.method = method;
		this.position = position;
		this.mode = mode;
		this.policy = policy;
		this.active = active;
	}
	
}
