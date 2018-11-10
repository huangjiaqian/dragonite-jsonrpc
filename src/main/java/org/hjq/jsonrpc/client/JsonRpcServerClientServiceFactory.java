package org.hjq.jsonrpc.client;

import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class JsonRpcServerClientServiceFactory extends JsonRpcServiceFactory {
	
	public JsonRpcServerClientServiceFactory(DragoniteSocket dragoniteSocket) {
		super(dragoniteSocket, false, null);
	}
	
}
