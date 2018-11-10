package org.hjq.jsonrpc.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hjq.jsonrpc.JsonRpcRequestParam;
import org.hjq.jsonrpc.client.JsonRpcServerClientServiceFactory;
import org.hjq.jsonrpc.client.RequestService;
import org.hjq.jsonrpc.util.BinaryReader;
import org.hjq.jsonrpc.util.Util;

import com.alibaba.fastjson.JSONObject;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.vecsight.dragonite.sdk.exception.ConnectionNotAliveException;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class JsonRpcClientHandler {

	private ExecutorService requestES = Executors.newCachedThreadPool();

	private DragoniteSocket dragoniteSocket;

	private ServerServiceConfig serviceConfig;
	
	private RpcSessionListener rpcSessionListener;
	
	protected Map<Long, RequestService> requestMap;

	public JsonRpcClientHandler(DragoniteSocket dragoniteSocket, ServerServiceConfig serviceConfig, RpcSessionListener rpcSessionListener) {
		super();
		this.dragoniteSocket = dragoniteSocket;
		this.serviceConfig = serviceConfig;
		this.rpcSessionListener = rpcSessionListener;
		requestMap = new ConcurrentHashMap<>();
	}

	@SuppressWarnings("unchecked")
	public JSONRPC2Response genJSONRPC2Response(String className, JSONRPC2Request reqIn, RpcSession rpcSession) {
		Object result = null;
		JSONRPC2Response respOut = new JSONRPC2Response(result, reqIn.getID());
		Object serviceObj = this.serviceConfig.getService(className);

		if (serviceObj == null) {
			respOut.setError(JSONRPC2Error.METHOD_NOT_FOUND);
			return respOut;
		}
		Map<String, Object> paramMap = reqIn.getNamedParams();
		
		Method method = null;
		boolean noMethodErr = false;
		
		Class<?> serviceCls = serviceObj.getClass();
		Object[] params = null;
		Class<?>[] parameterTypes = null;
		
		//simple调用
		if(paramMap.get("parameterType") != null && "callSimple".equals(paramMap.get("parameterType"))) {
			Map<String, Object> rpcParam = null;
			if(paramMap.get("parameterMap") != null) {
				rpcParam = JSONObject.parseObject(paramMap.get("parameterMap").toString(), Map.class);
			}
			RpcRequest rpcRequest = RpcRequest.genRpcRequest(rpcSession, rpcParam);
			params = new Object[] {rpcRequest};
			parameterTypes = new Class<?>[] {RpcRequest.class};
		} else {
			JsonRpcRequestParam param = new JsonRpcRequestParam();
			param.reset(paramMap);
			
			params = param.getParameters();
			parameterTypes = param.getParameterTypes();
		}
		
		try {
			if (parameterTypes == null) {
				method = serviceCls.getMethod(reqIn.getMethod());
			} else {
				method = serviceCls.getMethod(reqIn.getMethod(), parameterTypes);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			noMethodErr = true;
		}

		if (method == null || noMethodErr) {
			respOut.setError(JSONRPC2Error.METHOD_NOT_FOUND);
			return respOut;
		}
		
		try {
			result = method.invoke(serviceObj, params);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			respOut.setError(JSONRPC2Error.INVALID_REQUEST);
			return respOut;
		}
		
		if(result != null) {
			String resultJson = JSONObject.toJSONString(result);
			Map<String, String> resultMap = new HashMap<>();
			resultMap.put("resultType", method.getReturnType().getName());
			resultMap.put("resultJson", resultJson);
			respOut.setResult(resultMap);
		}
		return respOut;
	}

	public void doRequest(byte[] bytes, RpcSession rpcSession) {
		BinaryReader reader = new BinaryReader(bytes);

		String className = new String(reader.getBytesGroupWithShortLength());
		byte[] jsonBytes = reader.getBytesGroupWithIntLength();

		if (className == null || jsonBytes == null || jsonBytes.length == 0) {
			return;
		}
		
		requestES.execute(() -> {

			String jsonString = "";
			JSONRPC2Request reqIn = null;
			try {
				jsonString = new String(jsonBytes, "UTF-8");
				reqIn = JSONRPC2Request.parse(jsonString);
			} catch (JSONRPC2ParseException | UnsupportedEncodingException e) {
				e.printStackTrace();
				return;
			}
			JSONRPC2Response respOut = genJSONRPC2Response(className, reqIn, rpcSession);
			byte[] buf = Util.appendBytes(new byte[] {(byte) 1}, respOut.toString().getBytes());
			try {
				Util.writeAndFlush(dragoniteSocket, buf);
			} catch (IncorrectSizeException | SenderClosedException | InterruptedException | IOException e) {
				e.printStackTrace();
			}
		});

	}
	
	private void doResponse(byte[] bytes) {
		JSONRPC2Response respIn = null;

		try {
			String jsonString = new String(bytes, "UTF-8");
			respIn = JSONRPC2Response.parse(jsonString);
		} catch (JSONRPC2ParseException | UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		
		Long reqId = (Long) respIn.getID();
		
		RequestService requestService = requestMap.get(reqId);
		if(requestService == null) {
			return;
		}

		if(requestService.getCallbackFunc() != null) { //异步调用
			requestMap.remove(reqId);
			
			Object result = null;
			
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = (Map<String, String>) respIn.getResult();
			if(resultMap != null) {
				String resultJson = resultMap.get("resultJson");
				Class<?> returnType = null;
				try {
					returnType = Class.forName(resultMap.get("resultType"));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				if(returnType == null) {
					result = JSONObject.parseObject(resultJson);					
				} else {
					result = JSONObject.parseObject(resultJson, returnType);
				}
				
			}
			
			requestService.getCallbackFunc().callback(result);
			
			return;
		}
		
		requestService.setRespIn(respIn);
		synchronized (requestService.getReqLock()) {
			try {
				requestService.getReqLock().notifyAll();			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	

	public void run() {
		byte[] dataBuffer = null; // 数据缓存
		int dataLen = 0; // 数据长度
		int currentLen = 0; // 当前长度

		RpcSession rpcSession = RpcSession.genRpcSession(new JsonRpcServerClientServiceFactory(dragoniteSocket));
		if(rpcSessionListener != null) {
			rpcSessionListener.create(rpcSession);			
		}
		
		while (true) {
			try {
				byte[] buf = dragoniteSocket.read();

				int lenByteLen = 0;
				if (dataBuffer == null) {
					lenByteLen = 4;

					byte[] lenByte = new byte[lenByteLen];
					System.arraycopy(buf, 0, lenByte, 0, lenByteLen);

					dataLen = Util.Byte2Int(lenByte);
					dataBuffer = new byte[dataLen]; // 大数据块
					currentLen = 0;

				}

				int currentDataLen = buf.length - lenByteLen;
				System.arraycopy(buf, lenByteLen, dataBuffer, currentLen, currentDataLen);
				currentLen += currentDataLen;

				if (new Integer(currentLen).equals(new Integer(dataLen))) {

					byte flag = dataBuffer[0];
					byte[] newBytes = new byte[dataBuffer.length - 1];
					System.arraycopy(dataBuffer, 1, newBytes, 0, newBytes.length);
					/////
					if((byte) 1 == flag) {
						doRequest(newBytes, rpcSession);
													
					} else {
						doResponse(newBytes);
					}
					/////

					dataBuffer = null;
				}
			} catch (InterruptedException | ConnectionNotAliveException e) {
				if (dragoniteSocket != null) {
					try {
						dragoniteSocket.closeGracefully();
					} catch (SenderClosedException | InterruptedException | IOException e1) {
						e1.printStackTrace();
					}
				}
				break;
			} finally {
				
			}
		}
		RpcSessionPool.removeSession(rpcSession.getSessionId());
		if(rpcSessionListener != null) {
			rpcSessionListener.destroy(rpcSession);
		}
	}
}
