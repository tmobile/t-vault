// =========================================================================
// Copyright 2019 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License")
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

package com.tmobile.cso.vault.api.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

@Component
public class CommonUtils {

	@Autowired
	private RequestProcessor reqProcessor;

	private Logger log = LogManager.getLogger(CommonUtils.class);


	public CommonUtils() {
		//Empty constructor
	}
	/**
	 * Converts policies string part of response received from Vault Rest call to string array
	 * @param objMapper
	 * @param policyJson
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public String[] getPoliciesAsArray(ObjectMapper objMapper, String policyJson) throws JsonProcessingException, IOException{
		ArrayList<String> policies = new ArrayList<String>();
		JsonNode policiesNode = objMapper.readTree(policyJson).get("policies");
		if (policiesNode.isContainerNode()) {
			Iterator<JsonNode> elementsIterator = policiesNode.elements();
		       while (elementsIterator.hasNext()) {
		    	   JsonNode element = elementsIterator.next();
		    	   policies.add(element.asText());
		       }
		}
		else {
			policies.add(policiesNode.asText());
		}

		// get identity policies
		JsonNode identityPoliciesNode = objMapper.readTree(policyJson).get("identity_policies");
		if (identityPoliciesNode.isContainerNode()) {
			Iterator<JsonNode> elementsIterator = identityPoliciesNode.elements();
			while (elementsIterator.hasNext()) {
				JsonNode element = elementsIterator.next();
				policies.add(element.asText());
			}
		}
		else {
			policies.add(identityPoliciesNode.asText());
		}

		return policies.toArray(new String[policies.size()]);
	}

	/**
	 * To get approle name from approle token lookup
	 * @param token
	 * @return
	 */
	public String getApproleNameFromLookup(String token) {
		String approleName = "";
		Response response = reqProcessor.process("/auth/tvault/lookup", "{}", token);
		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			ObjectMapper objMapper = new ObjectMapper();
			String responseJson = response.getResponse();
			try {
				JsonNode metaNode = objMapper.readTree(responseJson).get("meta");
				if (metaNode != null && metaNode.get("role_name") != null) {
					approleName = metaNode.get("role_name").asText();
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getApproleNameFromLookup").
						put(LogMessage.MESSAGE, "Error while trying to parse approle token lookup response").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
		return approleName;
	}

	/**
	 * To get the modified by info from user details.
	 * @param userDetails
	 * @return
	 */
	public String getModifiedByInfo(UserDetails userDetails) {
		String modifiedBy = userDetails.getEmail();
		if (StringUtils.isEmpty(modifiedBy)) {
			modifiedBy = "";
			// secret is being modified by approle or aws role
			// for approle take approle name from approle token lookup
			if (userDetails.getUsername().equalsIgnoreCase(TVaultConstants.APPROLE)) {
				String approleName = getApproleNameFromLookup(userDetails.getClientToken());
				if (!StringUtils.isEmpty(approleName)) {
					modifiedBy = approleName + " (AppRole)";
				}
				else {
					modifiedBy = "AppRole";
				}
			}
			// for aws roles set "AWS Role"
			if (userDetails.getUsername().startsWith("aws")) {
				modifiedBy = "AWS Role";
			}
		}
		return modifiedBy;
	}
}
