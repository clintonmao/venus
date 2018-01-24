/*
 * Copyright (C), 2013-2018, 上汽集团
 * FileName: IUsedCarMallService.java
 * Author:   guicailiang
 * Date:     2018年1月18日 上午10:20:44
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.shangcheng.service.api;

import java.util.List;

import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.annotations.Service;
import com.saic.usedcar.shangcheng.model.BrandEntity;
import com.saic.usedcar.shangcheng.model.UsedCarMerchandiseVO;
import com.saic.usedcar.shangcheng.model.UsedCarParamVO;
import com.saic.usedcar.shangcheng.pagination.Pagination;
import com.saic.usedcar.shangcheng.pagination.PaginationResult;

/**
 * 功能描述: 为了兼容, 为了迁出<br> 
 *
 * @deprecated
 * @author guicailiang
 */
@Service(name = "iUsedCarMallService")
public interface IUsedCarMallService {

    /**
     * 品牌查询筛选接口
     * @return 
     */
    @Endpoint(name="getBrandList" , async = false)
    List<BrandEntity> getBrandList();
    
    /**
     *  1) 参数:
     *  精确查询: 品牌 价格区间 车龄区间 级别 所在地 里程区间 来源 个性推荐 车辆营销标签
     *  2) 排序: 价格 里程 车龄
     *  3) 支持分页
     *  @param paramVO
     *  @param page
     *  @return
     */
    @Endpoint(name="getUsedCarInfoList" , async = false)
    PaginationResult<List<UsedCarMerchandiseVO>> getUsedCarInfoList (@Param(name = "paramVO") UsedCarParamVO paramVO ,
            @Param(name = "page") Pagination page);
    
    /**
     * 单辆车源接口  根据mdseCode查询
     * 
     * @param mdseCode 商品Code
     * @return
     */
    @Endpoint(name = "getUsedCarInfoByMdseCode" ,async = false)
    UsedCarMerchandiseVO getUsedCarInfoByMdseCode (@Param(name = "mdseCode") String mdseCode);
}
