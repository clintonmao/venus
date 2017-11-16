package com.meidusa.venus.registry;

import com.caucho.hessian.client.HessianProxyFactory;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.registry.service.RegisterService;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

/**
 * Venus注册中心工厂类，负责初始化注册中心
 * Created by Zhangzhihua on 2017/9/15.
 */
public class VenusRegistryFactory implements InitializingBean {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //注册中心读写超时时间,ms
    private static final int readTimeout = 3000;
    //注册中心连接超时时间,ms
    private static final int connectTimeout = 3000;

    /**
     * 注册中心地址，url地址
     */
    private String address;

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
        HessianProxyFactory proxyFactory = new HessianProxyFactory();
        proxyFactory.setReadTimeout(readTimeout);
        proxyFactory.setConnectTimeout(connectTimeout);
        try {
            RegisterService registerService = (RegisterService) proxyFactory.create(RegisterService.class, registerUrl);
            return registerService;
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("newHessianRegisterService error.",e);
            }
            return null;
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

}
