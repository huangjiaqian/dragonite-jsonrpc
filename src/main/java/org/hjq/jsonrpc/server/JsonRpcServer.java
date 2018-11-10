package org.hjq.jsonrpc.server;

import java.util.ArrayList;
import java.util.List;

import com.vecsight.dragonite.sdk.socket.DragoniteServer;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class JsonRpcServer {
	
	private DragoniteServer dragoniteServer;
	
	private Thread acceptThread;
	
	private ServerServiceConfig serviceConfig;
	
	private RpcSessionListener rpcSessionListener;

	public JsonRpcServer(DragoniteServer dragoniteServer, ServerServiceConfig serviceConfig) {
		this(dragoniteServer, serviceConfig, null);
	}
	
	public JsonRpcServer(DragoniteServer dragoniteServer, ServerServiceConfig serviceConfig, RpcSessionListener rpcSessionListener) {
		super();
		
		List<Object> cfgList = new ArrayList<>();
		serviceConfig.config(cfgList);
		serviceConfig.init(cfgList);
		
		this.dragoniteServer = dragoniteServer;
		this.serviceConfig = serviceConfig;
		this.rpcSessionListener = rpcSessionListener;
	}
	
	public void start() {
		if((acceptThread != null && acceptThread.isAlive()) || dragoniteServer == null) {
			return;
		}
		acceptThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					DragoniteSocket socket = null;
					while((socket = dragoniteServer.accept()) != null) {
						clientHandler(socket);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, "JSON-RPC-ACCEPT");
		
		acceptThread.start();
	}
	
	public void clientHandler(DragoniteSocket socket) {
		JsonRpcClientHandler handler = new JsonRpcClientHandler(socket, serviceConfig, rpcSessionListener);
		new Thread(handler::run, "JSON-RPC-READ-SOCKET").start();
	}
	
	public void stop() {
		acceptThread.interrupt();
		dragoniteServer.destroy();
	}
}
