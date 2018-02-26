package com.athena.venus.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 服务方法调用统计
 * 
 * @author longhaisheng
 *
 */
public class VenusMethodStaticDO implements Serializable {

	private static final long serialVersionUID = -9162303483604754942L;

	/** 应用名 */
	private String domain;

	/** 接口名 */
	private String interfaceName="";

	/** 服务名 */
	private String serviceName;

	/** 服务版本号 */
	private String version="";

	/** 所在IP */
	private String sourceIp;

	/** 方法名 */
	private String methodName;

	/** 所在时间,精确到分钟 */
	private Date createTime;

	/** 开始时间 */
	private Date startTime;

	/** 结束时间 */
	private Date endTime;
	
	/** 总次数 */
	private Integer totalCount;

	/** 失败次数 */
	private Integer failCount;
	
	/** 慢操作数 */
	private Integer slowCount;

	/** 平均耗时 */
	private Integer avgDuration;

	/** 最大耗时 */
	private Integer maxDuration;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getSourceIp() {
		return sourceIp;
	}

	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Integer getFailCount() {
		return failCount;
	}

	public void setFailCount(Integer failCount) {
		this.failCount = failCount;
	}

	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}

	public Integer getSlowCount() {
		return slowCount;
	}

	public void setSlowCount(Integer slowCount) {
		this.slowCount = slowCount;
	}

	public Integer getAvgDuration() {
		return avgDuration;
	}

	public void setAvgDuration(Integer avgDuration) {
		this.avgDuration = avgDuration;
	}

	public Integer getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(Integer maxDuration) {
		this.maxDuration = maxDuration;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

}
