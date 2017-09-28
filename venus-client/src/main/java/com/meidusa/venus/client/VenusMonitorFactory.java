package com.meidusa.venus.client;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.VenusContext;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;
import com.meidusa.venus.client.factory.xml.support.ServiceFactoryBean;
import com.meidusa.venus.exception.VenusConfigException;
import com.saic.framework.athena.configuration.ClientConfigManager;
import com.saic.framework.athena.configuration.DefaultClientConfigManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * venus监控工厂类
 * Created by Zhangzhihua on 2017/9/11.
 */
public class VenusMonitorFactory implements InitializingBean, BeanFactoryPostProcessor {

    private static Logger logger = LoggerFactory.getLogger(VenusMonitorFactory.class);

    /**
     * 注册中心url地址，injvm或者远程注册中心服务地址
     */
    private String url;

    private boolean enable;

    /**
     * athena上报配置管理
     */
    private ClientConfigManager clientConfigManager;

    /**
     * athena上报服务接口
     */
    private AthenaDataService athenaDataService;

    @Override
    public void afterPropertiesSet() throws Exception {
        valid();

        init();
    }

    /**
     * 校验
     */
    void valid(){
        if(StringUtils.isEmpty(url)){
            throw new VenusConfigException("url not allow empty.");
        }
    }

    void init(){
        //初始化configManager
        initConfigManager();

        //初始化athenaDataService
        initAthenaDataService(url);
    }


    /**
     * 初始化athena配置管理信息
     */
    void initConfigManager(){
        DefaultClientConfigManager clientConfigManager = new DefaultClientConfigManager();
        String appName = VenusContext.getInstance().getApplication();
        if(StringUtils.isEmpty(appName)){
            throw new VenusConfigException("application not inited.");
        }
        clientConfigManager.setAppName(appName);
        clientConfigManager.setMonitorEnabled(enable);
        this.clientConfigManager = clientConfigManager;
    }

    /**
     * 初始化athenaDataService
     * @param url
     */
    void initAthenaDataService(String url){
        String[] arr = url.split(":");
        SimpleServiceFactory factory = new SimpleServiceFactory(arr[0],Integer.parseInt(arr[1]));
        factory.setSoTimeout(16 * 1000);//可选,默认 15秒
        factory.setCoTimeout(5 * 1000);//可选,默认5秒
        AthenaDataService athenaDataService = factory.getService(AthenaDataService.class);
        if(athenaDataService == null){
            throw new RpcException("init athenaDataService failed.");
        }
        this.athenaDataService = athenaDataService;
        AthenaContext.getInstance().setAthenaDataService(athenaDataService);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //注册configManager
        registeClientConfigManager(beanFactory);

        //注册athenaDataService到上下文
        registeAthenaDataService(beanFactory);
    }

    /**
     * 注册configManager
     * @param beanFactory
     */
    void registeClientConfigManager(ConfigurableListableBeanFactory beanFactory){
        String beanName = clientConfigManager.getClass().getSimpleName();
        BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ServiceFactoryBean.class);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addIndexedArgumentValue(0, this.clientConfigManager);
        args.addIndexedArgumentValue(1, ClientConfigManager.class);
        beanDefinition.setConstructorArgumentValues(args);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
        reg.registerBeanDefinition(beanName, beanDefinition);
    }

    /**
     * 注册athenaDataService
     * @param beanFactory
     */
    void registeAthenaDataService(ConfigurableListableBeanFactory beanFactory){
        String beanName = athenaDataService.getClass().getSimpleName();
        BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ServiceFactoryBean.class);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addIndexedArgumentValue(0, this.athenaDataService);
        args.addIndexedArgumentValue(1, AthenaDataService.class);
        beanDefinition.setConstructorArgumentValues(args);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
        reg.registerBeanDefinition(beanName, beanDefinition);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public AthenaDataService getAthenaDataService() {
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }
}
