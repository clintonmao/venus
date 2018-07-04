package com.chexiang.venus.demo.provider.service.impl;

import com.chexiang.venus.demo.provider.service.SaleLeadsService;
import com.chexiang.venus.demo.provider.model.SgmSaleLeadsRequest;
import com.chexiang.venus.demo.provider.model.SgmSaleLeadsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("saleLeadsService")
public class SaleLeadsServiceImpl implements SaleLeadsService {

    private static Logger logger = LoggerFactory.getLogger(SaleLeadsServiceImpl.class);

    @Override
    public SgmSaleLeadsResponse receiveSgmSaleLeads(SgmSaleLeadsRequest sgmSaleLeadsRequest) {
        logger.info("param:{}",sgmSaleLeadsRequest);
        return null;
    }
}
