package com.enerwhere.gateway;


import com.enerwhere.gateway.task.EnerwhereTask;
import com.enerwhere.gateway.task.GenericTask;
import com.enerwhere.gateway.task.GetSystemLogParamConfigTask;
import com.enerwhere.gateway.task.GetSystemLoggedValuesTask;

public enum EnerwhereDeviceRequests {
	GET_SYSTEM_LOG_PARAMETER_CONFIG(GetSystemLogParamConfigTask.class, "https://api.netbiter.net/operation/v1/rest/json/system/{systemId}/log/config?accesskey={key}", "GET"),
	GET_SYSTEM_LOGGED_VALUES(GetSystemLoggedValuesTask.class, "https://api.netbiter.net/operation/v1/rest/json/system/{systemId}/log/{id}?accesskey={key}&startdate={startdate}&enddate={enddate}&sortorder={order}", "GET");

	private Class<? extends GenericTask> task;
	private String endpoint;
	private String httpmethod;

	EnerwhereDeviceRequests(Class<? extends GenericTask> class1, String path, String method) {
		task = class1;
		endpoint = path;
		httpmethod = method;

	}

	public EnerwhereTask getTask() {
		try {
			return task.getDeclaredConstructor(EnerwhereDeviceRequests.class).newInstance(this);
		} catch (Exception e) {

		}
		return null;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getHttpmethod() {
		return httpmethod;
	}

	public void setHttpmethod(String httpmethod) {
		this.httpmethod = httpmethod;
	}
}
