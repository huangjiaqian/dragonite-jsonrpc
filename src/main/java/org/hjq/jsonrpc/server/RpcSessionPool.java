package org.hjq.jsonrpc.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcSessionPool {
	public static final Map<String, RpcSession> RPC_SESSION_MAP = new ConcurrentHashMap<>();
	public static final RpcSession getSession(String sessionId) {
		return RPC_SESSION_MAP.get(sessionId);
	}
	public static final void removeSession(String sessionId) {
		RPC_SESSION_MAP.remove(sessionId);
	}
}
