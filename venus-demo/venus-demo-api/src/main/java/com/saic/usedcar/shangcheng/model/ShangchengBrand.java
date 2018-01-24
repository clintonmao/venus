package com.saic.usedcar.shangcheng.model;

import java.util.Date;

/**
 * 品牌实体类  
 * @author tangxiangquan
 */
public class ShangchengBrand {
	
	/** ID */
    private Long id;

    /** 编号 */
    private String brandCode;

    /** 名称 */
    private String brandName;

    /** 中文名称 */
    private String chName;

    /** 英文名称 */
    private String enName;

    /** 图片ID */
    private String logoId;

    /** 品牌拼音首字母 */
    private String brandPrefixLetter;

    /** 创建时间 */
    private Date createTime;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBrandCode() {
        return brandCode;
    }

    public void setBrandCode(String brandCode) {
        this.brandCode = brandCode == null ? null : brandCode.trim();
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName == null ? null : brandName.trim();
    }

    public String getChName() {
        return chName;
    }

    public void setChName(String chName) {
        this.chName = chName == null ? null : chName.trim();
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName == null ? null : enName.trim();
    }

    public String getLogoId() {
        return logoId;
    }

    public void setLogoId(String logoId) {
        this.logoId = logoId == null ? null : logoId.trim();
    }

    public String getBrandPrefixLetter() {
        return brandPrefixLetter;
    }

    public void setBrandPrefixLetter(String brandPrefixLetter) {
        this.brandPrefixLetter = brandPrefixLetter == null ? null : brandPrefixLetter.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}