package com.meidusa.venus.client.invoker.injvm;

import com.meidusa.venus.rpc.Invocation;
import com.meidusa.venus.rpc.Result;
import com.meidusa.venus.rpc.RpcException;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.rpc.Invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * injvm协议调用invoker，jvm内部调用
 * Created by Zhangzhihua on 2017/8/1.
 */
public class InjvmInvoker implements Invoker {

    private Map<String, Object> singletonServiceMap = new HashMap<String, Object>();

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        Method method = invocation.getMethod();
        Object[] args = invocation.getArgs();
        Service service = invocation.getService();
        Endpoint endpoint = invocation.getEndpoint();

        try {
            Object serviceImpl = null;
            if (service.singleton()) {
                serviceImpl = singletonServiceMap.get(service.implement());
                if (serviceImpl == null) {
                    synchronized (singletonServiceMap) {
                        serviceImpl = singletonServiceMap.get(service.implement());
                        if (serviceImpl == null) {
                            serviceImpl = Class.forName(service.implement(), true, Thread.currentThread().getContextClassLoader()).newInstance();
                            singletonServiceMap.put(service.implement(), serviceImpl);
                        }
                    }
                }
            } else {
                serviceImpl = Class.forName(service.implement(), true, Thread.currentThread().getContextClassLoader()).newInstance();
            }

            return new Result(method.invoke(serviceImpl, args));
        } catch (InstantiationException e) {
            throw new RpcException(e);
        } catch (IllegalAccessException e) {
            throw new RpcException(e);
        } catch (ClassNotFoundException e) {
            throw new RpcException(e);
        } catch (InvocationTargetException e) {
            throw new RpcException(e);
        }
    }

    @Override
    public void destroy() throws RpcException {
    }
}
