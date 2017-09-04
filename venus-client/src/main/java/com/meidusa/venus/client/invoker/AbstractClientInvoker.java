package com.meidusa.venus.client.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.util.VenusAnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 抽象invoker
 * Created by Zhangzhihua on 2017/8/2.
 */
public abstract class AbstractClientInvoker implements Invoker {

    private static Logger logger = LoggerFactory.getLogger(AbstractClientInvoker.class);

    private static Logger exceptionLogger = LoggerFactory.getLogger("venus.client.exception");

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        VenusThreadContext.set(VenusThreadContext.REQUEST_URL,url);
        Method method = invocation.getMethod();
        Service service = invocation.getService();
        Endpoint endpoint = invocation.getEndpoint();

        try {
            //初始化
            init();

            //调用相应协议实现
            Result result = doInvoke(invocation, url);
            VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
            return result;
        } catch (Throwable e) {
            if (!(e instanceof CodedException)) {
                if (exceptionLogger.isInfoEnabled()) {
                    exceptionLogger.info("invoke service error,api=" + VenusAnnotationUtils.getApiname(method, service, endpoint), e);
                }
            } else {
                if (exceptionLogger.isDebugEnabled()) {
                    exceptionLogger.debug("invoke service error,api=" + VenusAnnotationUtils.getApiname(method, service, endpoint), e);
                }
            }
            throw new RpcException(e);
        }
    }

    /**
     * 协议服务调用实现
     * @param invocation
     * @param url
     * @return
     * @throws RpcException
     */
    public abstract Result doInvoke(Invocation invocation, URL url) throws RpcException;
}
