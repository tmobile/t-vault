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

import com.tmobile.cso.vault.api.model.*;
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

import com.tmobile.cso.vault.api.service.AppRoleService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.servlet.http.HttpServletRequest;


@RestController
@CrossOrigin
@Api(description = "Manage AppRole Authentication", position = 12)
public class AppRoleControllerV2 {

	@Autowired
	private AppRoleService appRoleService;
	
	/**
	 * 
	 * @param token
	 * @param appRole
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.createAppRole.value}", notes = "${AppRoleControllerV2.createAppRole.notes}")
	@PostMapping(value="/v2/auth/approle/role", consumes="application/json", produces="application/json")
	public ResponseEntity<String> createAppRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AppRole appRole){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return appRoleService.createAppRole(token, appRole, userDetails);
	}
	
	/**
	 * ASSOCIATE APPROLE TO SDB
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.associateApprole.value}", notes = "${AppRoleControllerV2.associateApprole.notes}")
	@PostMapping(value="/v2/auth/approle/associateApprole",produces="application/json")
	public ResponseEntity<String> associateApprole(@RequestHeader(value="vault-token") String token, @RequestBody SafeAppRoleAccess safeAppRoleAccess){
		return appRoleService.associateApprole(token, safeAppRoleAccess);
	}
	/**
	 * READ APPROLE
	 * @param token
	 * @param rolename
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.readAppRole.value}", notes = "${AppRoleControllerV2.readAppRole.notes}")
	@GetMapping(value="/v2/auth/approle/role/{role_name}",produces="application/json")
	public ResponseEntity<String> readAppRole(@RequestHeader(value="vault-token") String token, @PathVariable("role_name" ) String rolename){
		return appRoleService.readAppRole(token, rolename);	
	}
	
	@ApiOperation(value = "${AppRoleControllerV2.listAppRoles.value}", notes = "${AppRoleControllerV2.listAppRoles.notes}")
	@GetMapping (value="/v2/auth/approle/role",produces="application/json")
	public ResponseEntity<String> getAppRoles(@RequestHeader(value="vault-token") String token){
		return appRoleService.readAppRoles(token);	
	}
	/**
	 * READ APPROLE ROLEID
	 * @param token
	 * @param rolename
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.readAppRoleRoleId.value}", notes = "${AppRoleControllerV2.readAppRoleRoleId.notes}")
	@GetMapping(value="/v2/auth/approle/role/{role_name}/role_id",produces="application/json")
	public ResponseEntity<String> readAppRoleRoleId(@RequestHeader(value="vault-token") String token, @PathVariable("role_name" ) String rolename){
		return appRoleService.readAppRoleRoleId(token, rolename);
	}
	/**
	 * DELETE APPROLE
	 * @param token
	 * @param username
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.deleteAppRole.value}", notes = "${AppRoleControllerV2.deleteAppRole.notes}")
	@DeleteMapping(value="/v2/auth/approle/role/{role_name}",produces="application/json")
	public ResponseEntity<String> deleteAppRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @PathVariable("role_name" ) String rolename){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		AppRole appRole = new AppRole();
		appRole.setRole_name(rolename);
		return appRoleService.deleteAppRole(token, appRole, userDetails);
	}
	/**
	 * CREATE SECRETID FOR APPROLE
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.createsecretId.value}", notes = "${AppRoleControllerV2.createsecretId.notes}")
	@PostMapping(value="/v2/auth/approle/role/secret_id", consumes="application/json", produces="application/json")
	public ResponseEntity<String> createsecretId(@RequestHeader(value="vault-token") String token, @RequestBody AppRoleSecretData appRoleSecretData){
		return appRoleService.createsecretId(token, appRoleSecretData);
	}
	/**
	 * DELETE SECRET FOR APPROLE
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.deleteSecretId.value}", notes = "${AppRoleControllerV2.deleteSecretId.notes}")
	@DeleteMapping(value="/v2/auth/approle/role/secret_id",produces="application/json")
	public ResponseEntity<String> deleteSecretId(@RequestHeader(value="vault-token") String token,  @RequestBody AppRoleNameSecretId appRoleNameSecretId){
		return appRoleService.deleteSecretId(token, appRoleNameSecretId);	
	}
	/**
	 * READ SECRETID FOR APPROLE
	 * @param token
	 * @param rolename
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.readSecretId.value}", notes = "${AppRoleControllerV2.readSecretId.notes}")
	@GetMapping(value="/v2/auth/approle/role/{role_name}/secret_id",produces="application/json")
	public ResponseEntity<String> readSecretId(@RequestHeader(value="vault-token") String token, @PathVariable("role_name" ) String rolename){
		return appRoleService.readAppRoleSecretId(token, rolename);
	}
	/**
	 * Login using AppRole
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${AppRoleControllerV2.login.value}", notes = "${AppRoleControllerV2.login.notes}")
	@PostMapping(value="/v2/auth/approle/login",produces="application/json")	
	public ResponseEntity<String> login(@RequestBody AppRoleIdSecretId appRoleIdSecretId){
		return appRoleService.login(appRoleIdSecretId);
	}
}
