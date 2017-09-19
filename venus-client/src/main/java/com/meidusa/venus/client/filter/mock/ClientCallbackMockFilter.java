package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 回调放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientCallbackMockFilter extends BaseMockFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ClientCallbackMockFilter.class);

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        if(!isEnableCallbackMock(clientInvocation, url)){
            return null;
        }
        //获取mock callback TODO 注入instance及动态构造method并传参
        Method callbackMethod = null;
        Object instance = null;
        Object[] args = null;
        try {
            Object retur = callbackMethod.invoke(instance,args);
            return new Result(retur);
        } catch (Exception e) {
            Result result = new Result();
            result.setException(e);
            return result;
        }
    }

    /**
     * 判断是否开启callback放通
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableCallbackMock(ClientInvocation invocation, URL url){
        String mockType = getMockType(invocation, url);
        return MOCK_TYPE_CALLBACK.equalsIgnoreCase(mockType);
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
