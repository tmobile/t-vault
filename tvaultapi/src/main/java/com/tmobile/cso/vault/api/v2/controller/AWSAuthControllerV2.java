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

package com.tmobile.cso.vault.api.v2.controller;

import com.tmobile.cso.vault.api.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.service.AWSAuthService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@Api(description = "Manage AWS Authentication", position = 13)
public class AWSAuthControllerV2 {
	
	@Autowired
	AWSAuthService awsAuthService;
	
	/**
	 * Method to authenticate using aws ec2 pkcs7 document and app role
	 * @param login
	 * @return
	 */
	@ApiOperation(value = "${AWSAuthControllerV2.authenticateEC2.value}", notes = "${AWSAuthControllerV2.authenticateEC2.notes}")
	@PostMapping(value="/v2/auth/aws/login",consumes="application/json",produces="application/json")
	public ResponseEntity<String> authenticateEC2( @RequestBody AWSLogin login){
		return awsAuthService.authenticateEC2(login);
	}
	/**
	 * Method to create an aws app role
	 * @param token
	 * @param awsLoginRole
	 * @return
	 */
	@ApiOperation(value = "${AWSAuthControllerV2.createRole.value}", notes = "${AWSAuthControllerV2.createRole.notes}")
	@PostMapping(value="/v2/auth/aws/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> createRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSLoginRole awsLoginRole) throws TVaultValidationException {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return awsAuthService.createRole(token, awsLoginRole, userDetails);
	}
	/**
	 * Method to update an aws app role. 
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${AWSAuthControllerV2.updateRole.value}", notes = "${AWSAuthControllerV2.updateRole.notes}")
	@PutMapping(value="/v2/auth/aws/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> updateRole(@RequestHeader(value="vault-token") String token, @RequestBody AWSLoginRole awsLoginRole) throws TVaultValidationException {
		return awsAuthService.updateRole(token, awsLoginRole);
	}
	/**
	 * Deleting an existing role.
	 * 
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${AWSAuthControllerV2.deleteRole.value}", notes = "${AWSAuthControllerV2.deleteRole.notes}")
	@DeleteMapping(value="/v2/auth/aws/role/{role}",produces="application/json")
	public ResponseEntity<String> deleteRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @PathVariable("role" ) String role){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return awsAuthService.deleteRole(token, role, userDetails);
	}
	/**
	 * Method to fetch information for an AWS EC2 role.
	 * @param token
	 * @param role
	 * @return
	 * 
	 * Sample return
	 * {"bound_ami_id":"testami",,"bound_account_id":"","bound_iam_instance_profile_arn":"","bound_iam_role_arn":""
	 * 		"policies":["default","testpolicy","testpolicy1"]
	 * }
	 */
	@ApiOperation(value = "${AWSAuthControllerV2.fetchRole.value}", notes = "${AWSAuthControllerV2.fetchRole.notes}")
	@GetMapping(value="/v2/auth/aws/role/{role}",produces="application/json")
	public ResponseEntity<String> fetchRole(@RequestHeader(value="vault-token") String token, @PathVariable("role") String role){
		return awsAuthService.fetchRole(token, role);
	}
	/**
	 * Method to get list of AWS Roles
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${AWSAuthControllerV2.listRoles.value}", notes = "${AWSAuthControllerV2.listRoles.notes}")
	@GetMapping(value="/roles",produces="application/json")
	public ResponseEntity<String> listRoles(@RequestHeader(value="vault-token") String token){
		return awsAuthService.listRoles(token);
	}


	/**
	 * 
	 * @param token
	 * @param awsClientConfiguration
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.configureClient.value}", notes = "${AWSIAMAuthControllerV2.configureClient.notes}")
	@PostMapping(value="/v2/auth/aws/config/client",consumes="application/json",produces="application/json")
	public ResponseEntity<String> configureClient(@RequestHeader(value="vault-token") String token, @RequestBody AWSClientConfiguration awsClientConfiguration){
		return awsAuthService.configureClient(awsClientConfiguration, token);
	}
	/**
	 * 
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.readClientConfiguration.value}", notes = "${AWSIAMAuthControllerV2.readClientConfiguration.notes}")
	@GetMapping(value="/v2/auth/aws/config/client",produces="application/json")
	public ResponseEntity<String> readConfiguration(@RequestHeader(value="vault-token") String token){
		return awsAuthService.readClientConfiguration(token);
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
		return awsAuthService.createSTSRole(awsStsRole, token);
	}
	
	/**
	 * 
	 * @param token
	 * @param awsiamLogin
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.authenticateIAM.value}", notes = "${AWSIAMAuthControllerV2.authenticateIAM.notes}")
	@PostMapping(value="/v2/auth/aws/iam/login",produces="application/json")
	public ResponseEntity<String> authenticateIAM(@RequestBody AWSIAMLogin awsiamLogin){
		return awsAuthService.authenticateIAM(awsiamLogin);
	}
	
	/**
	 * 
	 * @param token
	 * @param awsiamLogin
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.authenticateAWS.value}", notes = "${AWSIAMAuthControllerV2.authenticateAWS.notes}")
	@PostMapping(value="/v2/auth/aws/login/{authType}",produces="application/json")
	public ResponseEntity<String> authenticate(@PathVariable("authType" ) AWSAuthType authType, @RequestBody AWSAuthLogin awsAuthLogin){
		return awsAuthService.authenticate(authType, awsAuthLogin);
	}
	
}
