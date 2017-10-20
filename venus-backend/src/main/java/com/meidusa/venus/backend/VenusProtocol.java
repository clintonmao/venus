package com.meidusa.venus.backend;

import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.net.ConnectionAcceptor;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.authenticate.server.AuthenticateProvider;
import com.meidusa.toolkit.net.factory.FrontendConnectionFactory;
import com.meidusa.venus.VenusContext;
import com.meidusa.venus.backend.handler.VenusServerInvokerMessageHandler;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.monitor.VenusMonitorFactory;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.io.network.VenusBackendFrontendConnectionFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * venus协议，启动/销毁remoting、设置message handler相关操作
 * Created by Zhangzhihua on 2017/9/28.
 */
public class VenusProtocol implements InitializingBean,BeanFactoryPostProcessor,DisposableBean,BeanFactoryAware,ApplicationContextAware {

    private static boolean isRunning = false;

    private ConnectionAcceptor connectionAcceptor;

    private MessageHandler messageHandler;

    /**
     * 不注入依赖，由srvMgr向venusProtocol设置
     */
    private ServiceManager serviceManager;

    private AuthenticateProvider authenticateProvider;

    private VenusRegistryFactory venusRegistryFactory;

    private VenusMonitorFactory venusMonitorFactory;

    //自定义属性设置
    private String port;

    @Override
    public void afterPropertiesSet() throws Exception {
        valid();
    }

    /**
     * 有效性校验
     */
    void valid(){
        if(StringUtils.isEmpty(port)){
            throw new VenusConfigException("port property not config.");
        }
    }

    /**
     * 初始化venus协议，启动venus服务监听
     */
    public synchronized void init() throws Exception{
        if(!isRunning){
            VenusContext.getInstance().setPort(port);
            if(connectionAcceptor == null){
                connectionAcceptor = createConnectionAcceptor();
                connectionAcceptor.start();
            }
            isRunning = true;
        }

    }

    /**
     * 创建ConnectionAcceptor
     * @return
     */
    ConnectionAcceptor createConnectionAcceptor() throws InitialisationException {
        ConnectionAcceptor connectionAcceptor = new ConnectionAcceptor();
        connectionAcceptor.setName("Service socket Server");
        connectionAcceptor.setPort(Integer.parseInt(port));
        //TODO io线程数目设定
        connectionAcceptor.setExecutorSize(5);
        connectionAcceptor.setConnectionFactory(createConnectionFactory());
        return connectionAcceptor;
    }

    /**
     * 创建连接工厂
     * @return
     */
    FrontendConnectionFactory createConnectionFactory() throws InitialisationException {
        VenusBackendFrontendConnectionFactory connectionFactory = new VenusBackendFrontendConnectionFactory();
        //connectionFactory.setSendBufferSize(16);
        //connectionFactory.setReceiveBufferSize(8);
        MessageHandler messageHandler = createMessageHandler();
        this.messageHandler = messageHandler;
        connectionFactory.setMessageHandler(messageHandler);
        connectionFactory.setAuthenticateProvider(authenticateProvider);
        return connectionFactory;
    }

    /**
     * 创建messageHandler
     * @return
     */
    MessageHandler createMessageHandler() throws InitialisationException {
        VenusServerInvokerMessageHandler messageHandler = new VenusServerInvokerMessageHandler();
        //TODO 业务线程数量设定
        messageHandler.setMaxExecutionThread(100);
        messageHandler.setExecutorProtected(false);
        messageHandler.setExecutorEnabled(false);
        messageHandler.setUseThreadLocalExecutor(false);
        messageHandler.setServiceManager(this.serviceManager);
        messageHandler.init();
        return messageHandler;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    public String getPort() {
        return port;
    }


    //TODO 资源释放时机，application or protocol or invoker？同时consumer&provider都要释放
    @Override
    public void destroy() throws Exception {
        if(connectionAcceptor != null){
            connectionAcceptor.shutdown();
        }
    }

    public void setPort(String port) {
        this.port = port;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /*
    public void registeServiceManager(ServiceManager serviceManager) {
        if(this.messageHandler instanceof VenusServerInvokerMessageHandler){
            VenusServerInvokerMessageHandler serverInvokerMessageHandler =  (VenusServerInvokerMessageHandler)messageHandler;
            serverInvokerMessageHandler.setServiceManager(serviceManager);
        }else{
            throw new VenusConfigException("messageHandler config error.");
        }
    }
    */

    public AuthenticateProvider getAuthenticateProvider() {
        return authenticateProvider;
    }

    public void setAuthenticateProvider(AuthenticateProvider authenticateProvider) {
        this.authenticateProvider = authenticateProvider;
    }

    public VenusRegistryFactory getVenusRegistryFactory() {
        return venusRegistryFactory;
    }

    public void setVenusRegistryFactory(VenusRegistryFactory venusRegistryFactory) {
        this.venusRegistryFactory = venusRegistryFactory;
    }

    public VenusMonitorFactory getVenusMonitorFactory() {
        return venusMonitorFactory;
    }

    public void setVenusMonitorFactory(VenusMonitorFactory venusMonitorFactory) {
        this.venusMonitorFactory = venusMonitorFactory;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }

    //不使用属性依赖注入，由srvMgr反向注入
    public void setSrvMgr(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }
}
