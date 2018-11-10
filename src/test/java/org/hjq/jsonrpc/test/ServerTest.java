package org.hjq.jsonrpc.test;

import java.net.SocketException;
import java.util.List;

import org.hjq.jsonrpc.example.UserDao;
import org.hjq.jsonrpc.server.JsonRpcServer;
import org.hjq.jsonrpc.server.ServerServiceConfig;

import com.vecsight.dragonite.sdk.config.DragoniteSocketParameters;
import com.vecsight.dragonite.sdk.socket.DragoniteServer;

public class ServerTest {
	public static void main(String[] args) throws SocketException {
		DragoniteServer dragoniteServer = new DragoniteServer(12222, 1024 * 100, new DragoniteSocketParameters());
		JsonRpcServer jsonRpcServer = new JsonRpcServer(dragoniteServer, new ServerServiceConfig() {
			
			@Override
			public void config(List<Object> cfgList) {
				
				cfgList.add(new UserDao());
				
			}
		});
		jsonRpcServer.start();
	}
}
