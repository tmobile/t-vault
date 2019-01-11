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

import com.tmobile.cso.vault.api.model.LDAPGroup;
import com.tmobile.cso.vault.api.model.LDAPUser;
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
import com.tmobile.cso.vault.api.service.LDAPAuthService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@RestController
@CrossOrigin
@Api(description = "Manage LDAP Authentication", position = 14)
public class LDAPAuthControllerV2 {
	@Autowired
	private LDAPAuthService ldapAuthService;
	/**
	 * Method to authenticate against LDAP
	 * @param userLogin
	 * @return
	 * 
	 * Sample output
	 * 		{"client_token":"beae9c3d-c466-822b-e9c5-e490de1975c0"}
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.authenticateLdap.value}", notes = "${LDAPAuthControllerV2.authenticateLdap.notes}")
	@PostMapping(value="/v2/auth/ldap/login",consumes="application/json",produces="application/json")
	public ResponseEntity<String> authenticateLdap( @RequestBody UserLogin userLogin){
		return ldapAuthService.authenticateLdap(userLogin);
	}
	/***
	 * Method to configure a LDAP group in vault.
	 * 
	 * @param token
	 * @param JSON Groupname and polices associated
	 * @return Httpstatus 200 if group is successfully configured
	 * 
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.configureLdapGroup.value}", notes = "${LDAPAuthControllerV2.configureLdapGroup.notes}")
	@PutMapping(value="/v2/auth/ldap/groups",consumes="application/json",produces="application/json")
	public ResponseEntity<String> configureLdapGroup(@RequestHeader(value="vault-token") String token, @RequestBody LDAPGroup ldapGroup){
		return ldapAuthService.configureLdapGroup(token, ldapGroup);
	}
	/**
	 * The method to return all existing LDAP Groups configured in Vault
	 * @param token
	 * @return Ldap Groupnames configured in vault
	 * 
	 * Sample output
	 * 		{ "keys": ["ldapgroup1","ldapgroup2"] }
	 *
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.listLdapGroups.value}", notes = "${LDAPAuthControllerV2.listLdapGroups.notes}")
	@GetMapping(value="/v2/auth/ldap/groups",produces="application/json")
	public ResponseEntity<String> listLdapGroups(@RequestHeader(value="vault-token",required=false) String token){
		return ldapAuthService.listLdapGroups(token);
	}
	/**
	 * Method to retrieve 
	 * 
	 * @param token
	 * @param groupname
	 * @return  policies associated with the group, HttpStatus 200 
	 * 
	 * Sample output
	 * 	 {"data":{"policies":"c,d,default"}}
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.fetchLdapGroup.value}", notes = "${LDAPAuthControllerV2.fetchLdapGroup.notes}")
	@GetMapping(value="/v2/auth/ldap/groups/{groupname}",produces="application/json")
	public ResponseEntity<String> fetchLdapGroup(@RequestHeader(value="vault-token") String token,@PathVariable("groupname" ) String groupname){
		return ldapAuthService.fetchLdapGroup(token, groupname);
	}
	/**
	 * 
	 * @param token
	 * @param groupname
	 * @return
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.deleteLdapGroup.value}", notes = "${LDAPAuthControllerV2.deleteLdapGroup.notes}")
	@DeleteMapping(value="/v2/auth/ldap/groups/{groupname}",produces="application/json")
	public ResponseEntity<String> deleteLdapGroup(@RequestHeader(value="vault-token") String token,@PathVariable("groupname" ) String groupname){
		return ldapAuthService.deleteLdapGroup(token, groupname);	
	}
	/**
	 * Method to configure a LDAP user
	 * @param token   : Vault token
	 * @param 
	 * @return : Httpstatus 200 if user is configured successfully
	 * 
	 * Sample output 
	 * 		{ "Messages": ["LDAP user configured"]  }
	 * 
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.configureLdapUser.value}", notes = "${LDAPAuthControllerV2.configureLdapUser.notes}")
	@PutMapping(value="/v2/auth/ldap/users",consumes="application/json",produces="application/json")
	public ResponseEntity<String> configureLdapUser(@RequestHeader(value="vault-token") String token, @RequestBody LDAPUser ldapUser){
		return ldapAuthService.configureLdapUser(token, ldapUser);
	}
	/**
	 * Method to list all configured LDAP Users
	 * @param token
	 * @return : Httpstatus 200 and an list of configured users
	 * 
	 * Sample output
	 * 		{"keys":["userid1","userid2",....]}
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.listLdapUsers.value}", notes = "${LDAPAuthControllerV2.listLdapUsers.notes}")
	@GetMapping(value="/v2/auth/ldap/users",produces="application/json")
	public ResponseEntity<String> listLdapUsers(@RequestHeader(value="vault-token") String token){
		return ldapAuthService.listLdapUsers(token);
	}
	/***
	 * Method to fetch the details of LDAP User configured in vault.
	 * @param token
	 * @param username
	 * @return
	 * 
	 * Sample output
	 * 	{"data":
	 * 		{"groups":"group1,group2","policies":"policy1,policy2"}
	 *  }
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.fetchLdapUser.value}", notes = "${LDAPAuthControllerV2.fetchLdapUser.notes}")
	@GetMapping(value="/v2/auth/ldap/users/{username}",produces="application/json")
	public ResponseEntity<String> fetchLdapUser(@RequestHeader(value="vault-token") String token,@PathVariable("username" ) String username){
		return ldapAuthService.fetchLdapUser(token, username);
	}
	/***
	 * Method to delete a LDAP User
	 * @param token
	 * @param username
	 * @return Httpstatus 200 if user successfully deleted.
	 * 
	 */
	@ApiOperation(value = "${LDAPAuthControllerV2.deleteLdapUser.value}", notes = "${LDAPAuthControllerV2.deleteLdapUser.notes}")
	@DeleteMapping(value="/v2/auth/ldap/users/{username}",produces="application/json")
	public ResponseEntity<String> deleteLdapUser(@RequestHeader(value="vault-token") String token,@PathVariable("username" ) String username){
		return ldapAuthService.deleteLdapUser(token, username);
	}
}
