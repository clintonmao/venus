/*
 * Copyright (C), 2013-2018, 上汽集团
 * FileName: BaseEntity.java
 * Author:   guicailiang
 * Date:     2018年1月18日 上午10:26:48
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 功能描述 <br> 
 *
 * @author guicailiang
 */
public class BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;

    /**
     * Primary Key
     */
    private Long id;
    
    /**
     * 状态：0无效  1有效    2暂存
     */
    private Integer status;
    
    /**
     * 是否有效（用来做逻辑删除）
     */
    private boolean active;
    
    /**
     * 创建人Id
     */
    private String createUserId;
    
    /**
     * 创建人名字
     */
    private String createUserName;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新人Id
     */
    private String updateUserId;
    
    /**
     * 更新人Name
     */
    private String updateUserName;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 版本（用来做版本控制）
     */
    private Integer version;
    
    //@Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    //@Column(name = "id")
    public synchronized Long getId() {
        return id;
    }

    public synchronized void setId(Long id) {
        this.id = id;
    }

    //@Column(name = "active")
    public synchronized boolean getActive() {
        return active;
    }

    public synchronized void setActive(boolean active) {
        this.active = active;
    }

    //@Column(name = "create_user_id")
    public synchronized String getCreateUserId() {
        return createUserId;
    }

    public synchronized void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    //@Column(name = "create_user_name")
    public synchronized String getCreateUserName() {
        return createUserName;
    }

    public synchronized void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
    }

    //@Column(name = "create_time")
    public synchronized Date getCreateTime() {
        return createTime;
    }

    public synchronized void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    //@Column(name = "update_user_id")
    public synchronized String getUpdateUserId() {
        return updateUserId;
    }

    public synchronized void setUpdateUserId(String updateUserId) {
        this.updateUserId = updateUserId;
    }

    //@Column(name = "update_user_name")
    public synchronized String getUpdateUserName() {
        return updateUserName;
    }

    public synchronized void setUpdateUserName(String updateUserName) {
        this.updateUserName = updateUserName;
    }

    //@Column(name = "update_time")
    public synchronized Date getUpdateTime() {
        return updateTime;
    }

    public synchronized void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    //@Column(name = "version")
    public synchronized Integer getVersion() {
        return version;
    }

    public synchronized void setVersion(Integer version) {
        this.version = version;
    }

    //@Column(name = "status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
    
}
