package org.hjq.jsonrpc.client;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.hjq.jsonrpc.JsonRpcRequestParam;
import org.hjq.jsonrpc.util.BinaryWriter;
import org.hjq.jsonrpc.util.Util;

import com.alibaba.fastjson.JSONObject;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class MethodProxy implements InvocationHandler {
	
	private String className;
	
	private Map<Long, RequestService> requestMap;
	
	private DragoniteSocket dragoniteSocket;
	
	private boolean sendToServer = true; //客户端发送到服务器
	
	public MethodProxy(Map<Long, RequestService> requestMap, String className, DragoniteSocket dragoniteSocket, boolean sendToServer) {
		super();
		this.requestMap = requestMap;
		this.className = className;
		this.dragoniteSocket = dragoniteSocket;
		this.sendToServer = sendToServer;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 如果传进来是一个已实现的具体类（本次演示略过此逻辑)
		if (Object.class.equals(method.getDeclaringClass())) {
			try {
				return method.invoke(this, args);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			// 如果传进来的是一个接口（核心)
		} else {
			return run(method, args);
		}
		return null;
	}

	/**
	 * 实现接口的核心方法 
	 * @param method
	 * @param args
	 * @return
	 */
	public Object run(Method method, Object[] params) {

		Long reqId = genReqId();		
		/*
		System.out.println("Creating new request with properties :");
		System.out.println("\tmethod     : " + method);
		System.out.println("\tparameters : " + params);
		System.out.println("\tid         : " + reqId + "\n\n");
		*/
		
		
		JsonRpcRequestParam param = new JsonRpcRequestParam(method, params);
		Map<String, Object> paramMap = param.trans2Map();
		
		// Create request
		JSONRPC2Request reqOut = new JSONRPC2Request(method.getName(), paramMap, reqId);
		
		byte[] classNameBytes = className.getBytes();
		byte[] reqOutBytes = reqOut.toString().getBytes();
		
		BinaryWriter writer = new BinaryWriter(classNameBytes.length + reqOutBytes.length + 6 + 1);
		
		writer.putSignedByte(sendToServer ? (byte) 1 : (byte) 2);//第一个为标识位
		
		writer.putBytesGroupWithShortLength(classNameBytes);
		writer.putBytesGroupWithIntLength(reqOutBytes);
		
		try {
			Util.writeAndFlush(dragoniteSocket, writer.toBytes());
		} catch (IncorrectSizeException | SenderClosedException | InterruptedException | IOException e) {
			e.printStackTrace();
			return null;
		}
		
		RequestService requestService = new RequestService();
		requestService.setReqId(reqId);
		requestMap.put(reqId, requestService);
		
		synchronized (requestService.getReqLock()) {
			try {
				requestService.getReqLock().wait(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		requestMap.remove(reqId);
		
		JSONRPC2Response respIn = requestService.getRespIn();
		if(respIn == null) {
			return null;
		}
		
		//TODO
		// Check for success or error
		/*
		if (respIn.indicatesSuccess()) {
			System.out.println("The request succeeded :");

			System.out.println("\tresult : " + respIn.getResult());
			System.out.println("\tid     : " + respIn.getID());
			
		} else {
			
			System.out.println("The request failed :");

			JSONRPC2Error err = respIn.getError();

			System.out.println("\terror.code    : " + err.getCode());
			System.out.println("\terror.message : " + err.getMessage());
			System.out.println("\terror.data    : " + err.getData());
			
		}
		*/
		
		Object result = null;
		
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = (Map<String, String>) respIn.getResult();
		if(resultMap != null) {
			String resultJson = resultMap.get("resultJson");
			result = JSONObject.parseObject(resultJson, method.getReturnType());
		}
		
		return result;
	}

	
	private static volatile Long currentSocketId = 0L;
	
	private static final Long MAX_REQ_ID = Long.MAX_VALUE - 1;
	
	public static final synchronized Long genReqId() {
		currentSocketId++;
		if (currentSocketId > MAX_REQ_ID) {
			currentSocketId = 1L;
		}
		
		return currentSocketId;
	}
}
