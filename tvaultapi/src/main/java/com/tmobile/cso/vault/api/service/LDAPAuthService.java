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

import com.tmobile.cso.vault.api.model.LDAPGroup;
import com.tmobile.cso.vault.api.model.LDAPUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class  LDAPAuthService {

	@Value("${vault.port}")
	private String vaultPort;

	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger log = LogManager.getLogger(LDAPAuthService.class);
	/**
	 * To authenticate with LDAP
	 * @param userLogin
	 * @return
	 */
	public ResponseEntity<String> authenticateLdap(UserLogin userLogin){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, userLogin.getUsername()).
				  put(LogMessage.ACTION, "LDAP Login").
			      put(LogMessage.MESSAGE, "Trying to authenticate").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		String jsonStr = JSONUtil.getJSON(userLogin);
		Response response = reqProcessor.process("/auth/ldap/login",jsonStr,"");
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, userLogin.getUsername()).
					  put(LogMessage.ACTION, "LDAP Login").
				      put(LogMessage.MESSAGE, "Authentication Successful").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			if (HttpStatus.BAD_REQUEST.equals(response.getHttpstatus())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, userLogin.getUsername()).
						  put(LogMessage.ACTION, "LDAP Login").
					      put(LogMessage.MESSAGE, "User Authentication failed. Invalid username or password.").
					      put(LogMessage.RESPONSE, response.getResponse()).
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\": [\"User Authentication failed\", \"Invalid username or password. Please retry again after correcting username or password.\"]}");
			}
			else if (HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, userLogin.getUsername()).
						  put(LogMessage.ACTION, "LDAP Login").
					      put(LogMessage.MESSAGE, "User Authentication failed. Vault Services could be down.").
					      put(LogMessage.RESPONSE, response.getResponse()).
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\": [\"User Authentication failed\", \"This may be due to vault services are down or vault services are not reachable\"]}");
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, userLogin.getUsername()).
					  put(LogMessage.ACTION, "LDAP Login").
				      put(LogMessage.MESSAGE, "User Authentication failed.").
				      put(LogMessage.RESPONSE, response.getResponse()).
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Username Authentication Failed.\"]}");
		}
	}
	/**
	 * To Configure LDAP Group
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	public ResponseEntity<String> configureLdapGroup(String token, LDAPGroup ldapGroup){
		String jsonStr = JSONUtil.getJSON(ldapGroup);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Create LDAP Group").
			      put(LogMessage.MESSAGE, String.format ("Trying to create LDAP group [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
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
				currentPolicies = ControllerUtil.getPoliciesAsStringFromJson(objMapper, responseJson);
			} catch (IOException e) {
				log.error(e);
			}
		}
		Response response = reqProcessor.process("/auth/ldap/groups/configure",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) { // Group is configured. So update SDB metadata as well.
			response = ControllerUtil.updateMetaDataOnConfigChanges(groupName, "groups", currentPolicies, latestPolicies, token);
			if(!HttpStatus.OK.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Create LDAP group").
					      put(LogMessage.MESSAGE, "Creation of LDAP group completed").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"LDAP group configured\",\""+response.getResponse()+"\"]}");
			}
		}else{
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Create LDAP group").
				      put(LogMessage.MESSAGE, "Creation of LDAP group completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Create LDAP group").
			      put(LogMessage.MESSAGE, "Creation of LDAP group completed").
			      put(LogMessage.RESPONSE, response.getResponse()).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP Group configured\"]}");
	}
	/**
	 * To get list of LDAP Groups
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listLdapGroups(String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "List LDAP Groups").
			      put(LogMessage.MESSAGE, "Trying to list LDAP groups ").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if(token == null || "".equals(token)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing token \"]}");
		}
		Response response = reqProcessor.process("/auth/ldap/groups/list","{}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "List LDAP Groups").
			      put(LogMessage.MESSAGE, "Listing of LDAP groups completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To get LDAP group details
	 * @param token
	 * @param groupname
	 * @return
	 */
	public ResponseEntity<String> fetchLdapGroup(String token, String groupname){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Fetch LDAP Groups").
			      put(LogMessage.MESSAGE, String.format("Trying to fetch LDAP groups for [%s]", groupname)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupname+"\"}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Fetch LDAP Groups").
			      put(LogMessage.MESSAGE, "Fetching of LDAP groups completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To delete LDAP group
	 * @param token
	 * @param groupname
	 * @return
	 */
	public ResponseEntity<String> deleteLdapGroup(String token, String groupname){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete LDAP Group").
			      put(LogMessage.MESSAGE, String.format("Trying to delete LDAP group [%s]", groupname)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/ldap/groups/delete","{\"groupname\":\""+groupname+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete LDAP Group").
				      put(LogMessage.MESSAGE, "Deleting of LDAP group successful").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP Group deleted\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete LDAP Group").
			      put(LogMessage.MESSAGE, "Deleting of LDAP group failed").
			      put(LogMessage.RESPONSE, response.getResponse()).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To Configure LDAP user
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	public ResponseEntity<String> configureLdapUser(String token, LDAPUser ldapUser){
		String jsonStr = JSONUtil.getJSON(ldapUser);
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
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Configure LDAP User").
			      put(LogMessage.MESSAGE, String.format ("Trying to configure LDAP user [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
		String responseJson="";	
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				currentPolicies = ControllerUtil.getPoliciesAsStringFromJson(objMapper, responseJson);
			} catch (IOException e) {
				log.error(e);
			}
		}
		Response response = reqProcessor.process("/auth/ldap/users/configure",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			response = ControllerUtil.updateMetaDataOnConfigChanges(userName, "users", currentPolicies, latestPolicies, token);
			if(!HttpStatus.OK.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Configure LDAP User").
					      put(LogMessage.MESSAGE, "Configuring of LDAP user successful").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"LDAP user configured\",\""+response.getResponse()+"\"]}");
			}
		}else{
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Configure LDAP User").
				      put(LogMessage.MESSAGE, "Configuring of LDAP user failed").
				      put(LogMessage.RESPONSE, response.getResponse()).
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Configure LDAP User").
			      put(LogMessage.MESSAGE, "Configuring of LDAP user completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP user configured\"]}");
	}
	/**
	 * To get list of LDAP users
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listLdapUsers(String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "List LDAP User").
			      put(LogMessage.MESSAGE,  "Trying to list LDAP user").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/ldap/users/list","{}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "List LDAP User").
			      put(LogMessage.MESSAGE, "Listing of LDAP user completed successfully").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To get LDAP user details
	 * @param token
	 * @param username
	 * @return
	 */
	public ResponseEntity<String> fetchLdapUser(String token, String username){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Fetch LDAP user").
			      put(LogMessage.MESSAGE, String.format("Trying to fetch LDAP user for [%s]", username)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/ldap/users","{\"username\":\""+username+"\"}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Fetch LDAP user").
			      put(LogMessage.MESSAGE, "Listing of LDAP user completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To delete LDAP User
	 * @param token
	 * @param username
	 * @return
	 */
	public ResponseEntity<String> deleteLdapUser(String token, String username){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete LDAP user").
			      put(LogMessage.MESSAGE, String.format("Trying to delete LDAP user for [%s]", username)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/ldap/users/delete","{\"username\":\""+username+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete LDAP user").
				      put(LogMessage.MESSAGE, "Deletion of LDAP user completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP User deleted\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete LDAP user").
			      put(LogMessage.MESSAGE, "Deletion of LDAP user completed").
			      put(LogMessage.RESPONSE, response.getResponse()).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
}
