package com.athena.venus.domain;

import java.io.Serializable;
import java.util.Date;


/**
 * 方法调用明细
 * 
 * @author longhaisheng
 *
 */
public class VenusMethodCallDetailDO implements Serializable {

	private static final long serialVersionUID = -3255239904290423391L;

	/**  */
	private String id;

	/** 提供方应用名 */
	private String providerDomain="";

	/** 接口名 */
	private String interfaceName="";

	/** 服务名 */
	private String serviceName;

	/** 服务版本号 */
	private String version="";

	/** 方法名 */
	private String methodName;

	/** 提供方IP */
	private String providerIp;

	/** 消费方应用名 */
	private String consumerDomain="";

	/** 消费方IP */
	private String consumerIp;

	/** 来源 1:client,2:bus,3:server */
	private Integer sourceType;

	/** venus rpcId */
	private String rpcId;

	/** athena traceId */
	private String traceId;
	
	/** athena messageId */
	private String messageId;

	/** 调用状态:1成功，0失败 */
	private Integer status;

	/** 请求时间 */
	private Date requestTime;

	/** 响应时间 */
	private Date responseTime;

	/** 耗时 ：毫秒 */
	private Integer durationMillisecond;

	/** 输入大小 byte */
	private Integer inputSize=0;

	/** 输出大小 byte */
	private Integer outputSize=0;

	/** 错误类型 */
	private String errorType="0";

	/** 错误详细信息 */
	private String errorInfo;

	/** 请求参数 */
	private String requestJson;

	/** 返回值 */
	private String reponseJson;
	
	/** 同一rpcId是否是第一次请求  */
	private boolean first = true;
	
	/** 同一rpcId请的第几次  */
	private int rpcIdNum =1;

	public String getProviderDomain() {
		return providerDomain;
	}

	public void setProviderDomain(String providerDomain) {
		this.providerDomain = providerDomain;
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

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getProviderIp() {
		return providerIp;
	}

	public void setProviderIp(String providerIp) {
		this.providerIp = providerIp;
	}

	public String getConsumerDomain() {
		return consumerDomain;
	}

	public void setConsumerDomain(String consumerDomain) {
		this.consumerDomain = consumerDomain;
	}

	public String getConsumerIp() {
		return consumerIp;
	}

	public void setConsumerIp(String consumerIp) {
		this.consumerIp = consumerIp;
	}

	public String getRpcId() {
		return rpcId;
	}

	public void setRpcId(String rpcId) {
		this.rpcId = rpcId;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Date getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(Date requestTime) {
		this.requestTime = requestTime;
	}

	public Date getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(Date responseTime) {
		this.responseTime = responseTime;
	}

	public Integer getDurationMillisecond() {
		return durationMillisecond;
	}

	public void setDurationMillisecond(Integer durationMillisecond) {
		this.durationMillisecond = durationMillisecond;
	}

	public Integer getInputSize() {
		return inputSize;
	}

	public void setInputSize(Integer inputSize) {
		this.inputSize = inputSize;
	}

	public Integer getOutputSize() {
		return outputSize;
	}

	public void setOutputSize(Integer outputSize) {
		this.outputSize = outputSize;
	}

	public String getErrorType() {
		return errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

	public String getErrorInfo() {
		return errorInfo;
	}

	public void setErrorInfo(String errorInfo) {
		this.errorInfo = errorInfo;
	}

	public String getRequestJson() {
		return requestJson;
	}

	public void setRequestJson(String requestJson) {
		this.requestJson = requestJson;
	}

	public String getReponseJson() {
		return reponseJson;
	}

	public void setReponseJson(String reponseJson) {
		this.reponseJson = reponseJson;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getSourceType() {
		return sourceType;
	}

	public void setSourceType(Integer sourceType) {
		this.sourceType = sourceType;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}


	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public int getRpcIdNum() {
		return rpcIdNum;
	}

	public void setRpcIdNum(int rpcIdNum) {
		this.rpcIdNum = rpcIdNum;
	}

}
