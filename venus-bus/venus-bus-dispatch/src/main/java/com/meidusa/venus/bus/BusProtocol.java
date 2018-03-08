package com.meidusa.venus.bus;

import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.net.ConnectionAcceptor;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.authenticate.server.AuthenticateProvider;
import com.meidusa.toolkit.net.factory.FrontendConnectionFactory;
import com.meidusa.venus.backend.authenticate.SimpleAuthenticateProvider;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.io.network.Venus4FrontendConnectionFactory;
import com.meidusa.venus.support.VenusConstants;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * venus协议，启动/销毁remoting、设置message handler相关操作
 * Created by Zhangzhihua on 2017/9/28.
 */
public class BusProtocol implements InitializingBean,DisposableBean {

    private static boolean isRunning = false;

    private ConnectionAcceptor connectionAcceptor;

    private MessageHandler messageHandler;

    private AuthenticateProvider authenticateProvider;

    //自定义属性设置
    private String port;

    //venus协议默认线程数
    private int coreThreads = VenusConstants.VENUS_PROTOCOL_DEFAULT_CORE_THREADS;

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
        connectionAcceptor.setName("venus Acceptor-0");
        connectionAcceptor.setPort(Integer.parseInt(port));
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
     * 创建连接工厂
     * @return
     */
    FrontendConnectionFactory createConnectionFactory() throws InitialisationException {
        Venus4FrontendConnectionFactory connectionFactory = new Venus4FrontendConnectionFactory();
        //connectionFactory.setSendBufferSize(16);
        //connectionFactory.setReceiveBufferSize(8);
        //MessageHandler messageHandler = createMessageHandler();
        //this.messageHandler = messageHandler;
        connectionFactory.setMessageHandler(messageHandler);
        connectionFactory.setAuthenticateProvider(getAuthenticateProvider());
        return connectionFactory;
    }

    @Override
    public void destroy() throws Exception {
        //释放连接
        if(connectionAcceptor != null){
            connectionAcceptor.shutdown();
        }
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

    public int getCoreThreads() {
        return coreThreads;
    }

    public void setCoreThreads(int coreThreads) {
        this.coreThreads = coreThreads;
    }
}
