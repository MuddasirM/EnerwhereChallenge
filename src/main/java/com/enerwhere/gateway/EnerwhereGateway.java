package com.enerwhere.gateway;

import com.enerwhere.gateway.task.EnerwhereTask;

public class EnerwhereGateway {

	// Generic gateway code written here, based in the request the implementations
	// will be called.
	public void sendEnerwhereRequest(EnerwhereDeviceRequests request, EnerwherePackage ewPkg) {
		// Using the request passed in the sendEnerwhereRequest method call the task
		// will be picked along with the Http method and the common URL.
		EnerwhereTask task = request.getTask();
		//Check where the implementation is done based on the call made to continue in the flow.
		try {
			task.prepareUrl(request, ewPkg);

			task.execute(ewPkg);

			task.processResponse(ewPkg);
		} catch (Exception e) {
			System.out.println("Exception occured while sending device request");
		}
	}

}
