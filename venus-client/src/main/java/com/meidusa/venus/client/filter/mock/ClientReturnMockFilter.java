package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 快速返回放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientReturnMockFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ClientReturnMockFilter.class);

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
        if(!isEnableReturnMock(clientInvocation, url)){
            return null;
        }
        //获取mock返回值
        Object retru = getMockReturn(clientInvocation, url);
        //TODO 校验return
        return new Result(retru);
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    /**
     * 获取mock return值
     * @param invocation
     * @param url
     * @return
     */
    Object getMockReturn(ClientInvocation invocation, URL url){
        return "ok";
    }

    /**
     * 判断是否开启return放通
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableReturnMock(ClientInvocation invocation, URL url){
        String mockType = getMockType(invocation, url);
        return MOCK_TYPE_RETURN.equalsIgnoreCase(mockType);
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

    @Override
    public void destroy() throws RpcException {

    }
}
