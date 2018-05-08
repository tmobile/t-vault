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

package com.tmobile.cso.vault.api.controller;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

import io.swagger.annotations.Api;


@RestController
@RequestMapping(value="/auth/ldap")
@CrossOrigin
@Api(description = "Manage LDAP Authentication", position = 4)
public class LDAPAuthController {
	
	private Logger log = LogManager.getLogger(LDAPAuthController.class);
	@Autowired
	private RequestProcessor reqProcessor;  
	
	

	/***
	 * Method to authenticate against LDAP
	 * 
	 * @param jsonStr : username and password
	 * @return client token if authenticated
	 * 
	 * Sample output
	 * 		{"client_token":"beae9c3d-c466-822b-e9c5-e490de1975c0"}
	 */

	@PostMapping(value="/login",consumes="application/json",produces="application/json")
	public ResponseEntity<String> authenticateLdap( @RequestBody String jsonStr){
		
		Response response = reqProcessor.process("/auth/ldap/login",jsonStr,"");
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			if (HttpStatus.BAD_REQUEST.equals(response.getHttpstatus())) {
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\": [\"User Authentication failed\", \"Invalid username and/or password combination. Please retry again after correcting username/password.\"]}");
			}
			else if (HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\": [\"User Authentication failed\", \"This may be due to vault services are down or vault services are not reachable\"]}");
			}
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Username Authentication Failed.\"]}");
				}
		
	}
	
	/***
	 * Method to configure a LDAP group in vault.
	 * 
	 * @param token
	 * @param jsonStr : Groupname and polices associated
	 * @return Httpstatus 200 if group is successfully configured
	 * 
	 */
	
	@PostMapping(value="/groups/configure",consumes="application/json",produces="application/json")
	public ResponseEntity<String> configureLdapGroup(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
	
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String groupName = "" ;
		
		try {
			JsonNode root = objMapper.readTree(jsonStr);
			groupName = root.get("groupname").asText();
			latestPolicies = root.get("policies").asText();
		} catch (IOException e) {
			log.error(e);
		}
		
		// Fetch current policies associated with the group.
		
		Response grpResponse = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
		String responseJson="";	
		if(HttpStatus.OK.equals(grpResponse.getHttpstatus())){
			responseJson = grpResponse.getResponse();	
			try {
				currentPolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
			} catch (IOException e) {
				log.error(e);
			}
		}
		
		
		Response response = reqProcessor.process("/auth/ldap/groups/configure",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) { // Group is configured. So update SDB metadata as well.
			response = ControllerUtil.updateMetaDataOnConfigChanges(groupName, "groups", currentPolicies, latestPolicies, token);
			if(!HttpStatus.OK.equals(response.getHttpstatus()))
				return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"LDAP group configured\",\""+response.getResponse()+"\"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP Group configured\"]}");
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
	
	@GetMapping(value="/groups",produces="application/json")
	public ResponseEntity<String> listLdapGroups(@RequestHeader(value="vault-token",required=false) String token){
		
		if(token == null || "".equals(token)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing token \"]}");
		}
		Response response = reqProcessor.process("/auth/ldap/groups/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
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
	
	@GetMapping(value="/groups/{groupname}",produces="application/json")
	public ResponseEntity<String> fetchLdapGroup(@RequestHeader(value="vault-token") String token,@PathVariable("groupname" ) String groupname){
		
		Response response = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupname+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	@DeleteMapping(value="/groups/delete/{groupname}",produces="application/json")
	public ResponseEntity<String> deleteLdapGroup(@RequestHeader(value="vault-token") String token,@PathVariable("groupname" ) String groupname){
		
		Response response = reqProcessor.process("/auth/ldap/groups/delete","{\"groupname\":\""+groupname+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP Group deleted\"]}");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	/**
	 * Method to configure a LDAP user
	 * @param token   : Vault token
	 * @param jsonStr : Configuration parameters 
	 * @return : Httpstatus 200 if user is configured successfully
	 * 
	 * Sample output 
	 * 		{ "Messages": ["LDAP user configured"]  }
	 * 
	 */
	
	@PostMapping(value="/users/configure",consumes="application/json",produces="application/json")
	public ResponseEntity<String> configureLdapUser(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String userName = "" ;
		
		try {
			JsonNode root = objMapper.readTree(jsonStr);
			userName = root.get("username").asText();
			latestPolicies = root.get("policies").asText();
		} catch (IOException e) {
			log.error(e);
		}
		
		
		Response userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
		String responseJson="";	
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				currentPolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
			} catch (IOException e) {
				log.error(e);
			}
		}
	
		Response response = reqProcessor.process("/auth/ldap/users/configure",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			response = ControllerUtil.updateMetaDataOnConfigChanges(userName, "users", currentPolicies, latestPolicies, token);
			if(!HttpStatus.OK.equals(response.getHttpstatus()))
				return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"LDAP user configured\",\""+response.getResponse()+"\"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP user configured\"]}");
	}
	
	/**
	 * Method to list all configured LDAP Users
	 * @param token
	 * @return : Httpstatus 200 and an list of configured users
	 * 
	 * Sample output
	 * 		{"keys":["userid1","userid2",....]}
	 */
	
	@GetMapping(value="/users",produces="application/json")
	public ResponseEntity<String> listLdapUsers(@RequestHeader(value="vault-token") String token){
		
		Response response = reqProcessor.process("/auth/ldap/users/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
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
	@GetMapping(value="/users/{username}",produces="application/json")
	public ResponseEntity<String> fetchLdapUser(@RequestHeader(value="vault-token") String token,@PathVariable("username" ) String username){
		
		Response response = reqProcessor.process("/auth/ldap/users","{\"username\":\""+username+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	/***
	 * Method to delete a LDAP User
	 * @param token
	 * @param username
	 * @return Httpstatus 200 if user successfully deleted.
	 * 
	 */
	
	@DeleteMapping(value="/users/delete/{username}",produces="application/json")
	public ResponseEntity<String> deleteLdapUser(@RequestHeader(value="vault-token") String token,@PathVariable("username" ) String username){
				
		Response response = reqProcessor.process("/auth/ldap/users/delete","{\"username\":\""+username+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP User deleted\"]}");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
}
