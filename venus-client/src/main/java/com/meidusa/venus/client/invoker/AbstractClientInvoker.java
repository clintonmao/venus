package com.meidusa.venus.client.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 抽象invoker
 * Created by Zhangzhihua on 2017/8/2.
 */
public abstract class AbstractClientInvoker implements Invoker {

    private static Logger logger = LoggerFactory.getLogger(AbstractClientInvoker.class);

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        VenusThreadContext.set(VenusThreadContext.REQUEST_URL,url);
        init();

        //调用相应协议实现
        Result result = doInvoke(clientInvocation, url);
        return result;
    }

    /**
     * 协议服务调用实现
     * @param invocation
     * @param url
     * @return
     * @throws RpcException
     */
    public abstract Result doInvoke(ClientInvocation invocation, URL url) throws RpcException;
}
