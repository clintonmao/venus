package com.meidusa.venus.backend.invoker.sync;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.meidusa.venus.backend.services.InvocationObserver;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.services.RequestContext;
import com.meidusa.venus.backend.services.InterceptorMapping;
import com.meidusa.venus.backend.support.UtilTimerStack;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.exception.ServiceInvokeException;
import com.meidusa.venus.notify.InvocationListener;

/**
 * 
 * @author Struct
 * TODO 统一invoker接口定义
 */
public class VenusEndpointInvocation implements EndpointInvocation {

    private static String ENDPOINT_INVOKED = "handleRequest endpoint: ";
    
    /**
     * 拦截器列表
     */
    protected Iterator<InterceptorMapping> interceptors;

    /**
     * 调用前后的 InvocationObserver 列表, endpoint 调用前后会执行相应的方法
     */
    private List<InvocationObserver> observerList = new ArrayList<InvocationObserver>();

    /**
     * 是否已经执行
     */
    private boolean executed;

    /**
     * 执行结果
     */
    private Object result;

    /**
     * 相关的endpoint
     */
    private Endpoint endpoint;

    /**
     * 请求上下文
     */
    private RequestContext context;

    /**
     * 返回类型
     */
    private ResultType type = ResultType.RESPONSE;

    public VenusEndpointInvocation(RequestContext context, Endpoint endpoint) {
        this.endpoint = endpoint;
        if (endpoint.getInterceptorStack() != null) {
            interceptors = endpoint.getInterceptorStack().getInterceptors().iterator();
        }
        this.context = context;
    }

    public void addObserver(InvocationObserver observer) {
        if (!observerList.contains(observer)) {
            observerList.add(observer);
        }
    }

    @Override
    public RequestContext getContext() {
        return context;
    }

    public ResultType getType() {
        return type;
    }

    @Override
    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public Object invoke() {
        if (executed) {
            throw new IllegalStateException("Request has already executed");
        }

        if (interceptors != null && interceptors.hasNext()) {
            final InterceptorMapping interceptor = interceptors.next();
            String interceptorMsg = "interceptor: " + interceptor.getName();
            UtilTimerStack.push(interceptorMsg);
            try {
                result = interceptor.getInterceptor().intercept(VenusEndpointInvocation.this);
            } finally {
                UtilTimerStack.pop(interceptorMsg);
            }
        } else {
            try {
                UtilTimerStack.push(ENDPOINT_INVOKED);
                Object[] parameters = getContext().getEndPointer().getParameterValues(getContext().getParameters());

                if (this.getEndpoint().isAsync()) {
                    this.type = ResultType.NONE;
                }

                for (Object object : parameters) {
                    if (object instanceof InvocationListener) {
                        this.type = ResultType.NOTIFY;
                    }
                }
                for (InvocationObserver observer : observerList) {
                    observer.beforeInvoke(this, getContext());
                }
                Object instance = this.getEndpoint().getService().getInstance();
                result = getEndpoint().getMethod().invoke(instance, parameters);
                for (InvocationObserver observer : observerList) {
                    observer.afterInvoke(this, getContext());
                }
            } catch (IllegalArgumentException e) {
                throw new ServiceInvokeException(e);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() != null) {
                    throw new ServiceInvokeException(e.getTargetException());
                } else {
                    throw new ServiceInvokeException(e);
                }
            } catch (IllegalAccessException e) {
                throw new ServiceInvokeException(e);
            }finally {
                UtilTimerStack.pop(ENDPOINT_INVOKED);
            }
        }

        if (!executed) {
            executed = true;
        }
        return result;
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

}
