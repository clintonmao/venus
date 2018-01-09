package com.meidusa.venus.backend.invoker;

import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.*;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.context.RequestContext;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.backend.support.UtilTimerStack;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.support.VenusStatus;
import com.meidusa.venus.util.ThreadLocalConstant;
import com.meidusa.venus.util.ThreadLocalMap;
import com.meidusa.venus.util.VenusTracerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * venus服务调用
 * Created by Zhangzhihua on 2017/8/2.
 */
public class VenusServerInvoker implements Invoker {

    private static Logger logger = LoggerFactory.getLogger(VenusServerInvoker.class);

    private static String ENDPOINT_INVOKED_TIME = "invoked Total Time: ";

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

    private ServiceManager serviceManager;

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        ServerInvocation serverInvocation = (ServerInvocation)invocation;
        //获取调用信息
        Endpoint endpointDef = serverInvocation.getEndpointDef();
        //构造请求上下文信息
        RequestContext requestContext = serverInvocation.getRequestContext();

        try {
            //调用服务实例
            UtilTimerStack.push(ENDPOINT_INVOKED_TIME);

            VenusServerInvocationEndpoint invocationEndpoint = new VenusServerInvocationEndpoint(requestContext, endpointDef);
            Object result = invocationEndpoint.invoke();
            return new Result(result);
        } catch (Exception e) {
            throw e;
        } catch (Error e) {
            if(e instanceof OutOfMemoryError){
                VenusStatus.getInstance().setStatus(PacketConstant.VENUS_STATUS_OUT_OF_MEMORY);
            }
            throw e;
        } finally {
            ThreadLocalMap.remove(ThreadLocalConstant.REQUEST_CONTEXT);
            ThreadLocalMap.remove(VenusTracerUtil.REQUEST_TRACE_ID);

            UtilTimerStack.pop(ENDPOINT_INVOKED_TIME);
        }
    }

    @Override
    public void destroy() throws RpcException {
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

}
