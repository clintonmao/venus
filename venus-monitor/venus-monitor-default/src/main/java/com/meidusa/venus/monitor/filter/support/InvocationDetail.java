package com.meidusa.venus.monitor.filter.support;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;

import java.util.Date;

/**
 *
 * 调用明细信息
 * Created by Zhangzhihua on 2017/9/4.
 */
public class InvocationDetail {

    /**
     * 请求id
     */
    String id;

    /**
     * 请求rpcId
     */
    String rpcId;

    /**
     * 请求traceId
     */
    String traceId;

    /**
     * 请求来源
     */
    int from;

    /**
     * 请求时间
     */
    Date requestTime;

    /**
     * 请求对象
     */
    Invocation invocation;

    /**
     * 请求Url
     */
    URL url;

    /**
     * 响应时间
     */
    Date responseTime;

    /**
     * 响应结果
     */
    Result result;

    /**
     * 响应异常
     */
    Throwable exception;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public Date getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }

    public Invocation getInvocation() {
        return invocation;
    }

    public void setInvocation(Invocation invocation) {
        this.invocation = invocation;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Date getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Date responseTime) {
        this.responseTime = responseTime;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}
