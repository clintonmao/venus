package com.meidusa.venus.monitor.athena.filter;

import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.monitor.athena.reporter.AthenaExtensionResolver;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionId;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionDelegate;
import com.meidusa.venus.util.VenusAnnotationUtils;

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
        Service service = invocation.getService();
        Endpoint endpoint = invocation.getEndpoint();
        Method method = invocation.getMethod();
        //athena相关
        if(service != null && service.athenaFlag()){
            //athenaId
            String apiName = VenusAnnotationUtils.getApiname(method, service, endpoint);
            AthenaTransactionId athenaTransactionId = AthenaTransactionDelegate.getDelegate().startClientTransaction(apiName);
            VenusThreadContext.set(VenusThreadContext.ATHENA_TRANSACTION_ID,athenaTransactionId);
            if (athenaTransactionId != null) {
                //保存athena信息到上下文
                if (athenaTransactionId.getRootId() != null) {
                    byte[] athenaId = athenaTransactionId.getRootId().getBytes();
                    invocation.setAthenaId(athenaId);
                }
                if (athenaTransactionId.getParentId() != null) {
                    byte[] parentId = athenaTransactionId.getParentId().getBytes();
                    invocation.setParentId(parentId);
                }
                if (athenaTransactionId.getMessageId() != null) {
                    byte[] messageId = athenaTransactionId.getMessageId().getBytes();
                    invocation.setMessageId(messageId);
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
        if (invocation.getService().athenaFlag()) {
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
