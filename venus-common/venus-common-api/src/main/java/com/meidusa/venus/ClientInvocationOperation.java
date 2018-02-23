package com.meidusa.venus;

import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;

import java.lang.reflect.Method;

/**
 * client请求对象接口
 * Created by Zhangzhihua on 2018/2/23.
 */
public interface ClientInvocationOperation extends Invocation{

    String getRpcId();

    byte[] getAthenaId();

    byte[] getMessageId();

    Class<?> getServiceInterface();

    Method getMethod();

    Object[] getArgs();

    String getConsumerApp();

    String getConsumerIp();

    int getLookupType();

    void setAthenaId(byte[] athenaId);

    void setParentId(byte[] parentId);

    void setMessageId(byte[] messageId);

    EndpointWrapper getEndpoint();

    ServiceWrapper getService();

}
