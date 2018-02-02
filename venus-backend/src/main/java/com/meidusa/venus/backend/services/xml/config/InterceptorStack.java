package com.meidusa.venus.backend.services.xml.config;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class InterceptorStack {
    private String name;
    private List<InterceptorDef> interceptors = new ArrayList<InterceptorDef>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addInterceptor(InterceptorDef interceptor) {
        interceptors.add(interceptor);
    }

    public void addInterceptorStack(InterceptorStack stack) {
        interceptors.addAll(stack.getInterceptors());
    }

    public List<InterceptorDef> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<InterceptorDef> interceptors) {
        this.interceptors = interceptors;
    }

}
