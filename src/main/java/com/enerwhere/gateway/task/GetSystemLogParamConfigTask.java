package com.enerwhere.gateway.task;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.enerwhere.gateway.EnerwhereDeviceRequests;
import com.enerwhere.gateway.EnerwherePackage;
import com.enerwhere.message.DeviceType;
import com.enerwhere.message.DevicesType;

//This task will populate the devices data for each device and form a list and add it in the package
public class GetSystemLogParamConfigTask extends GenericTask {

	public GetSystemLogParamConfigTask(EnerwhereDeviceRequests requestType) {
		super(requestType);
	}

	@Override
	public void processResponse(EnerwherePackage ewPkg) {
		// The getApiResponse will get the response from the API call in string. This is
		// set in the generic task
		String getSystemLogConfigResponse = getApiResponse();
		JSONParser parser = new JSONParser();
		DevicesType devices = new DevicesType();
		List<DeviceType> devicesList = devices.getDevices();
		DeviceType deviceData = null;
		try {
			Object obj = parser.parse(getSystemLogConfigResponse);
			JSONArray devicesArrayListFromResponse = (JSONArray) obj;

			// After getting the response this will covert it into an array of json and only
			// pick data where the name is either 'Active power' or 'Tank level' to get the
			// devices we need
			for (int i = 0; i < devicesArrayListFromResponse.size(); i++) {
				JSONObject deviceListFromJson = (JSONObject) devicesArrayListFromResponse.get(i);
				if (deviceListFromJson.get("name").toString().contains("Active power")
						|| deviceListFromJson.get("name").toString().contains("Tank level")) {
					deviceData = new DeviceType();
					// Formatting the names to make them uniform and storing it in the list of
					// devices
					deviceData.setDeviceName(deviceListFromJson.get("name").toString().replaceFirst("\\s", ""));
					deviceData.setId(deviceListFromJson.get("id").toString());
					deviceData.setName(deviceListFromJson.get("deviceName").toString().replaceAll("\\s", ""));
					deviceData.setUnit(deviceListFromJson.get("unit").toString());
					devicesList.add(deviceData);
				}
			}
			// Adding the devices list in the package to use later for analysis
			ewPkg.addAttribute("DevicesList", devices);
		} catch (Exception e) {
			System.out
					.println("Exception occured while prepareing response from get system log param config task " + e);
		}
	}

}