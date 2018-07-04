package com.meidusa.venus.backend.services;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.backend.context.RequestContext;
import com.meidusa.venus.backend.services.xml.config.ExportMethod;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.TypeHandler;
import org.apache.commons.lang.ArrayUtils;

import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.exception.ConvertException;
import com.meidusa.venus.exception.EndPointNotFoundException;
import com.meidusa.venus.exception.ServiceNotFoundException;
import com.meidusa.venus.exception.SystemParameterRequiredException;
import com.meidusa.venus.exception.ServiceDefinitionException;
import com.meidusa.venus.util.Utils;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Struct
 */
public abstract class AbstractServiceManager implements ServiceManager {

    private boolean supportOverload = false;

    protected final Map<String, ServiceObject> serviceMap = new HashMap<String, ServiceObject>();

    @Override
    public ServiceObject getService(String serviceName) throws ServiceNotFoundException {
        if (serviceName == null) {
            throw new ServiceNotFoundException("Cannot find service with null");
        }
        if (!serviceMap.containsKey(serviceName)) {
            throw new ServiceNotFoundException("No service named " + serviceName);
        }

        return serviceMap.get(serviceName);
    }

    @Override
    public EndpointItem getEndpoint(String apiName) throws ServiceNotFoundException, EndPointNotFoundException, SystemParameterRequiredException {
        if (StringUtil.isEmpty(apiName)) {
            throw new EndPointNotFoundException("No method named " + apiName);
        }

        int index = apiName.lastIndexOf(".");
        if (index > 0) {
            String serviceName = apiName.substring(0, index);
            String endpointName = apiName.substring(index + 1);

            ServiceObject service = serviceMap.get(serviceName);
            if (service == null) {
                throw new ServiceNotFoundException("No service named " + serviceName);
            }

            // find endpoint
            Collection<EndpointItem> eps = service.getEndpoints().get(endpointName);
            if (eps == null || eps.isEmpty()) {
                throw new EndPointNotFoundException("No method named " + endpointName);
            }
            return eps.iterator().next();
        }
        throw new EndPointNotFoundException("No method named " + apiName);
    }

    @Override
    public EndpointItem getEndpoint(String serviceName, String endpointName, String[] paramNames) throws ServiceNotFoundException, EndPointNotFoundException,
            SystemParameterRequiredException {
        // find service
        ServiceObject service = serviceMap.get(serviceName);
        if (service == null) {
            throw new ServiceNotFoundException("No service named " + serviceName);
        }

        // find endpoint
        Collection<EndpointItem> eps = service.getEndpoints().get(endpointName);
        if (eps == null || eps.isEmpty()) {
            throw new EndPointNotFoundException("No method named " + endpointName);
        }

        if (supportOverload) {
            EndpointItem ep = findExactEndpoint(eps, paramNames);

            if (ep == null) {
                throw new EndPointNotFoundException("method not found, service=" + serviceName + "." + endpointName + " annotated with params: "
                        + ArrayUtils.toString(paramNames));
            }

            return ep;
        } else {
            return eps.iterator().next();
        }

    }

    /**
     * invoked when supportOverload, check parameters
     * 
     * @param endpoints
     * @param paramNames
     * @return
     */
    private EndpointItem findExactEndpoint(Collection<EndpointItem> endpoints, String[] paramNames) {
        if (endpoints.size() == 1) {
            EndpointItem ep = endpoints.iterator().next();

            // modified on 2010-3-19, check if required parameter omitted
            String[] requiredParameterNames = ep.getRequiredParameterNames();
            if (Utils.arrayContains(requiredParameterNames, paramNames)) {
                return ep;
            } else {
                return null;
            }

        }

        Iterator<EndpointItem> it = endpoints.iterator();
        while (it.hasNext()) {
            EndpointItem ep = it.next();
            if (ArrayUtils.isSameLength(ep.getParameters(), paramNames)) {
                String[] epParameterNames = ep.getParameterNames();
                if (Utils.arrayEquals(paramNames, epParameterNames)) {
                    return ep;
                }
            }
        }
        return null;
    }

    /**
     * 
     * @param method
     * @return
     * @throws ServiceDefinitionException
     * @throws ConvertException
     */
    protected EndpointItem initEndpoint(Method method, ExportMethod exportMethod) throws ServiceDefinitionException, ConvertException {
        EndpointItem ep = new EndpointItem();
        ep.setMethod(method);

        Endpoint endpointAnnotation = method.getAnnotation(Endpoint.class);

        if (!endpointAnnotation.name().isEmpty()) {
            ep.setName(endpointAnnotation.name());
        } else {
            ep.setName(method.getName());
        }
        ep.setTimeWait(endpointAnnotation.timeWait());
        ep.setVoid(method.getReturnType().equals(void.class));

        Type[] paramTypes = method.getGenericParameterTypes();

        // 判断最后一个参数是否是ctx
        if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1] == RequestContext.class) {
            ep.setHasCtxParam(true);
        }

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        List<ParameterItem> params = new ArrayList<ParameterItem>(paramTypes.length);
        for (int i = 0; i < paramTypes.length; i++) {
            ParameterItem param = loadParameter(method, paramTypes[i], paramAnnotations[i]);
            // 只暴露出@Param的方法
            if (param != null) {
                params.add(param);
            }
        }

        ep.setParameters(params.toArray(new ParameterItem[0]));
        //设置tracerLog日志输出设置参数
        if(exportMethod != null && StringUtils.isNotBlank(exportMethod.getPrintParam())){
            ep.setPrintParam(exportMethod.getPrintParam());
        }
        if(exportMethod != null && StringUtils.isNotBlank(exportMethod.getPrintResult())){
            ep.setPrintResult(exportMethod.getPrintResult());
        }
        return ep;
    }

    protected ParameterItem loadParameter(Method method, Type paramType, Annotation[] annotations) throws ServiceDefinitionException, ConvertException {
        ParameterItem p = new ParameterItem();
        // type
        p.setType(paramType);

        Param paramAnno = AnnotationUtil.getAnnotation(annotations, Param.class);
        if (paramAnno == null) {
            throw new ServiceDefinitionException("service=" + method.getDeclaringClass().getName() + ",method=" + method.getName()
                    + " ,one more param annotaions is absent");
        }
        // name
        p.setParamName(paramAnno.name());
        // optional or not
        p.setOptional(paramAnno.optional());

        // default value
        if (!paramAnno.defaultValue().isEmpty()) {
            try {
                p.setDefaultValue(TypeHandler.createValue(paramAnno.defaultValue(), paramType));
            } catch (ParseException e) {
                throw new ConvertException("parseError", e);
            }
        }

        return p;
    }

    public Map<String, ServiceObject> getServiceMap() {
        return serviceMap;
    }

    /**
     * @return the serviceInstancePool
     */
    public Collection<ServiceObject> getServices() {
        return serviceMap.values();
    }

    /**
     * @return the supportOverload
     */
    public boolean isSupportOverload() {
        return supportOverload;
    }

    /**
     * @param supportOverload the supportOverload to set
     */
    public void setSupportOverload(boolean supportOverload) {
        this.supportOverload = supportOverload;
    }


}
