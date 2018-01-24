/*
 * Copyright (C), 2002-2013, IBM
 * FileName: PaginationResult.java
 * Author:   12010065
 * Date:     2013-5-13 下午4:19:10
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.saic.usedcar.shangcheng.pagination;

import java.io.Serializable;

/**
 * 
 * 功能描述：封装返回信息
 * @deprecated
 * @author 作者13011806@cn.ibm.com
 * @version 1.0.0
 * @param <R>
 */
public class PaginationResult<R>  implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
    // 结果集
    private R          r;

    // 分页信息
    private Pagination pagination;
    
    public PaginationResult(){
        
    }

    /**
     * @param r
     * @param pagination
     */
    public PaginationResult(R r, Pagination pagination) {
        super();
        this.r = r;
        this.pagination = pagination;
    }

    public R getR() {
        return r;
    }

    public void setR(R r) {
        this.r = r;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
