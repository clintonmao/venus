package com.meidusa.venus.bus.registry.register;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.venus.URL;
import com.meidusa.venus.bus.VenusConnectionAcceptor;
import com.meidusa.venus.bus.registry.AbstractServiceManager;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.authenticate.Authenticator;
import com.meidusa.venus.service.registry.ServiceDefinition;
import com.meidusa.venus.util.DefaultRange;
import com.meidusa.venus.util.Range;
import com.meidusa.venus.util.RangeUtil;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 基于服务注册中心服务注册管理
 * 
 * @author Structchen
 * 
 */
@SuppressWarnings("rawtypes")
public class RegistryServiceManager extends AbstractServiceManager {
    private static Logger logger = LoggerFactory.getLogger(RegistryServiceManager.class);
    
    @Autowired
    private VenusConnectionAcceptor acceptor;
    /**
     * 注册中心主机IP
     */
    private String host;

    /**
     * 注册中心服务端口
     */
    private int port;

    /**
     * 与注册中心采用的认证方式
     */
    private Authenticator authenticator;

    private VenusExceptionFactory venusExceptionFactory;

    private List<ServiceDefinition> current;

    @Override
    public List<URL> lookup(String serviceName) {
        //TODO 若是动态注册服务，则订阅服务
        return null;
    }

    //TODO 静态路由、动态路由情况,分离静态还是统一？
    @Override
    protected Map<String, List<Tuple<Range, BackendConnectionPool>>> load() throws Exception {
        //从注册中心获取服务定义 TODO 提取、统一替换为register模块化接口
        SimpleServiceFactory factory = new SimpleServiceFactory(host, port);
        if (authenticator != null) {
            factory.setAuthenticator(authenticator);
        }
        factory.setVenusExceptionFactory(venusExceptionFactory);
        //TODO 处理serviceRegistery依赖
        //final ServiceRegistry registry = factory.getService(ServiceRegistry.class);
        //List<ServiceDefinition> list = registry.getServiceDefinitions();

        List<ServiceDefinition> list = null;

        //根据服务地址列表创建对应的连接池并设置消息处理类 TODO 提取、抽象
        final Map<String, List<Tuple<Range, BackendConnectionPool>>> serviceMap = new HashMap<String, List<Tuple<Range, BackendConnectionPool>>>();

        for (ServiceDefinition definition : list) {
            List<Tuple<Range, BackendConnectionPool>> l = serviceMap.get(definition.getName());
            if (l == null) {
                l = new ArrayList<Tuple<Range, BackendConnectionPool>>();
                serviceMap.put(definition.getName(), l);
            }
            
            for(String ip:localAddress){
                definition.getIpAddress().remove(ip+":"+acceptor.getPort());
            }
            if(definition.getIpAddress().size()>0){
                String[] ips = definition.getIpAddress().toArray(new String[] {});
                BackendConnectionPool pool = this.createVirtualPool(ips, authenticator);
                Range range = RangeUtil.getVersionRange(definition.getVersionRange());
                Tuple<Range, BackendConnectionPool> tuple = new Tuple<Range, BackendConnectionPool>(range, pool);
                l.add(tuple);
            }
        }
        factory.destroy();
        this.current = list;

        //处理服务定义更新
        new Thread() {
            {
                this.setDaemon(true);
            }

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    try {
                        SimpleServiceFactory factory = new SimpleServiceFactory(host, port);
                        if (authenticator != null) {
                            factory.setAuthenticator(authenticator);
                        }
                        factory.setVenusExceptionFactory(venusExceptionFactory);
                        //TODO 处理serviceRegistery依赖
                        //final ServiceRegistry registry = factory.getService(ServiceRegistry.class);
                        //List<ServiceDefinition> list = registry.getServiceDefinitions();
                        List<ServiceDefinition> list = null;
                        modifier(list, current);
                        current = list;
                        factory.destroy();
                        removeUnusedConnection(list);
                    } catch (Throwable e) {
                        logger.info("services  scheduled update error", e);
                    }
                }
            }

