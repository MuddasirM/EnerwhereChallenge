package com.enerwhere.gateway;

import java.util.HashMap;
import java.util.Map;

public class EnerwherePackage {

	public Map<String, Object> attributes= new HashMap<String, Object>();

	public void addAttribute(String key,Object value) {
		attributes.put(key, value);
	}
	
	public Object getAttribute(String key) {
		return attributes.get(key) == null ? null :attributes.get(key);
	}
	
}
