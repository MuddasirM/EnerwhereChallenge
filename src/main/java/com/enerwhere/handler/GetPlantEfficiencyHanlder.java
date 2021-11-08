package com.enerwhere.handler;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.enerwhere.gateway.EnerwhereDeviceRequests;
import com.enerwhere.gateway.EnerwhereGateway;
import com.enerwhere.gateway.EnerwherePackage;
import com.enerwhere.message.DeviceType;
import com.enerwhere.message.DevicesType;

//Handler to start data collecting and analysis for the time period set in the startDate and endDate variables 
public class GetPlantEfficiencyHanlder {
	public static final String startTime = "T00:00:00Z";
	public static final String endTime = "T23:59:00Z";
	public static final String HOUR = "Hour";
	public static final String MINUTE = "Minute";

	public static void main(String[] args) {
		// Start date and end date format in string "dd MMM yyyy"
		String startDate = "26 Oct 2021";
		// End date means the count will stop one day before. End date day will not be
		// counted
		String endDate = "02 Nov 2021";
		// Default analysis will be done in Hours. If 'Minute'(case sensitive) value is
		// passed in the
		// variable minute wise analysis is done
		String analysisUnit = "Hour";
		try {
			// Package created to store and pass data using a single object. This package is
			// an map of String and object.
			EnerwherePackage ewPackage = new EnerwherePackage();
			// method to validate id the date passed is in the right format.
			validateAndPrepareDates(startDate, endDate, ewPackage);
			if (analysisUnit.equals("Minute")) {
				ewPackage.addAttribute("hourAnalysis", false);
			} else {
				ewPackage.addAttribute("hourAnalysis", true);
			}
			// The gateway created a generic code that will be used for all external API
			// calls
			EnerwhereGateway gateway = new EnerwhereGateway();

			// First call made is to get the data of the required devices (DGs, Solar
			// Inverter, tank).
			// Here we will decide which call to make based on the request we pass in the
			// arguments of the method.
			gateway.sendEnerwhereRequest(EnerwhereDeviceRequests.GET_SYSTEM_LOG_PARAMETER_CONFIG, ewPackage);

			// Then using the data we got from the first call we will use the device Id and
			// units and make the call
			// to get tank and power sources data
			getSystemLoggedValuesForAllDevices(gateway, ewPackage);

			// Once all the data is collected from the external calls we will then add the
			// values of all the power sources.
			getActivePowerFromPowerSources(ewPackage);

			// At this point we should have all of the collected data in uniform manner to
			// then order it and use for analysis and plotting the graphs
			OrderingAndDataAnalysisHandler plot = new OrderingAndDataAnalysisHandler();

			// Using a different class to order the data and analyze it
			plot.plotGraphForActivePowerPerDay(ewPackage);

		} catch (Exception e) {
			System.out.println("Exception occured " + e);
		}
	}

