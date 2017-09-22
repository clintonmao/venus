package com.meidusa.venus.registry.service;

import java.util.List;

import com.meidusa.venus.URL;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.annotations.PerformanceLevel;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;

/**
 * 注册中心服务操作接口
 * Created by Zhangzhihua on 2017/8/16.
 */
@Service(name = "registerService", version = 1, athenaFlag=false)
public interface RegisterService {

    /**
     * 服务注册
     * @param url
     * @throws VenusRegisteException
     */
	@Endpoint(name = "registe")
	@PerformanceLevel(printParams=false,printResult = false)
    void registe(@Param(name = "url") URL url) throws VenusRegisteException;

    /**
     * 服务反注册
     * @param url
     * @return TODO
     * @throws VenusRegisteException
     */
	@Endpoint(name = "unregiste")
	@PerformanceLevel(printParams=false,printResult = false)
    boolean unregiste(@Param(name = "url")URL url) throws VenusRegisteException;

    /**
     * 服务订阅
     * @param url
     * @throws VenusRegisteException
     */
	@Endpoint(name = "subscrible")
	@PerformanceLevel(printParams=false,printResult = false)
    void subscrible(@Param(name = "url")URL url) throws VenusRegisteException;

    /**
     * 服务反订阅
     * @param url
     * @return TODO
     * @throws VenusRegisteException
     */
	@Endpoint(name = "unsubscrible")
	@PerformanceLevel(printParams=false,printResult = false)
    boolean unsubscrible(@Param(name = "url")URL url) throws VenusRegisteException;

    /**
     * 根据URL对象返回服务定义对象
     * @param url
     * @return
     */
	@Endpoint(name = "findServiceDefinition")
	@PerformanceLevel(printParams=false,printResult = false)
	List<VenusServiceDefinitionDO> findServiceDefinitions(@Param(name = "url")URL url);
    
    /**
     * 根据URL更新注册接口的心跳时间
     * @param url
     */
	@Endpoint(name = "heartbeatRegister")
	@PerformanceLevel(printParams=false,printResult = false)
    void heartbeatRegister(@Param(name = "url")URL url);
    
    /**
     * 根据URL更新订阅接口的心跳时间
     * @param url
     */
	@Endpoint(name = "heartbeatSubcribe")
	@PerformanceLevel(printParams=false,printResult = false)
    void heartbeatSubcribe(@Param(name = "url")URL url);
    
    /**
     * 清理无效的服务映射关系
     * @param currentDateTime
     * @param updateTime
     */
	@Endpoint(name = "clearInvalidService")
	@PerformanceLevel(printParams=false,printResult = false)
    void clearInvalidService(@Param(name = "currentDateTime")String currentDateTime,@Param(name = "updateTime")String updateTime);

    /**
     * 设置连接Url
     * @param connectUrl
     */
	@Endpoint(name = "setConnectUrl")
	@PerformanceLevel(printParams=false,printResult = false)
    void setConnectUrl(@Param(name = "connectUrl")String connectUrl);

    /**
     * 初始化连接相关
     */
	@Endpoint(name = "init")
	@PerformanceLevel(printParams=false,printResult = false)
    void init();
	
	/**
	 * 根据 host port添加主机
	 * @param host
	 * @param port
	 * @return
	 */
	@Endpoint(name = "addServer")
	@PerformanceLevel(printParams=false,printResult = false)
	int addServer(@Param(name = "host")String host,@Param(name = "port")int port);
	
	/**
	 * 根据服务名添加服务(用于数据同步)
	 * @param serviceName
	 * @param description 
	 * @param version 
	 * @return
	 */
	@Endpoint(name = "addService")
	@PerformanceLevel(printParams=false,printResult = false)
	int addService(@Param(name = "serviceName")String serviceName, @Param(name = "description")String description, @Param(name = "version")String version);

	/**
	 * 增加服务映射关系(用于数据同步)
	 * @param hostName
	 * @param port 
	 * @param serviceName
	 * @param version 
	 * @return
	 */
	@Endpoint(name = "addNewServiceMapping")
	@PerformanceLevel(printParams=false,printResult = false)
	void addNewServiceMapping(@Param(name = "hostName")String hostName,@Param(name = "port")int port,@Param(name = "serviceName")String serviceName,@Param(name = "version")String version) ;
	
}
