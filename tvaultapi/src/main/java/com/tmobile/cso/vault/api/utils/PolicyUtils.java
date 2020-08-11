// =========================================================================
// Copyright 2019 T-Mobile, US
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

import java.io.IOException;
import java.util.ArrayList;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.Response;
@Component
public class PolicyUtils {

	private Logger log = LogManager.getLogger(PolicyUtils.class);
	
	@Value("${vault.auth.method}")
	private String vaultAuthMethod;
	
	@Autowired
	private OIDCUtil oidcUtil;
	
	public PolicyUtils() {
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * Gets the list of policies to be checked for a given safe
	 * @param safeType
	 * @param safeName
	 * @return
	 */
	public ArrayList<String> getPoliciesTobeCheked(String safeType, String safeName) {
		ArrayList<String> policiesTobeChecked = new ArrayList<String>();
		policiesTobeChecked.addAll(getAdminPolicies()); 
		policiesTobeChecked.addAll(getSudoPolicies(safeType, safeName));
		return policiesTobeChecked;
	}
	/**
	 * To get the list of admin policies
	 * @return
	 */
	public ArrayList<String> getAdminPolicies() {
		ArrayList<String> adminPolicies = new ArrayList<String>();
		// TODO: Currently this list is based on the hard coded policy name, may be, this needs to externalized
		adminPolicies.add("safeadmin");
		return adminPolicies;
	}
	
	/**
	 * 
	 * @return
	 */
	private ArrayList<String> getSudoPolicies(String safeType, String safeName) {
		ArrayList<String> sudoPolicies = new ArrayList<String>();
		sudoPolicies.add(new StringBuffer().append("s_").append(safeType).append("_").append(safeName).toString());
		return sudoPolicies;
	}
	/**
	 * Gets the latest policies from Vault for the given user. This will include added policies/exclude removed policies
	 * after the user has logged in and obtained token
	 * @param token
	 * @param username
	 * @return
	 */
	public String[] getCurrentPolicies(String token, String username) {
		Response userResponse = new Response();
		String[] policies = {};
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = ControllerUtil.getReqProcessor().process("/auth/userpass/read","{\"username\":\""+username+"\"}",token);
		}
		else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			userResponse = ControllerUtil.getReqProcessor().process("/auth/ldap/users","{\"username\":\""+username+"\"}",token);
		}
		else if(TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, username);
			userResponse.setHttpstatus(responseEntity.getStatusCode());
			if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
				policies = responseEntity.getBody().getPolicies().stream().toArray(String[] :: new);
			}
		}
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			String responseJson = userResponse.getResponse();
			try {
				if(!TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					ObjectMapper objMapper = new ObjectMapper();
					String policiesStr = ControllerUtil.getPoliciesAsStringFromJson(objMapper, responseJson);
					policies = policiesStr.split(",");
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "getPolicies").
					      put(LogMessage.MESSAGE, "Error while trying to list of policies for the user").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));			
			}
		}
		return policies;
	}
}
