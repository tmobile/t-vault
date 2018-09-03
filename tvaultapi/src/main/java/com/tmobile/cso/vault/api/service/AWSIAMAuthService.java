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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.model.AWSClientConfiguration;
import com.tmobile.cso.vault.api.model.AWSIAMLogin;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
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
	 * Registers a role in the method.
	 * @param awsiamRole
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> createIAMRole(AWSIAMRole awsiamRole, String token){
		String jsonStr = JSONUtil.getJSON(awsiamRole);
		Response response = reqProcessor.process("/auth/aws/iam/role/create",jsonStr, token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM Role created successfully \"]}");
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
	/**
	 * Logs in 
	 * @param awsiamLogin
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> login(AWSIAMLogin awsiamLogin,  String token){
		String jsonStr = JSONUtil.getJSON(awsiamLogin);
		Response response = reqProcessor.process("/auth/aws/iam/login",jsonStr,"");
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else {
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
}
