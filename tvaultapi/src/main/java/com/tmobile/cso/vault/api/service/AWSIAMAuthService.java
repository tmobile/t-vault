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
import com.tmobile.cso.vault.api.model.AWSClientConfiguration;
import com.tmobile.cso.vault.api.model.AWSIAMLogin;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.AWSStsRole;
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

	/**
	 * Registers a role in the method.
	 * @param awsiamRole
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> createIAMRole(AWSIAMRole awsiamRole, String token) throws TVaultValidationException{
		if (!ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)) {
			//return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid inputs for the given aws login type");
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		String jsonStr = JSONUtil.getJSON(awsiamRole);
		Response response = reqProcessor.process("/auth/aws/iam/role/create",jsonStr, token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM Role created successfully \"]}");
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
			//return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid inputs for the given aws login type");
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		String jsonStr = JSONUtil.getJSON(awsiamRole);
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

		Response awsResponse = reqProcessor.process("/auth/aws/iam/roles","{\"role\":\""+roleName+"\"}",token);
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
	 * Gets the registered role
	 * @param token
	 * @param role
	 * @return
	 */
	public ResponseEntity<String> fetchIAMRole(String token, String role){
		String jsoninput= "{\"role\":\""+role+"\"}";
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

		Response response = reqProcessor.process("/auth/aws/iam/roles/delete","{\"role\":\""+role+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"IAM Role deleted \"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}

}
