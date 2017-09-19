package com.meidusa.venus.bus.registry.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meidusa.venus.bus.registry.xml.config.BusRemoteConfig;
import com.meidusa.venus.bus.registry.xml.config.ServiceConfig;
import com.meidusa.venus.bus.registry.xml.config.VenusBusConfig;
import com.meidusa.venus.bus.registry.ServiceManager;
import com.meidusa.venus.exception.VenusConfigException;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;

import com.meidusa.toolkit.common.bean.config.ConfigUtil;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.venus.digester.DigesterRuleParser;
import com.meidusa.venus.util.DefaultRange;
import com.meidusa.venus.util.Range;
import com.meidusa.venus.util.RangeUtil;
import org.apache.commons.lang.StringUtils;

/**
 * XML方式服务注册管理
 * 
 * @author structchen
 * 
 */
public class XmlServiceManager implements ServiceManager {

    private String[] configFiles;

    /**
     * 服务名称-服务配置映射表 TODO 版本号，1:M
     */
    private Map<String,BusRemoteConfig> serviceRemoteConfigMap = new HashMap<String,BusRemoteConfig>();


    /**
     * 初始化配置
     */
    void init(){
        VenusBusConfig busConfig = parseBusConfig();
        for (ServiceConfig serviceConfig : busConfig.getServiceConfigMap()) {
            //TODO 确认range用途
            Range range = RangeUtil.getVersionRange(serviceConfig.getVersion());
            if (range == null) {
                range = new DefaultRange();
            }
            String serviceName = serviceConfig.getServiceName();
            BusRemoteConfig remoteConfig = null;
            //init remoteConfig
            if(StringUtils.isNotEmpty(serviceConfig.getRemote())){
                remoteConfig = busConfig.getRemoteConfigMap().get(serviceConfig.getRemote());
            }else if(StringUtils.isNotEmpty(serviceConfig.getIpAddressList())){
                String ipAddressList = serviceConfig.getIpAddressList();
                remoteConfig = BusRemoteConfig.parse(ipAddressList);
            }else{
                throw new VenusConfigException("invliad bus registry config.");
            }
            serviceRemoteConfigMap.put(serviceName,remoteConfig);
        }
    }

    /**
     * 解析bus配置
     * @return
     */
    VenusBusConfig parseBusConfig() {
        VenusBusConfig busConfig = new VenusBusConfig();
        for (String configFile : configFiles) {
            configFile = (String) ConfigUtil.filter(configFile);
            RuleSet ruleSet = new FromXmlRuleSet(this.getClass().getResource("venusRemoteServiceRule.xml"), new DigesterRuleParser());
            Digester digester = new Digester();
            digester.setValidating(false);
            digester.addRuleSet(ruleSet);

            InputStream is = null;
            if (configFile.startsWith("classpath:")) {
                configFile = configFile.substring("classpath:".length());
                is = this.getClass().getClassLoader().getResourceAsStream(configFile);
                if (is == null) {
                    throw new ConfigurationException("configFile not found in classpath=" + configFile);
                }
            } else {
                if (configFile.startsWith("file:")) {
                    configFile = configFile.substring("file:".length());
                }
                try {
                    is = new FileInputStream(new File(configFile));
                } catch (FileNotFoundException e) {
                    throw new ConfigurationException("configFile not found with file=" + configFile, e);
                }
            }

            try {
                VenusBusConfig venus = (VenusBusConfig) digester.parse(is);
                for (ServiceConfig config : venus.getServiceConfigMap()) {
                    if (config.getServiceName() == null) {
                        throw new ConfigurationException("Service name can not be null:" + configFile);
                    }
                }
                busConfig.getRemoteConfigMap().putAll(venus.getRemoteConfigMap());
                busConfig.getServiceConfigMap().addAll(venus.getServiceConfigMap());
            } catch (Exception e) {
                throw new ConfigurationException("can not parser xml:" + configFile, e);
            }
        }

        return busConfig;
    }

