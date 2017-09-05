package com.meidusa.venus.backend.services.xml;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.toolkit.common.bean.BeanContextBean;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.toolkit.common.util.StringUtil;
import com.meidusa.venus.URL;
import com.meidusa.venus.annotations.PerformanceLevel;
import com.meidusa.venus.annotations.util.AnnotationUtil;
import com.meidusa.venus.backend.interceptor.Configurable;
import com.meidusa.venus.backend.interceptor.config.InterceptorConfig;
import com.meidusa.venus.backend.invoker.support.CodeMapScanner;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.backend.services.xml.bean.*;
import com.meidusa.venus.backend.services.xml.support.BackendBeanContext;
import com.meidusa.venus.backend.services.xml.support.BackendBeanUtilsBean;
import com.meidusa.venus.backend.services.xml.support.VenusServiceRegistry;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;
import com.meidusa.venus.digester.DigesterRuleParser;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.monitor.athena.reporter.AthenaExtensionResolver;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterService;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.service.monitor.MonitorRuntime;
import com.meidusa.venus.service.monitor.MonitorService;
import com.meidusa.venus.service.monitor.VenusMonitorService;
import com.meidusa.venus.service.registry.HostPort;
import com.meidusa.venus.service.registry.ServiceRegistry;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusBeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 基于XML配置服务管理类
 */
public class XmlFileServiceManager extends AbstractServiceManager implements InitializingBean, BeanFactoryAware {

    private static Logger logger = LoggerFactory.getLogger(XmlFileServiceManager.class);

    private Resource[] configFiles;

    private BeanFactory beanFactory;

    private BeanContext beanContext;

    private Register register;

    public Resource[] getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(Resource... configFiles) {
        this.configFiles = configFiles;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        beanContext = new BackendBeanContext(beanFactory);
        BeanContextBean.getInstance().setBeanContext(beanContext);
        VenusBeanUtilsBean.setInstance(new BackendBeanUtilsBean(new ConvertUtilsBean(), new PropertyUtilsBean(), beanContext));
        AthenaExtensionResolver.getInstance().resolver();
        CodeMapScanner.getCodeMap();

        //解析配置文件
        VenusServerConfig venusServerConfig = parseConfig();

        List<ServiceConfig> serviceConfigList = venusServerConfig.getServiceConfigs();
        addMonitorServiceConfig(serviceConfigList);
        addRegistryServiceConfig(serviceConfigList);

        Map<String, InterceptorMapping> interceptors = venusServerConfig.getInterceptors();
        Map<String, InterceptorStackConfig> interceptorStacks = venusServerConfig.getInterceptorStatcks();

        //初始化服务
        initServices(serviceConfigList, interceptors, interceptorStacks);

        //注册服务 TODO 改为依次实例化、注册
        registe(serviceConfigList);
    }

    /**
     * 服务注册
     */
    void registe(List<ServiceConfig> serviceConfigList){
        if(CollectionUtils.isNotEmpty(serviceConfigList)){
            Register register = getRegister();
            for (ServiceConfig serviceConfig : serviceConfigList) {
                URL registerUrl = getURL(serviceConfig);
                register.registe(registerUrl);
            }
        }
    }

    /**
     * 获取注册url
     * @param serviceConfig
     * @return
     */
    URL getURL(ServiceConfig serviceConfig){
        String strUrl = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0&host=%s&port=16800&methods=sayHello[java.lang.String]";
        strUrl = String.format(strUrl, NetUtil.getLocalIp());
        logger.info("registerUrl:%",strUrl);
        URL url = URL.parse(strUrl);
        return url;
    }

    /**
     * 获取注册中心
     * @return
     */
    Register getRegister(){
        if(register != null){
            return register;
        }
        register = MysqlRegister.getInstance(true,null);
        return register;
    }

