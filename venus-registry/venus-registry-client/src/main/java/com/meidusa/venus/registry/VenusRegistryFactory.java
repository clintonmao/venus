package com.meidusa.venus.registry;

import com.meidusa.venus.RpcException;
import com.meidusa.venus.ServiceFactory;
import com.meidusa.venus.ServiceFactoryExtra;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.registry.domain.HostPort;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.registry.service.RegisterService;
import com.meidusa.venus.registry.service.impl.MysqlRegisterService;
import com.meidusa.venus.util.ReftorUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Venus注册中心工厂类，负责初始化注册中心
 * Created by Zhangzhihua on 2017/9/15.
 */
public class VenusRegistryFactory implements InitializingBean, BeanFactoryPostProcessor {

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
     * simple服务调用工厂
     */
    private ServiceFactoryExtra serviceFactoryExtra;

    /**
     * 注册服务
     */
    private Register register;


    @Override
    public void afterPropertiesSet() throws Exception {
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
            registerService = newVenusRegisterService(registerUrl);
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
    RegisterService newVenusRegisterService(String registerUrl){
        String[] split = registerUrl.split(";");
        List<HostPort> hosts = new ArrayList<HostPort>();
        for (int i = 0; i < split.length; i++) {
            String str = split[i];
            String[] split2 = str.split(":");
            if (split2.length > 1) {
                String host = split2[0];
                String port = split2[1];
                HostPort hp = new HostPort(host, Integer.parseInt(port));
                hosts.add(hp);
            }
        }

        //TODO 允许设置多个目标地址
        HostPort hp = hosts.get(new Random().nextInt(hosts.size()));
        /*
        SimpleServiceFactory ssf = new SimpleServiceFactory(hp.getHost(), hp.getPort());
        ssf.setCoTimeout(60000);
        ssf.setSoTimeout(60000);
        RegisterService registerService = ssf.getService(RegisterService.class);
        */
        if(serviceFactoryExtra == null){
            initSimpleServiceFactory();
            String address = String.format("%s:%s",hp.getHost(),String.valueOf(hp.getPort()));
            String[] addresses = {address};
            List<String> addressList = Arrays.asList(addresses);
            serviceFactoryExtra.setAddressList(addressList);
        }
        RegisterService registerService = serviceFactoryExtra.getService(RegisterService.class);
        return registerService;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    public String getRegisterUrl() {
        return registerUrl;
    }

    public void setRegisterUrl(String registerUrl) {
        this.registerUrl = registerUrl;
    }

    /**
     * 初始化simpleServiceFactory
     */
    void initSimpleServiceFactory(){
        String className = "com.meidusa.venus.client.factory.simple.SimpleServiceFactory";
        Object obj = ReftorUtil.newInstance(className);
        if(obj == null){
            throw new VenusConfigException("init simpleServiceFactory failed.");
        }
        this.serviceFactoryExtra = (ServiceFactoryExtra)obj;
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