    @Override
    public List<BusRemoteConfig> lookup(String serviceName) {
        //TODO 版本号多记录处理
        List<BusRemoteConfig> remoteConfigList = new ArrayList<BusRemoteConfig>();
        BusRemoteConfig remoteConfig = serviceRemoteConfigMap.get(serviceName);
        if(remoteConfig != null){
            remoteConfigList.add(remoteConfig);
        }
        return remoteConfigList;
    }

//    Map<String, List<Tuple<Range, BackendConnectionPool>>> load() throws Exception {
//        VenusBusConfig busConfig = parseBusConfig();
//
//        Map<String, BackendConnectionPool> poolMap = null;//TODO 确认此段代码 initRemoteMap(all.getRemoteConfigMap());
//
//        Map<String, List<Tuple<Range, BackendConnectionPool>>> serviceMap = new HashMap<String, List<Tuple<Range, BackendConnectionPool>>>();
//
//        // create objectPool
//        for (ServiceConfig serviceConfig : busConfig.getServiceConfigMap()) {
//            BackendConnectionPool pool = null;
//            if (!StringUtil.isEmpty(serviceConfig.getRemote())) {
//                pool = poolMap.get(serviceConfig.getRemote());
//                if (pool == null) {
//                    throw new ConfigurationException("register=" + serviceConfig.getServiceName() + ",remote not found:" + serviceConfig.getRemote());
//                }
//            } else {
//                String ipAddress = serviceConfig.getIpAddressList();
//                if (!StringUtil.isEmpty(ipAddress)) {
//                    String ipList[] = StringUtil.split(serviceConfig.getIpAddressList(), ", ");
//                    //remove pool
//                    //pool = createVirtualPool(ipList, null);
//                } else {
//                    throw new ConfigurationException("Service or ipAddressList or remote can not be null:" + serviceConfig.getServiceName());
//                }
//            }
//
//            try {
//                Tuple<Range, BackendConnectionPool> tuple = new Tuple<Range, BackendConnectionPool>();
//                tuple.left = RangeUtil.getVersionRange(serviceConfig.getVersion());
//                if (tuple.left == null) {
//                    tuple.left = new DefaultRange();
//                }
//                tuple.right = pool;
//
//                List<Tuple<Range, BackendConnectionPool>> list = serviceMap.get(serviceConfig.getServiceName());
//                if (list == null) {
//                    list = new ArrayList<Tuple<Range, BackendConnectionPool>>();
//                    serviceMap.put(serviceConfig.getServiceName(), list);
//                }
//                list.add(tuple);
//
//            } catch (Exception e) {
//                throw new ConfigurationException("init remote register config error:", e);
//            }
//        }
//        return serviceMap;
//    }


//            private Map<String, BackendConnectionPool> initRemoteMap(Map<String, ClientRemoteConfig> remots) throws Exception {
//        Map<String, BackendConnectionPool> poolMap = new HashMap<String, BackendConnectionPool>();
//        for (Map.Entry<String, ClientRemoteConfig> entry : remots.entrySet()) {
//            ClientRemoteConfig remote = entry.getValue();
//            FactoryConfig factoryConfig = remote.getFactory();
//            if (factoryConfig == null || StringUtils.isEmpty(factoryConfig.getIpAddressList())) {
//                throw new ConfigurationException("remote name=" + remote.getName() + " factory config is null or ipAddress is null");
//            }
//            String ipAddress = factoryConfig.getIpAddressList();
//            String ipList[] = StringUtil.split(ipAddress, ", ");
//
//            BackendConnectionPool nioPools[] = new PollingBackendConnectionPool[ipList.length];
//
//            for (int i = 0; i < ipList.length; i++) {
//                BusBackendConnectionFactory nioFactory = new BusBackendConnectionFactory();
//                if (realPoolMap.get(ipList[i]) != null) {
//                    nioPools[i] = realPoolMap.get(ipList[i]);
//                    continue;
//                }
//
//                if (factoryConfig != null) {
//                    BeanUtils.copyProperties(nioFactory, factoryConfig);
//                }
//
//                String temp[] = StringUtil.split(ipList[i], ":");
//                if (temp.length > 1) {
//                    nioFactory.setHost(temp[0]);
//                    nioFactory.setPort(Integer.valueOf(temp[1]));
//                } else {
//                    nioFactory.setHost(temp[0]);
//                    nioFactory.setPort(PacketConstant.VENUS_DEFAULT_PORT);
//                }
//
//                if (remote.getAuthenticator() != null) {
//                    nioFactory.setAuthenticator(remote.getAuthenticator());
//                }
//
//                nioFactory.setConnector(this.getConnector());
//                nioFactory.setMessageHandler(getMessageHandler());
//
//                nioPools[i] = new PollingBackendConnectionPool(ipList[i], nioFactory, remote.getPoolSize());
//
//                nioPools[i].init();
//            }
//            String poolName = remote.getName();
//
//            MultipleLoadBalanceBackendConnectionPool nioPool = new MultipleLoadBalanceBackendConnectionPool(poolName,
//                    MultipleLoadBalanceObjectPool.LOADBALANCING_ROUNDROBIN, nioPools);
//
//            nioPool.init();
//            poolMap.put(remote.getName(), nioPool);
//
//        }
//
//        return poolMap;
//    }


    public String[] getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(String[] configFiles) {
        this.configFiles = configFiles;
    }
}
