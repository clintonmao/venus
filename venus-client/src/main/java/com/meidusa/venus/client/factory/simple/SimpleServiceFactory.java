package com.meidusa.venus.client.factory.simple;

import com.meidusa.toolkit.common.bean.config.ConfigUtil;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.ServiceFactoryExtra;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.client.factory.InvokerInvocationHandler;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.ReferenceService;
import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.exception.XmlVenusExceptionFactory;
import com.meidusa.venus.io.authenticate.Authenticator;
import com.meidusa.venus.io.authenticate.DummyAuthenticator;
import com.meidusa.venus.io.packet.DummyAuthenPacket;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于API方式调用venus服务
 * @author structchen
 * 
 */
public class SimpleServiceFactory implements ServiceFactoryExtra {

    private static Logger logger = LoggerFactory.getLogger(SimpleServiceFactory.class);

    /**
     * 地址列表，如:"192.168.0.1:9000;192.168.0.2:9000"
     */
    private String ipAddressList;

    /**
     * 读取返回数据包的超时时间
     */
    private int soTimeout = 15 * 1000;

    /**
     * 连接超时时间
     */
    private int coTimeout = 5 * 1000;

    private Authenticator authenticator;

    private Map<Class<?>, Tuple<Object, InvokerInvocationHandler>> servicesMap = new HashMap<Class<?>, Tuple<Object, InvokerInvocationHandler>>();

    public SimpleServiceFactory() {
    }

    public SimpleServiceFactory(String host, int port) {
        String address = String.format("%s:%s",host,String.valueOf(port));
        this.ipAddressList = address;
    }

    @Override
    public void setAddressList(String ipAddressList) {
        if(StringUtils.isEmpty(ipAddressList)){
            throw new VenusConfigException("ipAddressList is empty.");
        }
        //转换ucm属性地址
        ipAddressList = parsePropertyConfig(ipAddressList);
        //转换','分隔地址
        ipAddressList = ipAddressList.trim();
        if(ipAddressList.contains(",")){
            ipAddressList = ipAddressList.replace(",",";");
        }
        //校验地址有效性
        validAddress(ipAddressList);

        this.ipAddressList = ipAddressList;
    }


    /**
     * 解析spring或ucm属性配置，如${x.x.x}
     */
    String parsePropertyConfig(String ipAddressList){
        if(StringUtils.isNotEmpty(ipAddressList)){
            if(ipAddressList.startsWith("${") && ipAddressList.endsWith("}")){
                String realAddress = (String) ConfigUtil.filter(ipAddressList);
                if(StringUtils.isEmpty(realAddress)){
                    throw new VenusConfigException("ucm parse empty,ipAddressList config invalid.");
                }
                if(logger.isInfoEnabled()){
                    logger.info("##########realIpAddress:{}#############.",realAddress);
                }
                return realAddress;
            }
        }
        return ipAddressList;
    }

    /**
     * 校验地址有效性
     * @param ipAddressList
     */
    void validAddress(String ipAddressList){
        String[] addressArr = ipAddressList.split(";");
        if(addressArr == null || addressArr.length == 0){
            throw new VenusConfigException("ipAddressList invalid:" + ipAddressList);
        }
        for(String address:addressArr){
            String[] arr = address.split(":");
            if(arr == null || arr.length != 2){
                throw new VenusConfigException("ipAddressList invalid:" + ipAddressList);
            }
        }
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getCoTimeout() {
        return coTimeout;
    }

    public void setCoTimeout(int coTimeout) {
        this.coTimeout = coTimeout;
    }

    @Override
    public <T> T getService(Class<T> t) {
        Tuple<Object, InvokerInvocationHandler> object = servicesMap.get(t);
        if (object == null) {
            synchronized (servicesMap) {
                object = servicesMap.get(t);
                if (object == null) {
                    T obj = initService(t);
                    return obj;
                }
            }
        }
        
        return (T) object.left;
    }

    /**
     * 初始化服务
     * @param t
     * @param <T>
     * @return
     */
    protected <T> T initService(Class<T> t) {
        //初始化服务代理
        T serviceProxy = initServiceProxy(t);
        return serviceProxy;
    }

    /**
     * 初始化服务代理
     * @param t
     * @param <T>
     * @return
     */
    <T> T initServiceProxy(Class<T> t) {
        InvokerInvocationHandler invocationHandler = new InvokerInvocationHandler();
        invocationHandler.setServiceInterface(t);
        if(StringUtils.isNotEmpty(ipAddressList)){
            invocationHandler.setRemoteConfig(ClientRemoteConfig.newInstace(ipAddressList));
        }else{
            throw new VenusConfigException("ipAddressList not allow empty.");
        }

        if(this.getAuthenticator() == null){
            this.authenticator = new DummyAuthenticator<DummyAuthenPacket>();
        }
        //invocationHandler.setAuthenticator(this.getAuthenticator());

        T object = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { t }, invocationHandler);

        VenusExceptionFactory venusExceptionFactory = XmlVenusExceptionFactory.getInstance();
        for (Method method : t.getMethods()) {
            Endpoint endpoint = method.getAnnotation(Endpoint.class);
            if (endpoint != null) {
                Class[] eclazz = method.getExceptionTypes();
                for (Class clazz : eclazz) {
                    if (venusExceptionFactory != null && CodedException.class.isAssignableFrom(clazz)) {
                        venusExceptionFactory.addException(clazz);
                    }
                }
            }
        }

        Tuple<Object, InvokerInvocationHandler> serviceTuple = new Tuple<Object, InvokerInvocationHandler>(object, invocationHandler);
        servicesMap.put(t, serviceTuple);
        return object;
    }

    @Override
    public void destroy() {
    }

	@Override
	public <T> T getService(String name, Class<T> t) {
		return getService(t);
	}

    public String getIpAddressList() {
        return ipAddressList;
    }

    public void setIpAddressList(String ipAddressList) {
        this.ipAddressList = ipAddressList;
    }
}
