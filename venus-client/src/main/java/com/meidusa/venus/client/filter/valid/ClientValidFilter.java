package com.meidusa.venus.client.filter.valid;

import com.meidusa.venus.*;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 校验处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientValidFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ClientValidFilter.class);

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        //endpoint定义校验
        if(clientInvocation.getEndpoint() == null){
            Method method = clientInvocation.getMethod();
            if (method != null && !method.getDeclaringClass().equals(Object.class)) {
                logger.error("remote invoke error: endpoint annotation not declare on method=" + method.getName());
                throw new RpcException("remote invoke error: endpoint annotation not declare on method=" + method.getName());
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
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }
}
