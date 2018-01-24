package com.saic.usedcar.shangcheng.model;

import java.io.Serializable;

/**
 * 商品日志实体类
 * @author tangxiangquan
 */
public class ShangchengLog implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private Long id;

	/**  商品Code   */
    private String mdseCode;

    //日志内容
    private String content;
      
    //创建人
    private String createName;
      
    //创建时间
    private String createTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMdseCode() {
		return mdseCode;
	}

	public void setMdseCode(String mdseCode) {
		this.mdseCode = mdseCode;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getCreateName() {
		return createName;
	}

	public void setCreateName(String createName) {
		this.createName = createName;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
      
}