			private void removeUnusedConnection(List<ServiceDefinition> list) {
				if (list == null || list.size() == 0){
					return;
				}
				
				Map<String, List<Range>> serviceRangeMap = new HashMap<String, List<Range>>();
				
				for (ServiceDefinition sd : list){
					List<Range> ranges = serviceRangeMap.get(sd.getName());
					if (ranges == null){
						ranges = new ArrayList<Range>();
						serviceRangeMap.put(sd.getName(), ranges);
					}
					ranges.add(RangeUtil.getVersionRange(sd.getVersionRange()));
				}
				
				Iterator<String> iter = serviceMap.keySet().iterator();
				while(iter.hasNext()) {
					String serviceName = iter.next();
					List<Range> ranges = serviceRangeMap.get(serviceName);
					List<Tuple<Range, BackendConnectionPool>> tuples = serviceMap.get(serviceName);
					
					for(Tuple<Range, BackendConnectionPool> tuple: tuples) {
						if (tuple == null){
							continue;
						}
						if (ranges.contains(tuple.left)){
							continue;
						}
						
						try{
							logger.debug("close " + serviceName + (tuple.left instanceof DefaultRange ?  "无限制" : tuple.left));
							if (!tuple.right.isClosed()){
								tuple.right.close();
							}
						}catch(Exception e) {
							logger.error("close connection pool error:" , e);
						}
						
						iter.remove();
					}
					
				}
			}
        }.start();
        return serviceMap;
    }

    protected void update(ServiceDefinition newObj) {
        List<Tuple<Range, BackendConnectionPool>> list = serviceMap.get(newObj.getName());
        if (list == null) {
            list = new ArrayList<Tuple<Range, BackendConnectionPool>>();
            serviceMap.put(newObj.getName(), list);
        }

        for(String ip:localAddress){
            newObj.getIpAddress().remove(ip+":"+acceptor.getPort());
        }
        
        String[] ips = newObj.getIpAddress().toArray(new String[] {});
        Arrays.sort(ips);

        Range range = RangeUtil.getVersionRange(newObj.getVersionRange());

        boolean isNew = true;
        for (Tuple<Range, BackendConnectionPool> old : list) {
            if (old.left.equals(range)) {
                BackendConnectionPool pool = this.createVirtualPool(ips, authenticator);
                old.right = pool;
                isNew = false;
                logger.warn("update Service=" + newObj.getName() + ", new address=" + ArrayUtils.toString(ips));
                break;
            }
        }

        if (isNew) {
            BackendConnectionPool pool = this.createVirtualPool(ips, authenticator);
            Tuple<Range, BackendConnectionPool> tuple = new Tuple<Range, BackendConnectionPool>(range, pool);
            list.add(tuple);
            logger.warn("new Service=" + newObj.getName() + ", version=" + range + "address=" + ArrayUtils.toString(ips));
        }

    }

    /**
     * 
     * @param list
     * @param current
     */
    protected void modifier(List<ServiceDefinition> list, List<ServiceDefinition> current) {
        if (list == null) {
            return;
        }
        for (ServiceDefinition newObj : list) {
            boolean newService = true;
            boolean newVersion = true;
            boolean newHost = false;
            for (ServiceDefinition old : current) {

                Range newRange = RangeUtil.getVersionRange(newObj.getVersionRange());

                // 判断是否存在相同的服务
                if (StringUtil.equals(newObj.getName(), old.getName())) {

                    newService = false;
                    Range oldRange = RangeUtil.getVersionRange(old.getVersionRange());

                    // 是否存在相同的
                    if (newRange.equals(oldRange)) {
                        newVersion = false;
                        newHost = !newObj.getIpAddress().equals(old.getIpAddress());
                        break;
                    }
                }
            }

            // 如果存在新服务,新版本,新的ip地址,则需要更新
            if (newService || newVersion || newHost) {
                update(newObj);
            }
        }
        fixPools();
    }

    public VenusConnectionAcceptor getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(VenusConnectionAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }

}
