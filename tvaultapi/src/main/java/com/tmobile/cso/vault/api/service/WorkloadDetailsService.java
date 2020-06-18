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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.model.WorkloadAppDetails;
import com.tmobile.cso.vault.api.process.RestProcessor;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.springframework.util.StringUtils;

@Component
public class WorkloadDetailsService {

	@Value("${workload.endpoint}")
	private String workloadEndpoint;
	
	@Autowired
	RestProcessor restprocessor;
	
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
		workloadAppDetailsList.add(new WorkloadAppDetails(WorkloadAppDetails.APP_NAME_OTHER, WorkloadAppDetails.APP_TAG_OTHER, WorkloadAppDetails.APP_ID_OTHER));
		// get first response

		JsonObject response = getApiResponse(api);
		if (response != null) {
			JsonArray results = response.getAsJsonArray("items");
			// iterate json array to populate WorkloadAppDetails list
			for(JsonElement jsonElement: results) {
				if (jsonElement.getAsJsonObject() != null) {
					JsonObject metadata = jsonElement.getAsJsonObject().getAsJsonObject("metadata");
					JsonObject spec = jsonElement.getAsJsonObject().getAsJsonObject("spec");
					WorkloadAppDetails workloadAppDetails = new WorkloadAppDetails();
					if (spec != null) {
						workloadAppDetails.setAppName((spec.get("summary").isJsonNull()?"":spec.get("summary").getAsString()));
						workloadAppDetails.setAppID((spec.get("id").isJsonNull()?"":spec.get("id").getAsString()));
						workloadAppDetails.setAppTag((spec.get("id").isJsonNull()?"":spec.get("id").getAsString()));
						workloadAppDetailsList.add(workloadAppDetails);
					}
				}
			}
		}

		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(workloadAppDetailsList));
	}


	/**
	 * To get response from Workload endpoint
	 * @param api
	 * @return
	 */
	private JsonObject getApiResponse(String api)  {
		String workloadEndpointToken = new String(Base64.getDecoder().decode(ControllerUtil.getCwmToken()));
		if (StringUtils.isEmpty(workloadEndpointToken)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getApiResponse").
					put(LogMessage.MESSAGE, String.format ("Invalid workload token")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return null;
		}

		JsonParser jsonParser = new JsonParser();
		HttpClient httpClient =null;
		try {

			httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(
					NoopHostnameVerifier.INSTANCE).
						setSSLContext(
								new SSLContextBuilder().loadTrustMaterial(null,new TrustStrategy() {
							@Override
							public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
								return true;
							}
						}).build()
					).setRedirectStrategy(new LaxRedirectStrategy()).build();

				
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getApiResponse").
					put(LogMessage.MESSAGE, String.format ("Faile to create httpClient")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}

		HttpGet getRequest = new HttpGet(api);
		getRequest.addHeader("accept", "application/json");
		getRequest.addHeader("Authorization",workloadEndpointToken);
		String output = "";
		StringBuffer jsonResponse = new StringBuffer();

		try {
			HttpResponse apiResponse = httpClient.execute(getRequest);
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
