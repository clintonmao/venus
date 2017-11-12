package com.meidusa.venus.backend.filter.mock;

import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * server callback mock filter
 * Created by Zhangzhihua on 2017/8/30.
 */
public class ServerCallbackMockFilter implements Filter {

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
            ServerInvocation clientInvocation = (ServerInvocation)invocation;
            if(!isEnableCallbackMock(clientInvocation, url)){
                return null;
            }
            //获取mock callback TODO 注入instance及动态构造method并传参
            Method callbackMethod = null;
            Object instance = null;
            Object[] args = null;

            if(callbackMethod != null){
                Object retur = callbackMethod.invoke(instance,args);
                return new Result(retur);
            }else{
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new RpcException(e);
        } catch (InvocationTargetException e) {
            throw new RpcException(e);
        }catch(Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ServerCallbackMockFilter.beforeInvoke error.",e);
            }
            return null;
        }
    }

    /**
     * 判断是否开启callback放通
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableCallbackMock(ServerInvocation invocation, URL url){
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
