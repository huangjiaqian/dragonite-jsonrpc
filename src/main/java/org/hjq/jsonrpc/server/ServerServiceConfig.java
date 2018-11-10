package org.hjq.jsonrpc.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ServerServiceConfig {

	private Map<String, Object> serviceObjMap = null;

	public abstract void config(List<Object> cfgList);

	public synchronized void init(List<Object> cfgList) {
		if (serviceObjMap != null || cfgList == null || cfgList.isEmpty()) {
			return;
		}
		serviceObjMap = new HashMap<>();
		for (Object obj : cfgList) {
			serviceObjMap.put(obj.getClass().getName(), obj);
		}
	}

	public Object getService(String className) {
		if (serviceObjMap == null) {
			return null;
		}
		Object serviceObj = null;
		try {
			Class<?> interfaceCls = Class.forName(className);
			System.out.println(serviceObjMap.values());
			for (Object obj : serviceObjMap.values()) {
				if (interfaceCls.isAssignableFrom(obj.getClass())) {
					serviceObj = obj;
					break;
				}
			}

		} catch (Exception e) {

		}

		return serviceObj;
	}
}
