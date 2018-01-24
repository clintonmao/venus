/*
 * Copyright (C), 2002-2013, IBM
 * FileName: Pagination.java
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
 * 功能描述： 分页bean
 * @deprecated
 * @author 作者13011806@cn.ibm.com
 * @version 1.0.0
 */
public class Pagination implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final int PAGESIZE    = 20;

    private int             pagesize;

    private int             totalRows;

    private int currentPage;

    private int curPage;

//    public Pagination() {
//        this(PAGESIZE, 1);
//    }

//    public Pagination(int pagesize, int currentPage) {
//        this.pagesize = pagesize;
//        this.currentPage = currentPage;
//    }

    public int getFirstRowIndex() {
        return (getCurrentPage() - 1) * getPagesize();
    }

    public int getPagesize() {
        return this.pagesize;
    }

    public int getTotalRows() {
        return this.totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
        if (getCurrentPage() > getPageCount()) {
            setCurrentPage(1);
        }
    }

    public void setPagesize(int pagesize) {
        this.pagesize = pagesize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getCurPage() {
        return curPage;
    }

    public void setCurPage(int curPage) {
        this.curPage = curPage;
    }

    public int getPageCount() {
        if (getTotalRows() > 0) {
            return (totalRows / pagesize) + (totalRows % pagesize == 0 ? 0 : 1);
        } else {
            return 0;
        }
    }

}
