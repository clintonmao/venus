package com.meidusa.venus.frontend.http;

import com.meidusa.fastjson.JSON;
import com.meidusa.fastjson.JSONException;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.poolable.InvalidVirtualPoolException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.annotations.RemoteException;
import com.meidusa.venus.backend.services.RequestInfo;
import com.meidusa.venus.io.packet.json.JsonVenusNotifyPacket;
import com.meidusa.venus.io.packet.json.JsonVenusRequestPacket;
import com.meidusa.venus.io.packet.json.JsonVenusResponsePacket;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.frontend.support.LogHandler;
import com.meidusa.venus.backend.serializer.MediaTypes;
import com.meidusa.venus.Response;
import com.meidusa.venus.backend.support.UtilTimerStack;
import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.exception.VenusExceptionLevel;
import com.meidusa.venus.io.network.VenusBIOConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.util.UUID;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VenusHttpServlet2 extends HttpServlet {
	
	private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString};
	
    private static final String _VENUS_TRACE_ID = "_venus_trace_id_";
    private static final String _VENUS_CLIENT_ID = "_venus_client_id_";
    private static final String VERSION="v";
    private static final String VERSION_HEADER = "version";
    private static Logger logger = LoggerFactory.getLogger(VenusHttpServlet2.class);
    private static String ENDPOINT_INVOKED_TIME = "invoked Total Time: ";
    // private static String patternString = "([a-zA-Z_0-9.]+)/([a-zA-Z_0-9]+)(?:/(\\d+))*(?:/\\S*)*(?:\\?(?:.*?)*)*";
    private Pattern servicePattern = null;
    private String urlPattern;
    private static final long serialVersionUID = 1L;
    
    private Tuple<String,Integer>[] runtimeTuples = null;
    private Tuple<String,Integer>[] sourceIpAddressTuples = null;
    private volatile long currentIndex = 0;
    private String ipAddressList = null;
    Timer timer  = new Timer("Check remote Connection");
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String servletName = config.getServletName();

        urlPattern = config.getInitParameter("uri-pattern");

        if (StringUtils.isEmpty(urlPattern)) {
            throw new ServletException("servlet=" + servletName + "  init-param=uri-prefix cannot be null");
        }

        urlPattern = urlPattern.trim();

        servicePattern = Pattern.compile(urlPattern);

       ipAddressList = config.getInitParameter("remoteIpAddressList");
       List<Tuple<String,Integer>> list = new ArrayList<Tuple<String,Integer>>();
       String ipList[] = com.meidusa.toolkit.util.StringUtil.split(ipAddressList, ", ");
       for(String s : ipList){
    	   String tmp[] = com.meidusa.toolkit.util.StringUtil.split(s , ":");
    	    Tuple<String,Integer> tuple = new Tuple<String,Integer>();
    	    tuple.left = tmp[0];
    	    if(tmp.length >=2){
    	    	tuple.right = Integer.valueOf(tmp[1]);
    	    }else{
    	    	tuple.right =  16800;
    	    }
    	    
    	    list.add(tuple);
       }
       
       sourceIpAddressTuples = list.toArray(new Tuple[list.size()]);
       timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				VenusHttpServlet2.this.checkAll();
			}
		}, 3000L, 5000L);
       
    }

    /**
     * 用于定期检测Runtime ip列表数组的有效性（创建连接、认证成功,并且发送状态包，返回正常状态）
     */
    private void checkAll(){
    	List<Tuple<String,Integer>> list = new ArrayList<Tuple<String,Integer>>();
    	for(Tuple<String,Integer> tuple : sourceIpAddressTuples){
    		try {
				if(checkRemoteVenus(tuple.left,tuple.right)){
					list.add(tuple);
				}
			} catch (Exception e) {
				logger.error("check remote error",e);
			}
    	}
    	
    	this.runtimeTuples = list.toArray(new Tuple[list.size()]);
    }
    
    private boolean checkRemoteVenus(String ip,int port) throws Exception{
    	Socket socket = new Socket();
		socket.connect(new InetSocketAddress(ip,port));
		try{
			VenusBIOConnection conn =	new VenusBIOConnection(socket,System.currentTimeMillis());
			byte[] tmp = conn.read();
			HandshakePacket packet = new HandshakePacket();
			packet.init(tmp);
			DummyAuthenPacket dummy = new DummyAuthenPacket();
			socket.getOutputStream().write(dummy.toByteArray());
			tmp = conn.read();
			OKPacket ok = new OKPacket();
			ok.init(tmp);
			
			conn.write(new VenusStatusRequestPacket().toByteArray());
			tmp = conn.read();
			VenusStatusResponsePacket res = new VenusStatusResponsePacket();
			res.init(tmp);
			if(res.status == PacketConstant.VENUS_STATUS_RUNNING){
				return true;
			}
		}finally{
			socket.close();
		}
		return false;
    }
    
    private VenusBIOConnection getConnectionPolling() throws Exception{
    	int i = 0;
    	do{
    		try{
		    	if(this.runtimeTuples.length == 0){
		    		throw new InvalidVirtualPoolException(ipAddressList +" invalid");
		    	}
		    	Tuple<String,Integer>[] current = this.runtimeTuples;
		    	Tuple<String,Integer> tuple = current[(int)(this.currentIndex++ % current.length)];
		    	return getConnection(tuple.left,tuple.right);
    		}catch(Exception e){
    			i ++;
    		}
    	}while(i<=this.runtimeTuples.length);
    	throw new InvalidVirtualPoolException(ipAddressList +" invalid");
    }
    
    private VenusBIOConnection getConnection(String ip,int port) throws Exception{
		Socket socket = new Socket();
		socket.setSoTimeout(10000);
		socket.setTcpNoDelay(true);
		socket.connect(new InetSocketAddress(ip,port),5000);
		VenusBIOConnection conn =	new VenusBIOConnection(socket,System.currentTimeMillis());
		byte[] tmp = conn.read();
		HandshakePacket packet = new HandshakePacket();
		packet.init(tmp);
		DummyAuthenPacket dummy = new DummyAuthenPacket();
		socket.getOutputStream().write(dummy.toByteArray());
		tmp = conn.read();
		OKPacket ok = new OKPacket();
		ok.init(tmp);
		return conn;
    }
    
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI().trim();

        if (!req.getContextPath().equals("/")) {
            uri = uri.substring(req.getContextPath().length());
        }

        if (!urlPattern.endsWith("/")) {
            if (uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1);
            }
        }
        Matcher matcher = servicePattern.matcher(uri);

        if (!matcher.matches() || matcher.groupCount() < 2) {
            Response result = new Response();
            result.setErrorCode(VenusExceptionCodeConstant.REQUEST_ILLEGAL);
            result.setErrorMessage("requst not matcher");
            writeResponse(req, resp, result);
            return;
        }

        resp.setContentType(MediaTypes.APPLICATION_JSON);
        Map<String, Object> parameterMap = null;
        String service = matcher.group(1);
        String method = matcher.group(2);
        String clientID = req.getHeader(_VENUS_CLIENT_ID);
        String traceId = req.getHeader(_VENUS_TRACE_ID);
        String apiName = service + "." + method;
        // ResultType resultType = ResultType.RESPONSE;
        String v = req.getParameter(VERSION);
        if (StringUtils.isEmpty(v)) {
        	v = req.getHeader(VERSION_HEADER);
        }
        // String sign = req.getParameter("sign");
        int version = 0;

        try {
            version = Integer.parseInt(v == null ? "0" : v);
        } catch (java.lang.NumberFormatException e) {
            version = 0;
        }

        // check endpoint
        try {

            if (req.getContentLength() > 0) {
                byte[] message = new byte[req.getContentLength()];
                req.getInputStream().read(message);
                parameterMap = JSON.parseObject(new String(message, "UTF-8"));
            } else {
                parameterMap = new HashMap<String, Object>();
            }

            Set<String> keys = req.getParameterMap().keySet();
            for (String key : keys) {
                parameterMap.put(key, req.getParameter(key));
            }
            parameterMap.remove(VERSION);
        } catch (Exception e) {
            int errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;

            if (e instanceof JSONException) {
                errorCode = VenusExceptionCodeConstant.REQUEST_ILLEGAL;
            }

            if (e instanceof CodedException) {
                CodedException codeEx = (CodedException) e;
                errorCode = codeEx.getErrorCode();
            }

            if (e instanceof VenusExceptionLevel) {
                if (((VenusExceptionLevel) e).getLevel() != null) {
                    LogHandler.logDependsOnLevel(((VenusExceptionLevel) e).getLevel(), logger,
                            e.getMessage() + " client:{clientID=" + clientID + ",ip=" + req.getRemoteAddr() + ", apiName=" + service + "." + method + "}", e);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage() + " [ip=" + req.getRemoteAddr() + ", apiName=" + apiName + "]", e);
                }
            }

            Response result = new Response();
            result.setErrorCode(errorCode);
            result.setErrorMessage(e.getMessage());

            writeResponse(req, resp, result);
            return;
        }

        Response result = null;
        long startTime = TimeUtil.currentTimeMillis();
        boolean isError = false;
        byte[] traceIdByt = null;
        try {
        	if(StringUtil.isEmpty(traceId)){
        		traceIdByt = VenusTracerUtil.randomUUID();
        		UUID uuid = new UUID(VenusTracerUtil.randomUUID());
        		traceId = uuid.toString();
        	}
            // handleRequest service endpoint
            result = handleRequest(apiName, version, traceId, parameterMap);

           /* if (logger.isDebugEnabled()) {
                logger.debug("receive service request packet from " + req.getRemoteAddr());
                logger.debug("sending response to " + req.getRemoteAddr() + ": " + result + " ");
            }*/
        } catch (Exception e) {
            int errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
            if (e instanceof CodedException) {
                CodedException codeEx = (CodedException) e;
                errorCode = codeEx.getErrorCode();
            }
            if(errorCode >= 18000000 &&  errorCode < 19000000){
            	isError = true;
            }
            result = new Response();
            result.setErrorCode(errorCode);
            result.setErrorMessage(e.getMessage());

            logger.error("error when handleRequest", e);
            return;
        } finally {
            long endTime = TimeUtil.currentTimeMillis();
            VenusTracerUtil.logResult(endTime-startTime, traceId, apiName, JSON.toJSONString(parameterMap,JSON_FEATURE), JSON.toJSONString(result,JSON_FEATURE));
            writeResponse(req, resp, result);
            //MonitorRuntime.getInstance().calculateAverage(service, method, endTime - startTime,isError);
        }

    }

    private void writeResponse(final HttpServletRequest req, // NOPMD by structchen on 13-10-18 上午11:17
            final HttpServletResponse resp, Response result) throws IOException {
        resp.setContentType(MediaTypes.APPLICATION_JSON);
        resp.getOutputStream().write(
                JSON.toJSONString(result, SerializerFeature.PrettyFormat).getBytes(
                        PacketConstant.PACKET_CHARSET));
    }

    private void writeResponse(final HttpServletRequest req, // NOPMD by structchen on 13-10-18 上午11:17
            final HttpServletResponse resp, String result) throws IOException {
        resp.setContentType(MediaTypes.APPLICATION_JSON);
        resp.getOutputStream().write(result.getBytes(PacketConstant.PACKET_CHARSET));
    }
    
   
    
    private Response handleRequest(String api, int version, String traceId, Map<String, Object> paramters) {

        Response response = new Response();
        VenusBIOConnection conn = null;
        try {
            UtilTimerStack.push(ENDPOINT_INVOKED_TIME);
            conn = this.getConnectionPolling();
            JsonVenusRequestPacket request = new JsonVenusRequestPacket();
            request.apiName = api;
            request.params = JSON.toJSONString(paramters);
            request.serviceVersion = version;

            conn.write(request.toByteArray());

            byte[] bts = conn.read();

            int type = AbstractServicePacket.getType(bts);
            if (type == AbstractVenusPacket.PACKET_TYPE_ERROR) {
                ErrorPacket error = new ErrorPacket();
                error.init(bts);
                response.setErrorCode(error.errorCode);
                response.setErrorMessage(error.message);
            } else if (type == AbstractVenusPacket.PACKET_TYPE_NOTIFY_PUBLISH) {
                JsonVenusNotifyPacket notify = new JsonVenusNotifyPacket();
                notify.init(bts);
                response.setResult(notify.callbackObject);
            } else {
                JsonVenusResponsePacket responsePacket = new JsonVenusResponsePacket();
                responsePacket.init(bts);
                response.setResult(responsePacket.result);
            }

        } catch (Throwable e) {
        	RemoteException re = AnnotationUtil.getAnnotation(e.getClass().getAnnotations(), RemoteException.class);
        	if(re != null){
        		response.setErrorCode(re.errorCode());
        	}else{
        		response.setErrorCode(VenusExceptionCodeConstant.UNKNOW_EXCEPTION);
        	}
            response.setErrorMessage(e.getMessage());
        } finally {
            if (conn != null) {
            	try {
					conn.close();
				} catch (Exception e) {
				}
            }
            UtilTimerStack.pop(ENDPOINT_INVOKED_TIME);
        }

        return response;
    }
    
    /**
     * extract request info from connection and packet
     * 
     * @return
     */
    private RequestInfo getRequestInfo(final HttpServletRequest req) {
        RequestInfo info = new RequestInfo();
        info.setRemoteIp(req.getRemoteHost());
        info.setProtocol(RequestInfo.Protocol.HTTP);
        info.setAccept(MediaTypes.APPLICATION_JSON);
        return info;
    }

}
