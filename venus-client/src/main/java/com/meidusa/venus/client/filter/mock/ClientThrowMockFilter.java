package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异常放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientThrowMockFilter extends BaseMockFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ClientThrowMockFilter.class);

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        if(!isEnableThrowMock(invocation, url)){
            return null;
        }
        //获取mock exception
        Exception exception = getMockException(invocation, url);
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
    Exception getMockException(Invocation invocation, URL url){
        Exception exception = new RpcException("500 error");
        return exception;
    }

    /**
     * 判断是否开启throw放通
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableThrowMock(Invocation invocation, URL url){
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
}
