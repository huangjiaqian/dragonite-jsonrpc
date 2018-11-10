package org.hjq.jsonrpc.server;

import java.util.Map;
import java.util.UUID;

public class RpcRequest {
	
	
	public static final RpcRequest genRpcRequest(RpcSession rpcSession, Map<String, Object> paramMap) {
		RpcRequest request = new RpcRequest(rpcSession, paramMap);
		return request;
	}
	
	public static final RpcRequest genRpcRequest() {
		RpcRequest request = new RpcRequest();
		return request;
	}
	
	private RpcSession rpcSession;
	
	private String requestId;
	
	private Map<String, Object> paramMap;
	
	private RpcRequest() {
		requestId = UUID.randomUUID().toString();
	}
	private RpcRequest(RpcSession rpcSession, Map<String, Object> paramMap) {
		this();
		this.rpcSession = rpcSession;
		this.paramMap = paramMap;
	}
	
	public Object getParam(String key) {
		return paramMap == null ? null : paramMap.get(key);
	}
	
	public String getRequestId() {
		return requestId;
	}
	
	public RpcSession getRpcSession() {
		return rpcSession;
	}
}
