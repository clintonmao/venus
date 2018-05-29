/*
 * Copyright (C), 2013-2014, 上海汽车集团股份有限公司
 * FileName: SaleLeadsDto.java
 * Author:   saic-generator
 * Date:     2014-4-28 14:24:29
 * Description: 官网销售线索实体类
 */
 
package com.chexiang.venus.demo.provider.model;

import java.io.Serializable;




/**
 * 官网销售线索实体类
 * 
 * @author saic-tools-generator
 */
public class SgmSaleLeadsDto implements Serializable {
    /** Serial UID */
    private static final long serialVersionUID = 1L;
    
    /** recordId 官网销售线索ID */
    private String recordId;
    
    /** custName 姓名 */
    private String custName;
    
    /** mobile 手机号码 */
    private String mobile;
    
    /** interestedVehicleSeries 意向车系 */
    private String interestedVehicleSeries;
    
    /** purchaseIntention 意向购车时间 */
    private String purchaseIntention;
    
    /** dealerName 意向经销商 */
    private String dealerName;
    
    /** dataAddTime 注册时间 */
    private String dataAddTime;
    
    /** pushBy 接口调用方代码 */
    private String pushBy;
    
    /** subject 数据所属项目 */
    private String subject;
    
    /** requiredType 客户需求 */
    private String requiredType;
    
    /** isRequest 是否需意向获取 */
    private String isRequest;
    
    /** title 性别 */
    private String title;
    
    /** birthday 生日 */
    private String birthday;
    
    /** secondMobileContact 备用手机号 */
    private String secondMobileContact;
    
    /** phone 固定电话 */
    private String phone;
    
    /** email 电子邮箱 */
    private String email;
    
    /** address 地址 */
    private String address;
    
    /** postCode 邮编 */
    private String postCode;
    
    /** province 省 */
    private String province;
    
    /** city 市 */
    private String city;
    
    /** distict 区县 */
    private String distict;
    
    /** isProductManuals 是否索取产品手册 */
    private String isProductManuals;
    
    /** interestedVehicleBrand 意向品牌 */
    private String interestedVehicleBrand;
    
    /** interestedVehicleModel 意向车型名称 */
    private String interestedVehicleModel;
    
    /** purchaseIntentionDetail 计划购买汽车具时间 */
    private String purchaseIntentionDetail;
    
    /** phrchaseBankroll 购车预算 */
    private String phrchaseBankroll;
    
    /** phrchaseBankrollDetail 具体购车预算 */
    private String phrchaseBankrollDetail;
    
    /** dataSharingPrivacy 是否接受SGM市场活动信息；1是 0否 */
    private String dataSharingPrivacy;
    
    /** dataSource 媒体渠道 */
    private String dataSource;
    
    /** dataSourceSub 媒体子渠道 */
    private String dataSourceSub;
    
    /** channelSource 获取渠道 */
    private String channelSource;
    
    /** channelSourceSub 子渠道 */
    private String channelSourceSub;
    
    /** isHavaCar 是否有车；1是 0否 */
    private String isHavaCar;
    
    /** ownedTime 原购车时间 */
    private String ownedTime;
    
    /** brand 现有车辆品牌 */
    private String brand;
    
    /** series 现有车系 */
    private String series;
    
    /** models 现有车型 */
    private String models;
    
    /** testDriveModels 试驾车型 */
    private String testDriveModels;
    
    /** firstTestDriveDate 首选试驾时间 */
    private String firstTestDriveDate;
    
    /** testDriveDate2 次选试驾时间 */
    private String testDriveDate2;
    
    /** vipCardNo 会员卡号 */
    private String vipCardNo;
    
    /** dealerProvince 经销商所在省 */
    private String dealerProvince;
    
    /** dealerCity 经销商所在市 */
    private String dealerCity;
    
    /** isMobile 是否同意电话联系 */
    private String isMobile;
    
    /** isEmail 是否同意邮件联系 */
    private String isEmail;
    
