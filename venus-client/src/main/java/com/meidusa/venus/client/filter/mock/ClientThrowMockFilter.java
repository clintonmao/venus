package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异常放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientThrowMockFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ClientThrowMockFilter.class);

    //降级类型-return
    static final String MOCK_TYPE_RETURN = "MOCK_TYPE_RETURN ";
    //降级类型-throw
    static final String MOCK_TYPE_THROW = "MOCK_TYPE_THROW";
    //降级类型-callback
    static final String MOCK_TYPE_CALLBACK = "MOCK_TYPE_CALLBACK";

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        if(!isEnableThrowMock(clientInvocation, url)){
            return null;
        }
        //获取mock exception
        Exception exception = getMockException(clientInvocation, url);
        //TODO 校验exception
        Result result = new Result();
        result.setException(exception);
        return result;
    }

    /**
     * 获取mock异常
     * @param invocation
     * @param url
     * @return
     */
    Exception getMockException(ClientInvocation invocation, URL url){
        Exception exception = new RpcException("500 error");
        return exception;
    }

    /**
     * 判断是否开启throw放通
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableThrowMock(ClientInvocation invocation, URL url){
        String mockType = getMockType(invocation, url);
        return MOCK_TYPE_THROW.equalsIgnoreCase(mockType);
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

    /**
     * 获取降级类型
     * @param invocation
     * @param url
     * @return
     */
    String getMockType(ClientInvocation invocation, URL url){
        //TODO 获取流控类型
        return null;//MOCK_TYPE_RETURN;
    }
}
