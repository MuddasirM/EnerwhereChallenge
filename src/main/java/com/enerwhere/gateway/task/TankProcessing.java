package com.enerwhere.gateway.task;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.enerwhere.gateway.EnerwhereDeviceRequests;
import com.enerwhere.gateway.EnerwherePackage;
import com.enerwhere.message.DeviceType;

public class TankProcessing extends GetSystemLogParamConfigTask {

	public TankProcessing(EnerwhereDeviceRequests requestType) {
		super(requestType);
		// TODO Auto-generated constructor stub
	}

	protected void processLiterData(JSONArray valuesArrayListFromResponse, DeviceType deviceData,
			EnerwherePackage ewPkg) {
		// If the take was refueled the processing will be done separately
		if (ewPkg.getAttribute("refueled") != null
				&& String.valueOf(ewPkg.getAttribute("refueled")).equals("PROCESS")) {
			handleRefuelingSceanrio(valuesArrayListFromResponse, deviceData, ewPkg);
		} else {
			handleFuelConsumtion(valuesArrayListFromResponse, deviceData, ewPkg);
		}
	}

	// This method will handle the case where the take was filled
	private void handleRefuelingSceanrio(JSONArray valuesArrayListFromResponse, DeviceType deviceData,
			EnerwherePackage ewPkg) {
		// Getting the day from timestamp to retrieve the map from device data
		String dateForMap = (String) ewPkg.getAttribute("startDateForApiCall").toString().split("T")[0];
		int dayForMap = Integer.valueOf(dateForMap.substring(dateForMap.length() - 2));
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(valuesArrayListFromResponse.toString());
			JSONArray valueArrayListFromResponse = (JSONArray) obj;
			Float litersConsumed = 0f;
			Float value1 = null;
			Float value2 = null;
			// IF no record is found in the json response the tag will be set to do and the
			// code will exit the while loop
			// ELSE it will pick the current liters value and the previous liters value and
			// subtract it
			// IF the value is -ve the fuel was consumed so that is stored in a variable
			// litersConsumed
			// ELSE it is ignored assuming that that was added and not consumed
			// IF the last value of the array is reached the liters from the data is used to
			// add it with the litersConsumed variable to find the total consumed liters for
			// that loop and stored back in the device data map for that day
			if (valueArrayListFromResponse.size() == 0) {
				ewPkg.addAttribute("refueled", "DONE");
			} else if (valueArrayListFromResponse.size() == 1) {
				ewPkg.addAttribute("endDateForApiCall", (String) ewPkg.getAttribute("refuelingDay"));
			} else {
				for (int i = 0; i < valueArrayListFromResponse.size(); i++) {
					JSONObject valueFromJson = (JSONObject) valueArrayListFromResponse.get(i);
					if (i == 0) {
						continue;
					} else {
						JSONObject previousValueFromJson = (JSONObject) valueArrayListFromResponse.get(i - 1);
						value1 = Float.parseFloat(previousValueFromJson.get("value").toString());
						value2 = Float.parseFloat(valueFromJson.get("value").toString());
						if ((value2 - value1) < 0) {
							litersConsumed += (-(value2 - value1));
						}
					}
					if (i == valueArrayListFromResponse.size() - 1) {
						ewPkg.addAttribute("refuelingDay", valueFromJson.get("timestamp").toString());
						Float totalConsumed = (Float) deviceData.getLoggedValues().get(dayForMap);
						if (totalConsumed < 0f) {
							totalConsumed = 0f;
							deviceData.getLoggedValues().put(dayForMap, totalConsumed);
						} else {
							totalConsumed += litersConsumed;
							deviceData.getLoggedValues().put(dayForMap, totalConsumed);
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Exception occured while processing refueling data " + e);
		}
	}

	// This method will only get the first value from the response and use the time
	// stamp to get the day and its liters values for that instance
	private void handleFuelConsumtion(JSONArray valuesArrayListFromResponse, DeviceType deviceData,
			EnerwherePackage ewPkg) {
		Map<Integer, Object> loggedValues = null;
		// As this call is made in asc and desc to then subtract the value it gets from
		// the start of the day with the value it gets from the end of the day, its
		// reading only the first value
		JSONObject valueFromJson = (JSONObject) valuesArrayListFromResponse.get(0);
		// Time stamp is used to get the day. The day is used to check in the existing
		// map if the entry is added or not
		String timestamp = valueFromJson.get("timestamp").toString();
		String splitTimestamp[] = timestamp.split("T");
		int day = Integer.parseInt(splitTimestamp[0].substring(splitTimestamp[0].length() - 2));
		Float litersValue = Float.parseFloat(valueFromJson.get("value").toString());
		// IF the device data contains the map with the logged
		// values(Day,LitersConsumed) it get the liters for that day and subtract the
		// stored value with the value its getting from the API response and adding it
		// back in the map
		// ELSE it will add the day with the liters value
		// IF the device doesn't contain a Map of day to liters consumed it will create
		// a new map and add the day and liters value in it
		if (deviceData.getLoggedValues() != null) {
			loggedValues = deviceData.getLoggedValues();
			if (loggedValues.containsKey(day)) {
				Float fuelConsumed = (Float) loggedValues.get(day) - litersValue;
				// Check to see if the day was used to refill the tank.
				checkIfTankWasFilled(fuelConsumed, ewPkg, deviceData);
				loggedValues.put(day, fuelConsumed);
			} else {
				loggedValues.put(day, litersValue);
			}
		} else {
			loggedValues = new HashMap<Integer, Object>();
			loggedValues.put(day, litersValue);
		}
		// Setting the logged values map in the device
		deviceData.setLoggedValues(loggedValues);
	}

	// This method will check to see if the tank was refueled
	// **On the period provided only one value was noted to be -ve, if multiple days
	// are found this code will have to be updated to store the values of the days
	// in a list**
	private void checkIfTankWasFilled(Float fuelConsumed, EnerwherePackage ewPkg, DeviceType deviceData) {
		// IF the value of the fuel consumed is less than 0 that would indicate that it
		// was filled so this method will store the data needed to re process that day
		if (fuelConsumed < 0) {
			ewPkg.addAttribute("refuelingDay", (String) ewPkg.getAttribute("startDateForApiCall"));
			ewPkg.addAttribute("tankId", deviceData.getId());
			ewPkg.addAttribute("refueledDevice", deviceData);
			System.out.println("Date for reprocessing  " + (String) ewPkg.getAttribute("refuelingDay"));
			// Setting the refueled tag to START here which will be used to indicate that
			// re processing is needed
			ewPkg.addAttribute("refueled", "START");
		}
	}

}
