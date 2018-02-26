package com.meidusa.venus.client.factory;

import com.meidusa.toolkit.common.bean.config.ConfigUtil;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * AbstractServiceFactory
 * Created by Zhangzhihua on 2017/12/8.
 */
public class AbstractServiceFactory {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    /**
     * 转化及校验地址配置
     * @param ipAddressList
     * @return
     */
    protected String parseAddress(String ipAddressList){
        //转换ucm属性地址
        ipAddressList = parseProperty(ipAddressList);
        if(StringUtils.isEmpty(ipAddressList)){
            throw new VenusConfigException("ipAddressList is null.");
        }

        //转换','分隔地址
        ipAddressList = ipAddressList.trim();
        if(ipAddressList.contains(",")){
            ipAddressList = ipAddressList.replace(",",";");
        }

        //校验地址有效性
        validAddress(ipAddressList);
        return ipAddressList;
    }

    /**
     * 校验地址有效性
     * @param ipAddressList
     */
    void validAddress(String ipAddressList){
        String[] addressArr = ipAddressList.split(";");
        if(addressArr == null || addressArr.length == 0){
            throw new VenusConfigException("ipAddressList invalid:" + ipAddressList);
        }
        for(String address:addressArr){
            String[] arr = address.split(":");
            if(arr == null || arr.length != 2){
                throw new VenusConfigException("ipAddressList invalid:" + ipAddressList);
            }
        }
    }

    /**
     * 解析spring或ucm属性配置，如${x.x.x}
     * @param propertyValue
     */
    protected String parseProperty(String propertyValue){
        if(StringUtils.isNotEmpty(propertyValue)){
            if(propertyValue.startsWith("${") && propertyValue.endsWith("}")){
                String ucmPropertyValue = (String) ConfigUtil.filter(propertyValue);
                if(logger.isInfoEnabled()){
                    logger.info("##########parse ucm config item,placeholder:{},value:{}#############.",propertyValue,ucmPropertyValue);
                }
                return ucmPropertyValue;
            }
        }
        return propertyValue;
    }


}
