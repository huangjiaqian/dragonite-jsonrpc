package org.hjq.jsonrpc.test;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.hjq.jsonrpc.client.CallbackFunc;
import org.hjq.jsonrpc.client.JsonRpcClientServiceFactory;
import org.hjq.jsonrpc.example.IUserDao;
import org.hjq.jsonrpc.example.Page;

import com.vecsight.dragonite.sdk.config.DragoniteSocketParameters;
import com.vecsight.dragonite.sdk.socket.DragoniteClientSocket;

public class ClientTest {
	public static void main(String[] args) throws SocketException {
		InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 12222);
		DragoniteClientSocket dragoniteSocket = new DragoniteClientSocket(remoteAddress, 1024 * 100, new DragoniteSocketParameters());
		JsonRpcClientServiceFactory factory = new JsonRpcClientServiceFactory(dragoniteSocket);
		IUserDao userDao = factory.getService(IUserDao.class);
		long start = System.currentTimeMillis();
		System.out.println(userDao.getUserName());
		
		CountDownLatch latch = new CountDownLatch(1000);
		
		for(int i = 0;i < 1000;i++) {
			
			//System.out.println(userDao.hello("小李" + i, i*2, new Date(), new Page(2, i)));
			factory.callAsync(IUserDao.class, "hello", new CallbackFunc() {
				
				@Override
				public void callback(Object result) {
					System.out.println(result);
					latch.countDown();
				}
				
			}, new Object[] {"小李" + i, i*2, new Date(), new Page(2, i)});
		}
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		for(int i = 0;i < 1000;i++) {
			System.out.println(userDao.hello("小李" + i, i*2, new Date(), new Page(2, i)));
		}
		*/
		
		long end = System.currentTimeMillis();
		System.out.println("耗时:" + (end - start) + "ms");
		
		/*
		Long i = 10L;
		System.out.println(i.intValue());
		*/
	}
}
