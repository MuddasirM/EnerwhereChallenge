package com.enerwhere.handler;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.enerwhere.gateway.EnerwherePackage;

@SuppressWarnings("unchecked")
public class OrderingAndDataAnalysisHandler {

	public void plotGraphForActivePowerPerDay(EnerwherePackage ewPackage) {
		// Getting the sum maps from the package to sort them in order for the time
		// period of analysis
		Map<Integer, Map<Integer, Float>> sumOfActivePowerMap = (Map<Integer, Map<Integer, Float>>) ewPackage
				.getAttribute("activePowerSumMap");
		Map<Integer, List<Float>> sumOfActivePowerMapList = (Map<Integer, List<Float>>) ewPackage
				.getAttribute("activePowerSumMapList");
		Map<Integer, Object> litersComsumedPerDayMap = (Map<Integer, Object>) ewPackage
				.getAttribute("litersConsumedMap");
		if ((Boolean) ewPackage.getAttribute("hourAnalysis")) {
			// This list is prepared to store the the day values in order to sort the logged
			// values
			List<Integer> daysInOrder = getListOfDatesInOrder((Integer) ewPackage.getAttribute("numberOfDays"),
					(String) ewPackage.getAttribute("startDate"));
			// Using the list of days in order the active power map is sorted in order to
			// store the values in a list of float values per hour
			Map<Integer, List<Float>> oderedSumOfActivePowerMap = convertActivePowerMapToOrderedList(
					sumOfActivePowerMap, daysInOrder);
			// Then the graphs are plotted and displayed with the total energy produced and
			// the plant efficiency of that day in the graphs title

			plotGraphsForPowerPerHourPerDay(oderedSumOfActivePowerMap, litersComsumedPerDayMap);
		} else {
			plotGraphsForPowerPerMinPerDay(sumOfActivePowerMapList, litersComsumedPerDayMap);
		}
	}

	private void plotGraphsForPowerPerHourPerDay(Map<Integer, List<Float>> oderedSumOfActivePowerMap,
			Map<Integer, Object> litersComsumedPerDayMap) {
		List<Float> oderedPowerValues = null;
		PlotGraphHandler plot = null;
		// Each day is picked and the sorted values are passed as the input for plotting
		// the graphs
		for (Entry<Integer, List<Float>> entry : oderedSumOfActivePowerMap.entrySet()) {
			float litersConsumed = (Float) litersComsumedPerDayMap.get(entry.getKey());
			oderedPowerValues = entry.getValue();
			plot = new PlotGraphHandler(oderedPowerValues);
			System.out.println(" Day " + entry.getKey() + " power sum odered list " + entry.getValue()
					+ " Liters consumed " + litersConsumed);
			plot.plotGraph(oderedPowerValues, entry.getKey(), litersConsumed);
		}
	}

	private void plotGraphsForPowerPerMinPerDay(Map<Integer, List<Float>> oderedSumOfActivePowerMap,
			Map<Integer, Object> litersComsumedPerDayMap) {
		List<Float> oderedPowerValues = null;
		PlotGraphHandler plot = null;
		for (Entry<Integer, List<Float>> entry : oderedSumOfActivePowerMap.entrySet()) {
			if (entry.getKey() != -1) {
				float litersConsumed = (Float) litersComsumedPerDayMap.get(entry.getKey());
				oderedPowerValues = entry.getValue();
				System.out.println(" Day " + entry.getKey() +" Liters consumed " + litersConsumed);
				plot = new PlotGraphHandler(oderedPowerValues);
				plot.plotGraph(oderedPowerValues, entry.getKey(), litersConsumed);
			}
		}
	}

	// This method will pick each days active power map using the daysInOrder list
	// to then sort the values from the hour power map and store the power in the
	// index the hour represents
	private Map<Integer, List<Float>> convertActivePowerMapToOrderedList(
			Map<Integer, Map<Integer, Float>> sumOfActivePowerMap, List<Integer> daysInOrder) {
		Map<Integer, List<Float>> dayToOderedPowerToHourMap = new HashMap<Integer, List<Float>>();

		List<Float> listOfPowerHourWise = null;
		for (int i = 0; i < daysInOrder.size(); i++) {
			Map<Integer, Float> hourToPowerUnodered = sumOfActivePowerMap.get(daysInOrder.get(i));
			listOfPowerHourWise = new ArrayList<Float>();
			// Since one has 24 hours the max list size used here is 24
			for (int j = 0; j < 24; j++) {
				if (hourToPowerUnodered.containsKey(j)) {
					listOfPowerHourWise.add(hourToPowerUnodered.get(j));
				}
			}
			// the sorted values are then added back into the sum map for that day
			dayToOderedPowerToHourMap.put(daysInOrder.get(i), listOfPowerHourWise);
		}
		return dayToOderedPowerToHourMap;
	}

	// This method will get the list of days in the order but using the start day
	// and adding the count of days using for the analysis
	// ** this will only work for at most a month. If data for more than a month are
	// to be analyzed we another logic will have to be implemented**
	private List<Integer> getListOfDatesInOrder(int numberOfDays, String startDate) {
		List<Integer> daysInOrder = new ArrayList<Integer>();
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			for (int i = 0; i < numberOfDays; i++) {
				cal.setTime(dateFormat.parse(startDate));
				cal.add(Calendar.DATE, i);
				String dateInString = dateFormat.format(cal.getTime()).toString();
				int dayFromDate = Integer.parseInt(dateInString.substring(8, 10));
				daysInOrder.add(dayFromDate);
			}
		} catch (Exception e) {
			System.out.println("Error while incrementing date " + e);
		}
		return daysInOrder;

	}
}
