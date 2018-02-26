package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

/**
 * 异常放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientThrowMockFilter implements Filter {

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
            if(!isEnableThrowMock(clientInvocation, url)){
                return null;
            }

            //获取mock exception
            Exception exception = getMockException(clientInvocation, url);
            Result result = new Result();
            result.setException(exception);
            return result;
        } catch (RpcException e) {
            throw e;
        }catch(Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ClientThrowMockFilter.beforeInvoke error.",e);
            }
            return null;
        }
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
        return false;
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
