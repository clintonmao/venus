package com.meidusa.venus.support;

import com.meidusa.venus.Result;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.io.packet.AbstractServicePacket;
import com.meidusa.venus.io.packet.ErrorPacket;
import com.meidusa.venus.io.packet.ServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.util.Utils;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

/**
 * exception与ErrorPacket/notifyPacket转换工具类
 * Created by Zhangzhihua on 2017/11/1.
 */
public class ErrorPacketConvert {

    private static Logger logger = LoggerFactory.getLogger(ErrorPacketConvert.class);

    /**
     * 将exception转换为errorPacket
     * @param result
     * @param request
     * @param serializer
     * @return
     * @throws Exception
     */
    public static ErrorPacket toErrorPacket(Result result,AbstractServicePacket request,Serializer serializer) throws Exception{
        ErrorPacket error = new ErrorPacket();
        AbstractServicePacket.copyHead(request, error);
        error.errorCode = result.getErrorCode();
        error.message = result.getErrorMessage();
        Throwable throwable = result.getException();
        if (throwable != null) {
            Map<String, PropertyDescriptor> mpd = Utils.getBeanPropertyDescriptor(throwable.getClass());
            Map<String, Object> additionalData = new HashMap<String, Object>();

            for (Map.Entry<String, PropertyDescriptor> entry : mpd.entrySet()) {
                additionalData.put(entry.getKey(), entry.getValue().getReadMethod().invoke(throwable));
            }
            error.additionalData = serializer.encode(additionalData);
        }
        AbstractServicePacket resultPacket = error;
        return error;
    }

    /**
     * 将exception转化为notifyPacket
     * @param result
     * @param request
     * @param serializer
     * @return
     * @throws Exception
     */
    public static ServiceNofityPacket toNotifyPacket(Result result, AbstractServicePacket request, Serializer serializer) throws Exception{
        Throwable e = null;
        if (result.getException() == null) {
            e = new DefaultVenusException(result.getErrorCode(), result.getErrorMessage());
        } else {
            e = result.getException();
        }

        ServiceNofityPacket response = new SerializeServiceNofityPacket(serializer, null);
        AbstractServicePacket.copyHead(request, response);
        if (e instanceof CodedException) {
            CodedException codedException = (CodedException) e;
            response.errorCode = codedException.getErrorCode();
        } else {
            response.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
        }

        if (e != null) {
            Map<String, PropertyDescriptor> mpd = Utils.getBeanPropertyDescriptor(e.getClass());
            Map<String, Object> additionalData = new HashMap<String, Object>();

            for (Map.Entry<String, PropertyDescriptor> entry : mpd.entrySet()) {
                try {
                    additionalData.put(entry.getKey(), entry.getValue().getReadMethod().invoke(e));
                } catch (Exception e1) {
                    logger.error("read config properpty error", e1);
                }
            }
            response.additionalData = serializer.encode(additionalData);
            response.errorMessage = e.getMessage();
        }

        response.apiName = ((SerializeServiceRequestPacket)request).apiName;
        response.identityData = new byte[]{};
        byte[] traceID = VenusTracerUtil.getTracerID();
        if (traceID == null) {
            traceID = VenusTracerUtil.randomTracerID();
        }
        response.traceId = traceID;

        return response;
    }

    /**
     * 将errorPacket转化为exception
     * @param errorPacket
     * @return
     * @throws Exception
     */
    public static Throwable toExceptionFromErrorPacket(ErrorPacket errorPacket,Serializer serializer,VenusExceptionFactory venusExceptionFactory) throws Exception{
        if(venusExceptionFactory == null){
            RpcException rpcException = new RpcException(errorPacket.errorCode,errorPacket.message);
            return rpcException;
        }

        //反序列化异常
        Exception exception = venusExceptionFactory.getException(errorPacket.errorCode, errorPacket.message);
        if (exception == null) {
            logger.error("receive error packet,errorCode=" + errorPacket.errorCode + ",message=" + errorPacket.message);
        } else {
            if (errorPacket.additionalData != null) {
                Object obj = serializer.decode(errorPacket.additionalData, Utils.getBeanFieldType(exception.getClass(), Exception.class));
                try {
                    BeanUtils.copyProperties(exception, obj);
                } catch (Exception e1) {
                    logger.error("copy properties error", e1);
                }
            }
            logger.error("receive error packet", exception);
        }
        return exception;
    }

    /**
     * 将notifyPacket错误信息转化为exception
     * @param nofityPacket
     * @return
     * @throws Exception
     */
    public static Throwable toExceptionFromNotifyPacket(SerializeServiceNofityPacket nofityPacket,Serializer serializer,VenusExceptionFactory venusExceptionFactory) throws Exception{
        if(venusExceptionFactory == null){
            RpcException rpcException = new RpcException(nofityPacket.errorCode,nofityPacket.errorMessage);
            return rpcException;
        }

        Exception exception = venusExceptionFactory.getException(nofityPacket.errorCode, nofityPacket.errorMessage);
        if (exception == null) {
            exception = new DefaultVenusException(nofityPacket.errorCode, nofityPacket.errorMessage);
        } else {
            if (nofityPacket.additionalData != null) {
                Object obj = serializer.decode(nofityPacket.additionalData, Utils.getBeanFieldType(exception.getClass(), Exception.class));
                try {
                    BeanUtils.copyProperties(exception, obj);
                } catch (Exception e1) {
                    logger.error("copy properties error", e1);
                }
            }
        }
        return exception;
    }

}
