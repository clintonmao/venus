package com.meidusa.venus.client;

import com.meidusa.venus.RpcException;
import com.meidusa.venus.VenusContext;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterContext;
import com.meidusa.venus.registry.RegisterService;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.service.registry.HostPort;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
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
    private String url;

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
        if(StringUtils.isEmpty(url)){
            throw new VenusConfigException("url not allow empty.");
        }
    }

    /**
     * 初始化注册中心
     */
    void initRegister(){
        //根据配置创建registerService实例，本地依赖或venus远程依赖
        RegisterService registerService = null;
        if (isInjvm(url)) {
            try {
                registerService = (RegisterService) Class
                        .forName("com.meidusa.venus.registry.service.MysqlRegisterService").newInstance();
            } catch (Exception e) {
                throw new RpcException(e);
            }
        } else {
            //TODO venus远程依赖
            throw new RpcException("todo impl.");
        }
        if(registerService == null){
            throw new RpcException("init register service failed.");
        }
        MysqlRegister register = new MysqlRegister(registerService);
        RegisterContext.getInstance().setRegister(register);
    }

    /**
     * 判断是否本地依赖RegisterService
     * @param url
     * @return
     */
    boolean isInjvm(String url){
        return url.startsWith("injvm:");
    }

    /**
     * 根据注册中心创建远程注册中心引用
     * @param registerUrl
     * @return
     */
    RegisterService createVenusRegisterService(String registerUrl){
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

        HostPort hp = hosts.get(new Random().nextInt(hosts.size()));
        SimpleServiceFactory ssf = new SimpleServiceFactory(hp.getHost(), hp.getPort());
        ssf.setCoTimeout(60000);
        ssf.setSoTimeout(60000);
        RegisterService registerService = ssf.getService(RegisterService.class);
        return registerService;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
