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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.model.UserpassUser;
import com.tmobile.cso.vault.api.service.UserPassService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@RestController
@CrossOrigin
@Api(description = "Manage Userpass Authentication", position = 18)
public class UserPassControllerV2 {


	@Autowired
	private UserPassService userpassService;

	/**
	 * Create User
	 * @param token
	 * @param user
	 * @return
	 */
	@ApiOperation(value = "${UserPassControllerV2.createUser.value}", notes = "${UserPassControllerV2.createUser.notes}")
	@PostMapping(value="/v2/auth/userpass/users", consumes="application/json", produces="application/json")
	public ResponseEntity<String> createUser(@RequestHeader(value="vault-token") String token, @RequestBody UserpassUser user){
		return userpassService.createUser(token, user);
	}

	/**
	 * Read User
	 * @param token
	 * @param username
	 * @return
	 */
	@ApiOperation(value = "${UserPassControllerV2.readUser.value}", notes = "${UserPassControllerV2.readUser.notes}")
	@GetMapping(value="/v2/auth/userpass/users/{username}",produces="application/json")
	public ResponseEntity<String> readUser(@RequestHeader(value="vault-token") String token, @PathVariable("username" ) String username){
		return userpassService.readUser(token, username);
	}


	/**
	 * Delete User
	 * @param token
	 * @param user
	 * @return
	 */
	@ApiOperation(value = "${UserPassControllerV2.deleteUser.value}", notes = "${UserPassControllerV2.deleteUser.notes}")
	@DeleteMapping(value="/v2/auth/userpass/users/{username}",produces="application/json")
	public ResponseEntity<String> deleteUser(@RequestHeader(value="vault-token") String token, @RequestBody String username){
		return userpassService.deleteUser(token, username);
	}

	/**
	 * Update userpassword
	 * @param token
	 * @param user
	 * @return
	 */
	@ApiOperation(value = "${UserPassControllerV2.updatePassword.value}", notes = "${UserPassControllerV2.updatePassword.notes}")
	@PutMapping(value="/v2/auth/userpass/users",produces="application/json")
	public ResponseEntity<String> updatePassword(@RequestHeader(value="vault-token") String token,  @RequestBody UserpassUser user){
		return userpassService.updatePassword(token, user);
	}

	/**
	 * List users
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${UserPassControllerV2.listUsers.value}", notes = "${UserPassControllerV2.listUsers.notes}")
	@GetMapping(value="/v2/auth/userpass/users",produces="application/json")
	public ResponseEntity<String> listUsers(@RequestHeader(value="vault-token") String token){
		return userpassService.listUsers(token);
	}

	/**
	 * 
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${UserPassControllerV2.login.value}", notes = "${UserPassControllerV2.login.notes}")
	@PostMapping(value="/v2/auth/userpass/login")	
	public ResponseEntity<String> login(@RequestBody UserLogin user){
		return userpassService.login(user);
	}

}

