package com.meidusa.venus.backend.services.xml;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.meidusa.fastjson.JSON;
import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.toolkit.common.bean.BeanContextBean;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.venus.URL;
import com.meidusa.venus.VenusApplication;
import com.meidusa.venus.backend.VenusProtocol;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.backend.services.xml.config.*;
import com.meidusa.venus.backend.services.xml.support.BackendBeanContext;
import com.meidusa.venus.backend.services.xml.support.BackendBeanUtilsBean;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.monitor.VenusMonitorFactory;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusBeanUtilsBean;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
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

    /**
     * 应用配置
     */
    private VenusApplication venusApplication;

    /**
     * venus协议
     */
    private VenusProtocol venusProtocol;

    /**
     * 注册中心工厂
     */
    private VenusRegistryFactory venusRegistryFactory;

    /**
     * 监听中心配置
     */
    private VenusMonitorFactory venusMonitorFactory;

    public Resource[] getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(Resource... configFiles) {
        this.configFiles = configFiles;
    }

    public XmlFileServiceManager(){
        VenusApplication.addServiceManager(this);
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
        if(venusApplication == null){
            throw new VenusConfigException("venusApplication not config.");
        }
        if(venusProtocol == null){
            throw new VenusConfigException("venusProtocol not config.");
        }
        if(venusRegistryFactory == null || venusRegistryFactory.getRegister() == null){
            if(logger.isWarnEnabled()){
                logger.warn("venusRegistryFactory not enabled,will disable service registe.");
            }
        }
        if(venusMonitorFactory == null){
            if(logger.isWarnEnabled()){
                logger.warn("venusMonitorFactory not enabled,will disable monitor reporte.");
            }
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
            venusProtocol.setSrvMgr(this);
            venusProtocol.init();
        } catch (Exception e) {
            throw new RuntimeException("init protocol failed.",e);
        }
    }

    /**
     * 初始化配置
     */
    void initConfiguration(){
        //解析配置文件
        VenusServerConfig venusServerConfig = parseServerConfig();
        List<ExportService> exportServiceList = venusServerConfig.getExportServices();

        //初始化服务
        if(CollectionUtils.isEmpty(exportServiceList)){
            if(logger.isWarnEnabled()){
                logger.warn("not config export services.");
            }
            return;
        }
        for (ExportService exportService : exportServiceList) {
            initSerivce(exportService);
        }
    }

    /**
     * 解析配置文件
     * @return
     */
    private VenusServerConfig parseServerConfig() {
        //所有导出的beans
        Map<String,String> exportBeanMap = new HashMap<String,String>();
        //所有导出服务配置
        VenusServerConfig allVenusServerConfig = new VenusServerConfig();
        List<ExportService> allExportServiceList = new ArrayList<ExportService>();
        allVenusServerConfig.setExportServices(allExportServiceList);

        XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.processAnnotations(VenusServerConfig.class);
        xStream.processAnnotations(ExportService.class);
        xStream.processAnnotations(InterceptorDef.class);

        for (Resource configFile : configFiles) {
            try {
                VenusServerConfig venusServerConfig = (VenusServerConfig) xStream.fromXML(configFile.getURL());
                //初始化interceptors
                Map<String,InterceptorDef> interceptorDefMap = new HashMap<>();
                if(CollectionUtils.isNotEmpty(venusServerConfig.getInterceptorDefList())){
                    for(InterceptorDef interceptorDef:venusServerConfig.getInterceptorDefList()){
                        String interceptorClassName = interceptorDef.getClazz();
                        try {
                            Object obj = Class.forName(interceptorClassName).newInstance();
                            if(!(obj instanceof Interceptor)){
                                throw new ConfigurationException("init inteceptor failed:" + interceptorClassName + ",not interceptor class.");
                            }
                            interceptorDef.setInterceptor((Interceptor)obj);
                        } catch (Exception e) {
                            throw new ConfigurationException("init inteceptor failed:" + interceptorClassName,e);
                        }
                        interceptorDefMap.put(interceptorDef.getName(),interceptorDef);
                    }
                }

                //初始化exporte service
                if(CollectionUtils.isEmpty(venusServerConfig.getExportServices())){
                    throw new VenusConfigException("not found service config.");
                }
                for(ExportService exportService:venusServerConfig.getExportServices()){
                    //接口名称
                    String serviceInterfaceName = exportService.getType();
                    if (serviceInterfaceName == null) {
                        throw new VenusConfigException("Service type can not be null:" + configFile);
                    }
                    Class<?> serviceInterface = null;
                    try {
                        serviceInterface = Class.forName(serviceInterfaceName);
                        exportService.setServiceInterface(serviceInterface);
                    } catch (ClassNotFoundException e) {
                       throw new VenusConfigException("service interface class not found:" + serviceInterfaceName);
                    }
                    //引用实例
                    String refBeanName = exportService.getRef();
                    try {
                        Object refBean = beanFactory.getBean(refBeanName);
                        exportService.setActive(true);
                        exportService.setInstance(refBean);
                        exportBeanMap.put(serviceInterfaceName,refBean.getClass() + "@" + refBean.hashCode());
                    } catch (BeansException e) {
                        throw new ConfigurationException("ref bean not found:" + refBeanName,e);
                    }
                    //服务定义
                    com.meidusa.venus.annotations.Service serviceAnno = serviceInterface.getAnnotation(com.meidusa.venus.annotations.Service.class);
                    if(serviceAnno == null){
                        throw new VenusConfigException(String.format("service %s service annotation not declare",serviceInterface.getName()));
                    }
                    String serviceName = serviceAnno.name();
                    if(StringUtils.isEmpty(serviceName)){
                        serviceName = serviceInterface.getCanonicalName();
                    }
                    exportService.setServiceName(serviceName);
                    exportService.setVersion(serviceAnno.version());
                    exportService.setAthenaFlag(serviceAnno.athenaFlag());
                    exportService.setDescription(serviceAnno.description());
                    //interceptor引用
                    String interceptors = exportService.getInterceptors();
                    if(StringUtils.isNotEmpty(interceptors)){
                        String[] inters = interceptors.trim().split(",");
                        if(inters != null && inters.length > 0){
                            List<Interceptor> interceptorList = new ArrayList<>();
                            for(String inter:inters){
                                if(interceptorDefMap.get(inter) == null){
                                    throw new VenusConfigException("interceptor:" + inter + " not defined.");
                                }
                                InterceptorDef  interceptorDef = interceptorDefMap.get(inter);
                                interceptorList.add(interceptorDef.getInterceptor());
                            }
                            exportService.setInterceptorList(interceptorList);
                        }
                    }
                }

                //添加到all列表
                allExportServiceList.addAll(venusServerConfig.getExportServices());
            } catch (Exception e) {
                throw new ConfigurationException("parser venus server config failed:" + configFile.getFilename(), e);
            }
        }

        if(logger.isInfoEnabled()){
            logger.info("##########export beans###########:\n{}.",JSON.toJSONString(exportBeanMap,true));
        }
        return allVenusServerConfig;
    }

    /**
     * 初始化服务
     * @param exportService
     */
    void initSerivce(ExportService exportService){
        //初始化服务stub
        Service service = initServiceStub(exportService);

        //若开启注册中心，则注册服务
        if(isNeedRegiste(exportService)){
            try {
                registeService(exportService, service);
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("registe service failed,will retry.",e);
                }
            }
        }
    }

    /**
     * 初始化服务stub
     * @param exportService
     * @return
     */
    protected Service initServiceStub(ExportService exportService) {
        if(logger.isInfoEnabled()){
            logger.info("init service stub:{}.",exportService.getServiceInterface().getName());
        }
        //初始化service
        SingletonService service = new SingletonService();
        service.setType(exportService.getServiceInterface());
        service.setInstance(exportService.getInstance());
        Class<?> serviceInterface = exportService.getServiceInterface();
        serviceInterface.cast(exportService.getInstance());
        service.setName(exportService.getServiceName());
        service.setVersion(exportService.getVersion());
        service.setSupportVersionRange(exportService.getSupportVersionRange());
        service.setAthenaFlag(exportService.isAthenaFlag());
        service.setDescription(exportService.getDescription());
        service.setActive(exportService.isActive());

        //初始化endpoints
        Method[] methods = service.getType().getMethods();
        Multimap<String, Endpoint> endpoints = initEndpoinits(service,methods,exportService);
        service.setEndpoints(endpoints);

        this.serviceMap.put(exportService.getServiceName(), service);
        return service;
    }

    /**
     * 判断是否需要注册服务
     * @param exportService
     * @return
     */
    boolean isNeedRegiste(ExportService exportService){
        return venusRegistryFactory != null && venusRegistryFactory.getRegister() != null;
    }

    /**
     * 注册服务
     */
    void registeService(ExportService exportService, Service service){
        String appName = venusApplication.getName();
        //String protocol = "venus";
        String serviceInterfaceName = exportService.getServiceInterface().getName();
        String serviceName = exportService.getServiceName();
        int version = exportService.getVersion();
        String host = NetUtil.getLocalIp();
        String port = String.valueOf(venusProtocol.getPort());

        StringBuffer buf = new StringBuffer();
        buf.append("/").append(serviceInterfaceName);
        buf.append("/").append(serviceName);
        buf.append("?version=").append(String.valueOf(version));
        buf.append("&application=").append(appName);
        buf.append("&host=").append(host);
        buf.append("&port=").append(port);
        //获取方法定义列表
        String methodsDef = getMethodsDefOfService(service);
        if(StringUtils.isNotEmpty(methodsDef)){
            buf.append("&methods=").append(methodsDef);
        }
        //获取endpoint定义列表
        String endpointNames = getEndpointMethodsDefOfService(service);
        if(StringUtils.isNotEmpty(endpointNames)){
            buf.append("&endpoints=").append(endpointNames);
        }
        if(exportService.getSupportVersionRange() != null){
            buf.append("&versionRange=").append(exportService.getSupportVersionRange().toString());
        }
        String registerUrl = buf.toString();
        URL url = URL.parse(registerUrl);

        //注册服务
        venusRegistryFactory.getRegister().registe(url);
    }

    @Override
    public void destroy() throws Exception {
        //反注册
        if(venusRegistryFactory != null && venusRegistryFactory.getRegister() != null){
            Register register = venusRegistryFactory.getRegister();
            if(register != null){
                register.destroy();
            }
        }
    }

    /**
     * 获取服务所有方法定义字符串
     * @param service
     * @return
     */
    String getMethodsDefOfService(Service service){
        StringBuffer buf = new StringBuffer();
        Multimap<String, Endpoint> endpointMultimap = service.getEndpoints();
        Collection<Endpoint> endpoints = endpointMultimap.values();
        if(CollectionUtils.isNotEmpty(endpoints)){
            int i = 0;
            for(Endpoint endpoint:endpoints){
                String methodDef = getMethodDefOfEndpoint(endpoint);
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
    String getMethodDefOfEndpoint(Endpoint endpoint){
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
     * 获取endpoinit names
     * @param service
     * @return
     */
    String getEndpointMethodsDefOfService(Service service){
        StringBuffer buf = new StringBuffer();
        Multimap<String, Endpoint> endpointMultimap = service.getEndpoints();
        Collection<Endpoint> endpoints = endpointMultimap.values();
        if(CollectionUtils.isNotEmpty(endpoints)){
            int i = 0;
            for(Endpoint endpoint:endpoints){
                String endpointName = endpoint.getName();
                buf.append(endpointName);
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
     * 初始化endpoints
     * @param service
     * @param methods
     * @param exportService
     */
    Multimap<String, Endpoint> initEndpoinits(Service service, Method[] methods, ExportService exportService){
        Multimap<String, Endpoint> endpointMultimap = HashMultimap.create();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(com.meidusa.venus.annotations.Endpoint.class)) {
                continue;
            }

            //初始化endpoinit
            Endpoint endpoint = initEndpoint(method);
            if(CollectionUtils.isNotEmpty(exportService.getInterceptorList())){
                endpoint.setInterceptorList(exportService.getInterceptorList());
            }
            endpoint.setService(service);
            endpointMultimap.put(endpoint.getName(), endpoint);
        }
        return endpointMultimap;
    }

    /*
    protected void loadInterceptors(Map<String, InterceptorStackConfig> interceptorStatcks, Map<String, InterceptorDef> interceptors, String id,
                                    List<InterceptorDef> result, Map<String, InterceptorConfig> configs, Class<?> clazz, String ep) throws VenusConfigException {
        InterceptorStackConfig stackConfig = interceptorStatcks.get(id);
        if (stackConfig == null) {
            throw new VenusConfigException("filte stack not found with name=" + id);
        }
        for (Object s : stackConfig.getInterceptors()) {
            if (s instanceof InterceptorRef) {
                InterceptorDef mapping = interceptors.get(((InterceptorRef) s).getName());
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
    */

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

    public VenusApplication getVenusApplication() {
        return venusApplication;
    }

    public void setVenusApplication(VenusApplication venusApplication) {
        this.venusApplication = venusApplication;
    }

    public VenusMonitorFactory getVenusMonitorFactory() {
        return venusMonitorFactory;
    }

    public void setVenusMonitorFactory(VenusMonitorFactory venusMonitorFactory) {
        this.venusMonitorFactory = venusMonitorFactory;
    }
}
