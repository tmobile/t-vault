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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class  AppRoleService {

	@Value("${vault.port}")
	private String vaultPort;
	
	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger log = LogManager.getLogger(AppRoleService.class);
	/**
	 * Create AppRole
	 * @param token
	 * @param appRole
	 * @return
	 */
	public ResponseEntity<String> createAppRole(String token, AppRole appRole, UserDetails userDetails){
		String jsonStr = JSONUtil.getJSON(appRole);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Create AppRole").
			      put(LogMessage.MESSAGE, String.format("Trying to create AppRole [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if (!ControllerUtil.areAppRoleInputsValid(appRole)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values for AppRole creation\"]}");
		}
		jsonStr = ControllerUtil.convertAppRoleInputsToLowerCase(jsonStr);
		boolean isDuplicate = isAppRoleDuplicate(appRole.getRole_name().toLowerCase(), token);
		
		if (!isDuplicate) {
			Response response = reqProcessor.process("/auth/approle/role/create", jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
				if(createMetadata(appRole, token, userDetails)) {
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created successfully\"]}");
				}
				// revert approle creation
				Response deleteResponse = reqProcessor.process("/auth/approle/role/delete",jsonStr,token);
				if (deleteResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AppRole creation failed.\"]}");
				}
				else {
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created however metadata update failed. Please try with AppRole/update \"]}");
				}
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Create AppRole").
				      put(LogMessage.MESSAGE, "Creation of AppRole failed").
				      put(LogMessage.RESPONSE, response.getResponse()).
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole already exists and can't be created\"]}");
		}
	
	}

	/**
	 * Creates Metadata for AppRole
	 * @param roleName
	 * @param token
	 * @param userDetails
	 * @return
	 */

	private boolean createMetadata(AppRole approle, String token, UserDetails userDetails) {
		String metadataJson = populateMetaJson(approle.getRole_name(), userDetails.getUsername());
		Response response = reqProcessor.process("/write",metadataJson,token);
		boolean isMetaDataUpdated = false;

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			isMetaDataUpdated = true;
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createMetadata").
					put(LogMessage.MESSAGE, "Metadata created successfully for the AppRole").
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createMetadata").
					put(LogMessage.MESSAGE, "Failed to create metadata for the AppRole").
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return isMetaDataUpdated;
	}

    /**
     * Populate approle metadata json
     * @param appRoleName
     * @param username
     * @return
     */
	private String populateMetaJson(String appRoleName, String username) {
		String _path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + appRoleName;
		AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails(appRoleName);
		appRoleMetadataDetails.setCreatedBy(username);
		AppRoleMetadata appRoleMetadata =  new AppRoleMetadata(_path, appRoleMetadataDetails);
		String jsonStr = JSONUtil.getJSON(appRoleMetadata);
		Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		rqstParams.put("path",_path);
		return ControllerUtil.convetToJson(rqstParams);
	}

	/**
	 * Checks for duplicated AppRole
	 * @param jsonStr
	 * @param token
	 * @return
	 */
	private boolean isAppRoleDuplicate(String appRoleName, String token) {
		boolean isDuplicate = false;
		ResponseEntity<String> appRolesListResponseEntity = readAppRoles(token);
		String appRolesListRes = appRolesListResponseEntity.getBody();
		Map<String,Object> appRolesList = (Map<String,Object>) ControllerUtil.parseJson(appRolesListRes);
		ArrayList<String> existingAppRoles = (ArrayList<String>) appRolesList.get("keys");
		if (!CollectionUtils.isEmpty(existingAppRoles)) {
			for (String existingAppRole: existingAppRoles) {
				if (existingAppRole.equalsIgnoreCase(appRoleName)) {
					isDuplicate = true;
					break;
				}
			}
		}
		return isDuplicate;
	}
	/**
	 * Reads approle information
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> readAppRole(String token, String rolename){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Read AppRole").
			      put(LogMessage.MESSAGE, String.format("Trying to read AppRole [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+rolename+"\"}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Read AppRole").
			      put(LogMessage.MESSAGE, "Reading AppRole completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * Reads the list of AppRoles
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> readAppRoles(String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "listAppRoles").
			      put(LogMessage.MESSAGE, String.format("Trying to get list of AppRole")).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/role/list","{}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "listAppRoles").
			      put(LogMessage.MESSAGE, "Reading List of AppRoles completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * Gets roleid for approle
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> readAppRoleRoleId(String token, String rolename){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Read AppRoleId").
			      put(LogMessage.MESSAGE, String.format("Trying to read AppRoleId [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+rolename+"\"}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Read AppRoleId").
			      put(LogMessage.MESSAGE, "Reading AppRoleId completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * Creates secret id for approle auth
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	public ResponseEntity<String> createsecretId(String token, AppRoleSecretData appRoleSecretData){
		String jsonStr = JSONUtil.getJSON(appRoleSecretData);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Create SecretId").
			      put(LogMessage.MESSAGE, String.format("Trying to create SecretId [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		jsonStr = ControllerUtil.convertAppRoleSecretIdToLowerCase(jsonStr);
		Response response = reqProcessor.process("/auth/approle/secretid/create", jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Create SecretId").
				      put(LogMessage.MESSAGE, "Create SecretId completed successfully").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret ID created for AppRole\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Create SecretId").
			      put(LogMessage.MESSAGE, "Create SecretId failed").
			      put(LogMessage.RESPONSE, response.getResponse()).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * Reads secret id for a given rolename
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> readSecretId(String token, String rolename){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Read SecretId").
			      put(LogMessage.MESSAGE, String.format("Trying to read SecretId [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+rolename+"\"}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Read SecretId").
			      put(LogMessage.MESSAGE, "Read SecretId completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * Deletes Secret id
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	public ResponseEntity<String> deleteSecretId(String token, AppRoleNameSecretId appRoleNameSecretId){
		String jsonStr = JSONUtil.getJSON(appRoleNameSecretId);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete SecretId").
			      put(LogMessage.MESSAGE, String.format("Trying to delete SecretId [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/secret/delete",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete SecretId").
				      put(LogMessage.MESSAGE, "Deletion of SecretId completed successfully").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SecretId for AppRole deleted\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete SecretId").
			      put(LogMessage.MESSAGE, "Deletion of SecretId failed").
			      put(LogMessage.RESPONSE, response.getResponse()).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * Deletes an approle
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> deleteAppRole(String token, AppRole appRole, UserDetails userDetails){
		if (TVaultConstants.SELF_SERVICE_APPROLE_NAME.equals(appRole.getRole_name())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Not authorized to delete this ApPRole\"]}");
		}
		Response permissionResponse = canDeleteAppRole(appRole, token, userDetails);
		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(permissionResponse.getHttpstatus()) || HttpStatus.UNAUTHORIZED.equals(permissionResponse.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+permissionResponse.getResponse()+"\"]}");
		}
		String jsonStr = JSONUtil.getJSON(appRole);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete AppRoleId").
			      put(LogMessage.MESSAGE, String.format("Trying to delete AppRoleId [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/role/delete",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete AppRole").
				      put(LogMessage.MESSAGE, "Delete AppRole completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			// delete metada
			String _path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + appRole.getRole_name();
			String jsonstr = populateMetaJson(appRole.getRole_name(), userDetails.getUsername());
			Response resp = reqProcessor.process("/delete",jsonstr,token);
			if (HttpStatus.NO_CONTENT.equals(resp.getHttpstatus())) {
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted\"]}");
			}
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted, metadata delete failed\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete AppRole").
			      put(LogMessage.MESSAGE, "Reading AppRole failed").
			      put(LogMessage.RESPONSE, response.getResponse()).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Login using appRole
	 * @param appRoleIdSecretId
	 * @return
	 */
	public ResponseEntity<String> login(AppRoleIdSecretId appRoleIdSecretId){
		String jsonStr = JSONUtil.getJSON(appRoleIdSecretId);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "AppRole Login").
			      put(LogMessage.MESSAGE, "Trying to authenticate with AppRole").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/login",jsonStr,"");
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "AppRole Login").
				      put(LogMessage.MESSAGE, "AppRole Authentication Successful").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "AppRole Login").
				      put(LogMessage.MESSAGE, "AppRole Authentication failed.").
				      put(LogMessage.RESPONSE, response.getResponse()).
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Approle Login Failed.\"]}" + "HTTP STATUSCODE  :" + response.getHttpstatus());
		}
	}
	/**
	 * Associates an AppRole to a Safe
	 * @param token
	 * @param safeAppRoleAccess
	 * @return
	 */
	public ResponseEntity<String> associateApprole(String token, SafeAppRoleAccess safeAppRoleAccess){
		ResponseEntity<String> response = associateApproletoSafe(token,safeAppRoleAccess);
		if(response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
		}
		else if(response.getStatusCode().equals(HttpStatus.OK)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
		}
		else {
			return ResponseEntity.status(response.getStatusCode()).body(response.toString());	
		}
	}
	
	/**
	 * Associates an AppRole to a Safe
	 * @param token
	 * @param safeAppRoleAccess
	 * @return
	 */
	private ResponseEntity<String>associateApproletoSafe(String token,  SafeAppRoleAccess safeAppRoleAccess){
		String jsonstr = JSONUtil.getJSON(safeAppRoleAccess);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Associate AppRole to SDB").
			      put(LogMessage.MESSAGE, String.format ("Trying to associate AppRole to SDB [%s]", jsonstr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));		
		
		Map<String,Object> requestMap = ControllerUtil.parseJson(jsonstr);
		if(!ControllerUtil.areSafeAppRoleInputsValid(requestMap)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		String approle = requestMap.get("role_name").toString();
		String path = requestMap.get("path").toString();
		String access = requestMap.get("access").toString();
		
		boolean canAddAppRole = ControllerUtil.canAddPermission(path, token);
		if(canAddAppRole){

			log.info("Associate approle to SDB -  path :" + path + "valid" );

			String folders[] = path.split("[/]+");
			
			String policy ="";
			
			switch (access){
				case TVaultConstants.READ_POLICY: policy = "r_" + folders[0].toLowerCase() + "_" + folders[1] ; break ;
				case TVaultConstants.WRITE_POLICY: policy = "w_"  + folders[0].toLowerCase() + "_" + folders[1] ;break;
				case TVaultConstants.DENY_POLICY: policy = "d_"  + folders[0].toLowerCase() + "_" + folders[1] ;break;
			}
			
			if(TVaultConstants.EMPTY.equals(policy)){
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}
			

			log.info("Associate approle to SDB -  policy :" + policy + " is being configured" );
			
			//Call controller to update the policy for approle
			Response approleControllerResp = ControllerUtil.configureApprole(approle,policy,token);
			if(HttpStatus.OK.equals(approleControllerResp.getHttpstatus()) || (HttpStatus.NO_CONTENT.equals(approleControllerResp.getHttpstatus()))) {
					
				log.info("Associate approle to SDB -  policy :" + policy + " is associated" );
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Associate AppRole to SDB").
					      put(LogMessage.MESSAGE, "Association of AppRole to SDB success").
					      put(LogMessage.STATUS, approleControllerResp.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "app-roles");
				params.put("name",approle);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Add AppRole To SDB").
						      put(LogMessage.MESSAGE, "AppRole is successfully associated").
						      put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :" + approle + " is successfully associated with SDB\"]}");		
				}else{
					String safeType = ControllerUtil.getSafeType(path);
					String safeName = ControllerUtil.getSafeName(path);
					List<String> safeNames = ControllerUtil.getAllExistingSafeNames(safeType, token);
					String newPath = path;
					if (safeNames != null ) {
						
						for (String existingSafeName: safeNames) {
							if (existingSafeName.equalsIgnoreCase(safeName)) {
								// It will come here when there is only one valid safe
								newPath = safeType + "/" + existingSafeName;
								break;
							}
						} 
						
					}
					params.put("path",newPath);
					metadataResponse = ControllerUtil.updateMetadata(params,token);
					if(metadataResponse !=null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								  put(LogMessage.ACTION, "Add AppRole To SDB").
							      put(LogMessage.MESSAGE, "AppRole is successfully associated").
							      put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							      build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :" + approle + " is successfully associated with SDB\"]}");		
					}
					else {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								  put(LogMessage.ACTION, "Add AppRole To SDB").
							      put(LogMessage.MESSAGE, "AppRole configuration failed.").
							      put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
							      put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
							      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							      build()));
						//Trying to revert the metadata update in case of failure
						approleControllerResp = ControllerUtil.configureApprole(approle,policy,token);
						if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
									  put(LogMessage.ACTION, "Add AppRole To SDB").
								      put(LogMessage.MESSAGE, "Reverting user policy update failed").
								      put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
								      put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
								      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								      build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
						}else{
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
									  put(LogMessage.ACTION, "Add AppRole To SDB").
								      put(LogMessage.MESSAGE, "Reverting user policy update failed").
								      put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
								      put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
								      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								      build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Contact Admin \"]}");
						}
					}
				}
			
		}else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Associate AppRole to SDB").
				      put(LogMessage.MESSAGE, "Association of AppRole to SDB failed").
				      put(LogMessage.RESPONSE, approleControllerResp.getResponse()).
				      put(LogMessage.STATUS, approleControllerResp.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
				log.error( "Associate Approle" +approle + "to sdb FAILED");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle :" + approle + " failed to be associated with SDB\"]}");		
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Associate AppRole to SDB").
				      put(LogMessage.MESSAGE, "Association of AppRole to SDB failed").
				      put(LogMessage.RESPONSE, "Invalid Path").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle :" + approle + " failed to be associated with SDB.. Invalid Path specified\"]}");		
		
		}
	}

	/**
	 * Check whether the current user can delete this AppRole
	 * @param approle
	 * @param token
	 * @param userDetails
	 * @return
	 */
	private Response canDeleteAppRole(AppRole approle, String token, UserDetails userDetails) {
		Response readResponse = new Response();
		String _path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + approle.getRole_name();
		Response response = reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token);
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(response.getResponse());
			if(responseMap.isEmpty()) {
				response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				response.setResponse("Error reading role info");
				response.setSuccess(false);
				return readResponse;
			}
			// Safeadmin can always delete any approle
			if (userDetails.isAdmin()) {
				response.setHttpstatus(HttpStatus.OK);
				response.setResponse(TVaultConstants.EMPTY);
				response.setSuccess(true);
				return readResponse;
			}
			// normal users
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			if (userDetails.getUsername().equals((String)metadataMap.get("createdBy"))) {
				response.setHttpstatus(HttpStatus.OK);
				response.setResponse(TVaultConstants.EMPTY);
				response.setSuccess(true);
				return readResponse;
			}
		} else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus()) && userDetails.isAdmin()) {
			response.setHttpstatus(HttpStatus.OK);
			response.setResponse(TVaultConstants.EMPTY);
			response.setSuccess(true);
			return readResponse;
		}
		response.setHttpstatus(HttpStatus.UNAUTHORIZED);
		response.setResponse("Not authorized to delete this ApPRole");
		response.setSuccess(false);
		return response;
	}
	
}