	@SuppressWarnings("unchecked")
	// This method will get the sum of the powers per hour of all devices
	private static void getActivePowerFromPowerSources(EnerwherePackage ewPackage) {
		// Get devices list from package to pick power devices based on the Units value
		DevicesType devices = (DevicesType) ewPackage.getAttribute("DevicesList");
		List<DeviceType> devicesList = devices.getDevices();
		// New Map to store the summed power values per hour per day
		Map<Integer, Map<Integer, Float>> sumOfActivePowerMap = new HashMap<Integer, Map<Integer, Float>>();
		Map<Integer, List<Float>> sumOfActivePowerMapListValues = new HashMap<Integer, List<Float>>();
		// Map to store sum of power per hour
		Map<Integer, Float> sumOfHourToPower = null;
		List<Float> sumPowerValuePerMin = null;
		List<Float> loggedPowerValuePerMin = null;

		for (DeviceType device : devicesList) {
			Map<Integer, Float> loggedHourPowerFromDevice = null;
			// Only pick if the device unit is kW indicating its a Power device.
			if (device.getUnit().equals("kW")) {
				// loop through the Logged values map from each device for each day and get the
				// hour power map from it
				for (Map.Entry<Integer, Object> entry : device.getLoggedValues().entrySet()) {
					if ((Boolean) ewPackage.getAttribute("hourAnalysis")) {
						int day = entry.getKey();
						loggedHourPowerFromDevice = (Map<Integer, Float>) entry.getValue();
						// For each day we will have to see if the day value is set in the sum map.
						// IF its there we will take that map
						// ELSE we will create a new entry in the map indicating a new day was picked
						if (sumOfActivePowerMap.containsKey(day)) {
							sumOfHourToPower = sumOfActivePowerMap.get(day);
						} else {
							sumOfHourToPower = new HashMap<Integer, Float>();
							sumOfActivePowerMap.put(day, sumOfHourToPower);
						}
						// Then get the power to hour values from the device data map
						for (Entry<Integer, Float> innerEntry : loggedHourPowerFromDevice.entrySet()) {
							int hourFromLoggedValue = innerEntry.getKey();
							Float powerFromLoggedValue = innerEntry.getValue();
							// IF the sum map has the same hour as the logged value map it will take that
							// power value and add it to the power value in the sum hour power map and add
							// it back in the sum hour power map
							// ELSE just add the values indicating that its the first entry for that hour
							if (sumOfHourToPower.containsKey(hourFromLoggedValue)) {
								Float sum = sumOfHourToPower.get(hourFromLoggedValue);
								sum += powerFromLoggedValue;
								sumOfHourToPower.put(hourFromLoggedValue, sum);
							} else {
								sumOfHourToPower.put(hourFromLoggedValue, powerFromLoggedValue);
							}
						}
					} else {
						// IF analysis is done by minute the values a new array of float will be created
						// for each days entry and used to store the sum of the values per min
						int day = entry.getKey();
						loggedPowerValuePerMin = (List<Float>) entry.getValue();
						if (sumOfActivePowerMapListValues.containsKey(day)) {
							sumPowerValuePerMin = sumOfActivePowerMapListValues.get(day);
						} else {
							sumPowerValuePerMin = new ArrayList<Float>();
							sumOfActivePowerMapListValues.put(day, sumPowerValuePerMin);
						}
						int lastIndex = 0;
						int loggedValuesListIndex = loggedPowerValuePerMin.size();
						int sumOfpowerListIndex = sumPowerValuePerMin.size();
						// To add the values of the two arrays it will first check which array is longer
						// and set that as the lastIndex
						if (loggedValuesListIndex > sumOfpowerListIndex) {
							lastIndex = loggedValuesListIndex;
						} else {
							lastIndex = sumOfpowerListIndex;
						}
						// Using the lastIndex both the logged valus list and sum list is looped and
						// checked if there is a value in that index
						// When a value is found in either list it is used to get the sum of power in
						// that minute
						// The sum is then stored back into the map against the day
						for (int i = 0; i < lastIndex; i++) {
							float indexValueforSumPower = 0;
							float indexValueforLogged = 0;
							float sum = 0;
							if (i < sumOfpowerListIndex) {
								indexValueforSumPower = sumPowerValuePerMin.get(i);
							}
							if (i < loggedValuesListIndex) {
								indexValueforLogged = loggedPowerValuePerMin.get(i);
							}
							sum = indexValueforSumPower + indexValueforLogged;
							// If the sum arrays index is less than the logged values array an index out of
							// bound exception will be thrown
							// In that case the value of the sum will be added in the array
							// If the array has the particular index the value is replaced
							try {
								sumPowerValuePerMin.set(i, sum);
							} catch (IndexOutOfBoundsException e) {
								sumPowerValuePerMin.add(i, sum);
							}
						}
						sumOfActivePowerMapListValues.put(day, sumPowerValuePerMin);
					}
				}
			}
			// Adding the map in the package
			ewPackage.addAttribute("activePowerSumMap", sumOfActivePowerMap);
			ewPackage.addAttribute("activePowerSumMapList", sumOfActivePowerMapListValues);

		}
	}

