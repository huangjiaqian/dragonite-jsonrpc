package org.hjq.jsonrpc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;

public class JsonRpcRequestParam {
	
	private Class<?>[] parameterTypes;
	
	private Object[] parameters;

	
	public JsonRpcRequestParam() {
		super();
		// TODO Auto-generated constructor stub
	}

	public JsonRpcRequestParam(Method method, Object[] parameters) {
		this.parameters = parameters;
		this.parameterTypes = method.getParameterTypes();
	}
	
	private String parseTypesJson() {
		if(parameterTypes == null || parameterTypes.length == 0) {
			return null;
		}
		List<Object> typeNameList = new ArrayList<>(parameterTypes.length);
		for(int i = 0;i < parameterTypes.length;i++) {
			typeNameList.add(parameterTypes[i].getName());
		}
		JSONArray array = new JSONArray(typeNameList);
		return array.toJSONString();
	}
	
	private String parseParamJson() {
		if(parameters == null || parameters.length == 0) {
			return null;
		}
		JSONArray array = new JSONArray(Arrays.asList(parameters));
		return array.toJSONString();
	}
	
	private void resetType(String jsonStr) {
		if(jsonStr == null) {
			return;
		}
		JSONArray array = JSONArray.parseArray(jsonStr);
		Object[] typeNames = array.toArray();
		
		parameterTypes = new Class<?>[typeNames.length];
		
		for (int i = 0;i < typeNames.length;i++) {
			try {
				parameterTypes[i] = Class.forName((String)typeNames[i]);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void resetParam(String jsonStr) {
		if(jsonStr == null) {
			return;
		}
		List<Object> array = JSONArray.parseArray(jsonStr, this.parameterTypes);
		parameters = array.toArray();
	}
	
	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}
	
	public Map<String, Object> trans2Map() {
		Map<String, Object> map = new HashMap<>();
		map.put("parameterTypes", parseTypesJson());
		map.put("parameters", parseParamJson());
		return map;
	}
	
	public void reset(Map<String, Object> map) {
		resetType((String) map.get("parameterTypes"));
		resetParam((String) map.get("parameters"));
	}
	
}
