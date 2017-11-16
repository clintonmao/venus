package com.meidusa.venus.backend.services.xml;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.toolkit.common.bean.BeanContextBean;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.toolkit.common.util.StringUtil;
import com.meidusa.venus.Application;
import com.meidusa.venus.URL;
import com.meidusa.venus.annotations.PerformanceLevel;
import com.meidusa.venus.backend.VenusProtocol;
import com.meidusa.venus.backend.interceptor.Configurable;
import com.meidusa.venus.backend.interceptor.config.InterceptorConfig;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.backend.services.xml.config.*;
import com.meidusa.venus.backend.services.xml.support.BackendBeanContext;
import com.meidusa.venus.backend.services.xml.support.BackendBeanUtilsBean;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.support.VenusConstants;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusBeanUtilsBean;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 基于XML配置服务管理类
 */
public class XmlFileServiceManager extends AbstractServiceManager implements InitializingBean,BeanFactoryAware,ApplicationContextAware{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private Resource[] configFiles;

    private BeanFactory beanFactory;

    private BeanContext beanContext;

    private ApplicationContext applicationContext;

    private Application application;

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

    public XmlFileServiceManager(){
        Application.addServiceManager(this);
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
        VenusServerConfig venusServerConfig = parseServerConfig();
        List<ExportService> serviceConfigList = venusServerConfig.getExportServices();
        //addMonitorServiceConfig(serviceConfigList);
        //addRegistryServiceConfig(serviceConfigList);
        Map<String, InterceptorMapping> interceptors = venusServerConfig.getInterceptors();
        Map<String, InterceptorStackConfig> interceptorStacks = venusServerConfig.getInterceptorStatcks();

        //初始化服务
        for (ExportService serviceConfig : serviceConfigList) {
            initSerivce(serviceConfig,interceptors,interceptorStacks);
        }
    }

