// =========================================================================
// Copyright 2019 T-Mobile, US
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.AppRole;
import com.tmobile.cso.vault.api.model.AppRoleAccessorId;
import com.tmobile.cso.vault.api.model.AppRoleAccessorIds;
import com.tmobile.cso.vault.api.model.AppRoleDetails;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.model.AppRoleMetadata;
import com.tmobile.cso.vault.api.model.AppRoleMetadataDetails;
import com.tmobile.cso.vault.api.model.AppRoleNameSecretId;
import com.tmobile.cso.vault.api.model.AppRoleSecretData;
import com.tmobile.cso.vault.api.model.SafeAppRoleAccess;
import com.tmobile.cso.vault.api.model.UserDetails;
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
		if (appRole.getRole_name().equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to create an approle named "+TVaultConstants.SELF_SERVICE_APPROLE_NAME+"\"]}");
		}
		jsonStr = ControllerUtil.convertAppRoleInputsToLowerCase(jsonStr);
		boolean isDuplicate = isAppRoleDuplicate(appRole.getRole_name().toLowerCase(), token);
		
		if (!isDuplicate) {
			Response response = reqProcessor.process("/auth/approle/role/create", jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT) || response.getHttpstatus().equals(HttpStatus.OK)) {
				String metadataJson = ControllerUtil.populateAppRoleMetaJson(appRole.getRole_name(), userDetails.getUsername());
				boolean appRoleMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson, token);
				String appRoleUsermetadataJson = ControllerUtil.populateUserMetaJson(appRole.getRole_name(), userDetails.getUsername());
				boolean appRoleUserMetaDataCreationStatus = ControllerUtil.createMetadata(appRoleUsermetadataJson, token);
				if(appRoleMetaDataCreationStatus && appRoleUserMetaDataCreationStatus) {
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
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"AppRole already exists and can't be created\"]}");
		}
	
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
		if (TVaultConstants.HIDEMASTERAPPROLE && rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to read this AppRole\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Read AppRole").
			      put(LogMessage.MESSAGE, String.format("Trying to read AppRole [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+rolename+"\"}",token);
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Read AppRole").
				      put(LogMessage.MESSAGE, "Reading AppRole completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Read AppRole").
				      put(LogMessage.MESSAGE, "Reading AppRole completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"AppRole doesn't exist\"]}");
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Read AppRole").
				      put(LogMessage.MESSAGE, "Reading AppRole completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
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
		if (response!= null && HttpStatus.OK.equals(response.getHttpstatus()) && TVaultConstants.HIDEMASTERAPPROLE) {
			response = ControllerUtil.hideMasterAppRoleFromResponse(response);
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Reads the list of AppRoles
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listAppRoles(String token,  UserDetails userDetails) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "listAppRoles").
			      put(LogMessage.MESSAGE, String.format("Trying to get list of AppRole")).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		String _path = TVaultConstants.APPROLE_USERS_METADATA_MOUNT_PATH + "/" + userDetails.getUsername();
		Response response = null;
		if (userDetails.isAdmin()) {
			response = reqProcessor.process("/auth/approle/role/list","{\"path\":\""+_path+"\"}",token);
		}
		else {
			response = reqProcessor.process("/auth/approles/rolesbyuser/list","{\"path\":\""+_path+"\"}",userDetails.getSelfSupportToken());
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "listAppRoles").
			      put(LogMessage.MESSAGE, "Reading List of AppRoles completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			if (TVaultConstants.HIDEMASTERAPPROLE) {
				response = ControllerUtil.hideMasterAppRoleFromResponse(response);
			}
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"keys\":[]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Gets roleid for approle
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> readAppRoleRoleId(String token, String rolename){
		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to read roleID of this AppRole\"]}");
		}
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
	 * Gets roleid for approle
	 * @param token
	 * @param rolename
	 * @return
	 */
	public String readRoleId(String token, String rolename){
		String roleId = null;
		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "readRoleId").
				      put(LogMessage.MESSAGE, "Access denied: no permission to read roleID of this AppRole").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return roleId;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readRoleId").
			      put(LogMessage.MESSAGE, String.format("Trying to read role_id for [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response readResponse = reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+rolename+"\"}",token);
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(readResponse.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(readResponse.getResponse());
			if(responseMap.isEmpty()) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "readRoleId").
					      put(LogMessage.MESSAGE, "Reading role_id for AppRole failed").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return roleId;
			}
			Map<String,Object> roleIdDataMap = (Map<String,Object>) responseMap.get("data");
			if (roleIdDataMap != null) {
				roleId=(String)roleIdDataMap.get("role_id");
			}
			return roleId;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readRoleId").
			      put(LogMessage.MESSAGE, "Reading role_id for AppRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return roleId;
	}
	/**
	 * Read accessors of all the SecretIDs issued against the AppRole.
	 * @param token
	 * @param rolename
	 * @param userDetails
	 * @return
	 */
	public List<String> readAccessorIds(String token, String rolename) {
		ArrayList<String> accessorIds = null;
		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "readAccessorsIds").
				      put(LogMessage.MESSAGE, "Access denied: no permission to read roleID of this AppRole").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return accessorIds;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readAccessorsIds").
			      put(LogMessage.MESSAGE, String.format("Trying to read accessor_id for [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/auth/approle/role/accessors/list","{\"role_name\":\""+rolename+"\"}",token);
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(response.getResponse());
			if(responseMap.isEmpty()) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "readAccessorsIds").
					      put(LogMessage.MESSAGE, "Reading accessor_ids for AppRole failed. No accessor_id information available.").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return accessorIds;
			}
			accessorIds = (ArrayList<String>) responseMap.get("keys");
			return accessorIds;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readAccessorsOfSecretIds").
			      put(LogMessage.MESSAGE, "Reading accessors of all the SecretIDs for AppRole completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return accessorIds;	
	}
	/**
	 * Reads the metadata associated with an AppRole
	 * @param token
	 * @param rolename
	 * @return AppRoleMetadata
	 */
	public AppRoleMetadata readAppRoleMetadata(String token, String rolename) {
		AppRoleMetadata appRoleMetadata = null;
		String _path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + rolename;
		Response readResponse = reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token);
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(readResponse.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(readResponse.getResponse());
			if(responseMap.isEmpty()) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "getMetaDataForAppRole").
					      put(LogMessage.MESSAGE, "Reading Metadata for AppRole failed").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return appRoleMetadata;
			}

			Map<String,Object> appRoleMetadataMap = (Map<String,Object>) responseMap.get("data");
			if (appRoleMetadataMap != null) {
				appRoleMetadata = new AppRoleMetadata();
				AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails();
				appRoleMetadataDetails.setCreatedBy((String)appRoleMetadataMap.get("createdBy"));
				appRoleMetadataDetails.setName(rolename);
				appRoleMetadata.setAppRoleMetadataDetails(appRoleMetadataDetails);
				appRoleMetadata.setPath(_path);
			}
			return appRoleMetadata;
		}
		else if (HttpStatus.NOT_FOUND.equals(readResponse.getHttpstatus())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "getMetaDataForAppRole").
				      put(LogMessage.MESSAGE, "Reading Metadata for AppRole failed. AppRole Not found.").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return appRoleMetadata;
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "getMetaDataForAppRole").
			      put(LogMessage.MESSAGE, "Reading Metadata for AppRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return appRoleMetadata;
	}
	
	/**
	 * Reads the AppRole
	 * @param token
	 * @param rolename
	 * @return
	 */
	public AppRole readAppRoleBasicDetails(String token, String rolename) {
		AppRole appRole = null;
		if (TVaultConstants.HIDEMASTERAPPROLE && rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "getAppRole").
				      put(LogMessage.MESSAGE, "Access denied: Not enough permission to read the AppRole information").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return appRole;
		}
		Response readResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+rolename+"\"}",token);
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(readResponse.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(readResponse.getResponse());
			if(responseMap.isEmpty()) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "getAppRole").
					      put(LogMessage.MESSAGE, "Reading AppRole failed").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return appRole;
			}

			Map<String,Object> appRoleMap = (Map<String,Object>) responseMap.get("data");
			if (appRoleMap != null) {
				String[] policies = null;
				if (appRoleMap.get("policies") != null && ((ArrayList<String>)appRoleMap.get("policies")) != null) {
					ArrayList<String> policiesList = ((ArrayList<String>)appRoleMap.get("policies"));
					policies = policiesList.toArray(new String[policiesList.size()]);
				}
				appRole = new AppRole(rolename, 
						policies, 
						((Boolean)appRoleMap.get("bind_secret_id")).booleanValue(),
						((Integer) appRoleMap.get("secret_id_num_uses")),
						((Integer) appRoleMap.get("secret_id_ttl")),
						(Integer)appRoleMap.get("token_num_uses"),
						(Integer)appRoleMap.get("token_ttl"),
						(Integer)appRoleMap.get("token_max_ttl")
						);
			}
			return appRole;
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "getAppRole").
			      put(LogMessage.MESSAGE, "Reading AppRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return appRole;
	}
	/**
	 * Checks whether given AppRole operation is allowed for a given used based on the ownership of the AppRole
	 * @param userDetails
	 * @param rolename
	 * @return
	 */
	private boolean isAllowed(String rolename, UserDetails userDetails, String operation) {
		boolean isAllowed = false;
		if (userDetails.isAdmin()) {
			// As an admin, I can read, delete, update anybody's AppRole
			isAllowed = true;
		}
		else {
			AppRoleMetadata appRoleMetadata = readAppRoleMetadata(userDetails.getSelfSupportToken(), rolename);
			String appRoleOwner = null;
			if (appRoleMetadata != null && appRoleMetadata.getAppRoleMetadataDetails() != null) {
				appRoleOwner = appRoleMetadata.getAppRoleMetadataDetails().getCreatedBy();
			}
			if (Objects.equals(userDetails.getUsername(), appRoleOwner)) {
				if (TVaultConstants.APPROLE_READ_OPERATION.equals(operation)
						|| TVaultConstants.APPROLE_DELETE_OPERATION.equals(operation)
						|| TVaultConstants.APPROLE_UPDATE_OPERATION.equals(operation)
						) {
					// As a owner of the AppRole, I can read, delete, update my AppRole
					isAllowed = true;
				}
				
			}
		}
		return isAllowed;
	}
	/**
	 * Checks whether an AppRole exists or not
	 * @param rolename
	 * @param userDetails
	 * @param operation
	 * @return
	 */
	private boolean doesAppRoleExist(String rolename, UserDetails userDetails) {
		boolean exists = false;
		String roleId = null;
		if (userDetails.isAdmin()) {
			roleId = readRoleId(userDetails.getClientToken(), rolename); 
		}
		else {
			roleId = readRoleId(userDetails.getSelfSupportToken(), rolename);
		}
		if (roleId != null) {
			exists = true;
		}
		return exists;
	}
	
	/**
	 * Reads/Gets role_id for AppRole
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> readAppRoleRoleId(String token, String rolename, UserDetails userDetails){
		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read the role_id associated with the AppRole\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readAppRoleRoleId").
			      put(LogMessage.MESSAGE, String.format("Trying to read role_id for [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if (!userDetails.isAdmin()) {
			// Non admin owners, who created AppRoles using SelfService feature, need to use SelfSupportToken in order to read role_id
			token = userDetails.getSelfSupportToken();
		}
		AppRole appRole = readAppRoleBasicDetails(token, rolename);
		if (appRole ==null) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "readAppRoleRoleId").
				      put(LogMessage.MESSAGE, String.format("Unable to read AppRole information. AppRole [%s] doesn't exist", rolename)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"AppRole doesn't exist\"]}");

		}
		boolean isAllowed = isAllowed(rolename, userDetails, TVaultConstants.APPROLE_READ_OPERATION);
		if (isAllowed) {
			Response response = reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+rolename+"\"}",token);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "readAppRoleRoleId").
				      put(LogMessage.MESSAGE, "Reading AppRoleId completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readAppRoleRoleId").
			      put(LogMessage.MESSAGE, "Reading role_id for AppRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read the role_id associated with the AppRole\"]}");
	}
	/**
	 * Reads/Gets secret_id for AppRole
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> readAppRoleSecretId(String token, String rolename, UserDetails userDetails){
		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read the secret_id associated with the AppRole\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readAppRoleSecretId").
			      put(LogMessage.MESSAGE, String.format("Trying to read secret_id for the AppRole[%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if (!userDetails.isAdmin()) {
			// Non owners, who created AppRoles using SelfService feature, need to use SelfSupportToken in order to read secret_id
			token = userDetails.getSelfSupportToken();
		}
		AppRole appRole = readAppRoleBasicDetails(token, rolename);
		if (appRole ==null) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "readAppRoleSecretId").
				      put(LogMessage.MESSAGE, "Unable to read AppRole. AppRole does not exist.").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"AppRole doesn't exist\"]}");

		}
		boolean isAllowed = isAllowed(rolename, userDetails, TVaultConstants.APPROLE_READ_OPERATION);
		if (isAllowed) {
			Response response = reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+rolename+"\"}",token);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "readAppRoleSecretId").
				      put(LogMessage.MESSAGE, "Reading AppRoleId completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readAppRoleSecretId").
			      put(LogMessage.MESSAGE, "Reading secret_id for AppRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read the secret_id associated with the AppRole\"]}");
	}
	/**
	 * Read accessors of all the SecretIDs issued against the AppRole.
	 * @param token
	 * @param rolename
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readSecretIdAccessors(String token, String rolename, UserDetails userDetails){
		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read the accessors of SecretIds associated with the AppRole\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readSecretIdAccessors").
			      put(LogMessage.MESSAGE, String.format("Trying to read accessors of all the SecretIDs [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if (!userDetails.isAdmin()) {
			// Non owners, who created AppRoles using SelfService feature, need to use SelfSupportToken in order to read secret_id
			token = userDetails.getSelfSupportToken();
		}
		if (!doesAppRoleExist(rolename, userDetails)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "readSecretIdAccessors").
				      put(LogMessage.MESSAGE, "Unable to read accessors of all the SecretIDs. AppRole may not exist").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"Unable to read AppRole. AppRole does not exist.\"]}");
		}
		boolean isAllowed = isAllowed(rolename, userDetails, TVaultConstants.APPROLE_READ_OPERATION);
		if (isAllowed) {
			Response response = reqProcessor.process("/auth/approle/role/accessors/list","{\"role_name\":\""+rolename+"\"}",token);
			if(HttpStatus.OK.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "readSecretIdAccessors").
					      put(LogMessage.MESSAGE, "Reading accessors of all the SecretIDs for AppRole completed").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
			else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "readSecretIdAccessors").
					      put(LogMessage.MESSAGE, "Reading accessors of all the SecretIDs for AppRole completed. There are no accessor_ids.").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"keys\":[]}");
			}
			else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "readSecretIdAccessors").
					      put(LogMessage.MESSAGE, "Reading accessors of all the SecretIDs for AppRole completed").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Not able to read accessor_ids.\"]}");
			}

		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readSecretIdAccessors").
			      put(LogMessage.MESSAGE, "Reading accessors of all the SecretIDs for AppRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read the accessors of SecretIds associated with the AppRole\"]}");
	}
	
	/**
	 * Reads/Gets AppRole details
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> readAppRoleDetails(String token, String rolename, UserDetails userDetails){
		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read the information of the AppRole\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readAppRoleDetails").
			      put(LogMessage.MESSAGE, String.format("Trying to read AppRole information for [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if (!userDetails.isAdmin()) {
			// Non owners, who created AppRoles using SelfService feature, need to use SelfSupportToken in order to read secret_id
			token = userDetails.getSelfSupportToken();
		}
		AppRole appRole = readAppRoleBasicDetails(token, rolename);
		if (appRole ==null) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "readAppRoleSecretId").
				      put(LogMessage.MESSAGE, String.format("Unable to read AppRole information. AppRole [%s] doesn't exist", rolename)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"AppRole doesn't exist\"]}");

		}
		boolean isAllowed = isAllowed(rolename, userDetails, TVaultConstants.APPROLE_READ_OPERATION);
		if (isAllowed) {
			AppRoleMetadata appRoleMetadata = readAppRoleMetadata(token, rolename);
			String roleId = readRoleId(token, rolename);
			List<String> accessorIds = readAccessorIds(token, rolename);
			
			AppRoleDetails appRoleDetails = new AppRoleDetails();
			appRoleDetails.setAppRole(appRole);
			appRoleDetails.setRole_id(roleId);
			appRoleDetails.setAppRoleMetadata(appRoleMetadata);
			if (!CollectionUtils.isEmpty(accessorIds)) {
				appRoleDetails.setAccessorIds(accessorIds.toArray(new String[accessorIds.size()]));
			}
			// Create Response object
			Response response = new Response();
			response.setHttpstatus(HttpStatus.OK);
			response.setSuccess(true);
			response.setResponse(JSONUtil.getJSON(appRoleDetails));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "readAppRoleSecretId").
			      put(LogMessage.MESSAGE, "Reading secret_id for AppRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read the secret_id associated with the AppRole\"]}");
	}
	/**
	 * Creates secret id for approle auth
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	public ResponseEntity<String> createsecretId(String token, AppRoleSecretData appRoleSecretData){
		if (TVaultConstants.SELF_SERVICE_APPROLE_NAME.equals(appRoleSecretData.getRole_name())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to create secretID for this AppRole\"]}");
		}
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
	public ResponseEntity<String> readAppRoleSecretId(String token, String rolename){
		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to read secretID for this AppRole\"]}");
		}
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
		if (TVaultConstants.SELF_SERVICE_APPROLE_NAME.equals(appRoleNameSecretId.getRole_name())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to delete secretId for this approle\"]}");
		}
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
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to remove this AppRole\"]}");
		}
		Response permissionResponse =  ControllerUtil.canDeleteRole(appRole.getRole_name(), token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH);
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
		AppRoleMetadata appRoleMetadata = readAppRoleMetadata(token, appRole.getRole_name());
		String approleCreatedBy = userDetails.getUsername();
		if ( appRoleMetadata.getAppRoleMetadataDetails() != null) {
			approleCreatedBy = appRoleMetadata.getAppRoleMetadataDetails().getCreatedBy();
		}
		Response response = reqProcessor.process("/auth/approle/role/delete",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete AppRole").
				      put(LogMessage.MESSAGE, "Delete AppRole completed").
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			// delete metadata
			String jsonstr = ControllerUtil.populateAppRoleMetaJson(appRole.getRole_name(), userDetails.getUsername());
			Response resp = reqProcessor.process("/delete",jsonstr,token);
			String appRoleUsermetadataJson = ControllerUtil.populateUserMetaJson(appRole.getRole_name(),approleCreatedBy);
			Response appRoleUserMetaDataDeletionResponse = reqProcessor.process("/delete",appRoleUsermetadataJson,token);
			
			if (HttpStatus.NO_CONTENT.equals(resp.getHttpstatus()) && HttpStatus.NO_CONTENT.equals(appRoleUserMetaDataDeletionResponse.getHttpstatus())) {
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted\"]}");
			}
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted, metadata delete failed\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete AppRole").
			      put(LogMessage.MESSAGE, "Delete AppRole failed").
			      put(LogMessage.RESPONSE, response.getResponse()).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Deletes the SecretIds generated for the given AppRole
	 * @param token
	 * @param rolename
	 * @return
	 */
	public ResponseEntity<String> deleteSecretIds(String token, AppRoleAccessorIds appRoleAccessorIds, UserDetails userDetails){
		if (TVaultConstants.SELF_SERVICE_APPROLE_NAME.equals(appRoleAccessorIds.getRole_name())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).
					body(String.format("{\"errors\":[\"Access denied: You don't have enough permission to delete the secret_ids associated with the AppRole (%s) \"]}", appRoleAccessorIds.getRole_name()));
		}
		boolean isAllowed = isAllowed(appRoleAccessorIds.getRole_name(), userDetails, TVaultConstants.APPROLE_DELETE_OPERATION);
		if (!userDetails.isAdmin()) {
			// Non admin owners, who created AppRoles using SelfService feature, need to use SelfSupportToken in order to read role_id
			token = userDetails.getSelfSupportToken();
		}
		if (isAllowed) {
			String[] accessorIds = appRoleAccessorIds.getAccessorIds();
			ArrayList<String> failedAccessorIds = new ArrayList<String>();
			ArrayList<String> deletedAccessorIds = new ArrayList<String>();
			for (String accessorId: accessorIds) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "deleteSecretIds").
					      put(LogMessage.MESSAGE, String.format("Trying to read SecretId for accessorId [%s]", accessorId)).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				String jsonStr = JSONUtil.getJSON(new AppRoleAccessorId(appRoleAccessorIds.getRole_name(), accessorId));
				Response deleteSecretIdResponse = reqProcessor.process("/auth/approle/role/delete/secretids",jsonStr,token);
				if(HttpStatus.NO_CONTENT.equals(deleteSecretIdResponse.getHttpstatus())) {
					deletedAccessorIds.add(accessorId);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "deleteSecretIds").
						      put(LogMessage.MESSAGE, String.format("Successfully deleted SecretId for the accessor_id [%s]",accessorId)).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
				}
				else if (HttpStatus.INTERNAL_SERVER_ERROR.equals(deleteSecretIdResponse.getHttpstatus()) && 
					deleteSecretIdResponse.getResponse().contains("failed to find accessor entry for secret_id_accessor")) {
					failedAccessorIds.add(accessorId);
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "deleteSecretIds").
						      put(LogMessage.MESSAGE, String.format("Unable to delete SecretId for the accessor_id [%s] since accessor_id does not exist",accessorId)).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
				}
				else {
					failedAccessorIds.add(accessorId);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "deleteSecretIds").
						      put(LogMessage.MESSAGE, String.format("Unable to delete SecretId for the accessor_id [%s]",accessorId)).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
				}
			}
			StringBuilder message = new StringBuilder("Deletion of secret_ids completed as: ");
			if (!CollectionUtils.isEmpty(deletedAccessorIds)) {
				message.append(String.format("Succssfully deleted the secret_ids for the following accessor_ids: [%s]. ",StringUtils.join(deletedAccessorIds.toArray(), ",")));
			}
			if (!CollectionUtils.isEmpty(failedAccessorIds)) {
				message.append(String.format("Failed to delete the secret_ids for the following accessor_ids: [%s]",StringUtils.join(failedAccessorIds.toArray(), ",")));
			}
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\""+message+"\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "deleteSecretIds").
			      put(LogMessage.MESSAGE, "Deleting deleteSecretIds for AppRole failed").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to delete the secret_ids associated with the AppRole\"]}");

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
		if (TVaultConstants.SELF_SERVICE_APPROLE_NAME.equals(safeAppRoleAccess.getRole_name())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any safe\"]}");
		}
		ResponseEntity<String> response = associateApproletoSafe(token,safeAppRoleAccess);
		if(response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
		}
		else if(response.getStatusCode().equals(HttpStatus.OK)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
		}
		else {
			return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
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
			String policyPostfix = folders[0].toLowerCase() + "_" + folders[1];
			Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+approle+"\"}",token);
			String responseJson="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
				responseJson = roleResponse.getResponse();
				ObjectMapper objMapper = new ObjectMapper();
				try {
					JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
					for(JsonNode policyNode : policiesArry){
						currentpolicies.add(policyNode.asText());
					}
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);

				policies.remove("r_"+policyPostfix);
				policies.remove("w_"+policyPostfix);
				policies.remove("d_"+policyPostfix);

			} else {
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Non existing role name. Please configure approle as first step\"]}");
			}

			if(TVaultConstants.EMPTY.equals(policy)){
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}
			policies.add(policy);
			String policiesString = StringUtils.join(policies, ",");
			String currentpoliciesString = StringUtils.join(currentpolicies, ",");

			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Associate AppRole to SDB").
					put(LogMessage.MESSAGE, "Associate approle to SDB -  policy :" + policiesString + " is being configured" ).
		  			put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			//Call controller to update the policy for approle
			Response approleControllerResp = configureApprole(approle,policiesString,token);
			if(HttpStatus.OK.equals(approleControllerResp.getHttpstatus()) || (HttpStatus.NO_CONTENT.equals(approleControllerResp.getHttpstatus()))) {

				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Associate AppRole to SDB").
					      put(LogMessage.MESSAGE, "Associate approle to SDB -  policy :" + policiesString + " is associated").
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
						approleControllerResp = configureApprole(approle,currentpoliciesString,token);
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
	 * Updates an AppRole
	 * @param token
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> updateAppRole(String token, AppRole appRole, UserDetails userDetails){
		if (Objects.isNull(appRole) || StringUtils.isEmpty(appRole.getRole_name()) ) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "updateAppRole").
				      put(LogMessage.MESSAGE, "Not enough information to update AppRole").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"AppRole can't be updated since insufficient information has been provided.\"]}");		

		}
		String rolename = appRole.getRole_name();

		if (rolename.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to modify the AppRole information\"]}");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "updateAppRole").
			      put(LogMessage.MESSAGE, String.format("Trying to update AppRole [%s]", rolename)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if (!userDetails.isAdmin()) {
			// Non owners, who created AppRoles using SelfService feature, need to use SelfSupportToken in order to read secret_id
			token = userDetails.getSelfSupportToken();
		}
		AppRole existingAppRole = readAppRoleBasicDetails(token, rolename);
		if (existingAppRole == null) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "updateAppRole").
				      put(LogMessage.MESSAGE, String.format("Unable to read AppRole information. AppRole [%s] doesn't exist", rolename)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"AppRole doesn't exist\"]}");

		}
		
		appRole.setPolicies(existingAppRole.getPolicies());
		appRole.setBind_secret_id(existingAppRole.isBind_secret_id());
		String jsonStr = JSONUtil.getJSON(appRole);

		Response response = reqProcessor.process("/auth/approle/role/create", jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT) || response.getHttpstatus().equals(HttpStatus.OK)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "updateAppRole").
				      put(LogMessage.MESSAGE, "AppRole updated successfully").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole updated successfully.\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "updateAppRole").
			      put(LogMessage.MESSAGE, "Update of AppRole failed").
			      put(LogMessage.RESPONSE, response.getResponse()).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	
	}

	/**
	 * Configure approle
	 * @param rolename
	 * @param policies
	 * @param token
	 * @return
	 */
	public Response configureApprole(String rolename,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureUserMap = new HashMap<String,String>();
		configureUserMap.put("role_name", rolename);
		configureUserMap.put("policies", policies);
		String approleConfigJson =TVaultConstants.EMPTY;

		try {
			approleConfigJson = objMapper.writeValueAsString(configureUserMap);
		} catch (JsonProcessingException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "configureApprole").
					put(LogMessage.MESSAGE, String.format ("Unable to create approleConfigJson  [%s] with rolename [%s] policies [%s] ", e.getMessage(), rolename, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return reqProcessor.process("/auth/approle/role/create",approleConfigJson,token);
	}
}
