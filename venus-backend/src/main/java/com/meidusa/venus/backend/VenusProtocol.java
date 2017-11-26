package com.meidusa.venus.backend;

import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.net.ConnectionAcceptor;
import com.meidusa.toolkit.net.ConnectionManager;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.authenticate.server.AuthenticateProvider;
import com.meidusa.toolkit.net.factory.FrontendConnectionFactory;
import com.meidusa.venus.Application;
import com.meidusa.venus.Protocol;
import com.meidusa.venus.backend.authenticate.SimpleAuthenticateProvider;
import com.meidusa.venus.backend.handler.VenusServerConnectionObserver;
import com.meidusa.venus.backend.handler.VenusServerReceiveMessageHandler;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.io.network.VenusBackendFrontendConnectionFactory;
import com.meidusa.venus.support.VenusConstants;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;

/**
 * venus协议，启动/销毁remoting、设置message handler相关操作
 * Created by Zhangzhihua on 2017/9/28.
 */
public class VenusProtocol implements Protocol,InitializingBean{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static boolean isRunning = false;

    private ConnectionAcceptor connectionAcceptor;

    private MessageHandler messageHandler;

    //不注入依赖，由srvMgr向venusProtocol设置
    private ServiceManager serviceManager;

    private AuthenticateProvider authenticateProvider;

    //venus协议端口
    private int port;

    //venus协议默认线程数
    private int coreThreads = VenusConstants.VENUS_PROTOCOL_DEFAULT_CORE_THREADS;

    public VenusProtocol(){
        Application.addProtocol(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        valid();
    }

    /**
     * 有效性校验
     */
    void valid(){
        if(port == 0){
            throw new VenusConfigException("port property not config.");
        }
    }

    /**
     * 初始化venus协议，启动venus服务监听
     */
    @Override
    public void init() throws Exception{
        synchronized (VenusProtocol.class){
            if(!isRunning){
                if(connectionAcceptor == null){
                    connectionAcceptor = createConnectionAcceptor();
                    connectionAcceptor.start();
                }
                isRunning = true;
            }
        }

    }

    /**
     * 创建ConnectionAcceptor
     * @return
     */
    ConnectionAcceptor createConnectionAcceptor() throws InitialisationException {
        VenusConnectionAcceptor connectionAcceptor = new VenusConnectionAcceptor();
        connectionAcceptor.setName("venus Acceptor-0");
        connectionAcceptor.setPort(port);
        //计算每IO线程组业务平均线程池数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int perGroupCoreThread = coreThreads/cpuCores;
        if(perGroupCoreThread < cpuCores){
            perGroupCoreThread = cpuCores;
        }
        connectionAcceptor.setExecutorSize(perGroupCoreThread);
        connectionAcceptor.setConnectionFactory(createConnectionFactory());
        return connectionAcceptor;
    }

    /**
     * 扩展添加连接监听observer
     */
    class VenusConnectionAcceptor extends ConnectionAcceptor{
        @Override
        public void initProcessors() throws IOException {
            if(processors == null){
                processors = new ConnectionManager[Runtime.getRuntime().availableProcessors()];
                for(int i=0;i<processors.length;i++){
                    processors[i] = new ConnectionManager(this.getName()+"-Manager-"+i,getExecutorSize());
                    processors[i].addConnectionObserver(new VenusServerConnectionObserver());
                    processors[i].start();
                }
            }
        }
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
        connectionFactory.setAuthenticateProvider(getAuthenticateProvider());
        return connectionFactory;
    }

    /**
     * 创建messageHandler
     * @return
     */
    MessageHandler createMessageHandler() throws InitialisationException {
        VenusServerReceiveMessageHandler messageHandler = new VenusServerReceiveMessageHandler();
        messageHandler.setServiceManager(this.serviceManager);
        return messageHandler;
    }

    @Override
    public void destroy() throws Exception {
        if(logger.isInfoEnabled()){
            logger.info("detroy protocol:{}.",this);
        }
        //释放连接
        if(connectionAcceptor != null){
            connectionAcceptor.shutdown();
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public AuthenticateProvider getAuthenticateProvider() {
        if(authenticateProvider == null){
            //若认证为空，则默认设置为dummy方式
            SimpleAuthenticateProvider simpleAuthenticateProvider= new SimpleAuthenticateProvider();
            simpleAuthenticateProvider.setUseDummy(true);
            return simpleAuthenticateProvider;
        }
        return authenticateProvider;
    }

    public void setAuthenticateProvider(AuthenticateProvider authenticateProvider) {
        this.authenticateProvider = authenticateProvider;
    }

    //不使用属性依赖注入，由srvMgr反向注入
    public void setSrvMgr(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public int getCoreThreads() {
        return coreThreads;
    }

    public void setCoreThreads(int coreThreads) {
        this.coreThreads = coreThreads;
    }
}
