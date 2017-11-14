package com.meidusa.venus.registry;

import com.caucho.hessian.client.HessianProxyFactory;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.registry.service.RegisterService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.MalformedURLException;

/**
 * Venus注册中心工厂类，负责初始化注册中心
 * Created by Zhangzhihua on 2017/9/15.
 */
public class VenusRegistryFactory implements InitializingBean, DisposableBean {

    private static Logger logger = LoggerFactory.getLogger(VenusRegistryFactory.class);

    /**
     * 注册中心地址，url地址
     */
    private String address;

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
        if(StringUtils.isEmpty(address)){
            throw new VenusConfigException("address property not config.");
        }
    }

    /**
     * 初始化注册中心
     */
    void initRegister(){
        //实例registerService
        RegisterService registerService = newHessianRegisterService(address);
        if(registerService == null){
            throw new VenusRegisteException("init register service failed.");
        }

        //实例化register
        Register register = new MysqlRegister(registerService);
        if(register == null){
            throw new VenusRegisteException("init register failed.");
        }
        this.register = register;
    }

    /**
     * 根据注册中心创建远程注册中心引用
     * @param registerUrl
     * @return
     */
    RegisterService newHessianRegisterService(String registerUrl){
        //连接、读写默认3000ms
        int readTimeout = 3000;
        int connectTimeout = 3000;
        HessianProxyFactory proxyFactory = new HessianProxyFactory();
        proxyFactory.setReadTimeout(readTimeout);
        proxyFactory.setConnectTimeout(connectTimeout);
        try {
            RegisterService registerService = (RegisterService) proxyFactory.create(RegisterService.class, registerUrl);
            return registerService;
        } catch (MalformedURLException e) {
            logger.error("newHessianRegisterService error.",e);
            return null;
        }


    }


    @Override
    public void destroy() throws Exception {
        //释放注册服务资源
        if(register != null){
            register.destroy();
        }

    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
