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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.common.AzureServiceAccountConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;

@Component
public class AWSIAMAuthService {

	@Value("${vault.port}")
	private String vaultPort;

	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger logger = LogManager.getLogger(AWSIAMAuthService.class);
	
	private static final String POLICIESSTR = "policies";
	private static final String ROLESTR = "{\"role\":\"";
	private static final String ROLENONEXISTSTR = "{\"errors\":[\"IAM Role doesn't exist\"]}";

	/**
	 * Registers a role in the method.
	 * @param awsiamRole
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> createIAMRole(AWSIAMRole awsiamRole, String token, UserDetails userDetails) throws TVaultValidationException{
		if(!StringUtils.isEmpty(awsiamRole.getPolicies())) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Checking whether policy is added as an input param for creating AWS IAM role").
					put(LogMessage.MESSAGE, String.format("Trying to create AWS IAM Role [%s]", awsiamRole.getRole())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			throw new TVaultValidationException("Policies are not permitted during safe creation");
		}
		if (!ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)) {
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		String jsonStr = JSONUtil.getJSON(awsiamRole);
		Response response = reqProcessor.process("/auth/aws/iam/role/create",jsonStr, token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			String metadataJson = ControllerUtil.populateAWSMetaJson(awsiamRole.getRole(), userDetails.getUsername());
			if(ControllerUtil.createMetadata(metadataJson, token)) {
				boolean awsiamRoleMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson, token);
				String awsiamroleUsermetadataJson = ControllerUtil.populateUserMetaJson(awsiamRole.getRole(), userDetails.getUsername(),awsiamRole.getAuth_type());
				boolean awsiamRoleUserMetaDataCreationStatus = ControllerUtil.createMetadata(awsiamroleUsermetadataJson, token);
				if(awsiamRoleMetaDataCreationStatus && awsiamRoleUserMetaDataCreationStatus) {
				logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Creating AWS IAM role").
						put(LogMessage.MESSAGE, String.format("AWS IAM Role [%s] created successfully by [%s].", awsiamRole.getRole(),userDetails.getUsername())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM Role created successfully \"]}");
				}
			}
			// revert role creation
			Response deleteResponse = reqProcessor.process("/auth/aws/iam/roles/delete",ROLESTR+awsiamRole.getRole()+"\"}",token);
			if (deleteResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS IAM role creation failed.\"]}");
			}
			else {
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM role created however metadata update failed. Please try with AWS role/update \"]}");
			}
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}

	/**
	 * 
	 * @param token
	 * @param awsLoginRole
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> updateIAMRole(String token, AWSIAMRole awsiamRole) throws TVaultValidationException{
		if (!ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)) {
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		AWSIAMMetadataDetails existingIamRole =readIAMRoleDetails(token, awsiamRole.getRole());
		if (existingIamRole == null) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "updateAWSIAMRole").
				      put(LogMessage.MESSAGE, String.format("Unable to read Role information. Role [%s] doesn't exist", awsiamRole.getRole())).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body(ROLENONEXISTSTR);

		}
		String[] existingPolicy=existingIamRole.getPolicies();
		awsiamRole.setPolicies(existingPolicy);
		String jsonStr = JSONUtil.getJSON(awsiamRole);
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String roleName = "" ;

		try {
			JsonNode root = objMapper.readTree(jsonStr);
			currentPolicies = root.get("policies").asText();
			latestPolicies = currentPolicies;   
		} catch (IOException e) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Update IAM role").
					put(LogMessage.MESSAGE, String.format("Failed to extract role/policies from json string [%s]", jsonStr)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}


		Response response = reqProcessor.process("/auth/aws/roles/delete",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			response = reqProcessor.process("/auth/aws/iam/roles/update",jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				response = ControllerUtil.updateMetaDataOnConfigChanges(roleName, "aws-roles", currentPolicies, latestPolicies, token);
				if(!HttpStatus.OK.equals(response.getHttpstatus()))
					return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"AWS Role configured\",\""+response.getResponse()+"\"]}");
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	/**
	 * 
	 * @param token
	 * @param role
	 * @return
	 * @throws TVaultValidationException
	 */
	private AWSIAMMetadataDetails readIAMRoleDetails(String token, String role) throws TVaultValidationException {
		AWSIAMMetadataDetails awsiamMetadataDetails=null;
		Response awsIamResponse = reqProcessor.process("/auth/aws/iam/roles",ROLESTR+role+"\"}",token);
		String responseJson = awsIamResponse.getResponse();	
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(awsIamResponse.getHttpstatus())) {
			try {
				responseMap = new ObjectMapper().readValue(responseJson, new TypeReference<Map<String, Object>>(){});
			}catch (IOException e) {
				logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "getIAMRole").
					      put(LogMessage.MESSAGE, "Reading IAMRole failed").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return awsiamMetadataDetails;
			}
			if (responseMap != null) {
				String authType=(String) responseMap.get("auth_type");
				if(!authType.equalsIgnoreCase("iam")) {
					throw new TVaultValidationException("Please enter an AWS IAM role");
				}
				String[] policies = null;
				if (responseMap.get(POLICIESSTR) != null && ((ArrayList<String>)responseMap.get(POLICIESSTR)) != null) {
					ArrayList<String> policiesList = ((ArrayList<String>)responseMap.get(POLICIESSTR));
					String policyString = policiesList.stream().collect(Collectors.joining(",")).toString();
					policies = policyString.split(",");
				}
				awsiamMetadataDetails = new AWSIAMMetadataDetails
						((responseMap.get("auth_type").toString()),
						(null),
						(role),
						(policies),
						(null),
						(null),
						((Integer)responseMap.get("max_ttl")),
						((Boolean)responseMap.get("disallow_reauthentication")).booleanValue(),
						((Boolean)responseMap.get("allow_instance_migration")).booleanValue(),
						((Boolean)responseMap.get("resolve_aws_unique_ids")).booleanValue()
						);
			}
			return awsiamMetadataDetails;
		}
		logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "getIAMRole").
			      put(LogMessage.MESSAGE, "Reading IAMRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return awsiamMetadataDetails;
	}

	/**
	 * Gets the registered role
	 * @param token
	 * @param role
	 * @return
	 */
	public ResponseEntity<String> fetchIAMRole(String token, String role){
		String jsoninput= ROLESTR+role+"\"}";
		Response response = reqProcessor.process("/auth/aws/iam/roles",jsoninput,token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * Gets the list of registered roles
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listIAMRoles(String token){
		Response response = reqProcessor.process("/auth/aws/iam/roles/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}

	/**
	 * deletes a registered role
	 * @param token
	 * @param role
	 * @return
	 */
	public ResponseEntity<String> deleteIAMRole(String token, String role){

		Response response = reqProcessor.process("/auth/aws/iam/roles/delete",ROLESTR+role+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"IAM Role deleted \"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}

	/**
	 * To configure AWSIAM role
	 * @param roleName
	 * @param policies
	 * @param token
	 * @return
	 */
	public Response configureAWSIAMRole(String roleName,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureRoleMap = new HashMap<>();
		configureRoleMap.put("role", roleName);
		configureRoleMap.put(POLICIESSTR, policies);
		String awsConfigJson ="";
		try {
			awsConfigJson = objMapper.writeValueAsString(configureRoleMap);
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "configureAWSIAMRole").
					put(LogMessage.MESSAGE, String.format("AWS IAM Role [%s] successfully associated.",roleName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		} catch (JsonProcessingException e) {
			logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "configureAWSIAMRole").
					put(LogMessage.MESSAGE, String.format ("Unable to create awsConfigJson with message [%s] for roleName [%s] policies [%s] ", e.getMessage(), roleName, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return reqProcessor.process("/auth/aws/iam/roles/update",awsConfigJson,token);
	}

}
