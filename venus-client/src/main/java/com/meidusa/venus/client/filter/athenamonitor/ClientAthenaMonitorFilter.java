package com.meidusa.venus.client.filter.athenamonitor;

import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.extension.athena.AthenaTransactionId;
import com.meidusa.venus.extension.athena.delegate.AthenaTransactionDelegate;
import com.meidusa.venus.util.VenusAnnotationUtils;

import java.lang.reflect.Method;

/**
 * client athena监控切面处理
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ClientAthenaMonitorFilter implements Filter {

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        if(invocation.getService().athenaFlag()){
            Method method = invocation.getMethod();
            Service service = invocation.getService();
            Endpoint endpoint = invocation.getEndpoint();

            String apiName = VenusAnnotationUtils.getApiname(method, service, endpoint);

            AthenaTransactionId athenaTransactionId = AthenaTransactionDelegate.getDelegate().startClientTransaction(apiName);
            //保存athenaTransactionId上下文
            VenusContext.set(VenusContext.ATHENA_TRANSACTION_ID,athenaTransactionId);
        }
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        if (invocation.getService().athenaFlag()) {
            //从上下文设置请求、接收报文长度
            Integer clientOutputSize = (Integer) VenusContext.get(VenusContext.CLIENT_OUTPUT_SIZE);
            if(clientOutputSize != null){
                AthenaTransactionDelegate.getDelegate().setClientOutputSize(clientOutputSize.intValue());
            }
            Integer clientInputSize = (Integer) VenusContext.get(VenusContext.CLIENT_INPUT_SIZE);
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
