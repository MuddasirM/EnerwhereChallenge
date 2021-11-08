package com.enerwhere.message;

import java.util.Map;

public class DeviceType {

	protected String deviceName;
	protected String id;
	protected String name;
	protected String unit;
	protected Map<Integer, Object> loggedValues;

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public Map<Integer, Object> getLoggedValues() {
		return loggedValues;
	}

	public void setLoggedValues(Map<Integer, Object> loggedValues) {
		this.loggedValues = loggedValues;
	}

}
