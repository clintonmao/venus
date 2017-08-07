package com.meidusa.venus.client.interceptor.valid;

import com.meidusa.venus.rpc.Interceptor;
import com.meidusa.venus.rpc.Invocation;
import com.meidusa.venus.rpc.Result;
import com.meidusa.venus.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 校验处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ValidInterceptor implements Interceptor {

    private static Logger logger = LoggerFactory.getLogger(ValidInterceptor.class);

    @Override
    public Result intercept(Invocation invocation) throws RpcException {
        //endpoint定义校验
        if(invocation.getEndpoint() == null){
            Method method = invocation.getMethod();
            if (!method.getDeclaringClass().equals(Object.class)) {
                logger.error("remote invoke error: endpoint annotation not declare on method=" + method.getName());
                throw new RpcException("remote invoke error: endpoint annotation not declare on method=" + method.getName());
            }
        }

        //TODO 其它校验
        return null;
    }
}
