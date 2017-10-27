package com.meidusa.venus.registry;

import com.caucho.hessian.client.HessianProxyFactory;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.registry.service.RegisterService;
import com.meidusa.venus.registry.service.impl.MysqlRegisterService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.net.MalformedURLException;

/**
 * Venus注册中心工厂类，负责初始化注册中心
 * Created by Zhangzhihua on 2017/9/15.
 */
public class VenusRegistryFactory implements InitializingBean, DisposableBean,BeanFactoryPostProcessor {

    private static Logger logger = LoggerFactory.getLogger(VenusRegistryFactory.class);

    /**
     * 注册中心url
     */
    private String registerUrl;

    /**
     * 注册中心连接Url
     */
    private String connectUrl;

    /**
     * 注册服务
     */
    private Register register;

    @Override
    public void afterPropertiesSet() throws Exception {
        //valid
        valid();

        //初始化register
        initRegister();
    }

    /**
     * 校验有效性
     */
    void valid(){
        if(StringUtils.isEmpty(registerUrl) && StringUtils.isEmpty(connectUrl)){
            throw new VenusConfigException("registerUrl or connectUrl property not config.");
        }
    }

    /**
     * 初始化注册中心
     */
    void initRegister(){
        //实例registerService
        RegisterService registerService = newRegisterService();

        //实例化register
        Register register = new MysqlRegister(registerService);
        this.register = register;
        RegisterContext.getInstance().setRegister(register);
    }

    /**
     * 实例化register service
     * @return
     */
    RegisterService newRegisterService(){
        RegisterService registerService = null;
        //根据配置创建registerService实例，本地依赖或venus远程依赖
        if(StringUtils.isNotEmpty(registerUrl)){
            registerService = newHessianRegisterService(registerUrl);
        }else{
            registerService = newLocalRegisterService(connectUrl);
        }
        if(registerService == null){
            throw new RpcException("init register service failed.");
        }
        return registerService;
    }

    /**
     * 创建本地依赖实例，jvm本地依赖
     * @param connectUrl
     * @return
     */
    RegisterService newLocalRegisterService(String connectUrl){
        try {
            if(StringUtils.isEmpty(connectUrl)){
                throw new VenusConfigException("connectUrl not config with injvm.");
            }
            RegisterService registerService = new MysqlRegisterService(connectUrl);
            return registerService;
        } catch (Exception e) {
                throw new RpcException(e);
        }
    }

    /**
     * 根据注册中心创建远程注册中心引用
     * @param registerUrl
     * @return
     */
    RegisterService newHessianRegisterService(String registerUrl){
        HessianProxyFactory factory = new HessianProxyFactory();
        try {
            RegisterService registerService = (RegisterService) factory.create(RegisterService.class, registerUrl);
            return registerService;
        } catch (MalformedURLException e) {
            logger.error("newHessianRegisterService error.",e);
            return null;
        }


    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    //TODO 进程正常关闭及非正常关闭情况处理
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                logger.warn("process exit,release source...");
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        //释放注册服务资源
        if(register != null){
            register.destroy();
        }

    }

    public String getRegisterUrl() {
        return registerUrl;
    }

    public void setRegisterUrl(String registerUrl) {
        this.registerUrl = registerUrl;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    public String getConnectUrl() {
        return connectUrl;
    }

    public void setConnectUrl(String connectUrl) {
        this.connectUrl = connectUrl;
    }

}
