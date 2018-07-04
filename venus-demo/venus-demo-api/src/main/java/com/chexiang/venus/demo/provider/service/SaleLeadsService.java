package com.chexiang.venus.demo.provider.service;/*
 * Copyright (C), 2013-2014, 上海汽车集团股份有限公司
 * FileName: SgmSaleLeadsService.java
 * Author:   zhaomeng
 * Date:     2014年4月28日 下午2:57:45
 * Description: CCC接收SGM官网发送的销售线索   
 */

import com.chexiang.venus.demo.provider.model.SgmSaleLeadsRequest;
import com.chexiang.venus.demo.provider.model.SgmSaleLeadsResponse;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.annotations.Service;

/**
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.saic.ebiz&lt;/groupId&gt;
 *   &lt;artifactId&gt;ebiz-crmsgm-api&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 * 接口编号：XXX
 * CCC接收SGM官网发送的销售线索并验证后发给CEM  <br>
 * @author mark zhao
 */
@Service(name = "saleLeadsServiceEx", version = 1)
public interface SaleLeadsService {

	/**
     * 功能描述: <br>
     * SGM用户在官网上登记的信息必须实时传输到CCC，CCC对数据进行入库
     * @return SgmSaleLeadsResponse
     *  resultCode：
     *  	0：成功 
	 *		10：用户名或密码错误
	 *		11：参数错误
	 *		12：XML格式错误
	 *		13：数据级错误，具体请解析resultDetail
	 *		14：系统异常
     * 
     */
    @Endpoint(name = "receiveSaleLeads")
    SgmSaleLeadsResponse receiveSgmSaleLeads(@Param(name = "sgmSaleLeadsRequest") SgmSaleLeadsRequest sgmSaleLeadsRequest);

    

}	
