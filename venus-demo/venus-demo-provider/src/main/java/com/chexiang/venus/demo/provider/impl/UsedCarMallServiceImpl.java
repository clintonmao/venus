package com.chexiang.venus.demo.provider.impl;

import com.alibaba.fastjson.JSON;
import com.saic.usedcar.shangcheng.model.BrandEntity;
import com.saic.usedcar.shangcheng.model.UsedCarMerchandiseVO;
import com.saic.usedcar.shangcheng.model.UsedCarParamVO;
import com.saic.usedcar.shangcheng.pagination.Pagination;
import com.saic.usedcar.shangcheng.pagination.PaginationResult;
import com.saic.usedcar.shangcheng.service.api.IUsedCarMallService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Zhangzhihua on 2018/1/24.
 */
@Service("usedCarMallService")
public class UsedCarMallServiceImpl implements IUsedCarMallService {

    @Override
    public List<BrandEntity> getBrandList() {
        return null;
    }

    @Override
    public PaginationResult<List<UsedCarMerchandiseVO>> getUsedCarInfoList(UsedCarParamVO paramVO, Pagination page) {
        System.out.println(JSON.toJSONString(paramVO));
        System.out.println(JSON.toJSONString(page));
        return null;
    }

    @Override
    public UsedCarMerchandiseVO getUsedCarInfoByMdseCode(String mdseCode) {
        return null;
    }
}
