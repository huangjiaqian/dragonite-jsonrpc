package org.hjq.jsonrpc.example;

import java.util.Date;

public interface IUserDao {
	public String getUserName();
	public Page hello(String name, Integer age, Date time, Page page);
}
