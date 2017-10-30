package com.meidusa.venus.backend.services.xml;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.toolkit.common.bean.BeanContextBean;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.toolkit.common.util.StringUtil;
import com.meidusa.venus.URL;
import com.meidusa.venus.annotations.PerformanceLevel;
import com.meidusa.venus.backend.VenusProtocol;
import com.meidusa.venus.backend.interceptor.Configurable;
import com.meidusa.venus.backend.interceptor.config.InterceptorConfig;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.backend.services.xml.config.*;
import com.meidusa.venus.backend.services.xml.support.BackendBeanContext;
import com.meidusa.venus.backend.services.xml.support.BackendBeanUtilsBean;
import com.meidusa.venus.digester.DigesterRuleParser;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.support.VenusConstants;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusBeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 基于XML配置服务管理类
 */
public class XmlFileServiceManager extends AbstractServiceManager implements InitializingBean,BeanFactoryPostProcessor,BeanFactoryAware,ApplicationContextAware{

    private static Logger logger = LoggerFactory.getLogger(XmlFileServiceManager.class);

    private Resource[] configFiles;

    private BeanFactory beanFactory;

    private BeanContext beanContext;

    private ApplicationContext applicationContext;

    /**
     * venus协议
     */
    private VenusProtocol venusProtocol;

    /**
     * 注册中心工厂
     */
    private VenusRegistryFactory venusRegistryFactory;

    public Resource[] getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(Resource... configFiles) {
        this.configFiles = configFiles;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //校验
        valid();

        //初始化
        init();
    }

    /**
     * 校验
     */
    void valid(){
        if(venusProtocol == null){
            throw new VenusConfigException("venus protocol not config.");
        }
    }

    /**
     * 初始化
     */
    void init(){
        //初始化上下文
        initContext();

        //初始化协议
        initProtocol();

        //初始化配置
        initConfiguration();
    }


    /**
     * 初始化上下文
     */
    void initContext(){
        if(applicationContext != null){
            VenusContext.getInstance().setApplicationContext(applicationContext);
        }
        beanContext = new BackendBeanContext(beanFactory);
        BeanContextBean.getInstance().setBeanContext(beanContext);
        VenusBeanUtilsBean.setInstance(new BackendBeanUtilsBean(new ConvertUtilsBean(), new PropertyUtilsBean(), beanContext));
        if(beanContext != null){
            VenusContext.getInstance().setBeanContext(beanContext);
        }
    }

    /**
     * 初始化协议，启动相应协议的监听
     */
    void initProtocol(){
        try {
            if(venusProtocol == null){
                throw new VenusConfigException("venus protocol not config.");
            }
            venusProtocol.setSrvMgr(this);
            venusProtocol.init();
        } catch (Exception e) {
            throw new RpcException("init protocol failed.",e);
        }
    }

