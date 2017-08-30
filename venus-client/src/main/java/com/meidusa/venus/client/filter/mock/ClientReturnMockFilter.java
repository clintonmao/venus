package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 快速返回放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientReturnMockFilter extends BaseMockFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ClientReturnMockFilter.class);

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        if(!isEnableReturnMock(invocation, url)){
            return null;
        }
        //获取mock返回值
        Object retru = getMockReturn(invocation, url);
        //TODO 校验return
        return new Result(retru);
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url) throws RpcException {
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
    Object getMockReturn(Invocation invocation, URL url){
        return "ok";
    }

    /**
     * 判断是否开启return放通
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableReturnMock(Invocation invocation, URL url){
        String mockType = getMockType(invocation, url);
        return MOCK_TYPE_RETURN.equalsIgnoreCase(mockType);
    }

    @Override
    public void destroy() throws RpcException {

    }
}
