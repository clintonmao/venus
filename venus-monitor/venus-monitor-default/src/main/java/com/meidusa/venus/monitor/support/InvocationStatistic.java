package com.meidusa.venus.monitor.support;

import com.meidusa.venus.*;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 调用汇总
 * Created by Zhangzhihua on 2017/9/4.
 */
public class InvocationStatistic {

    //接口名
    private String serviceInterfaceName;

    //服务名
    private String serviceName;

    //版本号
    private String version;

    //方法名
    private String method;

    //提供方应用
    private String providerApp;

    //提供方机器
    private String providerIp;

    //开始时间
    private Date beginTime;

    //结束时间
    private Date endTime;

    //总数
    private AtomicInteger totalNum = new AtomicInteger(0);

    //失败数
    private AtomicInteger failNum = new AtomicInteger(0);

    //慢操作数
    private AtomicInteger slowNum = new AtomicInteger(0);

    //平均耗时
    private AtomicLong avgCostTime = new AtomicLong(0);

    //最大耗时
    private AtomicLong maxCostTime = new AtomicLong(0);

    public InvocationStatistic(){
    }

    /**
     * 初始化服务统计基本信息
     * @param detail
     */
    public void init(InvocationDetail detail){
        Invocation invocation = detail.getInvocation();
        URL url = detail.getUrl();
        String serviceInterfaceName = invocation.getServiceInterfaceName();
        String serviceName = invocation.getServiceName();
        String version = invocation.getVersion();
        String methodName = invocation.getMethodName();
        Date beginTime = getBeginTimeOfMinutes(invocation.getRequestTime());
        Date endTime = getEndTimeOfMinutes(invocation.getRequestTime());
        this.setServiceInterfaceName(serviceInterfaceName);
        this.setServiceName(serviceName);
        this.setVersion(version);
        this.setMethod(methodName);
        this.setBeginTime(beginTime);
        this.setEndTime(endTime);
        if(url != null){
            this.setProviderApp(url.getApplication());
            this.setProviderIp(url.getHost());
        }
    }

    /**
     * 获取开始准点时间，精确到分钟
     * @param date
     * @return
     */
    Date getBeginTimeOfMinutes(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.SECOND,0);
        return calendar.getTime();
    }

    /**
     * 获取结束准点时间，精确到分钟
     * @param date
     * @return
     */
    Date getEndTimeOfMinutes(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MINUTE,calendar.get(Calendar.MINUTE) + 1);
        return calendar.getTime();
    }

    /**
     * 添加明细并累加统计
     * @param detail
     */
    public void append(InvocationDetail detail){
        totalNum.incrementAndGet();
        if(VenusMonitorUtil.isExceptionOperation(detail)){
            failNum.incrementAndGet();
        }else if(VenusMonitorUtil.isSlowOperation(detail)){
            slowNum.incrementAndGet();
        }
        //若超过，更新最大耗时
        long costTime = getCostTime(detail);
        if(costTime > maxCostTime.longValue()){
            maxCostTime.set(costTime);
        }
        //计算平均耗时
        long newAvgCostTime  = ((totalNum.intValue()*avgCostTime.intValue()) + costTime)/(totalNum.intValue()+1);
        avgCostTime.set(newAvgCostTime);
    }



    /**
     * 获取耗时
     * @param detail
     * @return
     */
    long getCostTime(InvocationDetail detail){
        Date requestTime = null;
        if(detail.getInvocation() instanceof ClientInvocationOperation){
            ClientInvocationOperation clientInvocation = (ClientInvocationOperation)detail.getInvocation();
            requestTime = clientInvocation.getRequestTime();
        }else if(detail.getInvocation() instanceof ServerInvocationOperation){
            ServerInvocationOperation serverInvocation = (ServerInvocationOperation)detail.getInvocation();
            requestTime = serverInvocation.getRequestTime();
        }
        Date responseTime = detail.getResponseTime();
        if(responseTime == null){
            return 99999;
        }
        return responseTime.getTime() - requestTime.getTime();
    }

    /**
     * 重置计数及相关信息
     */
    public void reset(){
        //重置计数及时间相关
        totalNum = new AtomicInteger(0);
        failNum = new AtomicInteger(0);
        slowNum = new AtomicInteger(0);
        avgCostTime = new AtomicLong(0);
        maxCostTime = new AtomicLong(0);
    }

    public String getProviderApp() {
        return providerApp;
    }

    public void setProviderApp(String providerApp) {
        this.providerApp = providerApp;
    }

    public String getProviderIp() {
        return providerIp;
    }

    public void setProviderIp(String providerIp) {
        this.providerIp = providerIp;
    }

    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    public void setServiceInterfaceName(String serviceInterfaceName) {
        this.serviceInterfaceName = serviceInterfaceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public AtomicInteger getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(AtomicInteger totalNum) {
        this.totalNum = totalNum;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public AtomicInteger getFailNum() {
        return failNum;
    }

    public void setFailNum(AtomicInteger failNum) {
        this.failNum = failNum;
    }

    public AtomicInteger getSlowNum() {
        return slowNum;
    }

    public void setSlowNum(AtomicInteger slowNum) {
        this.slowNum = slowNum;
    }

    public AtomicLong getAvgCostTime() {
        return avgCostTime;
    }

    public void setAvgCostTime(AtomicLong avgCostTime) {
        this.avgCostTime = avgCostTime;
    }

    public AtomicLong getMaxCostTime() {
        return maxCostTime;
    }

    public void setMaxCostTime(AtomicLong maxCostTime) {
        this.maxCostTime = maxCostTime;
    }
}
