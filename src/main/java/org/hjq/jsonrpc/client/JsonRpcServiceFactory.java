package org.hjq.jsonrpc.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hjq.jsonrpc.JsonRpcRequestParam;
import org.hjq.jsonrpc.server.ServerServiceConfig;
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

public class JsonRpcServiceFactory {
	private ExecutorService requestES = Executors.newCachedThreadPool();
	
	protected Map<Long, RequestService> requestMap;
	
	protected DragoniteSocket dragoniteSocket;

	protected Thread readThread;
	
	protected boolean sendToServer = true; //客户端发送到服务器
	
	protected ServerServiceConfig serviceConfig;
	
	public JsonRpcServiceFactory(DragoniteSocket dragoniteSocket, boolean sendToServer, ServerServiceConfig serviceConfig) {
		super();
		this.dragoniteSocket = dragoniteSocket;
		requestMap = new ConcurrentHashMap<>();
		this.sendToServer = sendToServer;
		this.serviceConfig = serviceConfig;
		if(serviceConfig != null) {
			List<Object> cfgList = new ArrayList<>();
			serviceConfig.config(cfgList);
			serviceConfig.init(cfgList);
		}
		if(sendToServer) { //客户端才启动读取线程
			startRead();
		}
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
	
	private void doRequest(byte[] bytes) {
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
			JSONRPC2Response respOut = genJSONRPC2Response(className, reqIn);
			byte[] buf = Util.appendBytes(new byte[] {(byte) 2}, respOut.toString().getBytes());
			try {
				Util.writeAndFlush(dragoniteSocket, buf);
			} catch (IncorrectSizeException | SenderClosedException | InterruptedException | IOException e) {
				e.printStackTrace();
			}
		});

	}
	
	private JSONRPC2Response genJSONRPC2Response(String className, JSONRPC2Request reqIn) {
		Object result = null;
		JSONRPC2Response respOut = new JSONRPC2Response(result, reqIn.getID());
		Object serviceObj = this.serviceConfig.getService(className);
		System.out.println("输出了：" + className + " " + reqIn.getMethod() + " " + serviceObj);
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
		

		JsonRpcRequestParam param = new JsonRpcRequestParam();
		param.reset(paramMap);
		
		params = param.getParameters();
		parameterTypes = param.getParameterTypes();

		System.out.println(serviceCls + "  " + reqIn.getMethod());
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

	private void startRead() {
		readThread = new Thread(() -> {
			byte[] dataBuffer = null; // 数据缓存
			int dataLen = 0; // 数据长度
			int currentLen = 0; // 当前长度

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
						if((byte) 1 == flag ) {
							doResponse(newBytes); //客户端接收到服务器返回的数据							
						} else {
							doRequest(newBytes); //处理服务器请求
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
				}
			}

		});
		
		readThread.start();
	}
	
	
	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> serviceCls) {
		MethodProxy invocationHandler = new MethodProxy(requestMap, serviceCls.getName(), dragoniteSocket, sendToServer);
		Object newProxyInstance = Proxy.newProxyInstance(serviceCls.getClassLoader(), new Class[] { serviceCls },
				invocationHandler);
		return (T) newProxyInstance;
	}

}