    /**
     * 解析配置文件
     * @return
     */
    private VenusServerConfig parseServerConfig() {
        VenusServerConfig allVenusServerConfig = new VenusServerConfig();
        List<ExportService> serviceConfigList = new ArrayList<ExportService>();
        Map<String, InterceptorMapping> interceptors = new HashMap<String, InterceptorMapping>();
        Map<String, InterceptorStackConfig> interceptorStacks = new HashMap<String, InterceptorStackConfig>();

        XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.processAnnotations(VenusServerConfig.class);
        xStream.processAnnotations(ExportService.class);

        for (Resource config : configFiles) {
            /*
            RuleSet ruleSet = new FromXmlRuleSet(this.getClass().getResource("venusServerRule.xml"), new DigesterRuleParser());
            Digester digester = new Digester();
            digester.addRuleSet(ruleSet);
             InputStream is = config.getInputStream();
            */
            try {
                VenusServerConfig venusServerConfig = (VenusServerConfig) xStream.fromXML(config.getURL());
                if(CollectionUtils.isEmpty(venusServerConfig.getExportServices())){
                    throw new VenusConfigException("not found service config.");
                }
                for(ExportService exportService:venusServerConfig.getExportServices()){
                    //接口名称
                    String interfaceType = exportService.getType();
                    try {
                        Class<?> interType = Class.forName(interfaceType);
                        exportService.setInterfaceType(interType);
                    } catch (ClassNotFoundException e) {
                       throw new VenusConfigException("not found service type class:" + interfaceType);
                    }
                    //引用实例
                    String refBeanName = exportService.getRef();
                    Object refBean = beanFactory.getBean(refBeanName);
                    if(refBean == null){
                        throw new VenusConfigException("ref bean not found:" + refBeanName);
                    }
                    exportService.setActive(true);
                    exportService.setInstance(refBean);
                }
                serviceConfigList.addAll(venusServerConfig.getExportServices());
                if(venusServerConfig.getInterceptors() != null){
                    interceptors.putAll(venusServerConfig.getInterceptors());
                }
                if(venusServerConfig.getInterceptorStatcks() != null){
                    interceptorStacks.putAll(venusServerConfig.getInterceptorStatcks());
                }
            } catch (Exception e) {
                throw new ConfigurationException("can not parser xml:" + config.getFilename(), e);
            }
        }

        allVenusServerConfig.setExportServices(serviceConfigList);
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
     * 初始化服务
     * @param serviceConfig
     * @param interceptors
     * @param interceptorStatcks
     */
    void initSerivce(ExportService serviceConfig, Map<String, InterceptorMapping> interceptors, Map<String, InterceptorStackConfig> interceptorStatcks){
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
    protected Service initServiceStub(ExportService serviceConfig, Map<String, InterceptorMapping> interceptors, Map<String, InterceptorStackConfig> interceptorStatcks) {
        //初始化service
        SingletonService service = new SingletonService();
        service.setType(serviceConfig.getInterfaceType());
        service.setInstance(serviceConfig.getInstance());
        Class<?> type = serviceConfig.getInterfaceType();
        type.cast(serviceConfig.getInstance());
        service.setActive(serviceConfig.isActive());
        service.setSupportVersionRange(serviceConfig.getSupportVersionRange());
        com.meidusa.venus.annotations.Service serviceAnnotation = type.getAnnotation(com.meidusa.venus.annotations.Service.class);
        if(serviceAnnotation == null){
        	throw new VenusConfigException("Service annotation not found in class="+type.getClass());
        }
        service.setAthenaFlag(serviceAnnotation.athenaFlag());
        if (!serviceAnnotation.name().isEmpty()) {
            service.setName(serviceAnnotation.name());
        } else {
            service.setName(type.getCanonicalName());
        }
        if(serviceAnnotation.version() != 0){
            service.setVersion(serviceAnnotation.version());
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
                //MonitorRuntime.getInstance().initEndPoint(service.getName(), entry.getValue().iterator().next().getName());
            }
        }
        return service;
    }

    /**
     * 注册服务
     */
    void registeService(ExportService serviceConfig, Service service, Register register){
        //获取服务注册url
        URL serviceRegisterUrl = parseRegisterUrl(serviceConfig,service);

        //注册服务
        try {
            register.registe(serviceRegisterUrl);
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("registe service failed,will retry.",e);
            }
        }
    }

    /**
     * 获取服务注册url
     * @param serviceConfig
     * @return
     */
    URL parseRegisterUrl(ExportService serviceConfig, Service service){
        String appName = application.getName();
        //String protocol = "venus";
        String serviceInterfaceName = serviceConfig.getInterfaceType().getName();
        String serviceName = service.getName();
        int version = VenusConstants.VERSION_DEFAULT;
        if(service.getVersion() != 0){
            version = service.getVersion();
        }
        String host = NetUtil.getLocalIp();
        String port = String.valueOf(venusProtocol.getPort());
        //获取方法定义列表
        String methodsDef = getMethodsDef(service);
        StringBuffer buf = new StringBuffer();
        buf.append("/").append(serviceInterfaceName);
        buf.append("/").append(serviceName);
        buf.append("?version=").append(String.valueOf(version));
        buf.append("&application=").append(appName);
        buf.append("&host=").append(host);
        buf.append("&port=").append(port);
        if(StringUtils.isNotEmpty(methodsDef)){
            buf.append("&methods=").append(methodsDef);
        }
        if(serviceConfig.getSupportVersionRange() != null){
            buf.append("&versionRange=").append(serviceConfig.getSupportVersionRange().toString());
        }
        String registerUrl = buf.toString();
        URL url = URL.parse(registerUrl);
        return url;
    }

