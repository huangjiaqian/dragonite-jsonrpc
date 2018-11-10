package org.hjq.jsonrpc.client;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class RequestService {
	
	private Long reqId;
	
	private JSONRPC2Response respIn; //返回信息
	
	private Object reqLock = new Object();
	
	private CallbackFunc callbackFunc;

	public Long getReqId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public JSONRPC2Response getRespIn() {
		return respIn;
	}

	public void setRespIn(JSONRPC2Response respIn) {
		this.respIn = respIn;
	}

	public Object getReqLock() {
		return reqLock;
	}

	public CallbackFunc getCallbackFunc() {
		return callbackFunc;
	}

	public void setCallbackFunc(CallbackFunc callbackFunc) {
		this.callbackFunc = callbackFunc;
	}
	
}