    /**
     * 获取注册中心远程服务
     * @param registerUrl
     * @return
     */
    RegisterService getRegisterService(String registerUrl){
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

    /**
     * 解析配置文件
     * @return
     */
    private VenusServerConfig parseConfig() {
        VenusServerConfig allVenusServerConfig = new VenusServerConfig();

        List<ServiceConfig> serviceConfigList = new ArrayList<ServiceConfig>();
        Map<String, InterceptorMapping> interceptors = new HashMap<String, InterceptorMapping>();
        Map<String, InterceptorStackConfig> interceptorStacks = new HashMap<String, InterceptorStackConfig>();

        for (Resource config : configFiles) {
            RuleSet ruleSet = new FromXmlRuleSet(this.getClass().getResource("venusServerRule.xml"), new DigesterRuleParser());
            Digester digester = new Digester();
            digester.addRuleSet(ruleSet);

            try {
                InputStream is = config.getInputStream();
                VenusServerConfig venusServerConfig = (VenusServerConfig) digester.parse(is);

                //TODO disgester初始化不成功，临时设置instance
                if(CollectionUtils.isNotEmpty(venusServerConfig.getServiceConfigs())){
                    for(ServiceConfig serviceConfig : venusServerConfig.getServiceConfigs()){
                        if(serviceConfig.getInstance() == null){
                            if(beanFactory != null){
                                Object object = beanFactory.getBean("helloService");
                                serviceConfig.setInstance(object);
                            }
                        }
                    }

                }
                serviceConfigList.addAll(venusServerConfig.getServiceConfigs());
                interceptors.putAll(venusServerConfig.getInterceptors());
                interceptorStacks.putAll(venusServerConfig.getInterceptorStatcks());
            } catch (Exception e) {
                throw new ConfigurationException("can not parser xml:" + config.getFilename(), e);
            }
        }

        allVenusServerConfig.setServiceConfigs(serviceConfigList);
        allVenusServerConfig.setInterceptors(interceptors);
        allVenusServerConfig.setInterceptorStatcks(interceptorStacks);
        return allVenusServerConfig;
    }

    /**
     * 添加服务监听实例配置
     * @param serviceConfigList
     */
    void addMonitorServiceConfig(List<ServiceConfig> serviceConfigList){
        ServiceConfig monitorServiceConfig = new ServiceConfig();
        monitorServiceConfig.setActive(true);
        monitorServiceConfig.setType(MonitorService.class);
        monitorServiceConfig.setInstance(new VenusMonitorService());
        serviceConfigList.add(monitorServiceConfig);
    }

    /**
     * 添加服务注册实例配置
     * @param serviceConfigList
     */
    void addRegistryServiceConfig(List<ServiceConfig> serviceConfigList) {
        ServiceConfig registerServiceConfig = new ServiceConfig();
        registerServiceConfig.setActive(true);
        registerServiceConfig.setType(ServiceRegistry.class);
        registerServiceConfig.setInstance(new VenusServiceRegistry(getServiceMappings()));
        serviceConfigList.add(registerServiceConfig);
    }

    /**
     * 初始化所有服务
     * @param serviceConfigList
     * @param interceptors
     * @param interceptorStacks
     */
    private void initServices(List<ServiceConfig> serviceConfigList, Map<String, InterceptorMapping> interceptors, Map<String, InterceptorStackConfig> interceptorStacks) {
        for (ServiceConfig config : serviceConfigList) {
            //初始化服务
            Service service = initService(config, interceptors, interceptorStacks);

            Map<String, Collection<Endpoint>> ends = service.getEndpoints().asMap();
            for (Map.Entry<String, Collection<Endpoint>> entry : ends.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    MonitorRuntime.getInstance().initEndPoint(service.getName(), entry.getValue().iterator().next().getName());
                }
            }
        }
    }

