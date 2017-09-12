package com.meidusa.venus.monitor.filter;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.*;
import com.meidusa.venus.monitor.filter.BaseMonitorFilter;
import com.meidusa.venus.monitor.filter.InvocationDetail;

import java.util.Date;

/**
 * server监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ServerMonitorFilter extends BaseMonitorFilter implements Filter{

    public ServerMonitorFilter(AthenaDataService athenaDataService){
        this.setAthenaDataService(athenaDataService);
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
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        Result result = (Result) VenusThreadContext.get(VenusThreadContext.RESPONSE_RESULT);
        Throwable e = (Throwable)VenusThreadContext.get(VenusThreadContext.RESPONSE_EXCEPTION);

        InvocationDetail invocationDetail = new InvocationDetail();
        invocationDetail.setFrom(InvocationDetail.FROM_SERVER);
        invocationDetail.setInvocation(invocation);
        invocationDetail.setResponseTime(new Date());
        invocationDetail.setResult(result);
        invocationDetail.setException(e);

        pubInvocationDetailQueue(invocationDetail);
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }
}
