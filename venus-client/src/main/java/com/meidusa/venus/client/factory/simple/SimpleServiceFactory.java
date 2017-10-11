package com.meidusa.venus.client.factory.simple;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.ServiceFactoryExtra;
import com.meidusa.venus.VenusContext;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.proxy.InvokerInvocationHandler;
import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.exception.XmlVenusExceptionFactory;
import com.meidusa.venus.io.authenticate.Authenticator;
import com.meidusa.venus.io.authenticate.DummyAuthenticator;
import com.meidusa.venus.io.packet.DummyAuthenPacket;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterContext;
import com.meidusa.venus.util.NetUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * <b>简单连接服务工厂:</b> <br/>
 * <li>没有连接池,每次请求都将创建连接,请求完毕以后关闭连接.</li> <li>只能针对某个具体的IP,Port而创建,多个地址需要创建不同对象</li> <li>不支持回调</li> <br/>
 * <b>使用场景:</b>
 * 
 * <pre>
 * 访问固定地址,并且使用频率较少的情况下.
 * 相关使用代码:
 * SimpleServiceFactory factory = new SimpleServiceFactory("127.0.0.1",16800);
 * factory.setSoTimeout(16 * 1000);//可选,默认 15秒
 * factory.setCoTimeout(5 * 1000);//可选,默认5秒
 * HelloService helloService = factory.getService(HelloService.class);
 * </pre>
 * 
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

    private VenusExceptionFactory venusExceptionFactory;

    private Authenticator authenticator;

    //TODO 版本号相关信息
    private Map<Class<?>, Tuple<Object, InvokerInvocationHandler>> servicesMap = new HashMap<Class<?>, Tuple<Object, InvokerInvocationHandler>>();

    /**
     * 注册中心client
     */
    private Register register;

    public SimpleServiceFactory() {
    }

    public SimpleServiceFactory(String host, int port) {
        String address = String.format("%s:%s",host,String.valueOf(port));
        this.ipAddressList = address;
    }

    @Override
    public void setAddressList(String[] addressArr) {
        if(addressArr == null || addressArr.length == 0){
            throw new VenusConfigException("addressArr is empty.");
        }
        this.ipAddressList = StringUtils.join(addressArr,";");
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
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

        //若需要订阅服务，TODO 订阅了服务但不使用服务的地址发现功能
        if(isNeedSubscribleService(t)){
            try {
                //初始化注册中心
                initRegister();

                //订阅服务
                subscribleService(t);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        invocationHandler.setRemoteConfig(ClientRemoteConfig.newInstace(ipAddressList));

        if(this.venusExceptionFactory == null){
            XmlVenusExceptionFactory venusExceptionFactory = new XmlVenusExceptionFactory();
            venusExceptionFactory.setConfigFiles(new String[]{"classpath:com/meidusa/venus/exception/VenusSystemException.xml"});
            venusExceptionFactory.init();
            this.venusExceptionFactory = venusExceptionFactory;
        }
        invocationHandler.setVenusExceptionFactory(this.getVenusExceptionFactory());

        //TODO 确认认证功能
        if(this.getAuthenticator() == null){
            this.authenticator = new DummyAuthenticator<DummyAuthenPacket>();
        }
        //invocationHandler.setAuthenticator(getAuthenticator());

        T object = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { t }, invocationHandler);

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

    /**
     * 判断是否需要订阅服务，TODO 对于提供方不走注册中心的服务，有问题？
     * @param clz
     * @return
     */
    boolean isNeedSubscribleService(Class clz){
        return false;
    }

    /**
     * 获取注册中心
     * @return
     */
    void initRegister(){
        if(this.register != null){
            return;
        }
        //TODO 改由注册工厂获取，这样不存在加载顺序问题
        Register register = RegisterContext.getInstance().getRegister();
        if(register == null){
            throw new RpcException("init register failed.");
        }
        this.register = register;
    }

    /**
     * 订阅服务
     */
    void subscribleService(Class serviceClazz){
        String application = VenusContext.getInstance().getApplication();
        String serviceInterfaceName = serviceClazz.getClass().getName();
        String serivceName = "null";//TODO 服务名称为空
        String version = "0.0.0";//TODO
        String consumerHost = NetUtil.getLocalIp();

        String subscribleUrl = String.format(
                "subscrible://%s/%s?version=%s&application=%s&host=%s",
                serviceInterfaceName,
                serivceName,
                version,
                application,
                consumerHost
        );
        com.meidusa.venus.URL url = com.meidusa.venus.URL.parse(subscribleUrl);
        logger.info("subscrible service:{}",url);
        register.subscrible(url);
    }

    @Override
    public void destroy() {
        // TODO
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
