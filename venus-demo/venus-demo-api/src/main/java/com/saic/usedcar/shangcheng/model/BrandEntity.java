/*
 * Copyright (C), 2013-2018, 上汽集团
 * FileName: BrandEntity.java
 * Author:   guicailiang
 * Date:     2018年1月18日 上午10:24:12
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

import java.io.Serializable;

/**
 * 功能描述 <br> 
 *
 * @deprecated
 * @author guicailiang
 */
public class BrandEntity extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;

    /**
     * 品牌编码
     */
    private String brandCode;

    /**
     * 品牌类别：1自有品牌、2代理品牌、3进口品牌、4其他特殊品牌
     */
    private Integer brandType;

    /**
     * 品牌名
     */
    private String brandName;

    /**
     * 中文名称
     */
    private String chName;

    /**
     * 英文名称
     */
    private String enName;

    /**
     * 品牌首字母
     */
    private String brandPrefixLetter;

    /**
     * 品牌LOGO
     */
    private String logoId;

    /**
     * 到期日YYYYMMDD
     */
    private String expireDate;

    //@Column(name="brand_code")
    public String getBrandCode() {
        return brandCode;
    }

    public void setBrandCode(String brandCode) {
        this.brandCode = brandCode;
    }

    //@Column(name="brand_type")
    public Integer getBrandType() {
        return brandType;
    }

    public void setBrandType(Integer brandType) {
        this.brandType = brandType;
    }

    //@Column(name="brand_name")
    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    //@Column(name="ch_name")
    public String getChName() {
        return chName;
    }

    public void setChName(String chName) {
        this.chName = chName;
    }

    //@Column(name="en_name")
    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    //@Column(name="brand_prefix_letter")
    public String getBrandPrefixLetter() {
        return brandPrefixLetter;
    }

    public void setBrandPrefixLetter(String brandPrefixLetter) {
        this.brandPrefixLetter = brandPrefixLetter;
    }

    //@Column(name="logo_id")
    public String getLogoId() {
        return logoId;
    }

    public void setLogoId(String logoId) {
        this.logoId = logoId;
    }

    //@Column(name="expire_date")
    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

}
