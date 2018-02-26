/**
 * 
 */
package com.meidusa.venus.backend.serializer;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * @author Sun Ning
 * 
 */
@Deprecated
public class NullableObjectMapperFactory {

    public static ObjectMapper getNullableObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        SerializationConfig config = om.getSerializationConfig();
        config.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        return om;
    }

}
