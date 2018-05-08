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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

import io.swagger.annotations.Api;

@RestController
@CrossOrigin
@Api(description = "Manage Safes/SDBs", position = 6)
public class SDBController {
	private Logger log = LogManager.getLogger(LDAPAuthController.class);

	@Value("${vault.auth.method}")
        private String vaultAuthMethod;

	@Autowired
	private RequestProcessor reqProcessor;
	
	@GetMapping(value="/sdb/list",produces="application/json")
	public ResponseEntity<String> getFolders(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		String _path = "";
		if( "apps".equals(path)||"shared".equals(path)||"users".equals(path)){
			_path = "metadata/"+path;
		}else{
			 _path = path;
		}
		
		Response response = reqProcessor.process("/sdb/list","{\"path\":\""+_path+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		
	}
	
	@GetMapping(value="/sdb",produces="application/json")
	public ResponseEntity<String> getInfo(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		
		String _path = "metadata/"+path;
		Response response = reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		
	}
	
	@PostMapping(value="/sdb/createfolder",produces="application/json")
	public ResponseEntity<String> createfolder(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		
		if(ControllerUtil.isValidDataPath(path)){
			//if(ControllerUtil.isValidSafe(path, token)){
				String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
				Response response = reqProcessor.process("/sdb/create",jsonStr,token);
				if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Folder created \"]}");
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			//}else{
			//	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid safe\"]}");
			//}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
		
	}
	
	@PostMapping(value="/sdb/update",consumes="application/json",produces="application/json")
	public ResponseEntity<String> updateSDB(@RequestHeader(value="vault-token" ) String token, @RequestBody String jsonStr){
		
		Map<String, Object> requestParams = ControllerUtil.parseJson(jsonStr);
		if (!ControllerUtil.areSDBInputsValid(requestParams)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		@SuppressWarnings("unchecked")
		Map<Object,Object> data = (Map<Object,Object>)requestParams.get("data");
		String path = requestParams.get("path").toString();
		String _path = "metadata/"+path;
		if(ControllerUtil.isValidSafePath(path)){
			// Get SDB metadataInfo
			Response response = reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token);
			Map<String, Object> responseMap = null;
			if(HttpStatus.OK.equals(response.getHttpstatus())){
				responseMap = ControllerUtil.parseJson(response.getResponse());
				if(responseMap.isEmpty())
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info \"]}");
			}else{
				log.error("Could not fetch the safe information. Possible path issue");
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified \"]}");
			}
			
			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			Object awsroles = metadataMap.get("aws-roles");
			Object groups = metadataMap.get("groups");
			Object users = metadataMap.get("users");
			data.put("aws-roles",awsroles);
			data.put("groups",groups);
			data.put("users",users);
			requestParams.put("path",_path);
			String metadataJson = ControllerUtil.convetToJson(requestParams) ;
			response = reqProcessor.process("/sdb/update",metadataJson,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB updated \"]}");
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	@PostMapping(value="/sdb/create",consumes="application/json",produces="application/json")
	public ResponseEntity<String> createSDB(@RequestHeader(value="vault-token" ) String token, @RequestBody String jsonStr){
		
		Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		if (!ControllerUtil.areSDBInputsValid(rqstParams)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		String path = rqstParams.get("path").toString();
		if(ControllerUtil.isValidSafePath(path)){
			Response response = reqProcessor.process("/sdb/create",jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				/*
				 * Store the metadata. Create policies if folders are created under the mount points
				 * 
				 */
				String _path = "metadata/"+path;
				rqstParams.put("path",_path);
				
				String metadataJson = 	ControllerUtil.convetToJson(rqstParams);
				response = reqProcessor.process("/write",metadataJson,token);
				
				boolean isMetaDataUpdated = false;
				
				if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					isMetaDataUpdated = true;
				}
				
				String folders[] = path.split("[/]+");
				if(folders.length==2){
					String sdb = folders[1];
					Map<String,Object> policyMap = new HashMap<String,Object>();
					Map<String,String> accessMap = new HashMap<String,String>();
					accessMap.put(path+"/*","read");
					
					policyMap.put("accessid", "r_"+folders[0]+"_"+sdb);
					policyMap.put("access", accessMap);
					
					String policyRequestJson = 	ControllerUtil.convetToJson(policyMap);
					
					Response r_response = reqProcessor.process("/access/update",policyRequestJson,token);
					//Write Policy
					accessMap.put(path+"/*","write");
					policyMap.put("accessid", "w_"+folders[0]+"_"+sdb);
					
					policyRequestJson = 	ControllerUtil.convetToJson(policyMap);
					Response w_response = reqProcessor.process("/access/update",policyRequestJson,token); 
					//deny Policy
					accessMap.put(path+"/*","deny");
					policyMap.put("accessid", "d_"+folders[0]+"_"+sdb);
					
					policyRequestJson = 	ControllerUtil.convetToJson(policyMap);
					Response d_response = reqProcessor.process("/access/update",policyRequestJson,token); 
					
					
					if(r_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) && 
							w_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
									d_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) ){
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB and associated read/write/deny policies created \"]}");
					}else{
						return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"SDB created however one ore more policy (read/write/deny) creation failed \"]}");
					}
				}
				if(isMetaDataUpdated)
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB created \"]}");
				else
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB created however metadata update failed. Please try with sdb/update \"]}");
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	@SuppressWarnings("unchecked")
	@DeleteMapping(value="/sdb/delete",produces="application/json")
	public ResponseEntity<String> deletesdb(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			Response response = new Response(); 
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
	
				String folders[] = path.split("[/]+");
				String r_policy = "r_"+folders[0]+"_"+folders[1];
				String w_policy = "w_"+folders[0]+"_"+folders[1];
				String d_policy = "d_"+folders[0]+"_"+folders[1];
				
				reqProcessor.process("/access/delete","{\"accessid\":\""+r_policy+"\"}",token);
				reqProcessor.process("/access/delete","{\"accessid\":\""+w_policy+"\"}",token);
				reqProcessor.process("/access/delete","{\"accessid\":\""+d_policy+"\"}",token);
							
				String _path = "metadata/"+path;
		
				// Get SDB metadataInfo
				response = reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
				Map<String, Object> responseMap = null;
				try {
					responseMap = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
				} catch (IOException e) {
					log.error(e);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info \"]}");
				}
				if(responseMap!=null && responseMap.get("data")!=null){
					Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
					Map<String,String> awsroles = (Map<String, String>)metadataMap.get("aws-roles");
					Map<String,String> groups = (Map<String, String>)metadataMap.get("groups");
					Map<String,String> users = (Map<String, String>) metadataMap.get("users");
					ControllerUtil.updateUserPolicyAssociationOnSDBDelete(path,users,token);
					ControllerUtil.updateGroupPolicyAssociationOnSDBDelete(path,groups,token);
					ControllerUtil.deleteAwsRoleOnSDBDelete(path,awsroles,token);
				}	
				ControllerUtil.recursivedeletesdb("{\"path\":\""+_path+"\"}",token,response);
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
				
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else if(ControllerUtil.isValidDataPath(path)){
			Response response = new Response(); 
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Folder deleted\"]}");
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
			
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	@SuppressWarnings("unchecked")
	@DeleteMapping(value="/v2/sdb/delete",produces="application/json")
	public ResponseEntity<String> deleteFolder(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		
		if(ControllerUtil.isPathValid(path) ){
			Response response = new Response(); 
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				String folders[] = path.split("[/]+");
				String r_policy = "r_"+folders[0]+"_"+folders[1];
				String w_policy = "w_"+folders[0]+"_"+folders[1];
				String d_policy = "d_"+folders[0]+"_"+folders[1];
				
				reqProcessor.process("/access/delete","{\"accessid\":\""+r_policy+"\"}",token);
				reqProcessor.process("/access/delete","{\"accessid\":\""+w_policy+"\"}",token);
				reqProcessor.process("/access/delete","{\"accessid\":\""+d_policy+"\"}",token);
							
				String _path = "metadata/"+path;
		
				// Get SDB metadataInfo
				response = reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
				Map<String, Object> responseMap = null;
				try {
					responseMap = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
				} catch (IOException e) {
					log.error(e);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info \"]}");
				}
				if(responseMap!=null && responseMap.get("data")!=null){
					Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
					Map<String,String> awsroles = (Map<String, String>)metadataMap.get("aws-roles");
					Map<String,String> groups = (Map<String, String>)metadataMap.get("groups");
					Map<String,String> users = (Map<String, String>) metadataMap.get("users");
					ControllerUtil.updateUserPolicyAssociationOnSDBDelete(path,users,token);
					ControllerUtil.updateGroupPolicyAssociationOnSDBDelete(path,groups,token);
					ControllerUtil.deleteAwsRoleOnSDBDelete(path,awsroles,token);
				}	
				ControllerUtil.recursivedeletesdb("{\"path\":\""+_path+"\"}",token,response);
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
				
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else if(ControllerUtil.isValidDataPath(path)){
			Response response = new Response(); 
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Folder deleted\"]}");
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
			
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}


	@PostMapping(value="/sdb/adduser",consumes="application/json",produces="application/json")
	public ResponseEntity<String> addUsertoSDB(@RequestHeader(value="vault-token") String token, @RequestBody String jsonstr){
		
		Map<String,Object> requestMap = ControllerUtil.parseJson(jsonstr);
		String userName = requestMap.get("username").toString();
		String path = requestMap.get("path").toString();
		String access = requestMap.get("access").toString();
		
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
		
			String folders[] = path.split("[/]+");
			
			String policyPrefix ="";
			switch (access){
				case "read": policyPrefix = "r_"; break ; 
				case "write": policyPrefix = "w_" ;break; 
				case "deny": policyPrefix = "d_" ;break; 
			}
			if("".equals(policyPrefix)){
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}
			
			String policy = policyPrefix+folders[0]+"_"+folders[1];
			String r_policy = "r_"+folders[0]+"_"+folders[1];
			String w_policy = "w_"+folders[0]+"_"+folders[1];
			String d_policy = "d_"+folders[0]+"_"+folders[1];
			Response userResponse;
			if ("userpass".equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token);	
			}
			else {
				userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
			}
			String responseJson="";
			
			
			String policies ="";
			String groups="";
			String currentpolicies ="";
			
			if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
				responseJson = userResponse.getResponse();	
				try {
					ObjectMapper objMapper = new ObjectMapper();
					currentpolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
					if (!("userpass".equals(vaultAuthMethod))) {
						groups =objMapper.readTree(responseJson).get("data").get("groups").asText();
					}
				} catch (IOException e) {
					log.error(e);
				}
				policies = currentpolicies;
				policies = policies.replaceAll(r_policy, "");
				policies = policies.replaceAll(w_policy, "");
				policies = policies.replaceAll(d_policy, "");
				policies = policies+","+policy;
			}else{
				// New user to be configured
				policies = policy;
			}
			
			Response ldapConfigresponse;
			if ("userpass".equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,policies,token);
			}
			else {
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,policies,groups,token);
			}

			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "users");
				params.put("name",userName);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");		
				}else{
					log.debug("Meta data update failed");
					log.debug(metadataResponse.getResponse());
					if ("userpass".equals(vaultAuthMethod)) {
						ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,currentpolicies,token);
					}
					else {
						ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,currentpolicies,groups,token);
					}
					if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
						log.debug("Reverting user policy uupdate");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");
					}else{
						log.debug("Reverting user policy update failed");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Contact Admin \"]}");
					}
				}		
			}else{
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");
			}	
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	/**
	 * 
	 * @param token
	 * @param jsonstr
	 * @return
	 */
	@PostMapping(value="/sdb/approle",consumes="application/json",produces="application/json")
	public ResponseEntity<String>associateApproletoSDB(@RequestHeader(value="vault-token") String token, @RequestBody String jsonstr){
		
		log.info("Associate approle to SDB -  JSON :" + jsonstr );
		
		Map<String,Object> requestMap = ControllerUtil.parseJson(jsonstr);
		
		String approle = requestMap.get("role_name").toString();
		String path = requestMap.get("path").toString();
		String access = requestMap.get("access").toString();
		
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){

			log.info("Associate approle to SDB -  path :" + path + "valid" );

			String folders[] = path.split("[/]+");
			
			String policy ="";
			
			switch (access){
				case "read": policy = "r_" + folders[0] + "_" + folders[1] ; break ; 
				case "write": policy = "w_"  + folders[0] + "_" + folders[1] ;break; 
				case "deny": policy = "d_"  + folders[0] + "_" + folders[1] ;break; 
			}
			
			if("".equals(policy)){
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}
			

			log.info("Associate approle to SDB -  policy :" + policy + " is being configured" );
			
			//Call controller to update the policy for approle
			Response approleControllerResp = ControllerUtil.configureApprole(approle,policy,token);

		
			if(HttpStatus.OK.equals(approleControllerResp.getHttpstatus()) || (HttpStatus.NO_CONTENT.equals(approleControllerResp.getHttpstatus()))) {
					
				log.info("Associate approle to SDB -  policy :" + policy + " is associated" );

				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :" + approle + " is successfully associated with SDB\"]}");		
			
		}else {

				log.error( "Associate Approle" +approle + "to sdb FAILED");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle :" + approle + " failed to be associated with SDB\"]}");		
			}
		} else {
			
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle :" + approle + " failed to be associated with SDB.. Invalid Path specified\"]}");		
		
		}

	}
	
	
	
	@PostMapping(value="/sdb/deleteuser",consumes="application/json",produces="application/json")
	public ResponseEntity<String> deleteUserSDB(@RequestHeader(value="vault-token") String token, @RequestBody String jsonstr){
		
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
		}
		
		String userName = requestMap.get("username");
		String path = requestMap.get("path");
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			String folders[] = path.split("[/]+");
			
			String r_policy = "r_"+folders[0]+"_"+folders[1];
			String w_policy = "w_"+folders[0]+"_"+folders[1];
			String d_policy = "d_"+folders[0]+"_"+folders[1];
			Response userResponse;
			if ("userpass".equals(vaultAuthMethod)) {	
				userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token);
			}
			else {
				userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
			}
			String responseJson="";
			String policies ="";
			String groups="";
			String currentpolicies ="";
			
			if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
				responseJson = userResponse.getResponse();	
				try {
					currentpolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
					if (!("userpass".equals(vaultAuthMethod))) {
						groups =objMapper.readTree(responseJson).get("data").get("groups").asText();
					}
				} catch (IOException e) {
					log.error(e);
				}
				policies = currentpolicies;
				policies = policies.replaceAll(r_policy, "");
				policies = policies.replaceAll(w_policy, "");
				policies = policies.replaceAll(d_policy, "");
				Response ldapConfigresponse;
				if ("userpass".equals(vaultAuthMethod)) {
					ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,policies,token);
				}
				else {
					ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,policies,groups,token);
				}
				if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					Map<String,String> params = new HashMap<String,String>();
					params.put("type", "users");
					params.put("name",userName);
					params.put("path",path);
					params.put("access","delete");
					Response metadataResponse = ControllerUtil.updateMetadata(params,token);
					if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
						return ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");		
					}else{
						log.debug("Meta data update failed");
						log.debug(metadataResponse.getResponse());
						if ("userpass".equals(vaultAuthMethod)) {
							ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,currentpolicies,token);
						}
						else {
							ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,currentpolicies,groups,token);
						}
						if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
							log.debug("Reverting user policy uupdate");
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");
						}else{
							log.debug("Reverting user policy update failed");
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Contact Admin \"]}");
						}
					}		
				}else{
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");
				}	
			}else{
				// Trying to remove the orphan entries if exists
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "users");
				params.put("name",userName);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					return ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");		
				}else{
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try again \"]}");
				}
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	@PostMapping(value="/sdb/deletegroup",consumes="application/json",produces="application/json")
	public ResponseEntity<String> deleteGroupSDB(@RequestHeader(value="vault-token") String token, @RequestBody String jsonstr){
		if ("userpass".equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":\"This operation is not supported for Userpass authentication. \"}");
		}	
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
		}
		
		String groupName = requestMap.get("groupname");
		String path = requestMap.get("path");
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			String folders[] = path.split("[/]+");
			
			String r_policy = "r_"+folders[0]+"_"+folders[1];
			String w_policy = "w_"+folders[0]+"_"+folders[1];
			String d_policy = "d_"+folders[0]+"_"+folders[1];
			Response userResponse = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
			String responseJson="";
			String policies ="";
			String currentpolicies ="";
			
			if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
				responseJson = userResponse.getResponse();	
				try {
					currentpolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
				} catch (IOException e) {
					log.error(e);
				}
				policies = currentpolicies;
				policies = policies.replaceAll(r_policy, "");
				policies = policies.replaceAll(w_policy, "");
				policies = policies.replaceAll(d_policy, "");
				Response ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,policies,token);
				if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
					Map<String,String> params = new HashMap<String,String>();
					params.put("type", "groups");
					params.put("name",groupName);
					params.put("path",path);
					params.put("access","delete");
					Response metadataResponse = ControllerUtil.updateMetadata(params,token);
					if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");		
					}else{
						log.debug("Meta data update failed");
						log.debug(metadataResponse.getResponse());
						ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,currentpolicies,token);
						if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
							log.debug("Reverting user policy update");
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Please try again\"]}");
						}else{
							log.debug("Reverting Group policy update failed");
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Contact Admin \"]}");
						}
					}		
				}else{
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");
				}	
			}else{
				// Trying to remove the orphan entries if exists
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "users");
				params.put("name",groupName);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					return ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"Group association is removed \"}");		
				}else{
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Try again \"]}");
				}
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	@PostMapping(value="/sdb/addrole",consumes="application/json",produces="application/json")
	public ResponseEntity<String> addAwsRoletoSDB(@RequestHeader(value="vault-token") String token, @RequestBody String jsonstr){
		
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
		}
		
		String role = requestMap.get("role");
		String path = requestMap.get("path");
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			String access = requestMap.get("access");
			String folders[] = path.split("[/]+");
			
			String policyPrefix ="";
			switch (access){
				case "read": policyPrefix = "r_"; break ; 
				case "write": policyPrefix = "w_" ;break; 
				case "deny": policyPrefix = "d_" ;break; 
			}
			if("".equals(policyPrefix)){
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}
			String policy = policyPrefix+folders[0]+"_"+folders[1];
			String r_policy = "r_"+folders[0]+"_"+folders[1];
			String w_policy = "w_"+folders[0]+"_"+folders[1];
			String d_policy = "d_"+folders[0]+"_"+folders[1];
			Response roleResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+role+"\"}",token);
			String responseJson="";
		
			String policies ="";
			String currentpolicies ="";
			
			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
					responseJson = roleResponse.getResponse();	
					try {
						JsonNode policiesArry =objMapper.readTree(responseJson).get("policies");
						for(JsonNode policyNode : policiesArry){
							currentpolicies =	(currentpolicies == "" ) ? currentpolicies+policyNode.asText():currentpolicies+","+policyNode.asText();
						}
					} catch (IOException e) {
						log.error(e);
					}
					policies = currentpolicies;
					policies = policies.replaceAll(r_policy, "");
					policies = policies.replaceAll(w_policy, "");
					policies = policies.replaceAll(d_policy, "");
					policies = policies+","+policy;
			}else{
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Non existing role name. Please configure it as first step\"]}");
			}
			
			Response ldapConfigresponse = ControllerUtil.configureAWSRole(role,policies,token);
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "aws-roles");
				params.put("name",role);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");		
				}else{
					System.out.println("Meta data update failed");
					System.out.println(metadataResponse.getResponse());
					ldapConfigresponse = ControllerUtil.configureAWSRole(role,policies,token);
					if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
						System.out.println("Reverting user policy uupdate");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
					}else{
						System.out.println("Reverting user policy update failed");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Contact Admin \"]}");
					}
				}		
			}else{
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Try Again\"]}");
			}	
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	/* Delete the role when adb association is removed as role can't exist its own as of now. Will revisit later

	@PostMapping(value="/sdb/deleterole",consumes="application/json",produces="application/json")
	public ResponseEntity<String> deleteRoleSDB(@RequestHeader(value="vault-token") String token, @RequestBody String jsonstr){
		
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request. please check the request json\"]}");
		}
		
		String role = requestMap.get("role");
		String path = requestMap.get("path");
		String folders[] = path.split("[/]+");
		
		String r_policy = "r_"+folders[0]+"_"+folders[1];
		String w_policy = "w_"+folders[0]+"_"+folders[1];
		String d_policy = "d_"+folders[0]+"_"+folders[1];
		Response userResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+role+"\"}",token);
		String responseJson="";
		String policies ="";
		String currentpolicies ="";
		
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				JsonNode policiesArry =objMapper.readTree(responseJson).get("policies");
				for(JsonNode policyNode : policiesArry){
					currentpolicies =	(currentpolicies == "" ) ? currentpolicies+policyNode.asText():currentpolicies+","+policyNode.asText();
				}
			} catch (IOException e) {
				log.error(e);
			}
			policies = currentpolicies;
			policies = policies.replaceAll(r_policy, "");
			policies = policies.replaceAll(w_policy, "");
			policies = policies.replaceAll(d_policy, "");
			Response ldapConfigresponse = ControllerUtil.configureAWSRole(role,policies,token);
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "aws-roles");
				params.put("name",role);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");		
				}else{
					System.out.println("Meta data update failed");
					System.out.println(metadataResponse.getResponse());
					ldapConfigresponse = ControllerUtil.configureAWSRole(role,currentpolicies,token);
					if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
						System.out.println("Reverting user policy update");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
					}else{
						System.out.println("Reverting Group policy update failed");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Contact Admin \"]}");
					}
				}		
			}else{
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"User configuration failed.Try Again\"]}");
			}	
		}else{
			// Trying to remove the orphan entries if exists
			Map<String,String> params = new HashMap<String,String>();
			params.put("type", "aws-roles");
			params.put("name",role);
			params.put("path",path);
			params.put("access","delete");
			Response metadataResponse = ControllerUtil.updateMetadata(params,token);
			if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");		
			}else{
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Try again \"]}");
			}
		}
	}
	*/
	// Update metadata and delete role
	
	@PostMapping(value="/sdb/deleterole",consumes="application/json",produces="application/json")
	public ResponseEntity<String> deleteRoleSDB(@RequestHeader(value="vault-token") String token, @RequestBody String jsonstr){
		
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request. please check the request json\"]}");
		}
		
		String role = requestMap.get("role");
		String path = requestMap.get("path");
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			
			Response response = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token);		
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				log.debug(role +" , AWS Role is deleted as part of detachment of role from SDB. Path "+ path );
				Map<String,String> params = new HashMap<>();
				params.put("type", "aws-roles");
				params.put("name",role);
				params.put("path",path);
				params.put("access","delete");
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");		
				}else{
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
				}	
			}else{
				log.debug(role +" , AWS Role deletion as part of sdb delete failed . SDB path "+ path );
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Try Again\"]}");
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	@PostMapping(value="/sdb/addgroup",consumes="application/json",produces="application/json")
	public ResponseEntity<String> addGrouptoSDB(@RequestHeader(value="vault-token") String token, @RequestBody String jsonstr){
		
		if ("userpass".equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":\"This operation is not supported for Userpass authentication. \"}");
		}	
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
		}
		
		String groupName = requestMap.get("groupname");
		String path = requestMap.get("path");
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			String access = requestMap.get("access");
			String folders[] = path.split("[/]+");
			
			String policyPrefix ="";
			switch (access){
				case "read": policyPrefix = "r_"; break ; 
				case "write": policyPrefix = "w_" ;break; 
				case "deny": policyPrefix = "d_" ;break; 
			}
			if("".equals(policyPrefix)){
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}
			String policy = policyPrefix+folders[0]+"_"+folders[1];
			String r_policy = "r_"+folders[0]+"_"+folders[1];
			String w_policy = "w_"+folders[0]+"_"+folders[1];
			String d_policy = "d_"+folders[0]+"_"+folders[1];
			Response getGrpResp = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
			String responseJson="";
			
			String policies ="";
			String currentpolicies ="";
			
			if(HttpStatus.OK.equals(getGrpResp.getHttpstatus())){
					responseJson = getGrpResp.getResponse();	
					try {
						currentpolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
					} catch (IOException e) {
						log.error(e);
					}
					policies = currentpolicies;
					policies = policies.replaceAll(r_policy, "");
					policies = policies.replaceAll(w_policy, "");
					policies = policies.replaceAll(d_policy, "");
					policies = policies+","+policy;
			}else{
				// New user to be configured
				policies = policy;
			}
			
			Response ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,policies,token);
			
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "groups");
				params.put("name",groupName);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with SDB\"]}");		
				}else{
					System.out.println("Meta data update failed");
					System.out.println(metadataResponse.getResponse());
					ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,currentpolicies,token);
					if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
						System.out.println("Reverting user policy uupdate");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"erros\":[\"Group configuration failed.Please try again\"]}");
					}else{
						System.out.println("Reverting user policy update failed");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Contact Admin \"]}");
					}
				}		
			}else{
				ldapConfigresponse.getResponse();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Try Again\"]}");
			}	
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	/**
	 * Reads the contents of a folder recursively
	 * @param token
	 * @param path
	 * @return
	 */
	@GetMapping(value="/v2/sdb/list",produces="application/json")
	public ResponseEntity<String> getFoldersRecursively(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		String _path = "";
		if( "apps".equals(path)||"shared".equals(path)||"users".equals(path)){
			_path = "metadata/"+path;
		}else{
			 _path = path;
		}
		
		Response response = reqProcessor.process("/sdb/list","{\"path\":\""+_path+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		
	}
	
	/**
	 * Creates a sub folder for a given folder
	 * @param token
	 * @param path
	 * @return
	 */
	@PostMapping(value="/v2/sdb/createfolder",produces="application/json")
	public ResponseEntity<String> createNestedfolder(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		
		if(ControllerUtil.isPathValid(path)){
			String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
			Response response = reqProcessor.process("/sdb/create",jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Folder created \"]}");
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
		
	}
}
