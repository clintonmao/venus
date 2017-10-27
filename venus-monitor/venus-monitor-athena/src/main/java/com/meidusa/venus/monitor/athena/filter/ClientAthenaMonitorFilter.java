package com.meidusa.venus.monitor.athena.filter;

import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.monitor.athena.reporter.AthenaExtensionResolver;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionId;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionDelegate;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;

import java.lang.reflect.Method;

/**
 * client athena监控切面处理
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ClientAthenaMonitorFilter implements Filter {

    static boolean isInited;

    public ClientAthenaMonitorFilter(){
        if(!isInited){
            init();
            isInited = true;
        }
    }

    @Override
    public void init() throws RpcException {
        //初始化athena
        AthenaExtensionResolver.getInstance().resolver();
    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        ServiceWrapper service = clientInvocation.getService();
        EndpointWrapper endpoint = clientInvocation.getEndpoint();
        Method method = clientInvocation.getMethod();
        //athena相关
        if(service != null && service.isAthenaFlag()){
            //athenaId
            String apiName = VenusUtil.getApiName(method,service,endpoint);//VenusAnnotationUtils.getApiname(method, service, endpoint);
            AthenaTransactionId athenaTransactionId = AthenaTransactionDelegate.getDelegate().startClientTransaction(apiName);
            VenusThreadContext.set(VenusThreadContext.ATHENA_TRANSACTION_ID,athenaTransactionId);
            if (athenaTransactionId != null) {
                //保存athena信息到上下文
                if (athenaTransactionId.getRootId() != null) {
                    byte[] athenaId = athenaTransactionId.getRootId().getBytes();
                    clientInvocation.setAthenaId(athenaId);
                }
                if (athenaTransactionId.getParentId() != null) {
                    byte[] parentId = athenaTransactionId.getParentId().getBytes();
                    clientInvocation.setParentId(parentId);
                }
                if (athenaTransactionId.getMessageId() != null) {
                    byte[] messageId = athenaTransactionId.getMessageId().getBytes();
                    clientInvocation.setMessageId(messageId);
                }
            }
        }
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        if (clientInvocation.getService().isAthenaFlag()) {
            //从上下文设置请求、接收报文长度
            Integer clientOutputSize = (Integer) VenusThreadContext.get(VenusThreadContext.CLIENT_OUTPUT_SIZE);
            if(clientOutputSize != null){
                AthenaTransactionDelegate.getDelegate().setClientOutputSize(clientOutputSize.intValue());
            }
            Integer clientInputSize = (Integer) VenusThreadContext.get(VenusThreadContext.CLIENT_INPUT_SIZE);
            if(clientInputSize != null){
                AthenaTransactionDelegate.getDelegate().setClientInputSize(clientInputSize.intValue());
            }
            //提交事务
            AthenaTransactionDelegate.getDelegate().completeClientTransaction();
        }
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }
}
