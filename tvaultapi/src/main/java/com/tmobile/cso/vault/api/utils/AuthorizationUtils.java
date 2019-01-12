// =========================================================================
// Copyright 2018 T-Mobile, US
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

package com.tmobile.cso.vault.api.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.Response;
@Component
public class AuthorizationUtils {
	
	private Logger log = LogManager.getLogger(AuthorizationUtils.class);
	
	public AuthorizationUtils() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * Checks whether the given user can edit the given safe.
	 * @param userDetails
	 * @param safeMetaData
	 * @param latestPolicies
	 * @param policiesTobeChecked
	 * @return
	 */
	public boolean isAuthorized(UserDetails userDetails, Safe safeMetaData, String[] latestPolicies, ArrayList<String> policiesTobeChecked, boolean forceCapabilityCheck) {
		boolean authorized = false;
		String powerToken = userDetails.getSelfSupportToken();
		if (userDetails.isAdmin()) {
			// Admin is always authorized. This flag is set when the user logs in with "safeadmin" policy
			return true;
		}
		else {
			// Non Admins
			if (userDetails.getUsername() != null && userDetails.getUsername().equals(safeMetaData.getSafeBasicDetails().getOwnerid())) {
				// As a owner of the safe, I am always authorized...
				if (!forceCapabilityCheck) {
					// Little lenient authorization (To be used carefully) 
					return true;
				}
			}
		}
		String safeType = getSafeTypeFromPath(safeMetaData.getPath());
		// Open each of the associated policy and check whether the user really has capability 
		for (String policyTobeChecked: policiesTobeChecked) {
			String policyKeyTobeChecked = new StringBuffer().append(safeType).toString();
			authorized = isAuthorized(latestPolicies, policyTobeChecked, policyKeyTobeChecked, powerToken);
			if (authorized) {
				break;
			}
		}
		return authorized;
	}

	/**
	 * 
	 * @param currentUserPolicies
	 * @param lookupPolicyName
	 * @param lookupPolicyKey
	 * @param powerToken
	 * @return
	 */
	private boolean isAuthorized(String[] currentUserPolicies, String lookupPolicyName, String lookupPolicyKey, String powerToken) {
		boolean authorized = false;
		for (String currentPolicy: currentUserPolicies) {
			if (currentPolicy.startsWith(lookupPolicyName)) {
				// Current user policy matches with one of the policy to be checked (for example, safeadmin or s_shared_mysafe)
				authorized = hasCapability(currentPolicy, lookupPolicyName, lookupPolicyKey, powerToken);
				break;
			}
		}
		return authorized;
	}
	
	/**
	 * 
	 * @param currentPolicy
	 * @param lookupPolicyName
	 * @param lookupPolicyKey
	 * @param powerToken
	 * @return
	 */
	private boolean hasCapability(String currentPolicy, String lookupPolicyName, String lookupPolicyKey, String powerToken) {
		boolean authorized = false;
		// Open the policy and check whether there is real policy entry...
		LinkedHashMap<String, LinkedHashMap<String, Object>> capabilitiesMap = getPolicyInfo(currentPolicy, powerToken);
		for (Map.Entry<String, LinkedHashMap<String, Object>> entry : capabilitiesMap.entrySet()) {
		    String capKey = entry.getKey();
		    LinkedHashMap<String, Object> value = entry.getValue();
		    // now work with key and value...
		    if (capKey.startsWith(lookupPolicyKey)) {
		    	for (Map.Entry<String, Object> valEntry : value.entrySet()) {
		    		if (valEntry.getValue() instanceof String) {
			    		//String valKey = valEntry.getKey();
			    		String capability = valEntry.getValue().toString();
			    		if (capability.toLowerCase().startsWith("write") || capability.toLowerCase().startsWith("sudo")) {
							authorized = true;
							break;
			    		}
		    		}
		    		else if (valEntry.getValue() instanceof ArrayList<?>) {
		    			ArrayList<Object> capabilities = (ArrayList<Object>)valEntry.getValue();
			    		if (capabilities.contains(TVaultConstants.CREATE)|| capabilities.contains(TVaultConstants.UPDATE) || capabilities.contains(TVaultConstants.DELETE) ) {

							authorized = true;
							break;
			    		}
		    		}
		    	}
		    }
		    if (authorized) {
		    	break;
		    }
		}
		return authorized;
	}
	/**
	 * Checks whether policies passed contains any admin policies
	 * @param policies
	 * @return
	 */
	public boolean containsAdminPolicies(List<String> policies, List<String> adminPolicies) {
		List<String> commonPolicies = (List<String>) CollectionUtils.intersection(policies, adminPolicies);
		return commonPolicies.size() > 0 ? true: false;
	}
	
	/**
	 * Get the details for the given policy
	 * @param policyName
	 * @param token
	 * @return
	 */
	private LinkedHashMap<String, LinkedHashMap<String, Object>> getPolicyInfo( String policyName, String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Get Policy information").
				put(LogMessage.MESSAGE, String.format("Trying to get policy information for [%s]", policyName)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if(StringUtils.isEmpty(policyName)){
			return null;
		}
		
		Response response = ControllerUtil.reqProcessor.process("/access","{\"accessid\":\""+policyName+"\"}",token);
		String policyJson = response.getResponse().toString();
		//TODO: Properly handle null/empty cases...
		LinkedHashMap<String, LinkedHashMap<String, Object>> capabilitiesMap = (LinkedHashMap<String, LinkedHashMap<String, Object>>) ControllerUtil.parseJson(ControllerUtil.parseJson(policyJson).get("rules").toString()).get("path");
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Get Policy information").
				put(LogMessage.MESSAGE, "Getting policy information Complete").
				put(LogMessage.STATUS, response.getHttpstatus().toString()).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return capabilitiesMap ;
	}
	
	/**
	 * Gets the safe type for a given path
	 * @param path
	 * @return
	 */
	private String getSafeTypeFromPath(String path){
		String safeType = TVaultConstants.UNKNOWN;
		if (!StringUtils.isEmpty(path)) {
			String paths[] =  path.split("/");
			if (paths != null && paths.length > 0) {
				safeType = paths[0];
			}
		}
		return safeType;
	}
}
