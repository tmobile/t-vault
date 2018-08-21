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

package com.tmobile.cso.vault.api.v2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.AWSClientConfiguration;
import com.tmobile.cso.vault.api.model.AWSIAMLogin;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSStsRole;
import com.tmobile.cso.vault.api.service.AWSIAMAuthService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api(description = "Manage AWS IAM Authentication", position = 13)
public class AWSIAMAuthControllerV2 {
	
	@Autowired
	AWSIAMAuthService awsIamAuthService;
	/**
	 * 
	 * @param token
	 * @param awsClientConfiguration
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.configureClient.value}", notes = "${AWSIAMAuthControllerV2.configureClient.notes}")
	@PostMapping(value="/v2/auth/aws/config/client",consumes="application/json",produces="application/json")
	public ResponseEntity<String> configureClient(@RequestHeader(value="vault-token") String token, @RequestBody AWSClientConfiguration awsClientConfiguration){
		return awsIamAuthService.configureClient(awsClientConfiguration, token);
	}
	/**
	 * 
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.readClientConfiguration.value}", notes = "${AWSIAMAuthControllerV2.readClientConfiguration.notes}")
	@GetMapping(value="/v2/auth/aws/config/client",produces="application/json")
	public ResponseEntity<String> readConfiguration(@RequestHeader(value="vault-token") String token){
		return awsIamAuthService.readClientConfiguration(token);
	}
	/**
	 * 
	 * @param token
	 * @param awsStsRole
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.createStsRole.value}", notes = "${AWSIAMAuthControllerV2.createStsRole.notes}")
	@PostMapping(value="/v2/auth/aws/config/sts",produces="application/json")
	public ResponseEntity<String> createSTSRole(@RequestHeader(value="vault-token") String token, @RequestBody AWSStsRole awsStsRole){
		return awsIamAuthService.createSTSRole(awsStsRole, token);
	}
	/**
	 * 
	 * @param token
	 * @param awsiamRole
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.createIamRole.value}", notes = "${AWSIAMAuthControllerV2.createIamRole.notes}")
	@PostMapping(value="/v2/auth/aws/iam/role",produces="application/json")
	public ResponseEntity<String> createIAMRole(@RequestHeader(value="vault-token") String token, @RequestBody AWSIAMRole awsiamRole){
		return awsIamAuthService.createIAMRole(awsiamRole, token);
	}
	/**
	 * 
	 * @param token
	 * @param role
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.fetchRole.value}", notes = "${AWSIAMAuthControllerV2.fetchRole.notes}")
	@GetMapping(value="/v2/auth/aws/iam/role/{role}",produces="application/json")
	public ResponseEntity<String> fetchIAMRole(@RequestHeader(value="vault-token") String token, @PathVariable("role") String role){
		return awsIamAuthService.fetchIAMRole(token, role);
	}
	/**
	 * Method to get list of AWS Roles
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.listRoles.value}", notes = "${AWSIAMAuthControllerV2.listRoles.notes}")
	@GetMapping(value="/v2/auth/aws/iam/roles",produces="application/json")
	public ResponseEntity<String> listIAMRoles(@RequestHeader(value="vault-token") String token){
		return awsIamAuthService.listIAMRoles(token);
	}
	/**
	 * 
	 * @param token
	 * @param role
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.deleteRole.value}", notes = "${AWSIAMAuthControllerV2.deleteRole.notes}")
	@DeleteMapping(value="/v2/auth/aws/iam/roles/{role}",produces="application/json")
	public ResponseEntity<String> deleteIAMRole(@RequestHeader(value="vault-token") String token, @PathVariable("role" ) String role){
		return awsIamAuthService.deleteIAMRole(token, role);
	}
	
	/**
	 * 
	 * @param token
	 * @param awsiamLogin
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.login.value}", notes = "${AWSIAMAuthControllerV2.login.notes}")
	@PostMapping(value="/v2/auth/aws/iam/login",produces="application/json")
	public ResponseEntity<String> login(@RequestHeader(value="vault-token") String token, @RequestBody AWSIAMLogin awsiamLogin){
		return awsIamAuthService.login(awsiamLogin, token);
	}

}