	// In this method we will get the system logged values for each device.
	private static void getSystemLoggedValuesForAllDevices(EnerwhereGateway gateway, EnerwherePackage ewPackage) {
		// Created a new DevicesType object to store list of all the devices collected
		// in the first call. Check POGO class for more info
		DevicesType devices = (DevicesType) ewPackage.getAttribute("DevicesList");
		List<DeviceType> devicesList = devices.getDevices();

		// Looping through the devices from the list of devices to call get logged
		// values call.
		for (DeviceType device : devicesList) {
			System.out.println("Device Picked : " + device.getDeviceName());
			// Storing the picked device's Id to pass in the URL to get relevant data from
			// the API call
			ewPackage.addAttribute("deviceId", device.getId());
			// Storing the picked device so I wont have to loop through the loop to get the
			// device, to update information again, till all its data is collected per
			// device
			ewPackage.addAttribute("device", device);
			// Will check if the unit is in liters or kW to determine processing
			if (device.getUnit().equals("l")) {
				getTankLoggedValues(ewPackage, gateway);
			} else {
				getGeneratorLoggedValues(ewPackage, gateway);
			}
		}
		// Check if there is a -ve value, could mean that the tank was refilled. Will
		// attempt to get total usage for particular day
		checkIfTankWasRefiledAndReprocessData(ewPackage, gateway);
	}

	private static void getGeneratorLoggedValues(EnerwherePackage ewPackage, EnerwhereGateway gateway) {
		// Getting the start date to use in the URL
		String startDate = (String) ewPackage.getAttribute("startDate");
		// Getting count to loop for only those number of days
		int count = (Integer) ewPackage.getAttribute("numberOfDays");
		// Setting the sort order to get get values from the start of the day
		ewPackage.addAttribute("sortOrder", "asc");
		for (int currentDay = 0, nextDay = 1; currentDay < count; currentDay++, nextDay++) {
			// Since there are multiple calls that have to be made for each day the loop
			// will check for this tag
			ewPackage.addAttribute("generatorProcessed", "START");
			incrementingDateForLoggedValues(currentDay, nextDay, startDate, ewPackage);
			// For each power source we will check if the last record was processed and if
			// so this tag is set to DONE
			while (!String.valueOf(ewPackage.getAttribute("generatorProcessed")).equals("DONE")) {
				// Get Logged Values API call
				callGetLoggedValues(gateway, ewPackage);
			}
		}
	}

	private static void callGetLoggedValues(EnerwhereGateway gateway, EnerwherePackage ewPackage) {
		gateway.sendEnerwhereRequest(EnerwhereDeviceRequests.GET_SYSTEM_LOGGED_VALUES, ewPackage);
	}

	private static void getTankLoggedValues(EnerwherePackage ewPackage, EnerwhereGateway gateway) {
		// Getting the start date to use in the URL
		String startDate = (String) ewPackage.getAttribute("startDate");
		// Getting count to loop for only those number of days
		int count = (Integer) ewPackage.getAttribute("numberOfDays");
		for (int currentDay = 0, nextDay = 1; currentDay < count; currentDay++, nextDay++) {
			// This method will set the start date and end date for each day
			incrementingDateForLoggedValues(currentDay, nextDay, startDate, ewPackage);
			// To get the liters consumed per day i am making one Asc call to get the first
			// value in that day then a Desc call to get the last logged value in the day
			// and subtracting them to get value of consumed liters
			ewPackage.addAttribute("sortOrder", "asc");
			callGetLoggedValues(gateway, ewPackage);
			ewPackage.addAttribute("sortOrder", "desc");
			callGetLoggedValues(gateway, ewPackage);
		}
	}

