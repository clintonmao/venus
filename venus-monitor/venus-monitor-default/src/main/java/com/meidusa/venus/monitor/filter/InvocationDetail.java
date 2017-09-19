package com.meidusa.venus.monitor.filter;

import com.meidusa.venus.ClientInvocation;
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

    //上报数据来源定义
    public static final int FROM_CLIENT = 0;
    public static final int FROM_BUS = 1;
    public static final int FROM_SERVER = 2;

    /**
     * 请求来源
     */
    int from;

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

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
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
