/*
 * Copyright (C), 2013-2014, 上海汽车集团有限公司
 * FileName: Sms.java
 * Author:   v_bjwangxl
 * Date:     2014年1月14日 下午1:09:37
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.saic.framework.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.saic.framework.service.sms.api.BusinessType;

/**
 * 短消息实体
 * @author v_bjwangxl
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class Sms implements Identified, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private String appId;

    private String schemaId;

    private List<String> destPhones;

    private String content;

    private Map<String, String> params;

    private BusinessType businessType;

    public Sms(String appId, String schemaId) {
        super();
        this.appId = appId;
        this.schemaId = schemaId;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("appId:").append(this.getAppId());
        buffer.append("schemaId:").append(this.getSchemaId());
        buffer.append("destPhones:").append(this.getDestPhones().size());
        buffer.append("content").append(this.getContent());
        buffer.append("businessType:").append(this.getBusinessType());
        return buffer.toString();
    }

    @Override
    public Collection<String> getDestAddrs() {
        //return destPhones;
        return null;
    }

    /**
     * 应用标识
     */
    @Override
    public String getAppId() {
        return this.appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     *
     * 功能描述: <br>
     * 目标手机号码集合
     *
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public List<String> getDestPhones() {
        return destPhones;
    }

    public void setDestPhones(List<String> destPhones) {
        this.destPhones = destPhones;
    }

    /**
     *
     * 功能描述: <br>
     * 短消息内容
     *
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 附加参数<br>
     * <ul>
     * <li>key   : 邮件模板中的变量名
     * <li>value : 要替换的内容
     * </ul>
     */
    @Override
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    /**
     *
     * 功能描述: <br>
     * 短消息类型
     *
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public BusinessType getBusinessType() {
        return businessType;
    }

    public void setBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }

    /**
     * 短消息模板标识
     */
    @Override
    public String getSchemaId() {
        return this.schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    @Override
    public Identified copy() throws CloneNotSupportedException {
        Sms copied = (Sms)super.clone();
        copied.setDestPhones(new ArrayList<String>());
        return copied;
    }

    @Override
    public void resetDestAddrs(List<String> splitedAddrs) {
        setDestPhones(splitedAddrs);
    }

}
