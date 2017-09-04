package com.meidusa.venus.monitor.filter;

import com.meidusa.venus.*;
import com.meidusa.venus.monitor.filter.support.InvocationDetail;

import java.util.Date;

/**
 * client监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ClientMonitorFilter extends BaseMonitorFilter implements Filter {

    public ClientMonitorFilter(){
        super();
    }

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        //异常信息
        if(e != null){
            VenusThreadContext.set(VenusThreadContext.RESPONSE_EXCEPTION,e);
        }
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        //请求url 上下文获取
        url = (URL)VenusThreadContext.get(VenusThreadContext.REQUEST_URL);
        //响应时间
        Date responseTime = new Date();
        //响应结果 上下文获取
        Result result = (Result) VenusThreadContext.get(VenusThreadContext.RESPONSE_RESULT);
        //响应异常 上下文获取
        Throwable e = (Throwable)VenusThreadContext.get(VenusThreadContext.RESPONSE_EXCEPTION);

        //组装并添加到明细队列
        InvocationDetail invocationDetail = new InvocationDetail();
        invocationDetail.setInvocation(invocation);
        invocationDetail.setUrl(url);
        invocationDetail.setResponseTime(responseTime);
        invocationDetail.setResult(result);
        invocationDetail.setException(e);
        super.pubInvocationDetailQueue(invocationDetail);
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }
}
