/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */

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
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.RestProcessor;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Component
public class WorkloadDetailsService {

	@Value("${workload.endpoint}")
	private String workloadEndpoint;
	@Value("${workload.endpoint.token}")
	private String workloadEndpointToken;
	
	@Autowired
	RestProcessor restprocessor;

	@Autowired
	private RequestProcessor reqProcessor;
	
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
				populateWorkloadAppDetails(workloadAppDetailsList, jsonElement);
			}
		}

		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(workloadAppDetailsList));
	}

	/**
	 * Method to populate application details
	 * @param workloadAppDetailsList
	 * @param jsonElement
	 */
	private void populateWorkloadAppDetails(List<WorkloadAppDetails> workloadAppDetailsList, JsonElement jsonElement) {
		if (jsonElement.getAsJsonObject() != null) {
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


	/**
	 * To get response from Workload endpoint
	 * @param api
	 * @return
	 */
	private JsonObject getApiResponse(String api)  {
		if (StringUtils.isEmpty(workloadEndpointToken)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getApiResponse").
					put(LogMessage.MESSAGE, "Invalid workload token").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
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
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getApiResponse").
					put(LogMessage.MESSAGE, "Faile to create httpClient").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}

		HttpGet getRequest = new HttpGet(api);
		getRequest.addHeader("accept", "application/json");
		getRequest.addHeader("Authorization",workloadEndpointToken);
		StringBuilder jsonResponse = new StringBuilder();
		try {
			HttpResponse apiResponse = null;
			if(httpClient != null) {
				apiResponse = httpClient.execute(getRequest);
				if (apiResponse.getStatusLine().getStatusCode() != 200) {
					return null;
				}
			}
			readAPIResponse(jsonResponse, apiResponse);

			return (JsonObject) jsonParser.parse(jsonResponse.toString());
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getApprolesFromCwm").
					put(LogMessage.MESSAGE, "Failed to parse CWM api response").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return null;
	}

	/**
	 * Method to read api response
	 * @param jsonResponse
	 * @param apiResponse
	 */
	private void readAPIResponse(StringBuilder jsonResponse, HttpResponse apiResponse) {
		String output;
		if(apiResponse != null) {
			try(BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())))){
				while ((output = br.readLine()) != null) {
					jsonResponse.append(output);
				}
			}catch(Exception ex) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getApprolesFromCwm").
						put(LogMessage.MESSAGE, "Failed to read CWM api response").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
	}

	public ResponseEntity<String> getWorkloadDetailsByAppName(String appName){
		JsonObject response = getApiResponse(workloadEndpoint + "/" + appName);
		if(null == response){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "Getting Application Details by app name ").
					put(LogMessage.MESSAGE, String.format("For an application name  = [%s]",appName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Application name doesn't " +
					"exist\"]}");
		}else {
			return ResponseEntity.status(HttpStatus.OK).body(response.toString());
		}
	}

	/**
	 * To get all Application details from CLM api
	 * @return
	 */
	public List<TMOAppMetadataDetails> getAllApplicationDetailsFromCLM() {
		log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "getAllApplicationDetailsFromCLM").
				put(LogMessage.MESSAGE, "Getting all application names from CLM").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		JsonObject response = getApiResponse(workloadEndpoint);
		List<TMOAppMetadataDetails> tmoAppMetadataList = new ArrayList<>();
		if (response != null) {
			JsonArray results = response.getAsJsonArray("items");
			// iterate json array to populate WorkloadAppDetails list
			for(JsonElement jsonElement: results) {
				if (jsonElement.getAsJsonObject() != null) {
					JsonObject spec = jsonElement.getAsJsonObject().getAsJsonObject("spec");
					TMOAppMetadataDetails tmoAppMetadata = new TMOAppMetadataDetails();
					if (spec != null) {
						System.out.println(spec);
						if (spec.get("id") != null) {
							tmoAppMetadata.setApplicationName((spec.get("id").isJsonNull()?"":spec.get("id").getAsString()));
						}
						if (spec.get("tag") != null) {
							tmoAppMetadata.setApplicationTag((spec.get("tag").isJsonNull()?"":spec.get("tag").getAsString()));
						}
						if (spec.get("brtContactEmail") != null) {
							tmoAppMetadata.setApplicationOwnerEmailId((spec.get("brtContactEmail").isJsonNull()?"":spec.get("brtContactEmail").getAsString()));
						}
						if (spec.get("projectLeadEmail") != null) {
							tmoAppMetadata.setProjectLeadEmailId((spec.get("projectLeadEmail").isJsonNull()?"":spec.get("projectLeadEmail").getAsString()));
						}
						tmoAppMetadataList.add(tmoAppMetadata);
					}
				}
			}
		}
		return tmoAppMetadataList;
	}

	/**
	 * To get metadata for all applications from metadata/tmo-applications.
	 * @param token
	 * @return
	 */
	public List<TMOAppMetadataDetails> getAllAppMetadata(String token) {
		String pathStr = TVaultConstants.TMO_APP_METADATA_PATH +"?list=true";
		List<TMOAppMetadataDetails> tmoAppMetadataList = new ArrayList<>();
		Response listResponse = reqProcessor.process("/tmo-applicaions","{\"path\":\""+pathStr+"\"}",token);
		if (HttpStatus.OK.equals(listResponse.getHttpstatus())) {
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = (JsonObject) jsonParser.parse(listResponse.getResponse());
			JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
			tmoAppMetadataList = getAllAppDetails(token, jsonArray);
		}
		return tmoAppMetadataList;
	}

	/**
	 * To get application metadata details as List<TMOAppMetadataDetails>.
	 * @param jsonArray
	 * @return
	 */
	private List<TMOAppMetadataDetails> getAllAppDetails(String token, JsonArray jsonArray) {
		List<TMOAppMetadataDetails> tmoAppMetadataList = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		if (!ObjectUtils.isEmpty(jsonArray)) {
			for (int i = 0; i < jsonArray.size(); i++) {
				String pathStr = TVaultConstants.TMO_APP_METADATA_PATH + "/" + jsonArray.get(i).getAsString();
				Response readResponse = reqProcessor.process("/tmo-applicaions","{\"path\":\""+pathStr+"\"}",token);
				if (HttpStatus.OK.equals(readResponse.getHttpstatus())) {
					try {
						TMOAppMetadata tmoAppMetadata = objectMapper.readValue(readResponse.getResponse(), new TypeReference<TMOAppMetadata>() {});
						log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, "getAllAppDetails").
								put(LogMessage.MESSAGE, String.format("Parsed metadata for application [%s]", jsonArray.get(i).getAsString())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						tmoAppMetadataList.add(tmoAppMetadata.getTmoAppMetadataDetails());
					}
					catch (IOException e) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, "getAllAppDetails").
								put(LogMessage.MESSAGE, String.format("Failed to parse metadata for application [%s]",jsonArray.get(i).getAsString())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
					}
				}
				else {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "getAllAppDetails").
							put(LogMessage.MESSAGE, String.format("Metadata not found for application [%s]",jsonArray.get(i).getAsString())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
			}
		}
		return tmoAppMetadataList;
	}


	/**
	 * Update metadata for an application.
	 * @param token
	 * @param currentApp
	 * @param clmApp
	 * @return
	 */
	public Response udpateApplicationMetadata(String token, TMOAppMetadataDetails currentApp, TMOAppMetadataDetails clmApp) {
		ObjectMapper objectMapper = new ObjectMapper();
		String path = TVaultConstants.TMO_APP_METADATA_PATH + "/" + currentApp.getApplicationName();

		if (clmApp != null) {
			currentApp.setProjectLeadEmailId(clmApp.getProjectLeadEmailId());
			currentApp.setApplicationOwnerEmailId(clmApp.getApplicationOwnerEmailId());
			currentApp.setUpdateFlag(true);
		}
		try {
			String metadataJson = objectMapper.writeValueAsString(currentApp);
			String writeJson =  "{\"path\":\"" + path +"\",\"data\":" + metadataJson + "}";
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "udpateApplicationMetadata").
					put(LogMessage.MESSAGE, String.format("Trying to udpate metadata for application [%s]", currentApp.getApplicationName())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return reqProcessor.process("/write", writeJson, token);
		} catch (JsonProcessingException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "udpateApplicationMetadata").
					put(LogMessage.MESSAGE, String.format("Failed to stringify metadata for application [%s]", currentApp.getApplicationName())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "udpateApplicationMetadata").
				put(LogMessage.MESSAGE, String.format("Failed to udpate metadata for application [%s]", currentApp.getApplicationName())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		Response failedResponse = new Response();
		failedResponse.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
		failedResponse.setResponse("{\"errors\":[\"Failed to update metadata for application\"]}");
		return failedResponse;
	}
}