    /** activityName 活动代码 */
    private String activityName;
    
    /** activityContent 活动内容 */
    private String activityContent;
    
    /** custInfoType 信息类型；1 试驾,0 索取信息 */
    private String custInfoType;
    
    /** transferTotal 传输总条数 */
    private String transferTotal;
    
    /** transferTime 传输总条数统计时间 */
    private String transferTime;
    
    /** exteriorColor 外饰颜色 */
    private String exteriorColor;
    
    /** interiorColor 内饰颜色 */
    private String interiorColor;
    
    /** exhaustVol 排量（车型） */
    private String exhaustVol;
    
    /** assemblyType 配置（款式） */
    private String assemblyType;
    
    /** ifHasLicense 是否有驾照；1-有,0-没有 */
    private String ifHasLicense;
    
    /** driverLicenseNo 驾照号码 */
    private String driverLicenseNo;
    
    /** wishJoinActivityTime 希望参加活动的时间 */
    private String wishJoinActivityTime;
    
    /** playBuyTimeYear 计划购买车辆的时间（年） */
    private String playBuyTimeYear;
    
    /** playBuyTimeMonth 计划购买车辆的时间（月） */
    private String playBuyTimeMonth;
    
    /** carBudgetLower 购车预算下限 */
    private String carBudgetLower;
    
    /** carBudgetUpper 购车预算上限 */
    private String carBudgetUpper;
    
    /** recommendbyName 推荐人姓名 */
    private String recommendbyName;
    
    /** recommendbyMobile 推荐人手机号 */
    private String recommendbyMobile;
    
    /** reserveTime 预约时间 */
    private String reserveTime;
    
    /** remark 备注 */
    private String remark;
    
    /** needCccYx 需CAC意向验证 */
    private String needCccYx;
    
    /** needCccFollow 需CAC跟进验证 */
    private String needCccFollow;
    
    /** dealerName  销售线索类型*/
    private Integer  saleleadstype;
    

	public Integer getSaleleadstype() {
		return saleleadstype;
	}

	public void setSaleleadstype(Integer saleleadstype) {
		this.saleleadstype = saleleadstype;
	}

	/**
	 * @return the recordId
	 */
	public String getRecordId() {
		return recordId;
	}

	/**
	 * @param recordId the recordId to set
	 */
	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	/**
	 * @return the custName
	 */
	public String getCustName() {
		return custName;
	}

	/**
	 * @param custName the custName to set
	 */
	public void setCustName(String custName) {
		this.custName = custName;
	}

	/**
	 * @return the mobile
	 */
	public String getMobile() {
		return mobile;
	}

	/**
	 * @param mobile the mobile to set
	 */
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	/**
	 * @return the interestedVehicleSeries
	 */
	public String getInterestedVehicleSeries() {
		return interestedVehicleSeries;
	}

	/**
	 * @param interestedVehicleSeries the interestedVehicleSeries to set
	 */
	public void setInterestedVehicleSeries(String interestedVehicleSeries) {
		this.interestedVehicleSeries = interestedVehicleSeries;
	}

	/**
	 * @return the purchaseIntention
	 */
	public String getPurchaseIntention() {
		return purchaseIntention;
	}

	/**
	 * @param purchaseIntention the purchaseIntention to set
	 */
	public void setPurchaseIntention(String purchaseIntention) {
		this.purchaseIntention = purchaseIntention;
	}

	/**
	 * @return the dealerName
	 */
	public String getDealerName() {
		return dealerName;
	}

	/**
	 * @param dealerName the dealerName to set
	 */
	public void setDealerName(String dealerName) {
		this.dealerName = dealerName;
	}

	/**
	 * @return the dataAddTime
	 */
	public String getDataAddTime() {
		return dataAddTime;
	}

	/**
	 * @param dataAddTime the dataAddTime to set
	 */
	public void setDataAddTime(String dataAddTime) {
		this.dataAddTime = dataAddTime;
	}