    /**
     * 初始化配置
     */
    void initConfiguration(){
        //解析配置文件
        VenusServerConfig venusServerConfig = parseConfig();
        List<ServiceConfig> serviceConfigList = venusServerConfig.getServiceConfigs();
        //addMonitorServiceConfig(serviceConfigList);
        //addRegistryServiceConfig(serviceConfigList);
        Map<String, InterceptorMapping> interceptors = venusServerConfig.getInterceptors();
        Map<String, InterceptorStackConfig> interceptorStacks = venusServerConfig.getInterceptorStatcks();

        //初始化服务
        for (ServiceConfig serviceConfig : serviceConfigList) {
            initSerivce(serviceConfig,interceptors,interceptorStacks);
        }
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

                ConfigurableListableBeanFactory cbf = (ConfigurableListableBeanFactory) beanFactory;
                if(CollectionUtils.isNotEmpty(venusServerConfig.getServiceConfigs())){
                    for(ServiceConfig serviceConfig : venusServerConfig.getServiceConfigs()){
                        //FIXME disgester初始化不成功，注册到上下文
                        if(serviceConfig.getInstance() == null){
                            Object serivceInstance = beanFactory.getBean(serviceConfig.getType());
                            if(serivceInstance == null){
                                String errorMsg = String.format("init venus config instance %s error.",serviceConfig.getType().getName());
                                throw new VenusConfigException(errorMsg);
                            }
                            //cbf.registerResolvableDependency(serviceConfig.getType(), serivceInstance);
                            serviceConfig.setInstance(serivceInstance);
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
     * 注册到spring上下文
     * @param beanFactory
     * @param clz
     * @param object
     */
    /*
    void registeServiceBean(ConfigurableListableBeanFactory beanFactory,Class<?> clz,Object object){
        String beanName = clz.getSimpleName();
        BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(clz);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addIndexedArgumentValue(0, object);
        args.addIndexedArgumentValue(1, clz);
        beanDefinition.setConstructorArgumentValues(args);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
        reg.registerBeanDefinition(beanName, beanDefinition);
    }
    */

    /**
     * 添加服务监听实例配置
     * @param serviceConfigList
     */
    /*
    void addMonitorServiceConfig(List<ServiceConfig> serviceConfigList){
        ServiceConfig monitorServiceConfig = new ServiceConfig();
        monitorServiceConfig.setActive(true);
        //TODO 提取接口到公共部门，实例动态生成或注入
        //monitorServiceConfig.setType(MonitorService.class);
        //monitorServiceConfig.setInstance(new VenusMonitorService());
        //serviceConfigList.add(monitorServiceConfig);
    }
    */

    /**
     * 添加服务注册实例配置
     * @param serviceConfigList
     */
    /*
    void addRegistryServiceConfig(List<ServiceConfig> serviceConfigList) {
        ServiceConfig registerServiceConfig = new ServiceConfig();
        registerServiceConfig.setActive(true);
        //TODO 提取接口到公共部门，实例动态生成或注入
        //registerServiceConfig.setType(ServiceRegistry.class);
        //registerServiceConfig.setInstance(new VenusServiceRegistry(getServiceMappings()));
        //serviceConfigList.add(registerServiceConfig);
    }
    */

    /**
     * 初始化服务
     * @param serviceConfig
     * @param interceptors
     * @param interceptorStatcks
     */
    void initSerivce(ServiceConfig serviceConfig, Map<String, InterceptorMapping> interceptors, Map<String, InterceptorStackConfig> interceptorStatcks){
        //初始化服务stub
        Service service = initServiceStub(serviceConfig, interceptors, interceptorStatcks);

        //若开启注册中心，则注册服务
        if(venusRegistryFactory != null && venusRegistryFactory.getRegister() != null){
            registeService(serviceConfig, service,venusRegistryFactory.getRegister());
        }

    }

    /**
     * 初始化服务stub
     * @param serviceConfig
     * @param interceptors
     * @param interceptorStatcks
     * @return
     */
    protected Service initServiceStub(ServiceConfig serviceConfig, Map<String, InterceptorMapping> interceptors, Map<String, InterceptorStackConfig> interceptorStatcks) {
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
        if(StringUtils.isNotEmpty(serviceAnnotation.versionx())){
            service.setVersionx(serviceAnnotation.versionx());
        }
        service.setDescription(serviceAnnotation.description());

        //初始化endpoints
        Method[] methods = service.getType().getMethods();
        Multimap<String, Endpoint> endpoints = initEndpoinits(service,methods,serviceConfig,interceptors,interceptorStatcks);
        service.setEndpoints(endpoints);

        this.serviceMap.put(service.getName(), service);

        Map<String, Collection<Endpoint>> ends = service.getEndpoints().asMap();
        for (Map.Entry<String, Collection<Endpoint>> entry : ends.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                //TODO 初始化monitorService
                //MonitorRuntime.getInstance().initEndPoint(service.getName(), entry.getValue().iterator().next().getName());
            }
        }
        return service;
    }

    /**
     * 注册服务
     */
    void registeService(ServiceConfig serviceConfig, Service service, Register register){
        //获取服务注册url
        URL serviceRegisterUrl = parseRegisterUrl(serviceConfig,service);

        //注册服务
        register.registe(serviceRegisterUrl);
    }

    /**
     * 获取服务注册url
     * @param serviceConfig
     * @return
     */
    URL parseRegisterUrl(ServiceConfig serviceConfig, Service service){
        String application = VenusContext.getInstance().getApplication();
        String protocol = "venus";
        String serviceInterfaceName = serviceConfig.getType().getName();
        String serviceName = service.getName();
        String version = VenusConstants.VERSION_DEFAULT;
        if(StringUtils.isNotEmpty(service.getVersionx())){
            version = service.getVersionx();
        }
        String host = NetUtil.getLocalIp();
        String port = String.valueOf(venusProtocol.getPort());
        String methods = "sayHello[java.lang.String]";//TODO
        String registerUrl = String.format(
                "%s://%s/%s?version=%s&application=%s&host=%s&port=%s&methods=%s",
                protocol,
                serviceInterfaceName,
                serviceName,
                version,
                application,
                host,
                port,
                methods
        );
        URL url = URL.parse(registerUrl);
        return url;
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
                if (logger.isDebugEnabled()) {
                    logger.debug("add endpoint: " + ep.getService().getName() + "." + ep.getName());
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
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    public VenusProtocol getVenusProtocol() {
        return venusProtocol;
    }

    public void setVenusProtocol(VenusProtocol venusProtocol) {
        this.venusProtocol = venusProtocol;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public VenusRegistryFactory getVenusRegistryFactory() {
        return venusRegistryFactory;
    }

    public void setVenusRegistryFactory(VenusRegistryFactory venusRegistryFactory) {
        this.venusRegistryFactory = venusRegistryFactory;
    }
}
