/* =========================================================================
 Copyright 2019 T-Mobile, US
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 See the readme.txt file for additional language around disclaimer of warranties.
========================================================================= */

package com.tmobile.cso.vault.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;

@Component
public class  AWSAuthService {

	@Value("${vault.port}")
	private String vaultPort;

	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger logger = LogManager.getLogger(AWSAuthService.class);
	private static final String ROLENONEXISTSTR = "{\"errors\":[\"EC2 Role doesn't exist\"]}";
	
	private static final String POLICIESSTR = "policies";
	private static final String ROLEDELETEPATHSTR = "/auth/aws/roles/delete";
	private static final String ROLESTR = "{\"role\":\"";
	private static final String PATHSTR = "{\"path\":\"";
	/**
	 * To authenticate using aws ec2 pkcs7 document and app role
	 * @param login
	 * @return
	 */
	public ResponseEntity<String> authenticateEC2(AWSLogin login){
		String jsonStr = JSONUtil.getJSON(login);
		if(jsonStr.toLowerCase().contains("nonce")){
			return ResponseEntity.badRequest().body("{\"errors\":[\"Not a valid request. Parameter 'nonce' is not expected \"]}");
		}

		String nonce= "";
		try {
			nonce = new ObjectMapper().readTree(jsonStr).at("/pkcs7").toString().substring(1,50);
		} catch (IOException e) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Authenticate EC2").
					put(LogMessage.MESSAGE, String.format("Failed to extract pkcs7 from json [%s]", jsonStr)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.badRequest().body("{\"errors\":[\"Not valid request. Check params \"]}");
		}
		String noncejson = "{\"nonce\":\""+nonce+"\",";
		jsonStr = noncejson + jsonStr.substring(1);

		Response response = reqProcessor.process("/auth/aws/login",jsonStr,"");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * To create an aws app role
	 * @param token
	 * @param awsLoginRole
	 * @return
	 */
	public ResponseEntity<String> createRole(String token, AWSLoginRole awsLoginRole, UserDetails userDetails) throws TVaultValidationException{
		if(!StringUtils.isEmpty(awsLoginRole.getPolicies())) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "Checking whether policy is added as an input param for create AWS role").
					put(LogMessage.MESSAGE, String.format("Trying to create AWS Role [%s]", awsLoginRole.getRole())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Policies are not permitted during role creation.\"]}");
		}
		if (!ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)) {
			throw new TVaultValidationException("Invalid inputs for the given AWS login type");
		}
		logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "Create AWS role").
				put(LogMessage.MESSAGE, String.format("Trying to create AWS Role [%s]", awsLoginRole.getRole())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		String jsonStr = JSONUtil.getJSON(awsLoginRole);
		String currentPolicies = "";
		String latestPolicies = "";
		String roleName = "" ;
		ObjectMapper objMapper = new ObjectMapper();

		try {
			JsonNode root = objMapper.readTree(jsonStr);
			roleName = root.get("role").asText();
			if(root.get("policies") != null)
				latestPolicies = root.get("policies").asText();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "Create AWS role").
					put(LogMessage.MESSAGE, String.format("Failed to extract role/policies from json string [%s]", jsonStr)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		Response response = reqProcessor.process("/auth/aws/roles/create",jsonStr,token);

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ // Role created with policies. Need to update SDB metadata too.
			String metadataJson = ControllerUtil.populateAWSMetaJson(awsLoginRole.getRole(), userDetails.getUsername());
			if(ControllerUtil.createMetadata(metadataJson, token)) {
				response = ControllerUtil.updateMetaDataOnConfigChanges(roleName, "roles", currentPolicies, latestPolicies, token);
				boolean awsec2RoleMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson, token);	
				String awsec2roleUsermetadataJson = ControllerUtil.populateUserMetaJson(awsLoginRole.getRole(), userDetails.getUsername(),awsLoginRole.getAuth_type());	
				boolean awsec2RoleUserMetaDataCreationStatus = ControllerUtil.createMetadata(awsec2roleUsermetadataJson, token);
				if(awsec2RoleMetaDataCreationStatus && awsec2RoleUserMetaDataCreationStatus) {
					logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Creating AWS EC2 role").
							put(LogMessage.MESSAGE, String.format("AWS EC2 Role [%s] created successfully by [%s]", awsLoginRole.getRole(),userDetails.getUsername())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS EC2 Role created \"]}");
				}
				else {
					return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"AWS Role configured\",\"" + response.getResponse() + "\"]}");
				}
			} else {
				// revert role creation
				Response deleteResponse = reqProcessor.process(ROLEDELETEPATHSTR,ROLESTR+awsLoginRole.getRole()+"\"}",token);
				if (deleteResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS role creation failed.\"]}");
				}
				else {
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS role created however metadata update failed. Please try with AWS role/update \"]}");
				}
			}
		} else{
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
	
	private AWSLoginRole readRoleDetails(String token, String role)throws TVaultValidationException {
		AWSLoginRole awsLoginRole=null; 
		Response awsec2Response = reqProcessor.process("/auth/aws/roles",ROLESTR+role+"\"}",token);
		String responseJson = awsec2Response.getResponse();	
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(awsec2Response.getHttpstatus())) {
			try {
				responseMap = new ObjectMapper().readValue(responseJson, new TypeReference<Map<String, Object>>(){});
			} catch (IOException e) {
				logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "getEC2Role").
					      put(LogMessage.MESSAGE, "Reading EC2Role failed").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return awsLoginRole;
			}
			if (responseMap != null) {
				String authType=(String) responseMap.get("auth_type");
				if(!authType.equalsIgnoreCase("ec2")) {
					throw new TVaultValidationException("Please enter an AWS EC2 role");
				}
				String policies = null;
				if (responseMap.get(POLICIESSTR) != null && ((ArrayList<String>)responseMap.get(POLICIESSTR)) != null) {
					ArrayList<String> policiesList = ((ArrayList<String>)responseMap.get(POLICIESSTR));
					policies = policiesList.stream().collect(Collectors.joining(",")).toString();
				}
				awsLoginRole = new AWSLoginRole
						((responseMap.get("auth_type").toString()),
						 role,
						(responseMap.get("bound_ami_id").toString()),
						(responseMap.get("bound_account_id").toString()),
						(responseMap.get("bound_region").toString()),
						(responseMap.get("bound_vpc_id").toString()),
						(responseMap.get("bound_subnet_id").toString()),
						(responseMap.get("bound_iam_role_arn").toString()),
						(responseMap.get("bound_iam_instance_profile_arn").toString()),
						(policies)
						);
			}
			return awsLoginRole;
		}
		logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "getec2Role").
			      put(LogMessage.MESSAGE, "Reading ec2Role failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return awsLoginRole;
	}
	/**
	 * Method to update an aws app role.
	 * @param token
	 * @param awsLoginRole
	 * @return
	 */
	public ResponseEntity<String> updateRole(String token, AWSLoginRole awsLoginRole) throws TVaultValidationException{
		if (!ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)) {
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		AWSLoginRole existingRole =readRoleDetails(token, awsLoginRole.getRole());
		if (existingRole == null) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "updateAWSEC2Role").
				      put(LogMessage.MESSAGE, String.format("Unable to read Role information. Role [%s] doesn't exist", awsLoginRole.getRole())).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body(ROLENONEXISTSTR);

		}
		String existingPolicy=existingRole.getPolicies();
		awsLoginRole.setPolicies(existingPolicy);
		String jsonStr = JSONUtil.getJSON(awsLoginRole);
		ObjectMapper objMapper = new ObjectMapper();
		String  currentPolicies = "";
		String latestPolicies = "";
		String roleName = "" ;

		try {
			JsonNode root = objMapper.readTree(jsonStr);
			currentPolicies = root.get("policies").asText();
			latestPolicies = currentPolicies;   
		} catch (IOException e) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Update AWS role").
					put(LogMessage.MESSAGE, String.format("Failed to extract role/policies from json string [%s]", jsonStr)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}


		Response response = reqProcessor.process(ROLEDELETEPATHSTR,jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			response = reqProcessor.process("/auth/aws/roles/update",jsonStr,token);
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
	 * Method to delete an existing role.
	 * @param token
	 * @param role
	 * @return
	 */
	public ResponseEntity<String> deleteRole(String token, String role, UserDetails userDetails){
		Response permissionResponse = ControllerUtil.canDeleteRole(role, token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH);
		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(permissionResponse.getHttpstatus()) || HttpStatus.UNAUTHORIZED.equals(permissionResponse.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+permissionResponse.getResponse()+"\"]}");
		}
		Response response = reqProcessor.process(ROLEDELETEPATHSTR,ROLESTR+role+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			// delete metadata
			String metaJson = ControllerUtil.populateAWSMetaJson(role, userDetails.getUsername());
			Response resp = reqProcessor.process("/delete",metaJson,token);
			if (HttpStatus.NO_CONTENT.equals(resp.getHttpstatus())) {
				logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Delete AWS Role").
						put(LogMessage.MESSAGE, "Metadata deleted").
						put(LogMessage.STATUS, response.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role deleted \"]}");
			}
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role deleted, metadata delete failed\"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	/**
	 * Method to fetch information for an aws approle.
	 * @param token
	 * @param role
	 * @return
	 */
	public ResponseEntity<String> fetchRole(String token, String role){
		String jsoninput= ROLESTR+role+"\"}";
		Response response = reqProcessor.process("/auth/aws/roles",jsoninput,token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To get list of AWS Roles
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listRoles(String token){
		Response response = reqProcessor.process("/auth/aws/roles/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	
	/**
	 * Configures the credentials required to perform API calls to AWS as well as custom endpoints to talk to AWS APIs.
	 * @param awsClientConfiguration
	 * @return
	 */
	public ResponseEntity<String> configureClient(AWSClientConfiguration awsClientConfiguration, String token){
		String jsonStr = JSONUtil.getJSON(awsClientConfiguration);
		Response response = reqProcessor.process("/auth/aws/config/configureclient",jsonStr, token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Client successfully configured \"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	/**
	 * Returns the configured AWS client access credentials.
	 * @param awsClientConfiguration
	 * @return
	 */
	public ResponseEntity<String> readClientConfiguration(String token){
		Response response = reqProcessor.process("/auth/aws/config/readclientconfig","{}", token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Allows the explicit association of STS roles to satellite AWS accounts
	 * @param awsStsRole
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> createSTSRole(AWSStsRole awsStsRole, String token){
		String jsonStr = JSONUtil.getJSON(awsStsRole);
		Response response = reqProcessor.process("/auth/aws/config/sts/create",jsonStr, token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"STS Role created successfully \"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	
	/**
	 * Logs in using IAM credentials
	 * @param awsiamLogin
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> authenticateIAM(AWSIAMLogin awsiamLogin){
		String jsonStr = JSONUtil.getJSON(awsiamLogin);
		Response response = reqProcessor.process("/auth/aws/iam/login",jsonStr,"");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * 
	 * @param authType
	 * @param awsAuthLogin
	 * @return
	 */
	public ResponseEntity<String> authenticate(AWSAuthType authType, AWSAuthLogin awsAuthLogin){
		if (!ControllerUtil.areAwsLoginInputsValid(authType, awsAuthLogin)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid inputs for the given aws login type");
		}
		if (AWSAuthType.EC2.equals(authType) ) {
			AWSLogin login = generateAWSEC2Login(awsAuthLogin);
			return authenticateEC2(login);
		}
		else if (AWSAuthType.IAM.equals(authType)) {
			AWSIAMLogin awsiamLogin = generateIAMLogin(awsAuthLogin);
			return authenticateIAM(awsiamLogin);
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Authentication type. Authentication type has to be either ec2 or iam");
		}
	}
	/**
	 * 
	 * @param awsAuthLogin
	 * @return
	 */
	private AWSLogin generateAWSEC2Login(AWSAuthLogin awsAuthLogin) {
		AWSLogin login = new AWSLogin();
		login.setPkcs7(awsAuthLogin.getPkcs7());
		login.setRole(awsAuthLogin.getRole());
		return login;
	}
	/**
	 * 
	 * @param awsAuthLogin
	 * @return
	 */
	private AWSIAMLogin generateIAMLogin(AWSAuthLogin awsAuthLogin) {
		AWSIAMLogin awsiamLogin  = new AWSIAMLogin();
		awsiamLogin.setIam_http_request_method(awsAuthLogin.getIam_http_request_method());
		awsiamLogin.setIam_request_body(awsAuthLogin.getIam_request_body());
		awsiamLogin.setIam_request_headers(awsAuthLogin.getIam_request_headers());
		awsiamLogin.setIam_request_url(awsAuthLogin.getIam_request_url());
		return awsiamLogin;
	}

	/**
	 * To configure AWS role
	 * @param roleName
	 * @param policies
	 * @param token
	 * @return
	 */
	public Response configureAWSRole(String roleName,String policies,String token ){
		logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "configureAWSRole")
				.put(LogMessage.MESSAGE,
						String.format("Trying to configure AWS EC2 Role [%s] with Azure Service Account.", roleName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
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
					put(LogMessage.MESSAGE, String.format("AWS EC2 Role [%s] successfully associated with Azure Service Account with policies [%s].",roleName,policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		} catch (JsonProcessingException e) {
			logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "configureAWSRole").
					put(LogMessage.MESSAGE, String.format ("Unable to create awsConfigJson [%s] with roleName [%s] policies [%s] ", e.getMessage(), roleName, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return reqProcessor.process("/auth/aws/roles/update",awsConfigJson,token);
	}
}