	/**
	 * @return the pushBy
	 */
	public String getPushBy() {
		return pushBy;
	}

	/**
	 * @param pushBy the pushBy to set
	 */
	public void setPushBy(String pushBy) {
		this.pushBy = pushBy;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @param subject the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * @return the requiredType
	 */
	public String getRequiredType() {
		return requiredType;
	}

	/**
	 * @param requiredType the requiredType to set
	 */
	public void setRequiredType(String requiredType) {
		this.requiredType = requiredType;
	}

	/**
	 * @return the isRequest
	 */
	public String getIsRequest() {
		return isRequest;
	}

	/**
	 * @param isRequest the isRequest to set
	 */
	public void setIsRequest(String isRequest) {
		this.isRequest = isRequest;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the birthday
	 */
	public String getBirthday() {
		return birthday;
	}

	/**
	 * @param birthday the birthday to set
	 */
	public void setBirthday(String birthday) {
		this.birthday = birthday;
	}

	/**
	 * @return the secondMobileContact
	 */
	public String getSecondMobileContact() {
		return secondMobileContact;
	}

	/**
	 * @param secondMobileContact the secondMobileContact to set
	 */
	public void setSecondMobileContact(String secondMobileContact) {
		this.secondMobileContact = secondMobileContact;
	}

	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the postCode
	 */
	public String getPostCode() {
		return postCode;
	}

	/**
	 * @param postCode the postCode to set
	 */
	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}

	/**
	 * @return the province
	 */
	public String getProvince() {
		return province;
	}

	/**
	 * @param province the province to set
	 */
	public void setProvince(String province) {
		this.province = province;
	}

	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}

	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * @return the distict
	 */
	public String getDistict() {
		return distict;
	}

	/**
	 * @param distict the distict to set
	 */
	public void setDistict(String distict) {
		this.distict = distict;
	}

	/**
	 * @return the isProductManuals
	 */
	public String getIsProductManuals() {
		return isProductManuals;
	}

	/**
	 * @param isProductManuals the isProductManuals to set
	 */
	public void setIsProductManuals(String isProductManuals) {
		this.isProductManuals = isProductManuals;
	}

	/**
	 * @return the interestedVehicleBrand
	 */
	public String getInterestedVehicleBrand() {
		return interestedVehicleBrand;
	}

	/**
	 * @param interestedVehicleBrand the interestedVehicleBrand to set
	 */
	public void setInterestedVehicleBrand(String interestedVehicleBrand) {
		this.interestedVehicleBrand = interestedVehicleBrand;
	}

	/**
	 * @return the interestedVehicleModel
	 */
	public String getInterestedVehicleModel() {
		return interestedVehicleModel;
	}

	/**
	 * @param interestedVehicleModel the interestedVehicleModel to set
	 */
	public void setInterestedVehicleModel(String interestedVehicleModel) {
		this.interestedVehicleModel = interestedVehicleModel;
	}

	/**
	 * @return the purchaseIntentionDetail
	 */
	public String getPurchaseIntentionDetail() {
		return purchaseIntentionDetail;
	}

	/**
	 * @param purchaseIntentionDetail the purchaseIntentionDetail to set
	 */
	public void setPurchaseIntentionDetail(String purchaseIntentionDetail) {
		this.purchaseIntentionDetail = purchaseIntentionDetail;
	}

	/**
	 * @return the phrchaseBankroll
	 */
	public String getPhrchaseBankroll() {
		return phrchaseBankroll;
	}

	/**
	 * @param phrchaseBankroll the phrchaseBankroll to set
	 */
	public void setPhrchaseBankroll(String phrchaseBankroll) {
		this.phrchaseBankroll = phrchaseBankroll;
	}

	/**
	 * @return the phrchaseBankrollDetail
	 */
	public String getPhrchaseBankrollDetail() {
		return phrchaseBankrollDetail;
	}

