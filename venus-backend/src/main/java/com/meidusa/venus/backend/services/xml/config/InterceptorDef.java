package com.meidusa.venus.backend.services.xml.config;

import com.meidusa.venus.backend.services.Interceptor;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("interceptor")
public class InterceptorDef {

    //interceptor名称
    @XStreamAsAttribute
    private String name;

    //interceptor classame
    @XStreamAsAttribute
    private String clazz;

    //interceptor实例
    private Interceptor interceptor;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    public Interceptor getInterceptor() {
        return interceptor;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final InterceptorDef that = (InterceptorDef) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        return result;
    }
}
