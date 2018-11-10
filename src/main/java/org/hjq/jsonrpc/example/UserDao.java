package org.hjq.jsonrpc.example;

import java.util.Date;

public class UserDao implements IUserDao {

	@Override
	public String getUserName() {
		System.out.println("客户端调用了getUserName方法！");
		return "小王";
	}

	@Override
	public Page hello(String name, Integer age, Date time, Page page) {
		System.out.println("hello " + name + "  " + age + " " + time + "  " + page);
		return page;
	}

}
