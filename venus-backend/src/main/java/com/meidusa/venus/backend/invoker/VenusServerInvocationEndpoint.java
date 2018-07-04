package com.meidusa.venus.backend.invoker;

import com.meidusa.venus.backend.context.RequestContext;
import com.meidusa.venus.backend.services.EndpointItem;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.services.Interceptor;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

/**
 * 服务端调用委托处理类，调用本地stub
 * @author Struct
 */
public class VenusServerInvocationEndpoint implements EndpointInvocation {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    /**
     * 拦截器列表
     */
    protected Iterator<Interceptor> interceptors;

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
    private EndpointItem endpoint;

    /**
     * 请求上下文
     */
    private RequestContext context;

    /**
     * 返回类型
     */
    private ResultType type = ResultType.RESPONSE;

    public VenusServerInvocationEndpoint(RequestContext context, EndpointItem endpoint) {
        this.endpoint = endpoint;
        this.context = context;
        if (CollectionUtils.isNotEmpty(endpoint.getInterceptorList())) {
            this.interceptors = endpoint.getInterceptorList().iterator();
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
    public EndpointItem getEndpoint() {
        return endpoint;
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public Object invoke() {
        if (interceptors != null && interceptors.hasNext()) {
            Interceptor interceptor = interceptors.next();
            result = interceptor.intercept(VenusServerInvocationEndpoint.this);
        } else {
            try {
                Object[] parameters = getContext().getEndPointer().getParameterValues(getContext().getParameters());

                EndpointItem ep = this.getEndpoint();
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
            } catch (IllegalAccessException e) {
                throw new RpcException(e);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() != null) {
                    throw new RpcException(e.getTargetException());
                } else {
                    throw new RpcException(e);
                }
            }
        }
        return result;
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

}
