package com.meidusa.venus.support;

import com.meidusa.toolkit.common.util.StringUtil;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.URL;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * venus util
 * Created by Zhangzhihua on 2017/9/21.
 */
public class VenusUtil {

    private static Logger logger = LoggerFactory.getLogger(VenusUtil.class);

    /**
     * 获取方法路径
     * @param invocation
     * @param url
     * @return
     */
    public static String getMethodPath(Invocation invocation, URL url){
        String methodPath = String.format(
                "%s/%s?version=%s&method=%s",
                invocation.getServiceInterfaceName(),
                invocation.getServiceName(),
                invocation.getVersion(),
                invocation.getMethodName()
        );
        if(logger.isDebugEnabled()){
            logger.debug("methodPath:{}.", methodPath);
        }
        return methodPath;
    }

    /**
     * 根据方法定义信息获取api名称
     * @param method
     * @param service
     * @param endpoint
     * @return
     */
    public static String getApiName(Method method, ServiceWrapper service, EndpointWrapper endpoint) {
        String serviceName = null;
        if (service == null || StringUtil.isEmpty(service.getName())) {
            serviceName = method.getDeclaringClass().getCanonicalName();
        } else {
            serviceName = service.getName();
        }

        String methodName = method.getName();
        if (endpoint == null || StringUtil.isEmpty(endpoint.getName())) {
            methodName = method.getName();
        } else {
            methodName = endpoint.getName();
        }

        return serviceName + "." + methodName;
    }

}
