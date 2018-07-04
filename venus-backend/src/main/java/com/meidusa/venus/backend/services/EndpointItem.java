/**
 * 
 */
package com.meidusa.venus.backend.services;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * describes a method of exposed service
 * 
 * @author Sun Ning
 * @since 2010-3-4
 */
public class EndpointItem {

    private String name;
    private Method method;
    private int timeWait;
    private boolean async = false;
    private ParameterItem[] parameters;
    private boolean hasCtxParam;
    private ServiceObject service;
    private String keyExpression;
    // cache
    private volatile transient String[] parameterNames;
    // cache
    private transient String[] requiredParameterNames;
    // cache
    private volatile transient Map<String, Type> parameterTypeDict;

    private List<Interceptor> interceptorList = new ArrayList<>();

    private boolean active = true;

    private boolean isVoid = true;

    //是否打印输入参数
    private String printParam;

    //是否打印输出结果
    private String printResult;


    public int getTimeWait() {
        return timeWait;
    }

    public void setTimeWait(int soTimeout) {
        this.timeWait = soTimeout;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public void setVoid(boolean isVoid) {
        this.isVoid = isVoid;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * 
     * @return
     */
    public Map<String, Type> getParameterTypeDict() {
        if (parameterTypeDict == null) {
            synchronized (this) {
                if (parameterTypeDict == null) {
                    parameterTypeDict = new HashMap<String, Type>(this.parameters.length);
                    for (int i = 0; i < this.parameters.length; i++) {
                        parameterTypeDict.put(this.parameters[i].getParamName(), this.parameters[i].getType());
                    }
                }
            }
        }

        return parameterTypeDict;
    }

    /**
     * 
     * @return
     */
    public String[] getParameterNames() {
        String temp[] = parameterNames;
        if (temp == null) {
            synchronized (this) {
                temp = parameterNames;
                if (temp == null) {
                    temp = parameterNames = new String[this.parameters.length];
                    for (int i = 0; i < this.parameters.length; i++) {
                        parameterNames[i] = this.parameters[i].getParamName();
                    }
                }
            }
        }
        return temp;
    }

    /**
     * create an array of parameter to run the method, in the order of declaration. System parameter (such as delimiter)
     * will be ignored
     * 
     * @param requestParams
     * @return
     */
    public Object[] getParameterValues(Map<String, Object> requestParams) {
        List<Object> values = new ArrayList<Object>();

        for (int i = 0; i < this.getParameterNames().length; i++) {
            String name = this.getParameterNames()[i];
            Object data = requestParams.get(name);

            if (data == null) {
                values.add(this.getParameters()[i].getDefaultValue());
            } else {
                values.add(data);
            }
        }

        return values.toArray();
    }

    public synchronized String[] getRequiredParameterNames() {
        if (requiredParameterNames == null) {
            List<String> requiredSubList = new ArrayList<String>();

            for (ParameterItem p : this.getParameters()) {
                if (!p.isOptional()) {
                    requiredSubList.add(p.getParamName());
                }
            }

            requiredParameterNames = requiredSubList.toArray(new String[0]);
        }
        return requiredParameterNames;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(Method method) {
        this.method = method;
    }

    /**
     * @return the arguments
     */
    public ParameterItem[] getParameters() {
        return parameters;
    }

    /**
     * @param arguments the arguments to set
     */
    public void setParameters(ParameterItem[] arguments) {
        this.parameters = arguments;
    }

    /**
     * @return the service
     */
    public ServiceObject getService() {
        return service;
    }

    /**
     * @param service the service to set
     */
    public void setService(ServiceObject service) {
        this.service = service;
    }

    public String getKeyExpression() {
        return keyExpression;
    }

    public void setKeyExpression(String keyExpression) {
        this.keyExpression = keyExpression;
    }

    public void setParameterTypeDict(Map<String, Type> parameterTypeDict) {
        this.parameterTypeDict = parameterTypeDict;
    }

    public void setRequiredParameterNames(String[] requiredParameterNames) {
        this.requiredParameterNames = requiredParameterNames;
    }

    public boolean isHasCtxParam() {
        return hasCtxParam;
    }

    public void setHasCtxParam(boolean hasCtxParam) {
        this.hasCtxParam = hasCtxParam;
    }

    public List<Interceptor> getInterceptorList() {
        return interceptorList;
    }

    public void setInterceptorList(List<Interceptor> interceptorList) {
        this.interceptorList = interceptorList;
    }

    public String getPrintParam() {
        return printParam;
    }

    public void setPrintParam(String printParam) {
        this.printParam = printParam;
    }

    public String getPrintResult() {
        return printResult;
    }

    public void setPrintResult(String printResult) {
        this.printResult = printResult;
    }
}
