package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

/**
 * 快速返回放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientReturnMockFilter implements Filter {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

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
        try {
            ClientInvocation clientInvocation = (ClientInvocation)invocation;
            if(!isEnableReturnMock(clientInvocation, url)){
                return null;
            }

            //获取mock返回值
            Object object = getMockReturn(clientInvocation, url);
            return new Result(object);
        } catch (RpcException e) {
            throw e;
        } catch (Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ClientReturnMockFilter.beforeInvoke error.",e);
            }
            return null;
        }
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
        return false;
    }

    @Override
    public void destroy() throws RpcException {

    }
}
