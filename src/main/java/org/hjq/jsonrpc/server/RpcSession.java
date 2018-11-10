package org.hjq.jsonrpc.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.hjq.jsonrpc.client.JsonRpcServerClientServiceFactory;

public class RpcSession {
	
	public static final RpcSession genRpcSession(JsonRpcServerClientServiceFactory factory) {
		RpcSession session = new RpcSession(factory);
		RpcSessionPool.RPC_SESSION_MAP.put(session.getSessionId(), session);
		return session;
	}
	
	private JsonRpcServerClientServiceFactory factory;
	
	private String sessionId;
	
	private Map<String, Object> attrMap = new ConcurrentHashMap<>();
	
	private RpcSession(JsonRpcServerClientServiceFactory factory) {
		this.sessionId = UUID.randomUUID().toString();
		this.factory = factory;
	}
	
	
	public Object getAttr(String key) {
		return attrMap.get(key);
	}
	
	public void setAttr(String key, Object value) {
		attrMap.put(key, value);
	}
	
	public void removeAttr(String key) {
		attrMap.remove(key);
	}
	
	public String getSessionId() {
		return sessionId;
	}


	public JsonRpcServerClientServiceFactory getFactory() {
		return factory;
	}
}
