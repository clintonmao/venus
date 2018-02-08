package com.meidusa.venus.monitor.athena.filter;

import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.monitor.athena.AthenaTransactionId;
import com.meidusa.venus.monitor.athena.reporter.ClientTransactionReporter;
import com.meidusa.venus.monitor.athena.reporter.impl.DefaultClientTransactionReporter;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * client athena监控切面处理
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ClientAthenaMonitorFilter implements Filter {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private ClientTransactionReporter clientTransactionReporter = null;

    public ClientAthenaMonitorFilter(){
    }

    @Override
    public void init() throws RpcException {
        clientTransactionReporter = new DefaultClientTransactionReporter();
    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        try {
            ClientInvocation clientInvocation = (ClientInvocation)invocation;
            ServiceWrapper service = clientInvocation.getService();
            if (service == null || !service.isAthenaFlag()) {
                return null;
            }

            EndpointWrapper endpoint = clientInvocation.getEndpoint();
            Method method = clientInvocation.getMethod();
            //athena相关
            String apiName = VenusUtil.getApiName(method,service,endpoint);
            AthenaTransactionId athenaTransactionId = clientTransactionReporter.startTransaction(apiName);
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
            return null;
        }catch(Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ClientAthenaMonitorFilter.beforeInvoke error.",e);
            }
            return null;
        }
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        try {
            ClientInvocation clientInvocation = (ClientInvocation)invocation;
            ServiceWrapper service = clientInvocation.getService();
            if (service == null || !service.isAthenaFlag()) {
                return null;
            }

            //从上下文设置请求、接收报文长度
            Integer clientOutputSize = (Integer) VenusThreadContext.get(VenusThreadContext.CLIENT_OUTPUT_SIZE);
            if(clientOutputSize != null){
                clientTransactionReporter.setOutputSize(clientOutputSize.intValue());
            }
            Integer clientInputSize = (Integer) VenusThreadContext.get(VenusThreadContext.CLIENT_INPUT_SIZE);
            if(clientInputSize != null){
                clientTransactionReporter.setInputSize(clientInputSize.intValue());
            }
            //提交事务
            clientTransactionReporter.commit();
            return null;
        }catch(Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ClientAthenaMonitorFilter.afterInvoke error.",e);
            }
            return null;
        }
    }

    @Override
    public void destroy() throws RpcException {

    }
}
