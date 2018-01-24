/*
 * Copyright (C), 2013-2018, 上汽集团
 * FileName: UsedCarMerchandiseVO.java
 * Author:   guicailiang
 * Date:     2018年1月18日 上午10:15:47
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.model;

import java.io.Serializable;
import java.util.List;

/**
 * 功能描述: 当前了为与平台商城的接口兼容, 后来又拆分开, 为了减少工作量与风险, 名称与原类一致 <br> 
 * 
 * @deprecated
 * @author guicailiang
 */
public class UsedCarMerchandiseVO implements Serializable {

	private static final long serialVersionUID = 1L;

    /**
     * 商品Code
     */
    private String mdseCode;
    
    /**
     * 二手车车辆交易号 
     */
    private String  transactionCode;
    
    /**
     * 品牌Code
     */
    private String brandCode;
    
    /**
     * 品牌名称
     */
    private String brandName;
    
    /**
     * 车系
     */
    private String series;
    
    /**
     * 车型
     */
    private String model;
    
    /**
     * 级别
     */
    private String level;
    
    /**
     * 所在地
     */
    private String location;
    
    /**
     * 里程
     */
    private double distance;
    
    /**
     * 车龄
     */
    private double carUsedTime;
    
    /**
     * 个性推荐
     */
    private String personRecommend;
    
    /**
     * 商城价
     */
    private double mallPrice;
    
    /**
     * 新车价
     */
    private double newCarPrice;
    
    /**
     * 购置税
     */
    private String purchaseTax;
    
    /**
     * 是否已售出
     */
    private String isSale;
    
    /**
     * 车辆总体分析
     */
    private String AllAnalysis;
    
    /**
     * 车主心声
     */
    private String carOwerWords;
    
    /**
     * 车辆8张照片
     */
    private List<String> images;
    
    /**
     * 是否加盟
     */
    private String isToJoin;
    
    /**
     * 门店名称
     */
    private String storeName;
    
    /**
     * 门店地址
     */
    private String storeAddress;
    
    /**
     * 联系电话
     */
    private String storePhone;
    
    /**
     * 用户印象 
     */
    private String userImpression;
    
    /**
     * 车辆营销标签
     */
    private String carMarketTag;
    
    /**
     * 富文本Id
     */
    private String mdseRichId;
    
    /**
     * 价格区间
     */
    private String priceSection;
    
    /**
     * 车龄区间
     */
    private String carUsedTimeSection;
    
    /**
     * 里程区间
     */
    private String distanceSection;

    /**
     * @return the mdseCode
     */
    public String getMdseCode() {
        return mdseCode;
    }

    /**
     * @param mdseCode the mdseCode to set
     */
    public void setMdseCode(String mdseCode) {
        this.mdseCode = mdseCode;
    }

    /**
     * @return the transactionCode
     */
    public String getTransactionCode() {
        return transactionCode;
    }

    /**
     * @param transactionCode the transactionCode to set
     */
    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

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
     * @return the brandName
     */
    public String getBrandName() {
        return brandName;
    }

    /**
     * @param brandName the brandName to set
     */
    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    /**
     * @return the series
     */
    public String getSeries() {
        return series;
    }

    /**
     * @param series the series to set
     */
    public void setSeries(String series) {
        this.series = series;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
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
     * @return the distance
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * @return the carUsedTime
     */
    public double getCarUsedTime() {
        return carUsedTime;
    }

    /**
     * @param carUsedTime the carUsedTime to set
     */
    public void setCarUsedTime(double carUsedTime) {
        this.carUsedTime = carUsedTime;
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
     * @return the mallPrice
     */
    public double getMallPrice() {
        return mallPrice;
    }

    /**
     * @param mallPrice the mallPrice to set
     */
    public void setMallPrice(double mallPrice) {
        this.mallPrice = mallPrice;
    }

    /**
     * @return the newCarPrice
     */
    public double getNewCarPrice() {
        return newCarPrice;
    }

    /**
     * @param newCarPrice the newCarPrice to set
     */
    public void setNewCarPrice(double newCarPrice) {
        this.newCarPrice = newCarPrice;
    }

    /**
     * @return the purchaseTax
     */
    public String getPurchaseTax() {
        return purchaseTax;
    }

    /**
     * @param purchaseTax the purchaseTax to set
     */
    public void setPurchaseTax(String purchaseTax) {
        this.purchaseTax = purchaseTax;
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

    /**
     * @return the allAnalysis
     */
    public String getAllAnalysis() {
        return AllAnalysis;
    }

    /**
     * @param allAnalysis the allAnalysis to set
     */
    public void setAllAnalysis(String allAnalysis) {
        AllAnalysis = allAnalysis;
    }

    /**
     * @return the carOwerWords
     */
    public String getCarOwerWords() {
        return carOwerWords;
    }

    /**
     * @param carOwerWords the carOwerWords to set
     */
    public void setCarOwerWords(String carOwerWords) {
        this.carOwerWords = carOwerWords;
    }

    /**
     * @return the images
     */
    public List<String> getImages() {
        return images;
    }

    /**
     * @param images the images to set
     */
    public void setImages(List<String> images) {
        this.images = images;
    }

    /**
     * @return the isToJoin
     */
    public String getIsToJoin() {
        return isToJoin;
    }

    /**
     * @param isToJoin the isToJoin to set
     */
    public void setIsToJoin(String isToJoin) {
        this.isToJoin = isToJoin;
    }

    /**
     * @return the storeName
     */
    public String getStoreName() {
        return storeName;
    }

    /**
     * @param storeName the storeName to set
     */
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    /**
     * @return the storeAddress
     */
    public String getStoreAddress() {
        return storeAddress;
    }

    /**
     * @param storeAddress the storeAddress to set
     */
    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    /**
     * @return the storePhone
     */
    public String getStorePhone() {
        return storePhone;
    }

    /**
     * @param storePhone the storePhone to set
     */
    public void setStorePhone(String storePhone) {
        this.storePhone = storePhone;
    }

    /**
     * @return the userImpression
     */
    public String getUserImpression() {
        return userImpression;
    }

    /**
     * @param userImpression the userImpression to set
     */
    public void setUserImpression(String userImpression) {
        this.userImpression = userImpression;
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
     * @return the mdseRichId
     */
    public String getMdseRichId() {
        return mdseRichId;
    }

    /**
     * @param mdseRichId the mdseRichId to set
     */
    public void setMdseRichId(String mdseRichId) {
        this.mdseRichId = mdseRichId;
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
}
