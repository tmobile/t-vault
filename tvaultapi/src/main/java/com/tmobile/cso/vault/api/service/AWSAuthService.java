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

package com.tmobile.cso.vault.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.AWSAuthLogin;
import com.tmobile.cso.vault.api.model.AWSAuthType;
import com.tmobile.cso.vault.api.model.AWSClientConfiguration;
import com.tmobile.cso.vault.api.model.AWSIAMLogin;
import com.tmobile.cso.vault.api.model.AWSLogin;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.AWSStsRole;
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
			logger.debug(e.getMessage());
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
	public ResponseEntity<String> createRole(String token, AWSLoginRole awsLoginRole) throws TVaultValidationException{
		if (!ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)) {
			//return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid inputs for the given aws login type");
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		String jsonStr = JSONUtil.getJSON(awsLoginRole);
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String roleName = "" ;

		try {
			JsonNode root = objMapper.readTree(jsonStr);
			roleName = root.get("role").asText();
			if(root.get("policies") != null)
				latestPolicies = root.get("policies").asText();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug(e.getMessage());
		}

		Response response = reqProcessor.process("/auth/aws/roles/create",jsonStr,token);

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ // Role created with policies. Need to update SDB metadata too.
			response = ControllerUtil.updateMetaDataOnConfigChanges(roleName, "roles", currentPolicies, latestPolicies, token);
			if(!HttpStatus.OK.equals(response.getHttpstatus()))
				return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"AWS Role configured\",\""+response.getResponse()+"\"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
	}
	/**
	 * Method to update an aws app role.
	 * @param token
	 * @param awsLoginRole
	 * @return
	 */
	public ResponseEntity<String> updateRole(String token, AWSLoginRole awsLoginRole) throws TVaultValidationException{
		if (!ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)) {
			//return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid inputs for the given aws login type");
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		String jsonStr = JSONUtil.getJSON(awsLoginRole);
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String roleName = "" ;

		try {
			JsonNode root = objMapper.readTree(jsonStr);
			roleName = root.get("role").asText();
			if(root.get("policies") != null)
				latestPolicies = root.get("policies").asText();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug(e.getMessage());
		}

		Response awsResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+roleName+"\"}",token);
		String responseJson="";	

		if(HttpStatus.OK.equals(awsResponse.getHttpstatus())){
			responseJson = awsResponse.getResponse();	
			try {
				Map<String,Object> responseMap; 
				responseMap = objMapper.readValue(responseJson, new TypeReference<Map<String, Object>>(){});
				@SuppressWarnings("unchecked")
				List<String> policies  = (List<String>) responseMap.get("policies");
				currentPolicies = policies.stream().collect(Collectors.joining(",")).toString();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.debug(e.getMessage());
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Update failed . AWS Role does not exist \"]}");
		}

		Response response = reqProcessor.process("/auth/aws/roles/delete",jsonStr,token);
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
	public ResponseEntity<String> deleteRole(String token, String role){
		Response response = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role deleted \"]}");
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
		String jsoninput= "{\"role\":\""+role+"\"}";
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
}
