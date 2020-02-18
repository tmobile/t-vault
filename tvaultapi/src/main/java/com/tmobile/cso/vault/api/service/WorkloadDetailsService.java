// =========================================================================
// Copyright 2020 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================

package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class WorkloadDetailsService {

	@Value("${workload.endpoint}")
	private String workloadEndpoint;

	private static Logger log = LogManager.getLogger(WorkloadDetailsService.class);

	/**
	 * To get application list from Workload endpoint
	 * @param token
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getWorkloadDetails(String token, UserDetails userDetails) {
		String api = workloadEndpoint;
		List<WorkloadAppDetails> workloadAppDetailsList = new ArrayList<>();

		// get first response
		JsonObject response = getApiResponse(api);
		JsonArray results = response.getAsJsonObject("data").getAsJsonArray("summary");
		String pagination = response.getAsJsonObject("data").get("paginationURL").getAsString();
		Integer total = response.getAsJsonObject("data").get("total").getAsInt();
		Integer maxResults = response.getAsJsonObject("data").get("maxResults").getAsInt();

		// call each pagination and populate results json
		if (total > maxResults) {
			for (int index = 1; index <= (total / maxResults); index++) {
				response = getApiResponse(api + "?" + pagination);
				results.addAll(response.getAsJsonObject("data").getAsJsonArray("summary"));
				if (results.size() < total)	{
					pagination = response.getAsJsonObject("data").get("paginationURL").getAsString();
				}
			}
		}

		// iterate json array to populate WorkloadAppDetails list
		for(JsonElement jsonElement: results) {
			JsonObject summary = jsonElement.getAsJsonObject();
			WorkloadAppDetails workloadAppDetails = new WorkloadAppDetails();
			workloadAppDetails.setAppID((summary.get("appID").isJsonNull()?"":summary.get("appID").getAsString()));
			workloadAppDetails.setAppName((summary.get("appName").isJsonNull()?"":summary.get("appName").getAsString()));
			workloadAppDetails.setAppTag((summary.get("appTag").isJsonNull()?"":summary.get("appTag").getAsString()));
			workloadAppDetailsList.add(workloadAppDetails);
		}
		// Add a default
		workloadAppDetailsList.add(new WorkloadAppDetails(WorkloadAppDetails.APP_NAME_OTHER, WorkloadAppDetails.APP_TAG_OTHER, WorkloadAppDetails.APP_ID_OTHER));
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(workloadAppDetailsList));
	}

	/**
	 * To get response from Workload endpoint
	 * @param api
	 * @return
	 */
	private JsonObject getApiResponse(String api) {
		JsonParser jsonParser = new JsonParser();
		Gson gson = new Gson();
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet getRequest = new HttpGet(api);
		getRequest.addHeader("accept", "application/json");

		String output = "";
		StringBuffer jsonResponse = new StringBuffer();

		try {
			HttpResponse apiResponse = apiResponse = httpClient.execute(getRequest);
			if (apiResponse.getStatusLine().getStatusCode() != 200) {
				return null;
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
			while ((output = br.readLine()) != null) {
				jsonResponse.append(output);
			}
			return (JsonObject) jsonParser.parse(jsonResponse.toString());
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getApprolesFromCwm").
					put(LogMessage.MESSAGE, String.format ("Failed to parse CWM api response")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return null;
	}
}
