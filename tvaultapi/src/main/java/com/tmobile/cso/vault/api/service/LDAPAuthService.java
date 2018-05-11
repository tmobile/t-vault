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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;

@Component
public class  LDAPAuthService {

	@Value("${vault.port}")
	private String vaultPort;

	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger logger = LogManager.getLogger(LDAPAuthService.class);
	/**
	 * To authenticate with LDAP
	 * @param userLogin
	 * @return
	 */
	public ResponseEntity<String> authenticateLdap(UserLogin userLogin){
		String jsonStr = JSONUtil.getJSON(userLogin);
		Response response = reqProcessor.process("/auth/ldap/login",jsonStr,"");
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			if (HttpStatus.BAD_REQUEST.equals(response.getHttpstatus())) {
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\": [\"User Authentication failed\", \"Invalid username or password. Please retry again after correcting username or password.\"]}");
			}
			else if (HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\": [\"User Authentication failed\", \"This may be due to vault services are down or vault services are not reachable\"]}");
			}
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Username Authentication Failed.\"]}");
		}
	}
	/**
	 * To Configure LDAP Group
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	public ResponseEntity<String> configureLdapGroup(String token, String jsonStr){
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String groupName = "" ;
		try {
			JsonNode root = objMapper.readTree(jsonStr);
			groupName = root.get("groupname").asText();
			latestPolicies = root.get("policies").asText();
		} catch (IOException e) {
			logger.error(e);
		}
		// Fetch current policies associated with the group.
		Response grpResponse = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
		String responseJson="";	
		if(HttpStatus.OK.equals(grpResponse.getHttpstatus())){
			responseJson = grpResponse.getResponse();	
			try {
				currentPolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
			} catch (IOException e) {
				logger.error(e);
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
	 * To get list of LDAP Groups
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listLdapGroups(String token) {
		if(token == null || "".equals(token)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing token \"]}");
		}
		Response response = reqProcessor.process("/auth/ldap/groups/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To get LDAP group details
	 * @param token
	 * @param groupname
	 * @return
	 */
	public ResponseEntity<String> fetchLdapGroup(String token, String groupname){
		Response response = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupname+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To delete LDAP group
	 * @param token
	 * @param groupname
	 * @return
	 */
	public ResponseEntity<String> deleteLdapGroup(String token, String groupname){
		Response response = reqProcessor.process("/auth/ldap/groups/delete","{\"groupname\":\""+groupname+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP Group deleted\"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To Configure LDAP user
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	public ResponseEntity<String> configureLdapUser(String token,  String jsonStr){
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String userName = "" ;
		try {
			JsonNode root = objMapper.readTree(jsonStr);
			userName = root.get("username").asText();
			latestPolicies = root.get("policies").asText();
		} catch (IOException e) {
			logger.error(e);
		}
		Response userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
		String responseJson="";	
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				currentPolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
			} catch (IOException e) {
				logger.error(e);
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
	 * To get list of LDAP users
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listLdapUsers(String token){
		Response response = reqProcessor.process("/auth/ldap/users/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To get LDAP user details
	 * @param token
	 * @param username
	 * @return
	 */
	public ResponseEntity<String> fetchLdapUser(String token, String username){
		Response response = reqProcessor.process("/auth/ldap/users","{\"username\":\""+username+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To delete LDAP User
	 * @param token
	 * @param username
	 * @return
	 */
	public ResponseEntity<String> deleteLdapUser(String token, String username){
		Response response = reqProcessor.process("/auth/ldap/users/delete","{\"username\":\""+username+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP User deleted\"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
}
