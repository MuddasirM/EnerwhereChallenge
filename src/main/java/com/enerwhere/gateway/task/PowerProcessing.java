package com.enerwhere.gateway.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.enerwhere.gateway.EnerwhereDeviceRequests;
import com.enerwhere.gateway.EnerwherePackage;
import com.enerwhere.message.DeviceType;

public class PowerProcessing extends GetSystemLogParamConfigTask {

	public PowerProcessing(EnerwhereDeviceRequests requestType) {
		super(requestType);
	}

	// This method will process generator and inverter data from the Json Response
	protected void processPowerData(JSONArray valuesArrayListFromResponse, DeviceType deviceData,
			EnerwherePackage ewPkg) {
		Map<Integer, Object> loggedValues = null;
		try {
			// If the start date contains the time value then the value is taken
			// This means that the call was made for a device with data present in the
			// logged value map
			String timestampFromUrl = ewPkg.getAttribute("startDateForApiCall").toString().contains("T")
					? ewPkg.getAttribute("startDateForApiCall").toString()
					: null;
			String dayInStringFromUrl = null;
			String hourInStringFromUrl = null;
			int dayInIntFromUrl = -1;
			// IF timestamp value in the url is not null then it will take the hour value
			// and the day value from it to use for the calculations
			// ELSE the values are taken from the start value stored in the package
			if (timestampFromUrl != null) {
				String splitUrlTimestamp[] = timestampFromUrl.split("T");
				dayInStringFromUrl = splitUrlTimestamp[0].substring(splitUrlTimestamp[0].length() - 2);
				hourInStringFromUrl = splitUrlTimestamp[1].substring(0, 2);
			} else {
				String dateFromPackage = ewPkg.getAttribute("startDateForApiCall").toString();
				dayInStringFromUrl = dateFromPackage.substring(dateFromPackage.length() - 2);
				dayInIntFromUrl = Integer.parseInt(dayInStringFromUrl);
			}
			Map<Integer, Float> hourPowerMap = null;
			List<Float> powerValuePerMin = null;
			if (valuesArrayListFromResponse.size() == 0) {
				// No Record Found
				ewPkg.addAttribute("generatorProcessed", "DONE");
			} else {
				// IF there is no data in the device it will create a new map and add it in the
				// device object
				// ELSE new maps are created to store data indicating that this is the first
				// call for the first day of the analysis period
				if ((Boolean) ewPkg.getAttribute("hourAnalysis")) {
					if (deviceData.getLoggedValues() != null) {
						loggedValues = deviceData.getLoggedValues();
						if (!deviceData.getLoggedValues().containsKey(dayInIntFromUrl)) {
							hourPowerMap = new HashMap<Integer, Float>();
							loggedValues.put(dayInIntFromUrl, hourPowerMap);
						}
					} else {
						loggedValues = new HashMap<Integer, Object>();
						hourPowerMap = new HashMap<Integer, Float>();
						loggedValues.put(dayInIntFromUrl, hourPowerMap);
					}
				} else {
					if (deviceData.getLoggedValues() != null) {
						loggedValues = deviceData.getLoggedValues();
						if (!deviceData.getLoggedValues().containsKey(dayInIntFromUrl)) {
							powerValuePerMin = new ArrayList<Float>();
							loggedValues.put(dayInIntFromUrl, powerValuePerMin);
						}
					} else {
						loggedValues = new HashMap<Integer, Object>();
						powerValuePerMin = new ArrayList<Float>();
						loggedValues.put(dayInIntFromUrl, powerValuePerMin);
					}
				}
				int i;
				// The array size is checked to process the last record if the count of the
				// array > 1
				if (valuesArrayListFromResponse.size() == 1) {
					i = 0;
					loggedValues = processRecords(i, valuesArrayListFromResponse, hourInStringFromUrl, loggedValues,
							ewPkg, hourPowerMap, deviceData, powerValuePerMin);
					ewPkg.addAttribute("generatorProcessed", "DONE");
				}
				for (i = 0; i < valuesArrayListFromResponse.size(); i++) {
					loggedValues = processRecords(i, valuesArrayListFromResponse, hourInStringFromUrl, loggedValues,
							ewPkg, hourPowerMap, deviceData, powerValuePerMin);
				}
				deviceData.setLoggedValues(loggedValues);
			}
		} catch (Exception e) {
			System.out.println("Exception occured while processPowerData for id :  Exception - " + e);
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unchecked")
	private Map<Integer, Object> processRecords(int i, JSONArray valuesArrayListFromResponse,
			String hourInStringFromUrl, Map<Integer, Object> loggedValues, EnerwherePackage ewPkg,
			Map<Integer, Float> hourPowerMap, DeviceType deviceData, List<Float> powerValuePerMin) {
		try {
			JSONObject valueFromJson = (JSONObject) valuesArrayListFromResponse.get(i);
			Float value = Float.parseFloat(valueFromJson.get("value").toString());
			String dateTimeInString[] = valueFromJson.get("timestamp").toString().split("T");
			String hourFromResponseInString = dateTimeInString[1].substring(0, 2);
			int hourFromResponseInInt = Integer.parseInt(hourFromResponseInString);
			String dayFromResponseInString = dateTimeInString[0].substring(dateTimeInString[0].length() - 2);
			int dayFromResponseInInt = Integer.parseInt(dayFromResponseInString);
			// getting sum of all the values per hour if loggedValues map is made
			// by getting the hour power map from the devices logged value map using the day
			// and hour from the first timestamp of the request
			// Then the value is added into the total sum of the hour and inserted back in
			// the logged value map
			if ((Boolean) ewPkg.getAttribute("hourAnalysis")) {
				if (loggedValues != null) {
					hourPowerMap = (Map<Integer, Float>) loggedValues.get(dayFromResponseInInt);
					if (hourPowerMap == null) {
						hourPowerMap = new HashMap<Integer, Float>();
					}
					if (hourPowerMap.containsKey(hourFromResponseInInt)) {
						Float valueSum = (Float) hourPowerMap.get(hourFromResponseInInt);
						valueSum += value;
						hourPowerMap.put(hourFromResponseInInt, valueSum);
						loggedValues.put(dayFromResponseInInt, hourPowerMap);
					} else {
						hourPowerMap.put(hourFromResponseInInt, value);
						loggedValues.put(dayFromResponseInInt, hourPowerMap);
					}
				}
			} else {
				if (loggedValues != null) {
					powerValuePerMin = (List<Float>) loggedValues.get(dayFromResponseInInt);
					if (powerValuePerMin.isEmpty()) {
						powerValuePerMin = new ArrayList<Float>();
					}
					// get Min value for the hour min from timestamp
					int minOfDay = getMinOfDay(dateTimeInString[1]);
					// get last populated index in list and subtract from that to min of day
					// add 0 from last populated value to min of day index
					int lastIndex = powerValuePerMin.size();
					if (lastIndex != minOfDay) {
						// If any value is missing in the response of the API that value is stored as 0
						// to maintain the minuite order
						for (; lastIndex < minOfDay; lastIndex++) {
							powerValuePerMin.add(0f);
						}
					}
					powerValuePerMin.add(value);
					loggedValues.put(dayFromResponseInInt, powerValuePerMin);

				}
			}
			// Setting the last timestamp as the start date for the next call
			if (i == 1 || i == valuesArrayListFromResponse.size() - 1) {
				ewPkg.addAttribute("startDateForApiCall", valueFromJson.get("timestamp").toString());
			}
		} catch (Exception e) {
			System.out.println(" Exception occured in processRecords " + e);
		}
		return loggedValues;
	}

	// This method will convert the hour and minute from the timestamp to the total
	// mins value in the day
	private int getMinOfDay(String time) {
		int hour;
		int minute;
		int minuteOfDay;
		hour = Integer.parseInt(time.substring(0, 2));
		minute = Integer.parseInt(time.substring(3, 5));
		minuteOfDay = (hour) * 60 + minute;
		return minuteOfDay;
	}

}
