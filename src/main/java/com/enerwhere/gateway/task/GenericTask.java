package com.enerwhere.gateway.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import com.enerwhere.gateway.EnerwhereDeviceRequests;
import com.enerwhere.gateway.EnerwherePackage;

// Generic task contains the execute method to make the external API call and get data and store as String for processing
public class GenericTask implements EnerwhereTask {
	protected EnerwhereDeviceRequests request;
	protected String apiResponse;
	protected String url;
	private static final String AUTH_KEY = "1DAFDA7BA4B7BF78884BC0CCE3FCD344";
	private static final String SYS_ID = "0030112B2A36";

	public GenericTask(EnerwhereDeviceRequests requestType) {
		super();
		this.request = requestType;
	}

	// Main execute method will be called once the URL is created and make the call
	// based on it but setting the URL, Http Method, and Accept to get data in Json
	public void execute(EnerwherePackage eWPackage) {
		HttpsURLConnection con = null;
		try {
			URL requestUrl = new URL(getUrl());
			System.out.println("Url prepared : " + getUrl());
			con = (HttpsURLConnection) requestUrl.openConnection();
			con.setRequestMethod(getRequest().getHttpmethod());
			con.setRequestProperty("Accept", "application/json");
			con.connect();
			System.out.println("Response Code : " + con.getResponseCode());
			if (con.getResponseCode() == 200) {
				// Request successful
				System.out.println("Request completed");
			} else {
				System.out.println("Request failed");
			}
			readResponse(con);
		} catch (Exception e) {
			System.out.println("Exception occured while processing API request "+e);
		}

	}

	// Method to read the response and store it as String for processing
	public void readResponse(HttpsURLConnection con) {
		try {
			if (con.getInputStream() != null) {

				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				StringBuilder res = new StringBuilder();
				String inputLine;
				while ((inputLine = br.readLine()) != null) {
					res.append(inputLine);
				}
				setApiResponse(res.toString());
			}
		} catch (IOException e) {
			System.out.println("IO Exception occured while reading response");
		}
	}

	// This method will be called for all API calls and will set the common values
	// in the URL
	public void prepareUrl(EnerwhereDeviceRequests requestType, EnerwherePackage ewPkg) {
		String path = requestType.getEndpoint();
		path = path.replace("{systemId}", SYS_ID);
		path = path.replace("{key}", AUTH_KEY);
		setUrl(path);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public EnerwhereDeviceRequests getRequest() {
		return request;
	}

	public void setRequest(EnerwhereDeviceRequests request) {
		this.request = request;
	}

	public void processResponse(EnerwherePackage ewPkg) {
		// implemented in specific task

	}

	public String getApiResponse() {
		return apiResponse;
	}

	public void setApiResponse(String apiResponse) {
		this.apiResponse = apiResponse;
	}

}
