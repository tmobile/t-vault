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

package com.tmobile.cso.vault.api.v3.controller;

import javax.servlet.http.HttpServletRequest;

import com.tmobile.cso.vault.api.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tmobile.cso.vault.api.service.SelfSupportService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api(description = "Manage Safes/SDBs", position = 21)
public class SelfSupportController {

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	@Autowired
	private SelfSupportService selfSupportService;

	/**
	 * Reads the contents of a folder recursively
	 * @param token
	 * @param path
	 * @return
	 */
	@GetMapping(value="/v3/sdb/list",produces="application/json")
	@ApiOperation(value = "${SelfSupportController.getFolders.value}", notes = "${SelfSupportController.getFolders.notes}")
	public ResponseEntity<String> getFoldersRecursively(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("path") String path) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.getFoldersRecursively(userDetails, token, path);
	}
	/**
	 * 
	 * @param token
	 * @param path
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.getSafeAsPowerUser.value}", notes = "${SelfSupportController.getSafeAsPowerUser.notes}")
	@GetMapping(value="/v3/sdb",produces="application/json")
	public ResponseEntity<String> getSafe(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.getSafe(userDetails, token, path);
	}
	/**
	 * Adds user with a Safe as Power User
	 * @param token
	 * @param safeUser
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.addUserToSafe.value}", notes = "${SelfSupportController.addUserToSafe.notes}")
	@PostMapping(value="/v3/sdb/user",consumes="application/json",produces="application/json")
	public ResponseEntity<String> addUsertoSafe(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody SafeUser safeUser){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.addUserToSafe(userDetails, token, safeUser);
	}
	/**
	 * 
	 * @param token
	 * @param safeUser
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.removeUserFromSafe.value}", notes = "${SelfSupportController.removeUserFromSafeAsPowerUser.notes}")
	@DeleteMapping(value="/v3/sdb/user")
	public ResponseEntity<String> deleteUserFromSafe(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody SafeUser safeUser){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.removeUserFromSafe(userDetails, token, safeUser);
	}
	/**
	 * Gets information about SDB
	 * @param token
	 * @param path
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.getInfoAsPowerUser.value}", notes = "${SelfSupportController.getInfoAsPowerUser.notes}")
	@GetMapping(value="/v3/sdb/folder/{path}",produces="application/json")
	public ResponseEntity<String> getInfo(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.getInfo(userDetails, token, path);
	}
	/**
	 * 
	 * @param token
	 * @param safe
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.createSafe.value}", notes = "${SelfSupportController.createSafe.notes}")
	@PostMapping(value="/v3/sdb", consumes="application/json",produces="application/json")
	public ResponseEntity<String> createSafe(HttpServletRequest request, @RequestHeader(value="vault-token" ) String token, @RequestBody Safe safe) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.createSafe(userDetails, token, safe);
	}
	/**
	 * 
	 * @param request
	 * @param token
	 * @param path
	 * @return
	 */
	@GetMapping(value="/auth/tvault/isauthorized",produces="application/json")
	@ApiOperation(value = "${VaultAuthControllerV2.authorized.value}", notes = "${SelfSupportController.authorized.notes}")
	public ResponseEntity<String> isAuthorized(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.isAuthorized(userDetails, path);
	}

	/**
	 *
	 * @param token
	 * @param safe
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.updateSafe.value}", notes = "${SelfSupportController.updateSafe.notes}")
	@PutMapping(value="/v3/sdb", consumes="application/json",produces="application/json")
	public ResponseEntity<String> updateSafe(HttpServletRequest request, @RequestHeader(value="vault-token" ) String token, @RequestBody Safe safe) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.updateSafe(userDetails, token, safe);
	}
	/**
	 * Deletes a SDB folder
	 * @param token
	 * @param path
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.deleteSafe.value}", notes = "${SelfSupportController.deleteSafe.notes}")
	@DeleteMapping(value="/v3/sdb/delete",produces="application/json")
	public ResponseEntity<String> deleteFolder(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.deletefolder(userDetails, token, path);
	}

	/**
	 * Adds a group to a safe
	 * @param token
	 * @param safeGroup
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.addGroupToSafe.value}", notes = "${SelfSupportController.addGroupToSafe.notes}")
	@PostMapping(value="/v3/sdb/group",consumes="application/json",produces="application/json")
	public ResponseEntity<String> addGrouptoSafe(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody SafeGroup safeGroup){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.addGroupToSafe(userDetails, token, safeGroup);
	}
	/**
	 * Removes a group from safe
	 * @param token
	 * @param safeGroup
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.deleteGroupFromSafe.value}", notes = "${SelfSupportController.deleteGroupFromSafe.notes}")
	@DeleteMapping (value="/v3/sdb/group",consumes="application/json",produces="application/json")
	public ResponseEntity<String> deleteGroupFromSafe(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody SafeGroup safeGroup){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.removeGroupFromSafe(userDetails, token, safeGroup);
	}

	/**
	 * Adds AWS role to a Safe
	 * @param token
	 * @param awsRole
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.addAWSRoleToSafe.value}", notes = "${SelfSupportController.addAWSRoleToSafe.notes}")
	@PostMapping (value="/v3/sdb/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> addAwsRoleToSafe(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSRole awsRole){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.addAwsRoleToSafe(userDetails, token, awsRole);
	}

	/**
	 * Remove AWS role from Safe and delete the role
	 * @param token
	 * @param awsRole
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.deleteAWSRoleFromSafe.value}", notes = "${SelfSupportController.deleteAWSRoleFromSafe.notes}")
	@DeleteMapping (value="/v3/sdb/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> deleteAwsRoleFromSafe(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSRole awsRole){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.removeAWSRoleFromSafe(userDetails, token, awsRole, false);
	}

	/**
	 * Detach AWS role from safe
	 * @param token
	 * @param awsRole
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.deleteAWSPermissionFromSafe.value}", notes = "${SelfSupportController.deleteAWSPermissionFromSafe.notes}")
	@PutMapping (value="/v3/sdb/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> detachAwsRoleFromSafe(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSRole awsRole) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.removeAWSRoleFromSafe(userDetails, token, awsRole, true);
	}

	/**
	 * Associate approle to Safe
	 * @param token
	 * @param jsonstr
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.associateApprole.value}", notes = "${SelfSupportController.associateApprole.notes}")
	@PostMapping(value="/v3/sdb/approle",consumes="application/json",produces="application/json")
	public ResponseEntity<String>associateApproletoSDB(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody String jsonstr) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.associateApproletoSDB(userDetails, token, jsonstr);
	}

	/**
	 * Delete approle from Safe
	 * @param token
	 * @param jsonstr
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.deleteApproleFromSafe.value}", notes = "${SelfSupportController.deleteApproleFromSafe.notes}")
	@DeleteMapping(value="/v3/sdb/approle",consumes="application/json",produces="application/json")
	public ResponseEntity<String>deleteApproleFromSDB(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody String jsonstr) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.deleteApproleFromSDB(userDetails, token, jsonstr);
	}
}
