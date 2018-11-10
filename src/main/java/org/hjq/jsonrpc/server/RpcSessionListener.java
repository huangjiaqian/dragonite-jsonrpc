package org.hjq.jsonrpc.server;

/**
 * 会话监听器
 * @author 黄钱钱
 *
 */
public interface RpcSessionListener {
	public void create(RpcSession rpcSession);
	public void destroy(RpcSession rpcSession);
}
