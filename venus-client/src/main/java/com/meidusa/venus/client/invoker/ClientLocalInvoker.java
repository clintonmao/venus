package com.meidusa.venus.client.invoker;

import com.meidusa.toolkit.net.Connection;
import com.meidusa.venus.*;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusThreadContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * injvm协议调用invoker，jvm内部调用
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientLocalInvoker implements Invoker {

    private Map<String, Object> singletonServiceMap = new HashMap<String, Object>();

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        VenusThreadContext.set(VenusThreadContext.REQUEST_URL,url);
        Method method = clientInvocation.getMethod();
        Object[] args = clientInvocation.getArgs();
        ServiceWrapper service = clientInvocation.getService();
        //Endpoint endpoint = invocation.getEndpoint();
        String interfaceName = service.getImplement();

        try {
            Object serviceImpl = null;
            if (service.isSingleton()) {
                serviceImpl = singletonServiceMap.get(interfaceName);
                if (serviceImpl == null) {
                    synchronized (singletonServiceMap) {
                        serviceImpl = singletonServiceMap.get(interfaceName);
                        if (serviceImpl == null) {
                            serviceImpl = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader()).newInstance();
                            singletonServiceMap.put(interfaceName, serviceImpl);
                        }
                    }
                }
            } else {
                serviceImpl = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader()).newInstance();
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
    public void releaseConnection(Connection conn) {
    }

    @Override
    public void destroy() throws RpcException {
    }
}