	private static void checkIfTankWasRefiledAndReprocessData(EnerwherePackage ewPackage, EnerwhereGateway gateway) {
		// Using refueled to check if tank was filled and using a different logic to get
		// the liters consumed for that particular day
		if (ewPackage.getAttribute("refueled") != null
				&& (String.valueOf(ewPackage.getAttribute("refueled")).equals("START")
						|| String.valueOf(ewPackage.getAttribute("refueled")).equals("PROCESS"))) {

			ewPackage.addAttribute("device", (DeviceType) ewPackage.getAttribute("refueledDevice"));
			// Here the end time is appended to get till the last value of the day as we
			// don't need to loop of one day
			ewPackage.addAttribute("endDateForApiCall", (String) ewPackage.getAttribute("refuelingDay") + endTime);
			ewPackage.addAttribute("sortOrder", "asc");
			while ((String.valueOf(ewPackage.getAttribute("refueled")).equals("START")
					|| String.valueOf(ewPackage.getAttribute("refueled")).equals("PROCESS"))
					&& !String.valueOf(ewPackage.getAttribute("refueled")).equals("DONE")) {

				ewPackage.addAttribute("startDateForApiCall", (String) ewPackage.getAttribute("refuelingDay"));
				ewPackage.addAttribute("refueled", "PROCESS");
				// Get Logged Values API call
				callGetLoggedValues(gateway, ewPackage);
			}
		}
		DeviceType device = (DeviceType) ewPackage.getAttribute("device");
		ewPackage.addAttribute("litersConsumedMap", device.getLoggedValues());
	}

	private static void incrementingDateForLoggedValues(int currentDay, int nextDay, String startDate,
			EnerwherePackage ewPackage) {
		try {
			// For the given format the date will be set in the package in keys
			// 'startDateForApiCall','endDateForApiCall' and incremented to get single day
			// data at a time
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			cal.setTime(dateFormat.parse(startDate));
			cal.add(Calendar.DATE, currentDay);
			String dateForLoggedCall = dateFormat.format(cal.getTime()).toString();
			System.out.println("startDateForApiCall date " + dateForLoggedCall);
			ewPackage.addAttribute("startDateForApiCall", dateForLoggedCall);
			// Setting end date to limit data per day
			cal.setTime(dateFormat.parse(startDate));
			cal.add(Calendar.DATE, nextDay);
			dateForLoggedCall = dateFormat.format(cal.getTime()).toString();
			System.out.println("endDateForApiCall date " + dateForLoggedCall);
			ewPackage.addAttribute("endDateForApiCall", dateForLoggedCall);
		} catch (Exception e) {
			System.out.println("Error while incrementing date " + e);
		}
	}

	private static void validateAndPrepareDates(String sDate, String eDate, EnerwherePackage ewPackage)
			throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
		LocalDate sDateL = null;
		LocalDate eDateL = null;
		try {
			// Dates in the required format for API 'yyyy-MM-dd'
			sDateL = LocalDate.parse(sDate, formatter);
			eDateL = LocalDate.parse(eDate, formatter);
		} catch (Exception e) {
			throw new Exception("Start date or end date is in incorrect format. Required format (dd MMM yyyy) " + e);
		}

		// Getting count of days and if count is negative, swapping dates
		Date startDate = Date.from(sDateL.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date endDate = Date.from(eDateL.atStartOfDay(ZoneId.systemDefault()).toInstant());
		int count = (int) TimeUnit.MILLISECONDS.toDays(endDate.getTime() - startDate.getTime());
		if (count == 0) {
			throw new Exception("Start date and end date are the same. Provide different values");
		}
		if (count < 0) {
			Date swapVariable = new Date();
			swapVariable = endDate;
			endDate = startDate;
			startDate = swapVariable;
			count = (int) TimeUnit.MILLISECONDS.toDays(endDate.getTime() - startDate.getTime());

			// Swap Local Dates
			LocalDate swapLString = eDateL;
			eDateL = sDateL;
			sDateL = swapLString;
		}
		// Adding formatted dates in package to use in API request
		ewPackage.addAttribute("startDate", sDateL.toString());
		ewPackage.addAttribute("endDate", eDateL.toString());
		ewPackage.addAttribute("numberOfDays", count);
	}
}
