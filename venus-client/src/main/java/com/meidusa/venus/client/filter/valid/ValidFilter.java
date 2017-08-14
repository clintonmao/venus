package com.meidusa.venus.client.filter.valid;

import com.meidusa.venus.Filter;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 校验处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ValidFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ValidFilter.class);

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
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
