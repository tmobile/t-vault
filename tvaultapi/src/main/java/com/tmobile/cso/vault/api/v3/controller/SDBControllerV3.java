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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeUser;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.service.SelfSupportService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api(description = "Manage Safes/SDBs", position = 21)
public class SDBControllerV3 {

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
	@ApiOperation(value = "${SDBControllerV3.getFolders.value}", notes = "${SDBControllerV3.getFolders.notes}")
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
	@ApiOperation(value = "${SDBControllerV3.getSafeAsPowerUser.value}", notes = "${SDBControllerV3.getSafeAsPowerUser.notes}")
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
	@ApiOperation(value = "${SDBControllerV3.addUserToSafe.value}", notes = "${SDBControllerV3.addUserToSafe.notes}")
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
	@ApiOperation(value = "${SDBControllerV3.removeUserFromSafe.value}", notes = "${SDBControllerV3.removeUserFromSafeAsPowerUser.notes}")
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
	@ApiOperation(value = "${SDBControllerV3.getInfoAsPowerUser.value}", notes = "${SDBControllerV3.getInfoAsPowerUser.notes}")
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
	@ApiOperation(value = "${SDBControllerV3.createSafe.value}", notes = "${SDBControllerV3.createSafe.notes}")
	@PostMapping(value="/v3/sdb", consumes="application/json",produces="application/json")
	public ResponseEntity<String> createSafe(HttpServletRequest request, @RequestHeader(value="vault-token" ) String token, @RequestBody Safe safe) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return selfSupportService.createSafe(userDetails, token, safe);
	}
}
