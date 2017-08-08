package com.meidusa.venus.registry.domain;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.meidusa.venus.govern.FlowControl;
import com.meidusa.venus.govern.MockConfig;
import com.meidusa.venus.govern.RouterRule;

/**
 * 服务配置
 * 
 * @author longhaisheng
 *
 */
public class VenusServiceConfigDO implements Serializable {

	private static final long serialVersionUID = -5737274852416471175L;

	private Integer id;

	private int type;

	private String config;

	private Integer serviceId;

	private String createName;

	private String updateName;

	private Date createTime;

	private Date updateTime;

	// 路由规则
	private RouterRule routerRule;

	// 流控配置
	private FlowControl flowControl;

	// 降级配置
	private MockConfig mockConfig;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public Integer getServiceId() {
		return serviceId;
	}

	public void setServiceId(Integer serviceId) {
		this.serviceId = serviceId;
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

	public RouterRule getRouterRule() {
		return routerRule;
	}

	public void setRouterRule(RouterRule routerRule) {
		this.routerRule = routerRule;
	}

	public FlowControl getFlowControl() {
		return flowControl;
	}

	public void setFlowControl(FlowControl flowControl) {
		this.flowControl = flowControl;
	}

	public MockConfig getMockConfig() {
		return mockConfig;
	}

	public void setMockConfig(MockConfig mockConfig) {
		this.mockConfig = mockConfig;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
