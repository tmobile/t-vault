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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.AWSRole;
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeGroup;
import com.tmobile.cso.vault.api.model.SafeUser;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class  SafesService {

	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	public static final String READ_POLICY="read";
	public static final String WRITE_POLICY="write";
	public static final String DENY_POLICY="deny";


	private static Logger log = LogManager.getLogger(SafesService.class);
	
	/**
	 * Get Folders
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getFolders( String token, String path){
		String _path = "";
		if( "apps".equals(path)||"shared".equals(path)||"users".equals(path)){
			_path = "metadata/"+path;
		}else{
			 _path = path;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Get Folders").
			      put(LogMessage.MESSAGE, String.format ("Trying to get folders for [%s]", path)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/sdb/list","{\"path\":\""+_path+"\"}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Get Folders").
			      put(LogMessage.MESSAGE, "Getting folders completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Get SDB Info
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getInfo(String token, String path){
		String _path = "metadata/"+path;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Get Info").
			      put(LogMessage.MESSAGE, String.format ("Trying to get Info for [%s]", path)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Get Info").
			      put(LogMessage.MESSAGE, "Getting Info completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		
	}
	/**
	 * Create a folder
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> createfolder(String token, String path){
		
		path = (path != null) ? path.toLowerCase() : path;
		if(ControllerUtil.isPathValid(path)){
			//if(ControllerUtil.isValidSafe(path, token)){
				String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Create Folder").
					      put(LogMessage.MESSAGE, String.format ("Trying to Create folder [%s]", path)).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				Response response = reqProcessor.process("/sdb/createfolder",jsonStr,token);
				if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Create Folder").
						      put(LogMessage.MESSAGE, "Create Folder completed").
						      put(LogMessage.STATUS, response.getHttpstatus().toString()).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Folder created \"]}");
				}
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			//}else{
			//	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid safe\"]}");
			//}
		}else{
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Create Folder").
				      put(LogMessage.MESSAGE, "Create Folder failed").
				      put(LogMessage.RESPONSE, "Invalid Path").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
		
	}
	/**
	 * Creates Safe
	 * @param token
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String> createSafe(String token, Safe safe) {

		String path = safe.getPath();
		String jsonStr = ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe));
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Create SDB").
			      put(LogMessage.MESSAGE, String.format ("Trying to Create SDB [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		if (!ControllerUtil.areSDBInputsValid(rqstParams)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
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
					String Safe = folders[1];
					Map<String,Object> policyMap = new HashMap<String,Object>();
					Map<String,String> accessMap = new HashMap<String,String>();
					accessMap.put(path+"/*","read");

					policyMap.put("accessid", "r_"+folders[0]+"_"+Safe);
					policyMap.put("access", accessMap);

					String policyRequestJson = 	ControllerUtil.convetToJson(policyMap);

					Response r_response = reqProcessor.process("/access/update",policyRequestJson,token);
					//Write Policy
					accessMap.put(path+"/*","write");
					policyMap.put("accessid", "w_"+folders[0]+"_"+Safe);

					policyRequestJson = 	ControllerUtil.convetToJson(policyMap);
					Response w_response = reqProcessor.process("/access/update",policyRequestJson,token); 
					//deny Policy
					accessMap.put(path+"/*","deny");
					policyMap.put("accessid", "d_"+folders[0]+"_"+Safe);

					policyRequestJson = 	ControllerUtil.convetToJson(policyMap);
					Response d_response = reqProcessor.process("/access/update",policyRequestJson,token); 


					if( (r_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) && 
							w_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
							d_response.getHttpstatus().equals(HttpStatus.NO_CONTENT)  
							) ||
							(r_response.getHttpstatus().equals(HttpStatus.OK) && 
									w_response.getHttpstatus().equals(HttpStatus.OK) &&
									d_response.getHttpstatus().equals(HttpStatus.OK))
						){	
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								  put(LogMessage.ACTION, "Create SDB").
							      put(LogMessage.MESSAGE, "SDB Create Success").
							      put(LogMessage.STATUS, response.getHttpstatus().toString()).
							      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							      build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
					}else{
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								  put(LogMessage.ACTION, "Create SDB").
							      put(LogMessage.MESSAGE, "SDB Create Success").
							      put(LogMessage.STATUS, response.getHttpstatus().toString()).
							      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							      build()));
						return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Safe created however one ore more policy (read/write/deny) creation failed \"]}");
					}
				}
				if(isMetaDataUpdated) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Create SDB").
						      put(LogMessage.MESSAGE, "SDB Create Success").
						      put(LogMessage.STATUS, response.getHttpstatus().toString()).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe created \"]}");
				}
				else {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Create SDB").
						      put(LogMessage.MESSAGE, "SDB Create Success").
						      put(LogMessage.STATUS, response.getHttpstatus().toString()).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe created however metadata update failed. Please try with Safe/update \"]}");
				}
			}else{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Create SDB").
					      put(LogMessage.MESSAGE, "SDB Create completed").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Create SDB").
				      put(LogMessage.MESSAGE, "SDB Creation failed").
				      put(LogMessage.RESPONSE, "Invalid Path").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	/**
	 * Gets Safe
	 */
	public ResponseEntity<String> getSafe(String token, String path) {
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
	 * Deletes Safe
	 * @param token
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String> deleteSafe(String token, Safe safe) {
		String path = safe.getPath();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete SDB").
			      put(LogMessage.MESSAGE, String.format ("Trying to Delete SDB [%s]", path)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			Response response = new Response(); 
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				deletePolicies(token, safe);
				return deleteSafeTree(token, safe);

			}else{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Delete SDB").
					      put(LogMessage.MESSAGE, "SDB Deletion completed").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else if(ControllerUtil.isValidDataPath(path)){
			Response response = new Response(); 
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Delete SDB").
					      put(LogMessage.MESSAGE, "SDB Deletion completed").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Folder deleted\"]}");
			}else{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Delete SDB").
					      put(LogMessage.MESSAGE, "SDB Deletion completed").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}

		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete SDB").
				      put(LogMessage.MESSAGE, "SDB Deletion failed").
				      put(LogMessage.RESPONSE, "Invalid Path").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}

	/**
	 * Updates Safe
	 * @param token
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String>  updateSafe(String token, Safe safe) {
		Map<String, Object> requestParams = ControllerUtil.parseJson(ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe)));
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Update SDB").
			      put(LogMessage.MESSAGE, String.format ("Trying to Update SDB ")).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if (!ControllerUtil.areSDBInputsValid(requestParams)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		@SuppressWarnings("unchecked")
		Map<Object,Object> data = (Map<Object,Object>)requestParams.get("data");
		String path = safe.getPath();
		String _path = "metadata/"+path;
		if(ControllerUtil.isValidSafePath(path)){
			// Get Safe metadataInfo
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
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Update SDB").
					      put(LogMessage.MESSAGE, "SDB Update Success").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");
			}else{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Update SDB").
					      put(LogMessage.MESSAGE, "SDB Update completed").
					      put(LogMessage.RESPONSE, response.getResponse()).
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Update SDB").
				      put(LogMessage.MESSAGE, "SDB Update failed").
				      put(LogMessage.RESPONSE, "Invalid Path").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	/**
	 * Deletes Safe Policies
	 * @param token
	 * @param safe
	 */
	private void deletePolicies (String token, Safe safe) {
		Response response = new Response(); 
		ControllerUtil.recursivedeletesdb("{\"path\":\""+safe.getPath()+"\"}",token,response);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){

			String folders[] = safe.getPath().split("[/]+");
			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}
			reqProcessor.process("/access/delete","{\"accessid\":\""+r_policy+"\"}",token);
			reqProcessor.process("/access/delete","{\"accessid\":\""+w_policy+"\"}",token);
			reqProcessor.process("/access/delete","{\"accessid\":\""+d_policy+"\"}",token);	
		}
	}

	/**
	 * Deletes Policies, Groups, Roles associated with Safe
	 * @param token
	 * @param safe
	 * @return
	 */
	private ResponseEntity<String> deleteSafeTree(String token, Safe safe) {
		String path = safe.getPath();
		String _path = "metadata/"+path;

		// Get Safe metadataInfo
		Response response = reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
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
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe deleted\"]}");


	}

	//	/**
	//	 * Helper method to create policies to be associated with Safe
	//	 * @param token
	//	 * @param safe
	//	 * @return
	//	 */
	//	private ResponseEntity<String> createPolicies(String token, Safe safe) {
	//		String path = safe.getPath();
	//		String _path = "metadata/"+path;
	//		safe.setPath(_path);
	//
	//		String metadataJson = 	JSONUtil.getJSON(safe);
	//		Response response = reqProcessor.process("/write",metadataJson,token);
	//
	//		boolean isMetaDataUpdated = false;
	//
	//		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
	//			isMetaDataUpdated = true;
	//		}
	//
	//		String folders[] = path.split("[/]+");
	//		if(folders.length==2){
	//			String Safe = folders[1];
	//
	//			Response r_response = createPolicy(AccessPolicy.READ, token, path, folders[0], Safe);
	//			Response w_response = createPolicy(AccessPolicy.WRITE, token, path, folders[0], Safe);
	//			Response d_response = createPolicy(AccessPolicy.DENY, token, path, folders[0], Safe);
	//
	//			if(r_response.getHttpstatus().equals(HttpStatus.OK) && 
	//					w_response.getHttpstatus().equals(HttpStatus.OK) &&
	//					d_response.getHttpstatus().equals(HttpStatus.OK) ){
	//				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
	//			}else{
	//				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Safe created however one ore more policy (read/write/deny) creation failed \"]}");
	//			}
	//		}
	//		if(isMetaDataUpdated)
	//			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe created \"]}");
	//		else
	//			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe created however metadata update failed. Please try with Safe/update \"]}");
	//	}
	//
	//	/**
	//	 * Helper method to create a policy
	//	 * @param policy
	//	 * @param token
	//	 * @param path
	//	 * @param folderName
	//	 * @param secretName
	//	 * @return
	//	 */
	//	private Response createPolicy(AccessPolicy policy, String token, String path, String folderName, String secretName) {
	//		Map<String,Object> policyMap = new HashMap<String,Object>();
	//		Map<String,String> accessMap = new HashMap<String,String>();
	//		accessMap.put(path+"/*",policy.policyLong());
	//
	//		policyMap.put("accessid", policy.policyShort()+ folderName +"_"+secretName );
	//		policyMap.put("access", accessMap);
	//
	//		String policyRequestJson = 	ControllerUtil.convetToJson(policyMap);
	//
	//		Response r_response = reqProcessor.process("/access/update",policyRequestJson,token);
	//		return r_response;
	//
	//	}

	/**
	 * Adds user to a group
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> addUserToSafe(String token, SafeUser safeUser) {
		String userName = safeUser.getUsername();
		String path = safeUser.getPath();
		String access = safeUser.getAccess();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Add User to SDB").
			      put(LogMessage.MESSAGE, String.format ("Trying to add user to SDB folder ")).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		userName = (userName !=null) ? userName.toLowerCase() : userName;
		path = (path != null) ? path.toLowerCase() : path;
		access = (access != null) ? access.toLowerCase(): access;
		
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
			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}
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
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Add User to SDB").
						      put(LogMessage.MESSAGE, "User configuration failed.").
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
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
	 * Adds group to an Safe
	 * @param token
	 * @param safeGroup
	 * @return
	 */
	public ResponseEntity<String> addGroupToSafe(String token, SafeGroup safeGroup) {
		String jsonstr = JSONUtil.getJSON(safeGroup);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Add Group to SDB").
			      put(LogMessage.MESSAGE, String.format ("Trying to add Group to SDB folder [%s]", jsonstr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
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
		String access = requestMap.get("access");
		groupName = (groupName !=null) ? groupName.toLowerCase() : groupName;
		path = (path != null) ? path.toLowerCase() : path;
		access = (access != null) ? access.toLowerCase(): access;
		
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
			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}
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
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Add Group to SDB").
						      put(LogMessage.MESSAGE, "Group configuration Success.").
						      put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");		
				}else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Add Group to SDB").
						      put(LogMessage.MESSAGE, "Group configuration failed.").
						      put(LogMessage.RESPONSE, metadataResponse.getResponse()).
						      put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,currentpolicies,token);
					if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								  put(LogMessage.ACTION, "Add Group to SDB").
							      put(LogMessage.MESSAGE, "Reverting user policy update failed").
							      put(LogMessage.RESPONSE, metadataResponse.getResponse()).
							      put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							      build()));
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"erros\":[\"Group configuration failed.Please try again\"]}");
					}else{
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								  put(LogMessage.ACTION, "Add Group to SDB").
							      put(LogMessage.MESSAGE, "Reverting user policy update failed").
							      put(LogMessage.RESPONSE, metadataResponse.getResponse()).
							      put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							      build()));
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
	 * Removes an associated user from Safe
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> removeUserFromSafe(String token, SafeUser safeUser) {
		String jsonstr = JSONUtil.getJSON(safeUser);
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

			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}
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
	/**
	 * Removes an associated group from LDAP
	 * @param token
	 * @param safeGroup
	 * @return
	 */
	public ResponseEntity<String> removeGroupFromSafe(String token, SafeGroup safeGroup) {
		String jsonstr = JSONUtil.getJSON(safeGroup);
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

			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}
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
	/**
	 * Adds AWS Configuration to Safe
	 * @param token
	 * @param SafeawsConfiguration
	 * @return
	 */
	public ResponseEntity<String> addAwsRoleToSafe(String token, AWSRole awsRole) {
		String jsonstr = JSONUtil.getJSON(awsRole);
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
		}

		String role = requestMap.get("role");
		String path = requestMap.get("path");
		String access = requestMap.get("access");

		role = (role !=null) ? role.toLowerCase() : role;
		path = (path != null) ? path.toLowerCase() : path;
		access = (access != null) ? access.toLowerCase(): access;
		
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
			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}
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

}