	/**
	 * @param phrchaseBankrollDetail the phrchaseBankrollDetail to set
	 */
	public void setPhrchaseBankrollDetail(String phrchaseBankrollDetail) {
		this.phrchaseBankrollDetail = phrchaseBankrollDetail;
	}

	/**
	 * @return the dataSharingPrivacy
	 */
	public String getDataSharingPrivacy() {
		return dataSharingPrivacy;
	}

	/**
	 * @param dataSharingPrivacy the dataSharingPrivacy to set
	 */
	public void setDataSharingPrivacy(String dataSharingPrivacy) {
		this.dataSharingPrivacy = dataSharingPrivacy;
	}

	/**
	 * @return the dataSource
	 */
	public String getDataSource() {
		return dataSource;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @return the dataSourceSub
	 */
	public String getDataSourceSub() {
		return dataSourceSub;
	}

	/**
	 * @param dataSourceSub the dataSourceSub to set
	 */
	public void setDataSourceSub(String dataSourceSub) {
		this.dataSourceSub = dataSourceSub;
	}

	/**
	 * @return the channelSource
	 */
	public String getChannelSource() {
		return channelSource;
	}

	/**
	 * @param channelSource the channelSource to set
	 */
	public void setChannelSource(String channelSource) {
		this.channelSource = channelSource;
	}

	/**
	 * @return the channelSourceSub
	 */
	public String getChannelSourceSub() {
		return channelSourceSub;
	}

	/**
	 * @param channelSourceSub the channelSourceSub to set
	 */
	public void setChannelSourceSub(String channelSourceSub) {
		this.channelSourceSub = channelSourceSub;
	}

	/**
	 * @return the isHavaCar
	 */
	public String getIsHavaCar() {
		return isHavaCar;
	}

	/**
	 * @param isHavaCar the isHavaCar to set
	 */
	public void setIsHavaCar(String isHavaCar) {
		this.isHavaCar = isHavaCar;
	}

	/**
	 * @return the ownedTime
	 */
	public String getOwnedTime() {
		return ownedTime;
	}

	/**
	 * @param ownedTime the ownedTime to set
	 */
	public void setOwnedTime(String ownedTime) {
		this.ownedTime = ownedTime;
	}

	/**
	 * @return the brand
	 */
	public String getBrand() {
		return brand;
	}

	/**
	 * @param brand the brand to set
	 */
	public void setBrand(String brand) {
		this.brand = brand;
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
	 * @return the models
	 */
	public String getModels() {
		return models;
	}

	/**
	 * @param models the models to set
	 */
	public void setModels(String models) {
		this.models = models;
	}

	/**
	 * @return the testDriveModels
	 */
	public String getTestDriveModels() {
		return testDriveModels;
	}

	/**
	 * @param testDriveModels the testDriveModels to set
	 */
	public void setTestDriveModels(String testDriveModels) {
		this.testDriveModels = testDriveModels;
	}

	/**
	 * @return the firstTestDriveDate
	 */
	public String getFirstTestDriveDate() {
		return firstTestDriveDate;
	}

	/**
	 * @param firstTestDriveDate the firstTestDriveDate to set
	 */
	public void setFirstTestDriveDate(String firstTestDriveDate) {
		this.firstTestDriveDate = firstTestDriveDate;
	}

	/**
	 * @return the testDriveDate2
	 */
	public String getTestDriveDate2() {
		return testDriveDate2;
	}

	/**
	 * @param testDriveDate2 the testDriveDate2 to set
	 */
	public void setTestDriveDate2(String testDriveDate2) {
		this.testDriveDate2 = testDriveDate2;
	}

	/**
	 * @return the vipCardNo
	 */
	public String getVipCardNo() {
		return vipCardNo;
	}

	/**
	 * @param vipCardNo the vipCardNo to set
	 */
	public void setVipCardNo(String vipCardNo) {
		this.vipCardNo = vipCardNo;
	}

	/**
	 * @return the dealerProvince
	 */
	public String getDealerProvince() {
		return dealerProvince;
	}

	/**
	 * @param dealerProvince the dealerProvince to set
	 */
	public void setDealerProvince(String dealerProvince) {
		this.dealerProvince = dealerProvince;
	}

	/**
	 * @return the dealerCity
	 */
	public String getDealerCity() {
		return dealerCity;
	}

	/**
	 * @param dealerCity the dealerCity to set
	 */
	public void setDealerCity(String dealerCity) {
		this.dealerCity = dealerCity;
	}

	/**
	 * @return the isMobile
	 */
	public String getIsMobile() {
		return isMobile;
	}

	/**
	 * @param isMobile the isMobile to set
	 */
	public void setIsMobile(String isMobile) {
		this.isMobile = isMobile;
	}

	/**
	 * @return the isEmail
	 */
	public String getIsEmail() {
		return isEmail;
	}

	/**
	 * @param isEmail the isEmail to set
	 */
	public void setIsEmail(String isEmail) {
		this.isEmail = isEmail;
	}

	/**
	 * @return the activityName
	 */
	public String getActivityName() {
		return activityName;
	}

	/**
	 * @param activityName the activityName to set
	 */
	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	/**
	 * @return the activityContent
	 */
	public String getActivityContent() {
		return activityContent;
	}

	/**
	 * @param activityContent the activityContent to set
	 */
	public void setActivityContent(String activityContent) {
		this.activityContent = activityContent;
	}

	/**
	 * @return the custInfoType
	 */
	public String getCustInfoType() {
		return custInfoType;
	}

	/**
	 * @param custInfoType the custInfoType to set
	 */
	public void setCustInfoType(String custInfoType) {
		this.custInfoType = custInfoType;
	}

	/**
	 * @return the transferTotal
	 */
	public String getTransferTotal() {
		return transferTotal;
	}

	/**
	 * @param transferTotal the transferTotal to set
	 */
	public void setTransferTotal(String transferTotal) {
		this.transferTotal = transferTotal;
	}

	/**
	 * @return the transferTime
	 */
	public String getTransferTime() {
		return transferTime;
	}

	/**
	 * @param transferTime the transferTime to set
	 */
	public void setTransferTime(String transferTime) {
		this.transferTime = transferTime;
	}

	/**
	 * @return the exteriorColor
	 */
	public String getExteriorColor() {
		return exteriorColor;
	}

	/**
	 * @param exteriorColor the exteriorColor to set
	 */
	public void setExteriorColor(String exteriorColor) {
		this.exteriorColor = exteriorColor;
	}

	/**
	 * @return the interiorColor
	 */
	public String getInteriorColor() {
		return interiorColor;
	}

	/**
	 * @param interiorColor the interiorColor to set
	 */
	public void setInteriorColor(String interiorColor) {
		this.interiorColor = interiorColor;
	}

	/**
	 * @return the exhaustVol
	 */
	public String getExhaustVol() {
		return exhaustVol;
	}

	/**
	 * @param exhaustVol the exhaustVol to set
	 */
	public void setExhaustVol(String exhaustVol) {
		this.exhaustVol = exhaustVol;
	}

	/**
	 * @return the assemblyType
	 */
	public String getAssemblyType() {
		return assemblyType;
	}

	/**
	 * @param assemblyType the assemblyType to set
	 */
	public void setAssemblyType(String assemblyType) {
		this.assemblyType = assemblyType;
	}

	/**
	 * @return the ifHasLicense
	 */
	public String getIfHasLicense() {
		return ifHasLicense;
	}

	/**
	 * @param ifHasLicense the ifHasLicense to set
	 */
	public void setIfHasLicense(String ifHasLicense) {
		this.ifHasLicense = ifHasLicense;
	}

	/**
	 * @return the driverLicenseNo
	 */
	public String getDriverLicenseNo() {
		return driverLicenseNo;
	}

	/**
	 * @param driverLicenseNo the driverLicenseNo to set
	 */
	public void setDriverLicenseNo(String driverLicenseNo) {
		this.driverLicenseNo = driverLicenseNo;
	}

	/**
	 * @return the wishJoinActivityTime
	 */
	public String getWishJoinActivityTime() {
		return wishJoinActivityTime;
	}

	/**
	 * @param wishJoinActivityTime the wishJoinActivityTime to set
	 */
	public void setWishJoinActivityTime(String wishJoinActivityTime) {
		this.wishJoinActivityTime = wishJoinActivityTime;
	}

	/**
	 * @return the playBuyTimeYear
	 */
	public String getPlayBuyTimeYear() {
		return playBuyTimeYear;
	}

	/**
	 * @param playBuyTimeYear the playBuyTimeYear to set
	 */
	public void setPlayBuyTimeYear(String playBuyTimeYear) {
		this.playBuyTimeYear = playBuyTimeYear;
	}

	/**
	 * @return the playBuyTimeMonth
	 */
	public String getPlayBuyTimeMonth() {
		return playBuyTimeMonth;
	}

	/**
	 * @param playBuyTimeMonth the playBuyTimeMonth to set
	 */
	public void setPlayBuyTimeMonth(String playBuyTimeMonth) {
		this.playBuyTimeMonth = playBuyTimeMonth;
	}

	/**
	 * @return the carBudgetLower
	 */
	public String getCarBudgetLower() {
		return carBudgetLower;
	}

	/**
	 * @param carBudgetLower the carBudgetLower to set
	 */
	public void setCarBudgetLower(String carBudgetLower) {
		this.carBudgetLower = carBudgetLower;
	}

	/**
	 * @return the carBudgetUpper
	 */
	public String getCarBudgetUpper() {
		return carBudgetUpper;
	}

	/**
	 * @param carBudgetUpper the carBudgetUpper to set
	 */
	public void setCarBudgetUpper(String carBudgetUpper) {
		this.carBudgetUpper = carBudgetUpper;
	}

	/**
	 * @return the recommendbyName
	 */
	public String getRecommendbyName() {
		return recommendbyName;
	}

	/**
	 * @param recommendbyName the recommendbyName to set
	 */
	public void setRecommendbyName(String recommendbyName) {
		this.recommendbyName = recommendbyName;
	}

	/**
	 * @return the recommendbyMobile
	 */
	public String getRecommendbyMobile() {
		return recommendbyMobile;
	}

	/**
	 * @param recommendbyMobile the recommendbyMobile to set
	 */
	public void setRecommendbyMobile(String recommendbyMobile) {
		this.recommendbyMobile = recommendbyMobile;
	}

	/**
	 * @return the reserveTime
	 */
	public String getReserveTime() {
		return reserveTime;
	}

	/**
	 * @param reserveTime the reserveTime to set
	 */
	public void setReserveTime(String reserveTime) {
		this.reserveTime = reserveTime;
	}

	/**
	 * @return the remark
	 */
	public String getRemark() {
		return remark;
	}

	/**
	 * @param remark the remark to set
	 */
	public void setRemark(String remark) {
		this.remark = remark;
	}

	/**
	 * @return the needCccYx
	 */
	public String getNeedCccYx() {
		return needCccYx;
	}

	/**
	 * @param needCccYx the needCccYx to set
	 */
	public void setNeedCccYx(String needCccYx) {
		this.needCccYx = needCccYx;
	}

	/**
	 * @return the needCccFollow
	 */
	public String getNeedCccFollow() {
		return needCccFollow;
	}

	/**
	 * @param needCccFollow the needCccFollow to set
	 */
	public void setNeedCccFollow(String needCccFollow) {
		this.needCccFollow = needCccFollow;
	}

	/**
     * 覆盖父类toString方法
     */    
    @Override
    public String toString() {
        return super.toString();
    }
}