package com.meidusa.venus;

import java.lang.reflect.Method;
import java.util.List;

/**
 * invocation
 * Created by Zhangzhihua on 2017/7/31.
 */
public class Invocation {

    private Class<?> serviceType;

    private Method method;

    private Object[] args;

    private List<Address> addressList;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Class<?> getServiceType() {
        return serviceType;
    }

    public void setServiceType(Class<?> serviceType) {
        this.serviceType = serviceType;
    }

    public List<Address> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<Address> addressList) {
        this.addressList = addressList;
    }
}
