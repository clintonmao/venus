package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.monitor.filter.AbstractMonitorFilter;
import com.meidusa.venus.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * client监控上报
 * Created by Zhangzhihua on 2017/9/22.
 */
public class ClientMonitorReporter extends AbstractMonitorReporter {

    private static Logger logger = LoggerFactory.getLogger(ClientMonitorReporter.class);

    /**
     * 转化为detailDo
     * @param detail
     * @return
     */
    MethodCallDetailDO convertDetail(InvocationDetail detail){
        ClientInvocation clientInvocation = (ClientInvocation)detail.getInvocation();
        URL url = detail.getUrl();
        Result result = detail.getResult();
        Throwable exception = detail.getException();

        MethodCallDetailDO detailDO = new MethodCallDetailDO();
        //TODO 设置application
        //基本信息
        detailDO.setId(UUIDUtil.create().toString());
        detailDO.setRpcId(clientInvocation.getRpcId());
        if(clientInvocation.getAthenaId() != null){
            detailDO.setTraceId(new String(clientInvocation.getAthenaId()));
        }
        if(clientInvocation.getMessageId() != null){
            detailDO.setMessageId(new String(clientInvocation.getMessageId()));
        }
        detailDO.setSourceType(detail.getFrom());
        //请求信息
        detailDO.setServiceName(clientInvocation.getServiceName());
        if(clientInvocation.getServiceInterface() != null){
            detailDO.setInterfaceName(clientInvocation.getServiceInterface().getName());
        }
        if(clientInvocation.getEndpoint() != null){
            detailDO.setMethodName(clientInvocation.getEndpoint().name());
        }else if(clientInvocation.getMethod() != null){
            detailDO.setMethodName(clientInvocation.getMethod().getName());
        }
        if(clientInvocation.getArgs() != null){
            //Athena上报接口不记输入、输出信息，存在递归拼接问题
            if(!isAthenaInterface(clientInvocation)){
                String requestJson = serialize(clientInvocation.getArgs());
                detailDO.setRequestJson(requestJson);
            }
        }
        detailDO.setRequestTime(clientInvocation.getRequestTime());
        detailDO.setConsumerIp(clientInvocation.getConsumerIp());
        if(url != null){
            detailDO.setProviderIp(url.getHost());
        }
        //响应信息
        detailDO.setResponseTime(detail.getResponseTime());
        //响应结果
        if(result != null){
            if(result.getErrorCode() == 0){
                //Athena上报接口不记输入、输出信息，存在递归拼接问题
                if(!isAthenaInterface(clientInvocation)){
                    String responseJson = serialize(result.getResult());
                    detailDO.setReponseJson(responseJson);
                }
                detailDO.setStatus(1);
            }else{
                //Athena上报接口不记输入、输出信息，存在递归拼接问题
                if(!isAthenaInterface(clientInvocation)){
                    String responseJson = serialize(result.getErrorCode());
                    detailDO.setReponseJson(responseJson);
                }
                detailDO.setStatus(1);
            }
        } else{
            //响应异常
            //Athena上报接口不记输入、输出信息，存在递归拼接问题
            if(!isAthenaInterface(clientInvocation)){
                String responseJsonForException = serialize(exception);
                detailDO.setErrorInfo(responseJsonForException);
            }
            detailDO.setStatus(0);
        }
        //耗时
        long costTime = detail.getResponseTime().getTime()-clientInvocation.getRequestTime().getTime();
        detailDO.setDurationMillisecond(Integer.parseInt(String.valueOf(costTime)));
        //TODO 响应地址
        //状态相关
        return detailDO;
    }
}
