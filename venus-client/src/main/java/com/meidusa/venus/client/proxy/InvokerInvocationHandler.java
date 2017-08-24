package com.meidusa.venus.client.proxy;

import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.annotations.util.AnnotationUtil;
import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.cluster.FailoverClusterInvoker;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.client.filter.athenamonitor.ClientAthenaMonitorFilter;
import com.meidusa.venus.client.filter.limit.ClientActivesLimitFilter;
import com.meidusa.venus.client.filter.limit.ClientTpsLimitFilter;
import com.meidusa.venus.client.filter.mock.ClientMockFilterProxy;
import com.meidusa.venus.client.filter.valid.ClientValidFilter;
import com.meidusa.venus.client.invoker.ClientInvokerProxy;
import com.meidusa.venus.client.invoker.injvm.InjvmInvoker;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.metainfo.EndpointParameterUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterService;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.service.registry.HostPort;
import com.meidusa.venus.service.registry.ServiceDefinition;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 客户端服务调用代理
 * @author Struct
 */

public class InvokerInvocationHandler implements InvocationHandler {

    private static Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);

    /**
     * 服务接口类型
     */
    private Class<?> serviceType;

    /**
     * 异常处理
     */
    private VenusExceptionFactory venusExceptionFactory;

    /**
     * 认证配置
     */
    private DummyAuthenticator authenticator;

    private RemoteConfig remoteConfig;

    /**
     * 注册中心地址
     */
    private String registerUrl = "192.168.1.1:9000";

    private ClientInvokerProxy clientInvokerProxy;

    public InvokerInvocationHandler(){
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            //构造请求
            Invocation invocation = buildInvocation(proxy, method, args);

            //通过代理调用服务
            Result result = clientInvokerProxy.invoke(invocation,null);

            if(result.getErrorCode() == 0){//调用成功
                return result.getResult();
            }else{//调用失败
                //TODO 细化异常信息
                throw new RpcException(String.format("%s-%s",String.valueOf(result.getErrorCode()),result.getErrorMessage()));
            }
        } catch (Throwable e) {
            logger.error("invoke error.",e);
            throw e;
        }
    }

    /**
     * 构造请求
     * @param proxy
     * @param method
     * @param args
     * @return
     */
    Invocation buildInvocation(Object proxy, Method method, Object[] args){
        Invocation invocation = new Invocation();
        invocation.setServiceType(serviceType);
        invocation.setMethod(method);
        invocation.setArgs(args);
        //invocation.setRemoteConfig(remoteConfig);
        Endpoint endpoint =  AnnotationUtil.getAnnotation(method.getAnnotations(), Endpoint.class);
        invocation.setEndpoint(endpoint);
        if (endpoint != null) {
            Service service = AnnotationUtil.getAnnotation(method.getDeclaringClass().getAnnotations(), Service.class);
            invocation.setService(service);
            if (service != null && com.meidusa.toolkit.common.util.StringUtil.isEmpty(service.implement())) {
                EndpointParameter[] params = EndpointParameterUtil.getPrameters(method);
                invocation.setParams(params);
            }
        }

        //设置traceId
        byte[] traceID = VenusTracerUtil.getTracerID();
        if (traceID == null) {
            traceID = VenusTracerUtil.randomTracerID();
        }
        //设置调用方式
        boolean async = false;
        if (endpoint.async()) {
            async = true;
        }
        return invocation;
    }

    public Class<?> getServiceType() {
        return serviceType;
    }

    public void setServiceType(Class<?> serviceType) {
        this.serviceType = serviceType;
    }

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }

    public DummyAuthenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(DummyAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    public String getRegisterUrl() {
        return registerUrl;
    }

    public void setRegisterUrl(String registerUrl) {
        this.registerUrl = registerUrl;
    }

}
