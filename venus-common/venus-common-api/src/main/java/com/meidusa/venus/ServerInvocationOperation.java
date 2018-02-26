package com.meidusa.venus;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.support.EndpointWrapper;

import java.lang.reflect.Method;

/**
 * server请求对象接口
 * Created by Zhangzhihua on 2018/2/23.
 */
public interface ServerInvocationOperation extends Invocation {

    String getRpcId();

    byte[] getAthenaId();

    byte[] getMessageId();

    Class<?> getServiceInterface();

    Method getMethod();

    Object[] getArgs();

    String getProviderApp();

    String getProviderIp();

    String getConsumerApp();

    String getConsumerIp();

    void setAthenaId(byte[] athenaId);

    void setParentId(byte[] parentId);

    void setMessageId(byte[] messageId);

    byte[] getParentId();

    EndpointWrapper getEndpoint();

    Tuple<Long, byte[]> getData();

    SerializeServiceRequestPacket getServiceRequestPacket();
}
