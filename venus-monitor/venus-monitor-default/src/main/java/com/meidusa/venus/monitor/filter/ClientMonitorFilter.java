package com.meidusa.venus.monitor.filter;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.*;
import com.meidusa.venus.monitor.reporter.AbstractMonitorReporter;
import com.meidusa.venus.monitor.reporter.ClientMonitorReporter;
import com.meidusa.venus.monitor.reporter.InvocationDetail;

import java.util.Date;

/**
 * client监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ClientMonitorFilter extends AbstractMonitorFilter implements Filter {

    public ClientMonitorFilter(){
    }

    public ClientMonitorFilter(AthenaDataService athenaDataService){
        this.setAthenaDataService(athenaDataService);
        startProcessAndReporterTread();
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
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        //请求url
        url = (URL)VenusThreadContext.get(VenusThreadContext.REQUEST_URL);
        //响应结果
        Result result = (Result) VenusThreadContext.get(VenusThreadContext.RESPONSE_RESULT);
        //响应异常
        Throwable e = (Throwable)VenusThreadContext.get(VenusThreadContext.RESPONSE_EXCEPTION);

        //组装并添加到明细队列
        InvocationDetail invocationDetail = new InvocationDetail();
        invocationDetail.setFrom(InvocationDetail.FROM_CLIENT);
        invocationDetail.setInvocation(clientInvocation);
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

    AbstractMonitorReporter getMonitorReporte(){
        if(monitorReporteDelegate == null){
            monitorReporteDelegate = new ClientMonitorReporter();
            monitorReporteDelegate.setAthenaDataService(this.getAthenaDataService());
        }
        return monitorReporteDelegate;
    }
}
