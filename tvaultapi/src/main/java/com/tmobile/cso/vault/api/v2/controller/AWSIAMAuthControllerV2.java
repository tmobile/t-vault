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

import com.tmobile.cso.vault.api.model.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.service.AWSIAMAuthService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@Api(description = "Manage AWS IAM Authentication", position = 13)
public class AWSIAMAuthControllerV2 {
	
	@Autowired
	private AWSIAMAuthService awsIamAuthService;
	/**
	 * 
	 * @param token
	 * @param awsiamRole
	 * @return
	 */
	@ApiOperation(value = "${AWSIAMAuthControllerV2.createIamRole.value}", notes = "${AWSIAMAuthControllerV2.createIamRole.notes}")
	@PostMapping(value="/v2/auth/aws/iam/role",produces="application/json")
	public ResponseEntity<String> createIAMRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSIAMRole awsiamRole) throws TVaultValidationException{
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return awsIamAuthService.createIAMRole(awsiamRole, token, userDetails);
	}
	
	@ApiOperation(value = "${AWSIAMAuthControllerV2.updateRole.value}", notes = "${AWSIAMAuthControllerV2.updateRole.notes}")
	@PutMapping(value="/v2/auth/aws/iam/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> updateRole(@RequestHeader(value="vault-token") String token, @RequestBody AWSIAMRole awsiamRole) throws TVaultValidationException {
		return awsIamAuthService.updateIAMRole(token, awsiamRole);
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

}
