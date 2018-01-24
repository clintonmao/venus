/*
 * Copyright (C), 2013-2018, 上汽集团
 * FileName: UsedCarParamVO.java
 * Author:   guicailiang
 * Date:     2018年1月18日 上午10:28:54
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
public class UsedCarParamVO implements Serializable {
	
	private static final long serialVersionUID = 1L;

    /**
     * 品牌
     */
    private String brandCode;
    
    /**
     * 价格区间
     */
    private String priceSection;
    
    /**
     * 车龄区间
     */
    private String carUsedTimeSection;
    
    /**
     * 级别
     */
    private String level;
    
    /**
     * 所在地
     */
    private String location;

    /**
     * 里程区间
     */
    private String distanceSection;
    
    /**
     * 来源
     */
    private String source;
    
    /**
     * 个性推荐
     */
    private String personRecommend;
    
    /**
     * 车辆营销标签
     */
    private String carMarketTag;
    
    /**
     * 根据XX排序    1.价格  2.里程  3.车龄  4.新增时间
     */
    private Integer orderBy;
    
    /**
     * 排序方式  1:DESC,2：ASC
     */
    private Integer isDesc;
    
    /**
     * 是否在售
     */
    private String isSale;

    /**
     * @return the brandCode
     */
    public String getBrandCode() {
        return brandCode;
    }

    /**
     * @param brandCode the brandCode to set
     */
    public void setBrandCode(String brandCode) {
        this.brandCode = brandCode;
    }

    /**
     * @return the priceSection
     */
    public String getPriceSection() {
        return priceSection;
    }

    /**
     * @param priceSection the priceSection to set
     */
    public void setPriceSection(String priceSection) {
        this.priceSection = priceSection;
    }

    /**
     * @return the carUsedTimeSection
     */
    public String getCarUsedTimeSection() {
        return carUsedTimeSection;
    }

    /**
     * @param carUsedTimeSection the carUsedTimeSection to set
     */
    public void setCarUsedTimeSection(String carUsedTimeSection) {
        this.carUsedTimeSection = carUsedTimeSection;
    }

    /**
     * @return the level
     */
    public String getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the distanceSection
     */
    public String getDistanceSection() {
        return distanceSection;
    }

    /**
     * @param distanceSection the distanceSection to set
     */
    public void setDistanceSection(String distanceSection) {
        this.distanceSection = distanceSection;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the personRecommend
     */
    public String getPersonRecommend() {
        return personRecommend;
    }

    /**
     * @param personRecommend the personRecommend to set
     */
    public void setPersonRecommend(String personRecommend) {
        this.personRecommend = personRecommend;
    }

    /**
     * @return the carMarketTag
     */
    public String getCarMarketTag() {
        return carMarketTag;
    }

    /**
     * @param carMarketTag the carMarketTag to set
     */
    public void setCarMarketTag(String carMarketTag) {
        this.carMarketTag = carMarketTag;
    }

    /**
     * @return the orderBy
     */
    public Integer getOrderBy() {
        return orderBy;
    }

    /**
     * @param orderBy the orderBy to set
     */
    public void setOrderBy(Integer orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * @return the isDESC
     */
    public Integer getIsDesc() {
        return isDesc;
    }

    /**
     * @param isDESC the isDESC to set
     */
    public void setIsDesc(Integer isDesc) {
        this.isDesc = isDesc;
    }

    /**
     * @return the isSale
     */
    public String getIsSale() {
        return isSale;
    }

    /**
     * @param isSale the isSale to set
     */
    public void setIsSale(String isSale) {
        this.isSale = isSale;
    }
    
}
