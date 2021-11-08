package com.enerwhere.message;

import java.util.ArrayList;
import java.util.List;

public class DevicesType {
	protected List<DeviceType> devices;

	public List<DeviceType> getDevices() {
		if (devices == null) {
			devices = new ArrayList<DeviceType>();
		}
		return this.devices;
	}

}