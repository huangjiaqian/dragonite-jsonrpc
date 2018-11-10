package org.hjq.jsonrpc.client;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hjq.jsonrpc.JsonRpcRequestParam;
import org.hjq.jsonrpc.server.ServerServiceConfig;
import org.hjq.jsonrpc.util.BinaryWriter;
import org.hjq.jsonrpc.util.Util;

import com.alibaba.fastjson.JSONObject;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class JsonRpcClientServiceFactory extends JsonRpcServiceFactory {


	public JsonRpcClientServiceFactory(DragoniteSocket dragoniteSocket) {
		super(dragoniteSocket, true, null);
	}

	public JsonRpcClientServiceFactory(DragoniteSocket dragoniteSocket, ServerServiceConfig serviceConfig) {
		super(dragoniteSocket, true, serviceConfig);
	}

	public void callSimple(Class<?> serviceClass, String methodName, CallbackFunc callbackFunc, Map<String , Object> param) {
		Long reqId = MethodProxy.genReqId();
		
		Map<String, Object> map = new HashMap<>();
		
		map.put("parameterType", "callSimple"); //普通调用
		map.put("parameterMap", JSONObject.toJSONString(param));
		
		// Create request
		JSONRPC2Request reqOut = new JSONRPC2Request(methodName, map, reqId);
		
		
		byte[] classNameBytes = serviceClass.getName().getBytes();
		byte[] reqOutBytes = reqOut.toString().getBytes();
		
		BinaryWriter writer = new BinaryWriter(classNameBytes.length + reqOutBytes.length + 6 + 1);
		writer.putSignedByte((byte) 1);
		writer.putBytesGroupWithShortLength(classNameBytes);
		writer.putBytesGroupWithIntLength(reqOutBytes);
		
		try {
			Util.writeAndFlush(dragoniteSocket, writer.toBytes());
		} catch (IncorrectSizeException | SenderClosedException | InterruptedException | IOException e) {
			e.printStackTrace();
			return;
		}
		
		RequestService requestService = new RequestService();
		requestService.setReqId(reqId);
		requestService.setCallbackFunc(callbackFunc);
		
		requestMap.put(reqId, requestService);
		
		
	}
	
	public void callAsync(Class<?> serviceClass, String methodName, CallbackFunc callbackFunc, Object[] params) {
		Long reqId = MethodProxy.genReqId();
		
		Method[] methods = Util.getMethodByName(serviceClass, methodName);
		if(methods == null || methods.length > 1) {
			// TODO 需要进行异常抛出
			return;
		}
		
		Method method = methods[0];
		
		JsonRpcRequestParam param = new JsonRpcRequestParam(method, params);
		Map<String, Object> paramMap = param.trans2Map();
		
		// Create request
		JSONRPC2Request reqOut = new JSONRPC2Request(method.getName(), paramMap, reqId);
		
		
		byte[] classNameBytes = serviceClass.getName().getBytes();
		byte[] reqOutBytes = reqOut.toString().getBytes();
		
		BinaryWriter writer = new BinaryWriter(classNameBytes.length + reqOutBytes.length + 6 + 1);
		writer.putSignedByte((byte) 1);
		writer.putBytesGroupWithShortLength(classNameBytes);
		writer.putBytesGroupWithIntLength(reqOutBytes);
		
		try {
			Util.writeAndFlush(dragoniteSocket, writer.toBytes());
		} catch (IncorrectSizeException | SenderClosedException | InterruptedException | IOException e) {
			e.printStackTrace();
			return;
		}
		
		RequestService requestService = new RequestService();
		requestService.setReqId(reqId);
		requestService.setCallbackFunc(callbackFunc);
		
		requestMap.put(reqId, requestService);
		
	}

}
