package com.meidusa.venus.backend.invoker;

import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.services.InterceptorMapping;
import com.meidusa.venus.backend.services.RequestContext;
import com.meidusa.venus.backend.support.UtilTimerStack;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.notify.InvocationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

/**
 * 服务端调用委托处理类，调用本地stub
 * @author Struct
 */
public class VenusServerInvocationEndpoint implements EndpointInvocation {

    private static Logger logger = LoggerFactory.getLogger(VenusServerInvocationEndpoint.class);

    private static String ENDPOINT_INVOKED = "handleRequest endpoint: ";
    
    /**
     * 拦截器列表
     */
    protected Iterator<InterceptorMapping> interceptors;

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

    public VenusServerInvocationEndpoint(RequestContext context, Endpoint endpoint) {
        this.endpoint = endpoint;
        if (endpoint.getInterceptorStack() != null) {
            interceptors = endpoint.getInterceptorStack().getInterceptors().iterator();
        }
        this.context = context;
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

        Endpoint ep = this.getEndpoint();

        if (interceptors != null && interceptors.hasNext()) {
            final InterceptorMapping interceptor = interceptors.next();
            String interceptorMsg = "filte: " + interceptor.getName();
            UtilTimerStack.push(interceptorMsg);
            try {
                result = interceptor.getInterceptor().intercept(VenusServerInvocationEndpoint.this);
            } finally {
                UtilTimerStack.pop(interceptorMsg);
            }
        } else {
            try {
                UtilTimerStack.push(ENDPOINT_INVOKED);
                Object[] parameters = getContext().getEndPointer().getParameterValues(getContext().getParameters());

                if (ep.isAsync()) {
                    this.type = ResultType.NONE;
                }

                for (Object object : parameters) {
                    if (object instanceof InvocationListener) {
                        this.type = ResultType.NOTIFY;
                    }
                }
                result = ep.getMethod().invoke(ep.getService().getInstance(), parameters);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (InvocationTargetException e) {
                if (e.getTargetException() != null) {
                    throw new RpcException(e.getTargetException());
                } else {
                    throw new RpcException(e);
                }
            } catch (IllegalAccessException e) {
                throw new RpcException(e);
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
