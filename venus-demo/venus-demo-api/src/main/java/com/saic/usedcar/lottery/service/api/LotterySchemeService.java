/*
 * Copyright (C), 2013-2017, 上汽集团
 * FileName: LotterySchemeService.java
 * Author:   guicailiang
 * Date:     2017年11月18日 下午1:22:10
 * Description: //模块目的、功能描述      
 */
package com.saic.usedcar.lottery.service.api;

import com.saic.usedcar.lottery.model.LotteryScheme;

/**
 * 功能描述 <br> 
 *
 * @author guicailiang
 */
public interface LotterySchemeService {

    /**
     * 
     * 功能描述: 根据主键查询, 有10分钟本地缓存<br>
     * 注意: 对象未克隆<br>
     *
     * @param id
     * @return
     */
    LotteryScheme selectByPrimaryKey(Integer id);
    
}
