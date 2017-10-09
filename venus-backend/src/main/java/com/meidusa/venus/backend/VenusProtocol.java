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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * venus协议，启动/销毁remoting、设置message handler相关操作
 * Created by Zhangzhihua on 2017/9/28.
 */
public class VenusProtocol implements InitializingBean,BeanFactoryPostProcessor,DisposableBean {

    private boolean isRunning = false;

    private ConnectionAcceptor connectionAcceptor;

    private MessageHandler messageHandler;

    private ServiceManager serviceManager;

    private AuthenticateProvider authenticateProvider;

    private VenusRegistryFactory venusRegistryFactory;

    private VenusMonitorFactory venusMonitorFactory;

    //自定义属性设置 TODO 其它
    private String port;

    @Override
    public void afterPropertiesSet() throws Exception {
        if(!isRunning){
            //校验
            valid();

            //初始化
            init();
            isRunning = true;
        }
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
     * 初始化venus协议
     */
    void init() throws Exception{
        VenusContext.getInstance().setPort(port);
        if(connectionAcceptor == null){
            connectionAcceptor = createConnectionAcceptor();
            //TODO 在serviceManager未初始化之前应关闭接收，处理先后关系
            connectionAcceptor.start();
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
        //TODO 线程数目设定
        connectionAcceptor.setExecutorSize(1);
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
        //TODO 属性设置
        VenusServerInvokerMessageHandler messageHandler = new VenusServerInvokerMessageHandler();
        //TODO 线程数量设定@@@
        messageHandler.setMaxExecutionThread(1);
        messageHandler.setExecutorProtected(false);
        messageHandler.setExecutorEnabled(false);
        messageHandler.setUseThreadLocalExecutor(false);
        messageHandler.setServiceManager(this.serviceManager);
        //TODO 初始化时机
        messageHandler.init();
        return messageHandler;
    }

    @Override
    public void destroy() throws Exception {
        if(connectionAcceptor != null){
            connectionAcceptor.shutdown();
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }


    public String getPort() {
        return port;
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

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
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
}
