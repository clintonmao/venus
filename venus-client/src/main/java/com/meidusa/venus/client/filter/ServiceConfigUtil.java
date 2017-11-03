package com.meidusa.venus.client.filter;

import com.meidusa.venus.URL;
import com.meidusa.venus.client.router.condition.rule.FullConditionRule;
import com.meidusa.venus.registry.domain.FlowControl;
import com.meidusa.venus.registry.domain.RouterRule;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务配置util
 * Created by Zhangzhihua on 2017/11/3.
 */
public class ServiceConfigUtil {

    /**
     * 获取服务流控配置
     * @param url
     * @return
     */
    public static List<FlowControl> getFlowConfigList(URL url){
        List<FlowControl> controls = new ArrayList<FlowControl>();

        VenusServiceDefinitionDO srvDef = (VenusServiceDefinitionDO)url.getServiceDefinition();
        if(srvDef == null){
            return controls;
        }
        List<VenusServiceConfigDO> srvCfgList = srvDef.getServiceConfigs();
        if(CollectionUtils.isEmpty(srvCfgList)){
            return controls;
        }

        for(VenusServiceConfigDO srvCfg:srvCfgList){
            FlowControl control = srvCfg.getFlowControl();
            controls.add(control);
        }

        return controls;
    }
}
