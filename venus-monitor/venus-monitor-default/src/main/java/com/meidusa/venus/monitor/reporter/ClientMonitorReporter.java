package com.meidusa.venus.monitor.reporter;

import com.athena.domain.MethodCallDetailDO;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.util.UUIDUtil;

/**
 * client监控上报
 * Created by Zhangzhihua on 2017/9/22.
 */
public class ClientMonitorReporter extends AbstractMonitorReporter {

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
            detailDO.setRequestJson(serialize(clientInvocation.getArgs()));
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
                detailDO.setReponseJson(serialize(result.getResult()));
                detailDO.setStatus(1);
            }else{
                detailDO.setReponseJson(serialize(result.getErrorCode()));
                detailDO.setStatus(1);
            }
        } else{
            //响应异常
            detailDO.setErrorInfo(serialize(exception));
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
