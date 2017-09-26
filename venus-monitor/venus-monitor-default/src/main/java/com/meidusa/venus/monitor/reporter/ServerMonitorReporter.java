package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.ServerInvocation;
import com.meidusa.venus.URL;
import com.meidusa.venus.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * server监控上报
 * Created by Zhangzhihua on 2017/9/22.
 */
public class ServerMonitorReporter extends AbstractMonitorReporter {

    private static Logger logger = LoggerFactory.getLogger(ServerMonitorReporter.class);

    /**
     * 转化为detailDo
     * @param detail
     * @return
     */
    MethodCallDetailDO convertDetail(InvocationDetail detail){
        ServerInvocation serverInvocation = (ServerInvocation)detail.getInvocation();
        URL url = detail.getUrl();
        Result result = detail.getResult();
        Throwable exception = detail.getException();

        MethodCallDetailDO detailDO = new MethodCallDetailDO();
        //基本信息
        detailDO.setId(UUIDUtil.create().toString());
        detailDO.setRpcId(serverInvocation.getRpcId());
        if(serverInvocation.getAthenaId() != null){
            detailDO.setTraceId(new String(serverInvocation.getAthenaId()));
        }
        if(serverInvocation.getMessageId() != null){
            detailDO.setMessageId(new String(serverInvocation.getMessageId()));
        }
        detailDO.setSourceType(detail.getFrom());
        //请求信息
        detailDO.setServiceName(serverInvocation.getServiceName());
        if(serverInvocation.getServiceInterface() != null){
            detailDO.setInterfaceName(serverInvocation.getServiceInterface().getName());
        }
        if(serverInvocation.getEndpoint() != null){
            detailDO.setMethodName(serverInvocation.getEndpoint().name());
        }else if(serverInvocation.getMethod() != null){
            detailDO.setMethodName(serverInvocation.getMethod().getName());
        }
        if(serverInvocation.getArgs() != null){
            //Athena上报接口不记输入、输出信息，存在递归拼接问题
            if(!isAthenaInterface(serverInvocation)){
                String requestJson = serialize(serverInvocation.getArgs());
                detailDO.setRequestJson(requestJson);
            }
        }
        detailDO.setRequestTime(serverInvocation.getRequestTime());
        detailDO.setConsumerIp(serverInvocation.getConsumerIp());
        if(url != null){
            detailDO.setProviderIp(url.getHost());
        }
        //响应信息
        detailDO.setResponseTime(detail.getResponseTime());
        //响应结果
        if(result != null){
            if(result.getErrorCode() == 0){
                //Athena上报接口不记输入、输出信息，存在递归拼接问题
                if(!isAthenaInterface(serverInvocation)){
                    String responseJson = serialize(result.getResult());
                    detailDO.setReponseJson(responseJson);
                }
                detailDO.setStatus(1);
            }else{
                //Athena上报接口不记输入、输出信息，存在递归拼接问题
                if(!isAthenaInterface(serverInvocation)){
                    String responseJson = serialize(result.getErrorCode());
                    detailDO.setReponseJson(responseJson);
                }
                detailDO.setStatus(1);
            }
        } else{
            //响应异常
            //Athena上报接口不记输入、输出信息，存在递归拼接问题
            if(!isAthenaInterface(serverInvocation)){
                String responseJsonForException = serialize(exception);
                detailDO.setErrorInfo(responseJsonForException);
            }
            detailDO.setStatus(0);
        }
        //耗时
        long costTime = detail.getResponseTime().getTime()-serverInvocation.getRequestTime().getTime();
        detailDO.setDurationMillisecond(Integer.parseInt(String.valueOf(costTime)));
        //TODO 响应地址
        //状态相关
        return detailDO;
    }
}
