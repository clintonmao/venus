package com.meidusa.venus.util;

import com.meidusa.fastjson.JSON;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSONUtil
 * Created by Zhangzhihua on 2017/9/28.
 */
public class JSONUtil {

    private static Logger logger = LoggerFactory.getLogger(JSONUtil.class);

    //private static ObjectMapper objectMapper;

    /*
    static {
        objectMapper = new ObjectMapper();
        // mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        // mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        // mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // //mapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
        // mapper.configure(Feature.AUTO_CLOSE_SOURCE, false);
        // mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
        serializationConfig.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }
    */

    /**
     * 转换为字符串
     * @param object
     * @return
     * @throws Exception
     */
    public static String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }
}