    /**
     * 初始化服务
     * @param serviceConfig
     * @param interceptors
     * @param interceptorStatcks
     * @return
     */
    protected Service initService(ServiceConfig serviceConfig, Map<String, InterceptorMapping> interceptors, Map<String, InterceptorStackConfig> interceptorStatcks) {
        //初始化service
        SingletonService service = new SingletonService();
        service.setType(serviceConfig.getType());
        service.setInstance(serviceConfig.getInstance());
        Class<?> type = serviceConfig.getType();
        type.cast(serviceConfig.getInstance());
        service.setActive(serviceConfig.isActive());
        service.setVersionRange(serviceConfig.getVersionRange());

        com.meidusa.venus.annotations.Service serviceAnnotation = type.getAnnotation(com.meidusa.venus.annotations.Service.class);
        
        if(serviceAnnotation == null){
        	logger.error("Service annotation not found in class="+type.getClass());
        	throw new VenusConfigException("Service annotation not found in class="+type.getClass());
        }

        service.setAthenaFlag(serviceAnnotation.athenaFlag());
        if (!serviceAnnotation.name().isEmpty()) {
            service.setName(serviceAnnotation.name());
        } else {
            service.setName(type.getCanonicalName());
        }
        service.setDescription(serviceAnnotation.description());

        //初始化endpoints
        Method[] methods = service.getType().getMethods();
        Multimap<String, Endpoint> endpoints = initEndpoinits(service,methods,serviceConfig,interceptors,interceptorStatcks);
        service.setEndpoints(endpoints);

        this.services.put(service.getName(), service);

        // register to resolvable dependency container
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            ConfigurableListableBeanFactory cbf = (ConfigurableListableBeanFactory) beanFactory;
            cbf.registerResolvableDependency(service.getType(), service.getInstance());
        }
        return service;
    }

    /**
     * 初始化endpoints
     * @param service
     * @param methods
     * @param serviceConfig
     * @param interceptors
     * @param interceptorStatcks
     */
    Multimap<String, Endpoint> initEndpoinits(Service service,Method[] methods,ServiceConfig serviceConfig,Map<String, InterceptorMapping> interceptors, Map<String, InterceptorStackConfig> interceptorStatcks){
        Multimap<String, Endpoint> endpoints = HashMultimap.create();
        for (Method method : methods) {
            if (method.isAnnotationPresent(com.meidusa.venus.annotations.Endpoint.class)) {
                Endpoint ep = loadEndpoint(method);

                EndpointConfig endpointConfig = serviceConfig.getEndpointConfig(ep.getName());

                String id = (endpointConfig == null ? serviceConfig.getInterceptorStack() : endpointConfig.getInterceptorStack());
                Map<String, InterceptorConfig> interceptorConfigs = null;
                if (endpointConfig != null) {
                    ep.setActive(endpointConfig.isActive());
                    if (endpointConfig.getTimeWait() > 0) {
                        ep.setTimeWait(endpointConfig.getTimeWait());
                    }
                    interceptorConfigs = endpointConfig.getInterceptorConfigs();
                }

                // ignore 'null' or empty filte stack name
                if (!StringUtil.isEmpty(id) && !"null".equalsIgnoreCase(id)) {
                    List<InterceptorMapping> list = new ArrayList<InterceptorMapping>();
                    InterceptorStackConfig stackConfig = interceptorStatcks.get(id);
                    if (stackConfig == null) {
                        throw new VenusConfigException("filte stack not found with name=" + id);
                    }
                    InterceptorStack stack = new InterceptorStack();
                    stack.setName(stackConfig.getName());

                    loadInterceptors(interceptorStatcks, interceptors, id, list, interceptorConfigs, service.getType(), ep.getName());
                    stack.setInterceptors(list);
                    ep.setInterceptorStack(stack);
                }

                PerformanceLogger pLogger = null;
                if (endpointConfig != null) {
                    pLogger = endpointConfig.getPerformanceLogger();
                }

                if (pLogger == null) {
                    PerformanceLevel pLevel = AnnotationUtil.getAnnotation(ep.getMethod().getAnnotations(), PerformanceLevel.class);
                    if (pLevel != null) {
                        pLogger = new PerformanceLogger();
                        pLogger.setError(pLevel.error());
                        pLogger.setInfo(pLevel.info());
                        pLogger.setWarn(pLevel.warn());
                        pLogger.setPrintParams(pLevel.printParams());
                        pLogger.setPrintResult(pLevel.printResult());
                    }
                }

                //TODO 确认代码用途及替换方案
                //ep.setPerformanceLogger(pLogger);

                ep.setService(service);
                if (logger.isInfoEnabled()) {
                    logger.info("Add Endpoint: " + ep.getService().getName() + "." + ep.getName());
                }
                endpoints.put(ep.getName(), ep);
            }
        }
        return endpoints;
    }

    protected void loadInterceptors(Map<String, InterceptorStackConfig> interceptorStatcks, Map<String, InterceptorMapping> interceptors, String id,
            List<InterceptorMapping> result, Map<String, InterceptorConfig> configs, Class<?> clazz, String ep) throws VenusConfigException {
        InterceptorStackConfig stackConfig = interceptorStatcks.get(id);
        if (stackConfig == null) {
            throw new VenusConfigException("filte stack not found with name=" + id);
        }
        for (Object s : stackConfig.getInterceptors()) {
            if (s instanceof InterceptorRef) {
                InterceptorMapping mapping = interceptors.get(((InterceptorRef) s).getName());
                if (mapping == null) {
                    throw new VenusConfigException("filte not found with name=" + s);
                }
                Interceptor interceptor = mapping.getInterceptor();
                if (configs != null) {
                    InterceptorConfig config = configs.get(mapping.getName());
                    if (config != null) {
                        if (interceptor instanceof Configurable) {
                            ((Configurable) interceptor).processConfig(clazz, ep, config);
                        }
                    }
                }
                result.add(mapping);
            } else if (s instanceof InterceptorStackRef) {
                loadInterceptors(interceptorStatcks, interceptors, ((InterceptorStackRef) s).getName(), result, configs, clazz, ep);
            } else {
                throw new VenusConfigException("unknow filte config with name=" + s);
            }
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
