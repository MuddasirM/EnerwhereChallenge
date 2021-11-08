package com.enerwhere.gateway.task;

import com.enerwhere.gateway.EnerwhereDeviceRequests;
import com.enerwhere.gateway.EnerwherePackage;

//Interface created to be used in the Generic task for API calls
public interface EnerwhereTask {
	
	public void execute(EnerwherePackage eWPackage);
	
	public void processResponse(EnerwherePackage ewPkg);
	
	public void prepareUrl(EnerwhereDeviceRequests requestType, EnerwherePackage ewPkg);
	

}
