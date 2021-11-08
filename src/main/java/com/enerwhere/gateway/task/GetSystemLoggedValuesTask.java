package com.enerwhere.gateway.task;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.enerwhere.gateway.EnerwhereDeviceRequests;
import com.enerwhere.gateway.EnerwherePackage;
import com.enerwhere.message.DeviceType;

public class GetSystemLoggedValuesTask extends GenericTask {

	public GetSystemLoggedValuesTask(EnerwhereDeviceRequests requestType) {
		super(requestType);
	}

	TankProcessing tankP = new TankProcessing(this.request);
	PowerProcessing powerP = new PowerProcessing(this.request);

	@Override
	// This method will prepare the URl for the Logged values call setting the start
	// date, end data and order in the URL and setting it in the generic task setter
	// for API call
	public void prepareUrl(EnerwhereDeviceRequests requestType, EnerwherePackage ewPkg) {
		super.prepareUrl(requestType, ewPkg);
		DeviceType deviceData = (DeviceType) ewPkg.getAttribute("device");
		String path = getUrl();
		path = path.replace("{id}", deviceData.getId());
		path = path.replace("{startdate}", (String) ewPkg.getAttribute("startDateForApiCall"));
		path = path.replace("{enddate}", (String) ewPkg.getAttribute("endDateForApiCall"));
		path = path.replace("{order}", (String) ewPkg.getAttribute("sortOrder"));
		setUrl(path);
	}

	@Override
	// This method will process the data we get for the get logged values call
	public void processResponse(EnerwherePackage ewPkg) {
		String getSystemLogggedValuesResponse = getApiResponse();
		JSONParser parser = new JSONParser();
		DeviceType deviceData = (DeviceType) ewPkg.getAttribute("device");

		try {
			Object obj = parser.parse(getSystemLogggedValuesResponse);
			JSONArray valuesArrayListFromResponse = (JSONArray) obj;
			// The data received from the APIs will be processed differently for tank and power sources 
			if (deviceData.getUnit().equals("l")) {
				tankP.processLiterData(valuesArrayListFromResponse, deviceData, ewPkg);
			} else if (deviceData.getUnit().equals("kW")) {
				powerP.processPowerData(valuesArrayListFromResponse, deviceData, ewPkg);
			}
		} catch (Exception e) {
			System.out.println("Exception occured while getting logged values for : " + deviceData.getDeviceName()
					+ " Exception - " + e);
		}
	}

}