    @Override
    public void destroy() throws Exception {
        //反注册
        if(venusRegistryFactory != null && venusRegistryFactory.getRegister() != null){
            Register register = venusRegistryFactory.getRegister();
            Set<URL> registeUrls = register.getRegisteUrls();
            if(CollectionUtils.isNotEmpty(registeUrls)){
                for(URL url:registeUrls){
                    try {
                        if(logger.isInfoEnabled()){
                            logger.info("unregiste url:{}.",url);
                        }
                        register.unregiste(url);
                    } catch (VenusRegisteException e) {
                        if(exceptionLogger.isErrorEnabled()){
                            String errorMsg = String.format("unregiste url:%s failed.",url);
                            exceptionLogger.error(errorMsg,e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取所有方法定义字符串
     * @param service
     * @return
     */
    String getMethodsDef(Service service){
        StringBuffer buf = new StringBuffer();
        Multimap<String, Endpoint> endpointMultimap = service.getEndpoints();
        Collection<Endpoint> endpoints = endpointMultimap.values();
        if(CollectionUtils.isNotEmpty(endpoints)){
            int i = 0;
            for(Endpoint endpoint:endpoints){
                String methodDef = getMethodDef(endpoint);
                buf.append(methodDef);
                if(i < (endpoints.size()-1)){
                    buf.append(";");
                }
                i++;
            }
        }
        String methodsDef = buf.toString();
        return methodsDef;
    }

    /**
     * 获取方法定义字符串
     * @param endpoint
     * @return
     */
    String getMethodDef(Endpoint endpoint){
        StringBuffer buf = new StringBuffer();
        buf.append(endpoint.getMethod().getName());
        buf.append("[");
        Class[] paramClzArr = endpoint.getMethod().getParameterTypes();
        if(paramClzArr != null && paramClzArr.length > 0){
            int i = 0;
            for(Class paramClz:paramClzArr){
                buf.append(paramClz.getName());
                if(i < (paramClzArr.length - 1)){
                    buf.append(",");
                }
                i++;
            }
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * 初始化endpoints
     * @param service
     * @param methods
     * @param exportService
     * @param interceptors
     * @param interceptorStatcks
     */
    Multimap<String, Endpoint> initEndpoinits(Service service, Method[] methods, ExportService exportService, Map<String, InterceptorMapping> interceptors, Map<String, InterceptorStackConfig> interceptorStatcks){
        Multimap<String, Endpoint> endpointMultimap = HashMultimap.create();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(com.meidusa.venus.annotations.Endpoint.class)) {
                continue;
            }
            Endpoint endpoint = loadEndpoint(method);

            if(MapUtils.isNotEmpty(exportService.getEndpointConfigMap())){
                ExportServiceConfig endpointConfig = exportService.getEndpointConfig(endpoint.getName());

                String id = (endpointConfig == null ? exportService.getInterceptorStack() : endpointConfig.getInterceptorStack());
                Map<String, InterceptorConfig> interceptorConfigs = null;
                if (endpointConfig != null) {
                    endpoint.setActive(endpointConfig.isActive());
                    if (endpointConfig.getTimeWait() > 0) {
                        endpoint.setTimeWait(endpointConfig.getTimeWait());
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

                    loadInterceptors(interceptorStatcks, interceptors, id, list, interceptorConfigs, service.getType(), endpoint.getName());
                    stack.setInterceptors(list);
                    endpoint.setInterceptorStack(stack);
                }

                //设置打印级别
                //设置日志打印级别
                PerformanceLogger pLogger = null;
                if (endpointConfig != null) {
                    pLogger = endpointConfig.getPerformanceLogger();
                }
                if (pLogger == null) {
                    PerformanceLevel pLevel = AnnotationUtil.getAnnotation(endpoint.getMethod().getAnnotations(), PerformanceLevel.class);
                    if (pLevel != null) {
                        pLogger = new PerformanceLogger();
                        pLogger.setError(pLevel.error());
                        pLogger.setInfo(pLevel.info());
                        pLogger.setWarn(pLevel.warn());
                        pLogger.setPrintParams(pLevel.printParams());
                        pLogger.setPrintResult(pLevel.printResult());
                    }
                }
                endpoint.setPerformanceLogger(pLogger);
            }

            endpoint.setService(service);
            endpointMultimap.put(endpoint.getName(), endpoint);
        }
        return endpointMultimap;
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

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }
}
