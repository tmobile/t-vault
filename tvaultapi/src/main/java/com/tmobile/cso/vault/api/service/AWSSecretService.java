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

package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class AWSSecretService {

	@Autowired
	private AccessService accessService;

	@Autowired
	private AppRoleService appRoleService;

	@Autowired
	private RequestProcessor reqProcessor;
	private static Logger logger = LogManager.getLogger(AWSSecretService.class);

	public ResponseEntity<String> createAWSRole(AWSDynamicRoleRequest awsDynamicRoleRequest, String token, UserDetails userDetails) {

		AWSTempRole awsTempRole = new AWSTempRole();
		awsTempRole.setName(awsDynamicRoleRequest.getName());
		awsTempRole.setCredential_type("iam_user");

		String[] actions = awsDynamicRoleRequest.getPermisisons().split(",");
		String[] resources = awsDynamicRoleRequest.getResources().split(",");

		Statement policyStatement = new Statement("Allow", actions, resources);

		PolicyDocument policyDocument = new PolicyDocument("2012-10-17",policyStatement);
		awsTempRole.setPolicy_document(policyDocument.toString());

		String roleString = JSONUtil.getJSON(awsTempRole);

		Response response = reqProcessor.process("/aws/roles/create",roleString,token);
		if (HttpStatus.OK.equals(response.getHttpstatus()) || HttpStatus.NO_CONTENT.equals(response.getHttpstatus())) {
			createPoliciesForTempCredentialsAWS(userDetails, token, awsDynamicRoleRequest.getName(), "gitlabrole");
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Vault AWS Role created successfully\"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	private Response createPoliciesForTempCredentialsAWS(UserDetails userDetails, String token,String roleName, String approle_name) {

		AccessPolicy accessPolicy = new AccessPolicy();
		String accessId = "r_awscreds_"+roleName;
		accessPolicy.setAccessid(accessId);
		HashMap<String,String> accessMap = new HashMap<String,String>();
		String svcAccCredsPath= "aws/creds/"+roleName;
		accessMap.put(svcAccCredsPath, TVaultConstants.READ_POLICY);
		accessPolicy.setAccess(accessMap);
		ResponseEntity<String> policyCreationStatus = accessService.createPolicy(token, accessPolicy);
		if (HttpStatus.UNPROCESSABLE_ENTITY.equals(policyCreationStatus.getStatusCode())) {
			policyCreationStatus = accessService.updatePolicy(token, accessPolicy);
		}
		Response response = new Response();
		if (!HttpStatus.OK.equals(policyCreationStatus.getStatusCode())) {
			response.setHttpstatus(HttpStatus.UNPROCESSABLE_ENTITY);
			response.setResponse("{\"errors\":[\"Failed to create policies\"]}");
			return response;
		}
		String r_policy = accessId;
		String approleName = approle_name;
		Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+approleName+"\"}",token);
		String responseJson="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
			responseJson = roleResponse.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			try {
				JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
				for(JsonNode policyNode : policiesArry){
					currentpolicies.add(policyNode.asText());
				}
			} catch (IOException e) {
				logger.error(e);
			}
			policies.addAll(currentpolicies);
			policies.addAll(currentpolicies);
			if (!currentpolicies.contains(r_policy)) {
				policies.add(r_policy);
			}
		}

		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");

		Response approleControllerResp = appRoleService.configureApprole(approleName,policiesString,token);
		return approleControllerResp;
	}

	private Response createPoliciesForTempCredentials(UserDetails userDetails, String token,String roleName) {

		AccessPolicy accessPolicy = new AccessPolicy();
		String accessId = "r_awscreds_"+roleName;
		accessPolicy.setAccessid(accessId);
		HashMap<String,String> accessMap = new HashMap<String,String>();
		String svcAccCredsPath= "aws/creds/"+roleName;
		accessMap.put(svcAccCredsPath, TVaultConstants.READ_POLICY);
		accessPolicy.setAccess(accessMap);
		ResponseEntity<String> policyCreationStatus = accessService.createPolicy(token, accessPolicy);
		if (HttpStatus.UNPROCESSABLE_ENTITY.equals(policyCreationStatus.getStatusCode())) {
			policyCreationStatus = accessService.updatePolicy(token, accessPolicy);
		}
		Response response = new Response();
		if (!HttpStatus.OK.equals(policyCreationStatus.getStatusCode())) {
			response.setHttpstatus(HttpStatus.UNPROCESSABLE_ENTITY);
			response.setResponse("{\"errors\":[\"Failed to create policies\"]}");
			return response;
		}

		String userName = userDetails.getUsername();
		Response userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
		String r_policy = accessId;

		String responseJson="";
		String groups="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();
			try {
				ObjectMapper objMapper = new ObjectMapper();
				currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
			} catch (IOException e) {
				logger.error(e);
			}

			policies.addAll(currentpolicies);
			if (!currentpolicies.contains(r_policy)) {
				policies.add(r_policy);
			}
		}else{
			// New user to be configured
			policies.add(r_policy);
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");

		response = ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
		return response;
	}

	public ResponseEntity<String> getTemporaryCredentials(String role_name, String token) {

		Response response = reqProcessor.process("/aws/creds/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	public ResponseEntity<String> deleteAWSRole(String role_name, String token, UserDetails userDetails) {
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		Response response = reqProcessor.process("/aws/roles/","{\"role_name\":\""+role_name+"\"}",token);
		String userName = userDetails.getUsername();
		String accessId = "r_awscreds_"+role_name;
		ResponseEntity<String> policyDeleteStatus = accessService.deletePolicyInfo(token, accessId);
		if (!HttpStatus.OK.equals(policyDeleteStatus.getStatusCode())) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"Failed to remove policy\"]}");
		}
		Response userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);

		String responseJson="";
		String groups="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();
			try {
				ObjectMapper objMapper = new ObjectMapper();
				currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
			} catch (IOException e) {
				logger.error(e);
			}
			policies.addAll(currentpolicies);
			policies.remove(accessId);
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");

		ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
}
