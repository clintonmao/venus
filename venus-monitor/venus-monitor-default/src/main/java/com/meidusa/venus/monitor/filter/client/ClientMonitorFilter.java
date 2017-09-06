package com.meidusa.venus.monitor.filter.client;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.*;
import com.meidusa.venus.monitor.filter.BaseMonitorFilter;

import java.util.Date;

/**
 * client监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ClientMonitorFilter extends BaseMonitorFilter implements Filter {

    public ClientMonitorFilter(){
    }

    public ClientMonitorFilter(AthenaDataService athenaDataService){
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
        //异常信息
        if(e != null){
            VenusThreadContext.set(VenusThreadContext.RESPONSE_EXCEPTION,e);
        }
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        //athenaId
        String athenaId = (String)VenusThreadContext.get(VenusThreadContext.ATHENA_ROOT_ID);
        invocation.setAthenaId(athenaId);
        //请求url
        url = (URL)VenusThreadContext.get(VenusThreadContext.REQUEST_URL);
        //响应结果
        Result result = (Result) VenusThreadContext.get(VenusThreadContext.RESPONSE_RESULT);
        //响应异常
        Throwable e = (Throwable)VenusThreadContext.get(VenusThreadContext.RESPONSE_EXCEPTION);

        //组装并添加到明细队列
        ClientInvocationDetail invocationDetail = new ClientInvocationDetail();
        invocationDetail.setFrom(ClientInvocationDetail.FROM_CLIENT);
        invocationDetail.setInvocation(invocation);
        invocationDetail.setUrl(url);
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
