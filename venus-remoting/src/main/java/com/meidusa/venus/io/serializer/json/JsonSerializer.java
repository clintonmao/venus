package com.meidusa.venus.io.serializer.json;

import com.meidusa.fastjson.JSON;
import com.meidusa.fastjson.parser.DefaultExtJSONParser;
import com.meidusa.fastjson.parser.ParserConfig;
import com.meidusa.fastjson.serializer.AfterFilter;
import com.meidusa.fastjson.serializer.NameFilter;
import com.meidusa.fastjson.serializer.SerializeFilter;
import com.meidusa.fastmark.Serialize;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import com.meidusa.venus.io.serializer.AbstractSerializer;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonSerializer extends AbstractSerializer implements PacketConstant {

    public Object decode(ServicePacketBuffer buffer, Type type) {
        if (buffer.hasRemaining()) {
            byte[] bts = buffer.readLengthCodedBytes();
            return decode(bts, type);
        }
        return null;
    }

    public Map<String, Object> decode(ServicePacketBuffer buffer, Map<String, Type> typeMap) {
        if (buffer.hasRemaining()) {
            byte[] bts = buffer.readLengthCodedBytes();
            return decode(bts, typeMap);
        }
        return null;
    }

    public void encode(ServicePacketBuffer buffer, Object obj) {
        if (obj != null) {
            byte[] bts = encode(obj);
            if (bts == null) {
                buffer.writeInt(0);
            } else {
                buffer.writeLengthCodedBytes(bts);
            }
        }
    }

    @Override
    public byte[] encode(final Object obj) {
        if (obj != null) {
            //处理MAP结构非字符串KEY未关闭字符问题 zhangzh 2018.1.17
            //String jsonValue = JSON.toJSONString(obj);
            SerializerFeature[] serializerFeature = new SerializerFeature[]{SerializerFeature.WriteNonStringKeyAsString,SerializerFeature.DisableCircularReferenceDetect};

            //处理序列化别名向下兼容问题 zhangzh 2018.5.29
            final List<EncodeItem> encodeItems = new ArrayList<>();
            //构造filter，过滤@Serialize别名字段
            SerializeFilter nameFilter = new NameFilter(){
                @Override
                public String process(Object object, String name, Object value) {
                    try {
                        if(object != null && StringUtils.isNotEmpty(name)){
                            Field field = object.getClass().getDeclaredField(name);
                            Serialize fieldAnnotation = field.getAnnotation(Serialize.class);
                            if(fieldAnnotation != null){
                                String aliaName = fieldAnnotation.name();
                                if(StringUtils.isNotBlank(aliaName)){
                                    //保存字段名称/值，用于afterFilter追加原始数据信息
                                    if(value != null){
                                        EncodeItem encodeItem = new EncodeItem();
                                        encodeItem.setObject(object);
                                        encodeItem.setName(field.getName());
                                        encodeItem.setAliasName(aliaName);
                                        encodeItem.setValue(value);
                                        encodeItems.add(encodeItem);
                                    }
                                    return aliaName;
                                }
                            }
                        }
                    } catch (NoSuchFieldException e) {
                        return name;
                    }
                    return name;
                }
            };

            //若包含有@Serialize别名字段，将原始字段属性名值也输出一份，为向下兼容
            AfterFilter afterFilter = new AfterFilter() {
                @Override
                public void writeAfter(Object object) {
                    if(object != null && encodeItems.size() > 0){
                        for(EncodeItem encodeItem:encodeItems){
                            if(encodeItem.getObject() == object){
                                if(encodeItem.getName() != null && encodeItem.getAliasName() != null){
                                    writeKeyValue(encodeItem.getName(),encodeItem.getValue());
                                }
                            }
                        }
                    }

                }

            };
            SerializeFilter[] filters = new SerializeFilter[]{nameFilter,afterFilter};
            String jsonValue = JSON.toJSONString(obj,filters,serializerFeature);
            encodeItems.clear();
            return jsonValue.getBytes(PACKET_CHARSET);
        }
        return null;
    }

    @Override
    public Object decode(byte[] bts, Type type) {
		return JSON.parseObject(new String(bts, PACKET_CHARSET).trim(), type);
    }

    @Override
    public Map<String, Object> decode(byte[] bts, Map<String, Type> typeMap) {
        if (bts != null && bts.length > 0) {
            DefaultExtJSONParser parser = new DefaultExtJSONParser(new String(bts, PACKET_CHARSET).trim(),
            		ParserConfig.getGlobalInstance(),JSON.DEFAULT_PARSER_FEATURE);
            try{
            	return parser.parseObjectWithTypeMap(typeMap);
            }finally{
            	parser.close();
            }
        }
        return null;
    }

    /**
     * 序列化属性项信息，用于@JsonField @Serialize序列化问题
     */
    class EncodeItem{
        private Object object;
        private String name;
        private String aliasName;
        private Object value;

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAliasName() {
            return aliasName;
        }

        public void setAliasName(String aliasName) {
            this.aliasName = aliasName;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

}
