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

package com.tmobile.cso.vault.api.service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.validator.TokenValidator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.SafeUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

@Component
public class  SafesService {

	@Autowired
	private RequestProcessor reqProcessor;

	@Autowired
	private TokenValidator tokenValidator;
	
	@Autowired
	private TokenUtils tokenUtils;
	
	@Autowired
	private WorkloadDetailsService workloadDetailsService;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	@Autowired
	private SafeUtils safeUtils;

	@Autowired
	private AppRoleService appRoleService;

	@Autowired
	private AWSAuthService awsAuthService;

	@Autowired
	private AWSIAMAuthService awsiamAuthService;
	
	@Autowired
	private OIDCAuthService oidcAuthService;
	
	@Autowired
	private OIDCUtil oidcUtil;
	
	private static Logger log = LogManager.getLogger(SafesService.class);
	private static final String METADATA = "metadata/";
	private static final String INVALID_PATH = "Invalid Path";
	private static final String CREATE_SDB = "Create SDB";
	private static final String UPDATE_SDB = "Update SDB";
	private static final String DELETE_SDB = "Delete SDB";
	private static final String ADD_USER_TO_SDB = "Add user to SDB";
	private static final String REMOVE_USER_FROM_SDB = "Remove User from SDB";
	private static final String ADD_GROUP_TO_SDB ="Add Group to SDB";
	private static final String REMOVE_GROUP_FROM_SDB  ="Remove Group from SDB";
	private static final String ADD_AWS_ROLE_TO_SDB  ="Add AwsRole to SDB";
	private static final String REMOVE_AWS_ROLE_FROM_SDB  ="Delete AWS Role from Safe";
	private static final String ADD_APPROLE_TO_SDB  ="Associate AppRole to SDB";
	private static final String REMOVE_APPROLE_FROM_SDB  ="Remove Approle from Safe";

	/**
	 * Get Folders
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getFolders( String token, String path){
		String _path = "";
		if(TVaultConstants.APPS.equals(path)||TVaultConstants.SHARED.equals(path)||TVaultConstants.USERS.equals(path)){
			_path = METADATA+path;
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
		String _path = METADATA+path;
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
			String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "CreateFolder").
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
			
		}else{
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Create Folder").
					put(LogMessage.MESSAGE, "Create Folder failed").
					put(LogMessage.RESPONSE, INVALID_PATH).
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
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, CREATE_SDB).
				put(LogMessage.MESSAGE, String.format ("Start trying to Create SDB [%s]",safe.getSafeBasicDetails().getName())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (!ControllerUtil.areSDBInputsValid(safe)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATE_SDB).
					put(LogMessage.MESSAGE, "Input param validation failed").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values for creating safe\"]}");
		}
		SafeBasicDetails basicDetails = safe.getSafeBasicDetails();
		String applicationTag=basicDetails.getAppName();
		basicDetails.setApplicationTag(applicationTag);
		safe.setSafeBasicDetails(basicDetails);
		String applicationName  =getValidAppName(basicDetails);
		basicDetails.setAppName(applicationName);
		safe.setSafeBasicDetails(basicDetails);
		if(applicationName == null) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATE_SDB).
					put(LogMessage.MESSAGE, "Application name validation failed").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid Application name\"]}");
		}
		if (safe.getSafeBasicDetails().getName().endsWith("_")) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATE_SDB).
					put(LogMessage.MESSAGE, "Invalid Safe name: unexpected character _ in the end").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid Safe name: unexpected character _ in the end\"]}");
		}
        if (safe.getSafeBasicDetails().getDescription().length() > 1024) {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATE_SDB).
					put(LogMessage.MESSAGE, "Invalid input values: Description too long").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values: Description too long\"]}");
        }
		if (safe.getSafeBasicDetails().getDescription().length() <10) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATE_SDB).
					put(LogMessage.MESSAGE, "Invalid Description: Please enter minimum 10 characters").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid Description: Please enter minimum 10 characters\"]}");
		}
		if (safe.getSafeBasicDetails().getName().length() <3) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATE_SDB).
					put(LogMessage.MESSAGE, "Invalid Safe name: Please enter minimum 3 characters").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid Safe name: Please enter minimum 3 characters\"]}");
		}
		ControllerUtil.converSDBInputsToLowerCase(safe);
		String path = safe.getPath();
		// Safe type
		safe.getSafeBasicDetails().setType(ControllerUtil.getSafeType(path));
		String jsonStr = JSONUtil.getJSON(safe);
		Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		
		if(ControllerUtil.isValidSafePath(path)){
			Response response = reqProcessor.process("/sdb/create",jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				/*
				 * Store the metadata. Create policies if folders are created under the mount points
				 * 
				 */
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, CREATE_SDB).
						put(LogMessage.MESSAGE, "Store the metadata and Create policies if folders are created under the mount points").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				String _path = METADATA+path;
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
					accessMap.put(path+"/*", TVaultConstants.READ_POLICY);

					policyMap.put("accessid", "r_"+folders[0]+"_"+Safe);
					policyMap.put("access", accessMap);

					String policyRequestJson = 	ControllerUtil.convetToJson(policyMap);

					Response r_response = reqProcessor.process("/access/update",policyRequestJson,token);
					//Write Policy
					accessMap.put(path+"/*", TVaultConstants.WRITE_POLICY);
					policyMap.put("accessid", "w_"+folders[0]+"_"+Safe);

					policyRequestJson = 	ControllerUtil.convetToJson(policyMap);
					Response w_response = reqProcessor.process("/access/update",policyRequestJson,token); 
					//deny Policy
					accessMap.put(path+"/*", TVaultConstants.DENY_POLICY);
					policyMap.put("accessid", "d_"+folders[0]+"_"+Safe);

					policyRequestJson = 	ControllerUtil.convetToJson(policyMap);
					Response d_response = reqProcessor.process("/access/update",policyRequestJson,token); 

					accessMap.put(path+"/*", TVaultConstants.SUDO_POLICY);
					accessMap.put(_path+"/*", TVaultConstants.SUDO_POLICY);
					policyMap.put("accessid", "s_"+folders[0]+"_"+Safe);
					policyRequestJson = ControllerUtil.convetToJson(policyMap);
					Response s_response = reqProcessor.process("/access/update",policyRequestJson,token);

					if( (r_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) && 
							w_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
							d_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
							s_response.getHttpstatus().equals(HttpStatus.NO_CONTENT) 
							) ||
							(r_response.getHttpstatus().equals(HttpStatus.OK) && 
									w_response.getHttpstatus().equals(HttpStatus.OK) &&
									d_response.getHttpstatus().equals(HttpStatus.OK)) &&
							s_response.getHttpstatus().equals(HttpStatus.OK) 
						){
						
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, CREATE_SDB).
								put(LogMessage.MESSAGE, String.format ("SDB [%s] Created successfully by [%s]",safe.getSafeBasicDetails().getName(),safe.getSafeBasicDetails().getOwner())).

								put(LogMessage.STATUS, response.getHttpstatus().toString()).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
					}else{
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, CREATE_SDB).
								put(LogMessage.MESSAGE, String.format ("SDB [%s] Created successfully by [%s]",safe.getSafeBasicDetails().getName(),safe.getSafeBasicDetails().getOwner())).

								put(LogMessage.STATUS, response.getHttpstatus().toString()).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
						return ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Safe created however one ore more policy (read/write/deny) creation failed \"]}");
					}
				}
				if(isMetaDataUpdated) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, CREATE_SDB).
							put(LogMessage.MESSAGE,  String.format ("SDB [%s] Created successfully by [%s]",safe.getSafeBasicDetails().getName(),safe.getSafeBasicDetails().getOwner())).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe created \"]}");
				}
				else {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, CREATE_SDB).
							put(LogMessage.MESSAGE, String.format ("SDB [%s] Created successfully by [%s] and [%s]",safe.getSafeBasicDetails().getName(),safe.getSafeBasicDetails().getOwner(),safe.getSafeBasicDetails().getOwnerid())).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe created however metadata update failed. Please try with Safe/update \"]}");
				}
			}else{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, CREATE_SDB).
						put(LogMessage.MESSAGE, "SDB Create completed").
						put(LogMessage.STATUS, response.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATE_SDB).
					put(LogMessage.MESSAGE, "SDB Creation failed").
					put(LogMessage.RESPONSE, INVALID_PATH).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	/**
	 * Method to validate the application name
	 *
	 * @param safeBasicDetails
	 * @return
	 */
	public String  getValidAppName(SafeBasicDetails basicDetails) {
		String appName = basicDetails.getAppName();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, TVaultConstants.VALIDATE_APPNAME).
				put(LogMessage.MESSAGE, TVaultConstants.VALIDATE_APPNAME).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if(appName.equalsIgnoreCase(TVaultConstants.OTHER_APPNAME)) {
			appName = TVaultConstants.OTHER_APPNAME_VALUE;
		}else {
		ResponseEntity<String> appResponse = workloadDetailsService
				.getWorkloadDetailsByAppName(appName);
		if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
			JsonParser jsonParser = new JsonParser();
			JsonObject response = (JsonObject) jsonParser.parse(appResponse.getBody());
			JsonObject jsonElement = null;
			if (Objects.nonNull(response)) {
				jsonElement = response.get("spec").getAsJsonObject();
				if (Objects.nonNull(jsonElement)) {
					appName = jsonElement.get("summary").getAsString();					
				}
			}
		}
			else {
				appName = null;
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, TVaultConstants.VALIDATE_APPNAME).
				put(LogMessage.MESSAGE, "Application name validation completed").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
			return appName;
		}

	/**
	 * Gets Safe
	 */
	public ResponseEntity<String> getSafe(String token, String path) {
		if (path != null && path.startsWith("/")) {
			path = path.substring(1, path.length());
		}
		if (path != null && path.endsWith("/")) {
			path = path.substring(0, path.length()-1);
		}
		String _path = METADATA+path;
		if( TVaultConstants.APPS.equals(path)||TVaultConstants.SHARED.equals(path)||TVaultConstants.USERS.equals(path)){
			Response response = reqProcessor.process("/sdb/list","{\"path\":\""+_path+"\"}",token);
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			boolean isPathValid = ControllerUtil.isValidSafePath(path);
			
			if (!isPathValid) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
			}
			String safeName = ControllerUtil.getSafeName(path);
			if (StringUtils.isEmpty(safeName)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'Safe Name' specified\"]}");
			}
			Response response = reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
			if (HttpStatus.OK.equals(response.getHttpstatus())) {
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
			else {
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Unable to get Safe information\"]}");
			}
		}
	}

	/**
	 * Deletes Safe
	 * @param token
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String> deleteSafe(String token, Safe safe, UserDetails userDetails) {
		String path = safe.getPath();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "DeleteSDB").
				put(LogMessage.MESSAGE, String.format ("Trying to Delete SDB [%s]", path)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			Response response = new Response(); 
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				deletePolicies(token, safe);
				return deleteSafeTree(token, safe, userDetails);

			}else{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "DeleteSDB").
						put(LogMessage.MESSAGE, "SDB Deletion is completed").
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
						put(LogMessage.ACTION, "Delete  SDB").
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
					put(LogMessage.RESPONSE, INVALID_PATH).
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
		// Safe type
		safe.getSafeBasicDetails().setType(ControllerUtil.getSafeType(safe.getPath()));
		Map<String, Object> requestParams = ControllerUtil.parseJson(JSONUtil.getJSON(safe));
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, UPDATE_SDB).
				put(LogMessage.MESSAGE, String.format("Start trying to Update SDB [%s].",safe.getSafeBasicDetails().getName())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (!ControllerUtil.areSDBInputsValidForUpdate(requestParams)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, UPDATE_SDB).
					put(LogMessage.MESSAGE, "Invalid input values ").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		SafeBasicDetails bsafeBasicDetails = safe.getSafeBasicDetails();
		String appName  =getValidAppName(bsafeBasicDetails);
		if(appName == null) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, UPDATE_SDB).
					put(LogMessage.MESSAGE, "Invalid Application name ").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid Application name\"]}");
		}
        if (safe.getSafeBasicDetails().getDescription().length() > 1024) {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, UPDATE_SDB).
					put(LogMessage.MESSAGE, "Invalid input values: Description too long").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values: Description too long\"]}");
        }
		@SuppressWarnings("unchecked")
		Map<Object,Object> data = (Map<Object,Object>)requestParams.get("data");
		String path = safe.getPath();
		String safeName = safe.getSafeBasicDetails().getName();
		String safeNameFromPath = ControllerUtil.getSafeName(path);
		String safeType = ControllerUtil.getSafeType(path);
		
		int redundantSafeNamesCount = ControllerUtil.getCountOfSafesForGivenSafeName(safeName, token);
		if (redundantSafeNamesCount > 1) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, UPDATE_SDB).
					put(LogMessage.MESSAGE, "Safe can't be updated since duplicate safe names are found").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Safe can't be updated since duplicate safe names are found\"]}");
		}

		String safePath = ControllerUtil.generateSafePath(safeName, safeType);
		String _path = METADATA+safeType+"/"+safeNameFromPath; //Path as passed 
		String _safePath = METADATA+safeType+"/"+safeName; // Path created from given safename and type
		String pathToBeUpdated = _path;
		
		if(ControllerUtil.isValidSafePath(path) || ControllerUtil.isValidSafePath(safePath)){
			// Get Safe metadataInfo
			Response response = reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token);
			Map<String, Object> responseMap = null;
			if(HttpStatus.OK.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATE_SDB).
						put(LogMessage.MESSAGE, "Start reading metadata from safepath").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				responseMap = ControllerUtil.parseJson(response.getResponse());
				if(responseMap.isEmpty()) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info \"]}");
				}
				pathToBeUpdated = _path;
			}
			else{
				response = reqProcessor.process("/read","{\"path\":\""+_safePath+"\"}",token);
				if(HttpStatus.OK.equals(response.getHttpstatus())){
					responseMap = ControllerUtil.parseJson(response.getResponse());
					if(responseMap.isEmpty()) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info \"]}");
					}
					pathToBeUpdated = _safePath;
				}
				else {
					log.error("Could not fetch the safe information. Possible path issue");
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified \"]}");
				}
			}

			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			Object awsroles = metadataMap.get(TVaultConstants.AWS_ROLES);
			Object groups = metadataMap.get(TVaultConstants.GROUPS);
			Object users = metadataMap.get(TVaultConstants.USERS);
			Object approles = metadataMap.get(TVaultConstants.APP_ROLES);
			data.put(TVaultConstants.AWS_ROLES,awsroles);
			data.put(TVaultConstants.GROUPS,groups);
			data.put(TVaultConstants.USERS,users);
			data.put(TVaultConstants.APP_ROLES,approles);
			requestParams.put("path",pathToBeUpdated);
			SafeBasicDetails basicDetails = safe.getSafeBasicDetails();
			String applicationTag=basicDetails.getAppName();
			String applicationName  =getValidAppName(basicDetails);
			((Map<String,Object>)requestParams.get("data")).put("appName",(String)applicationName);
			((Map<String,Object>)requestParams.get("data")).put("applicationTag",(String) applicationTag);
			
			// Do not alter the name of the safe
			((Map<String,Object>)requestParams.get("data")).put("name",(String) metadataMap.get("name"));
			// Do not alter the owner of the safe
            ((Map<String,Object>)requestParams.get("data")).put("ownerid",(String) metadataMap.get("ownerid"));

			String metadataJson = ControllerUtil.convetToJson(requestParams) ;
			response = reqProcessor.process("/sdb/update",metadataJson,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATE_SDB).
						put(LogMessage.MESSAGE,String.format("Safe [%s] updated successfully",safe.getSafeBasicDetails().getName())).
						put(LogMessage.STATUS, response.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");
			}else{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION,UPDATE_SDB).
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
					put(LogMessage.ACTION, UPDATE_SDB).
					put(LogMessage.MESSAGE, "SDB Update failed").
					put(LogMessage.RESPONSE, INVALID_PATH).
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
	private ResponseEntity<String> deleteSafeTree(String token, Safe safe, UserDetails userDetails) {
		String path = safe.getPath();
		String _path = METADATA+path;

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
			// always add safeowner to the users list whose policy should be updated
			String onwerId = (String) metadataMap.get("ownerid");
			if (!StringUtils.isEmpty(onwerId) && users !=null) {
				users.put(onwerId, "sudo");
			}
			ControllerUtil.updateUserPolicyAssociationOnSDBDelete(path,users,token, userDetails);
			ControllerUtil.updateGroupPolicyAssociationOnSDBDelete(path,groups,token, userDetails);
			ControllerUtil.deleteAwsRoleOnSDBDelete(path,awsroles,token);
		}	
		ControllerUtil.recursivedeletesdb("{\"path\":\""+_path+"\"}",token,response);
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe deleted\"]}");


	}

	/**
	 * Adds user to a group
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> addUserToSafe(String token, SafeUser safeUser, UserDetails userDetails, boolean isPartOfCreation) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, ADD_USER_TO_SDB).
				put(LogMessage.MESSAGE, String.format ("Start trying to add user to SDB folder[%s] by [%s] ",safeUser.getPath(),userDetails.getUsername())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		if(!ControllerUtil.areSafeUserInputsValid(safeUser)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, ADD_USER_TO_SDB).
					put(LogMessage.MESSAGE, String.format ("Invalid user inputs [%s]", safeUser.toString())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		
		String userName = safeUser.getUsername();
		String path = safeUser.getPath();
		String access = safeUser.getAccess();
		
		userName = (userName !=null) ? userName.toLowerCase() : userName;
		access = (access != null) ? access.toLowerCase(): access;
		boolean isAuthorized = true;
		if (userDetails != null && !isPartOfCreation) {
			isAuthorized = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, ADD_USER_TO_SDB).
					put(LogMessage.MESSAGE,"isAuthorized check is completed").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}

		boolean canAddUser = ControllerUtil.canAddPermission(path, token);
		if(isAuthorized && canAddUser){

			String folders[] = path.split("[/]+");

			String policyPrefix ="";
			switch (access){
			case TVaultConstants.READ_POLICY: policyPrefix = "r_"; break ;
			case TVaultConstants.WRITE_POLICY: policyPrefix = "w_" ;break;
			case TVaultConstants.DENY_POLICY: policyPrefix = "d_" ;break;
			case TVaultConstants.SUDO_POLICY: policyPrefix = "s_" ;break;
			}
			if(TVaultConstants.EMPTY.equals(policyPrefix)){
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION,ADD_USER_TO_SDB).
						put(LogMessage.MESSAGE, "Incorrect access requested. Valid values are read,write,deny").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}

			String policy = policyPrefix+folders[0].toLowerCase()+"_"+folders[1];
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, ADD_USER_TO_SDB).
					put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			String s_policy = "s_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
						s_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
						s_policy += folders[index] +"_";
					}
				}
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, ADD_USER_TO_SDB).
					put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s]", r_policy, w_policy, d_policy)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			Response userResponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/userpass/read", "{\"username\":\"" + userName + "\"}",
						token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/ldap/users", "{\"username\":\"" + userName + "\"}", token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)){
				// OIDC implementation changes
				ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, userDetails, true);
				if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
					if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, ADD_USER_TO_SDB)
								.put(LogMessage.MESSAGE,
										"Trying to fetch OIDC user policies, failed")
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
						return ResponseEntity.status(HttpStatus.FORBIDDEN)
								.body("{\"messages\":[\"User configuration failed. Please try again.\"]}");
					}
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
				}
				oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
				oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
				userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
				userResponse.setHttpstatus(responseEntity.getStatusCode());
				
			}
			
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, ADD_USER_TO_SDB).
					put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", userResponse.getHttpstatus())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			
			String responseJson="";
			String groups="";
			

			if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
				responseJson = userResponse.getResponse();	
				try {
					ObjectMapper objMapper = new ObjectMapper();
					if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						currentpolicies.addAll(oidcEntityResponse.getPolicies());
					} else {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
						if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
						}
					}
				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, ADD_USER_TO_SDB).
							put(LogMessage.MESSAGE,"Exception while creating currentpolicies or groups").
							put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				
				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
				policies.add(policy);
			}else{
				// New user to be configured
				policies.add(policy);
			}
			String policiesString = StringUtils.join(policies, ",");
			String currentpoliciesString = StringUtils.join(currentpolicies, ",");

			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION,ADD_USER_TO_SDB).
					put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureUserpassUser/configureLDAPUser", policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			Response ldapConfigresponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,policiesString,token);
			}
			else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
			}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)){
				//OIDC Implementation : Entity Update
				try {

					ldapConfigresponse = oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
					oidcUtil.renewUserToken(userDetails.getClientToken());
				}catch (Exception e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, ADD_USER_TO_SDB).
							put(LogMessage.MESSAGE, "Exception while adding or updating the identity ").
							put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				
			}

			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "users");
				params.put("name",userName);
				params.put("path",path);
				params.put("access",access);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, ADD_USER_TO_SDB).
						put(LogMessage.MESSAGE, String.format ("Trying to update metadata [%s]", params.toString())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION,  ADD_USER_TO_SDB).
							put(LogMessage.MESSAGE, String.format ("User[%s] is successfully associated with SDB [%s] with [%s] permission", safeUser.getUsername(),safeUser.getPath(),safeUser.getAccess())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");		
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
					if (metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, ADD_USER_TO_SDB).
								put(LogMessage.MESSAGE, String.format ("User[%s] is successfully associated with SDB [%s] with [%s] permission", safeUser.getUsername(),safeUser.getPath(),safeUser.getAccess())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");		
					}
					else {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, ADD_USER_TO_SDB).
								put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...").
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
							ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,currentpoliciesString,token);
						}
						else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
							ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,currentpoliciesString,groups,token);
						}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)){
							//OIDC changes
							try {
								ldapConfigresponse = oidcUtil.updateOIDCEntity(currentpolicies,
										oidcEntityResponse.getEntityName());
                                oidcUtil.renewUserToken(userDetails.getClientToken());
							} catch (Exception e) {
								log.error(e);
								log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
										.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
										.put(LogMessage.ACTION,ADD_USER_TO_SDB)
										.put(LogMessage.MESSAGE,
												String.format("Exception while adding or updating the identity "))
										.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
										.put(LogMessage.APIURL,
												ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
										.build()));
							}
						}
						if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
							log.debug("Reverting user policy uupdate");
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, ADD_USER_TO_SDB).
									put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...Passed").
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");
						}else{
							log.debug("Reverting user policy update failed");
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, ADD_USER_TO_SDB).
									put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...failed").
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Contact Admin \"]}");
						}
					}
				}		
			}else{
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, ADD_USER_TO_SDB).
						put(LogMessage.MESSAGE, "Trying to configureUserpassUser/configureLDAPUser failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
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
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> addGroupToSafe(String token, SafeGroup safeGroup, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, ADD_GROUP_TO_SDB).
				put(LogMessage.MESSAGE, String.format ("Start trying to add Group [%s] to SDB folder [%s]", safeGroup.getGroupname(),safeGroup.getPath())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		if(!ControllerUtil.areSafeGroupInputsValid(safeGroup)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		String jsonstr = JSONUtil.getJSON(safeGroup);
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
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
		path = (path != null) ? path.toLowerCase() : path;
		access = (access != null) ? access.toLowerCase(): access;

		boolean canAddGroup = ControllerUtil.canAddPermission(path, token);
		if(canAddGroup){
			String folders[] = path.split("[/]+");

			String policyPrefix ="";
			switch (access){
			case TVaultConstants.READ_POLICY: policyPrefix = "r_"; break ;
			case TVaultConstants.WRITE_POLICY: policyPrefix = "w_" ;break;
			case TVaultConstants.DENY_POLICY: policyPrefix = "d_" ;break;
			}
			if(TVaultConstants.EMPTY.equals(policyPrefix)){
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
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			Response getGrpResp = new Response();
			//OIDC Changes
			if(TVaultConstants.LDAP.equals(vaultAuthMethod)){
				getGrpResp = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
			}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				//call read api with groupname
				oidcGroup= oidcUtil.getIdentityGroupDetails(groupName, token);
				if (oidcGroup != null) {
					getGrpResp.setHttpstatus(HttpStatus.OK);
					getGrpResp.setResponse(oidcGroup.getPolicies().toString());
				} else {
					getGrpResp.setHttpstatus(HttpStatus.BAD_REQUEST);
				}
			}
			String responseJson="";

			

			if(HttpStatus.OK.equals(getGrpResp.getHttpstatus())){
				responseJson = getGrpResp.getResponse();	
				try {
					//OIDC Changes
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						currentpolicies.addAll(oidcGroup.getPolicies());
					}
					
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
				policies.add(policy);
			}else{
				// New group to be configured
				policies.add(policy);
			}
			String policiesString = StringUtils.join(policies, ",");
			String currentpoliciesString = StringUtils.join(currentpolicies, ",");
			Response ldapConfigresponse = new Response();
			//OIDC Changes
			if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, policiesString, token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, policies, currentpolicies,
						oidcGroup!=null?oidcGroup.getId(): null);
				oidcUtil.renewUserToken(userDetails.getClientToken());
			}
			if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
					|| ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "groups");
				params.put("name",groupName);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse !=null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, ADD_GROUP_TO_SDB).
							put(LogMessage.MESSAGE, String.format ("Group [%s] is successfully associated with Safe [%s] and permission is [%s]", safeGroup.getGroupname(),safeGroup.getPath(),safeGroup.getAccess())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");		
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
					if (metadataResponse !=null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, ADD_GROUP_TO_SDB).
								put(LogMessage.MESSAGE, String.format ("Group [%s] is successfully associated with Safe [%s] and permission is [%s]", safeGroup.getGroupname(),safeGroup.getPath(),safeGroup.getAccess())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");		
						
					}
					else {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, ADD_GROUP_TO_SDB).
								put(LogMessage.MESSAGE, "Group configuration failed.").
								put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
								put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						//OIDC Changes
						if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString,
									token);
						} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
							ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, currentpolicies,
									currentpolicies, oidcGroup.getId());
							oidcUtil.renewUserToken(userDetails.getClientToken());
						}
						if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, ADD_GROUP_TO_SDB).
									put(LogMessage.MESSAGE, "Reverting user policy update failed").
									put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
									put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"erros\":[\"Group configuration failed.Please try again\"]}");
						}else{
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, ADD_GROUP_TO_SDB).
									put(LogMessage.MESSAGE, "Reverting user policy update failed").
									put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
									put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Contact Admin \"]}");
						}
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
	 * @param userDetails
     * @return
	 */
	public ResponseEntity<String> removeUserFromSafe(String token, SafeUser safeUser, UserDetails userDetails) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		String jsonstr = JSONUtil.getJSON(safeUser);
		String removingAccess=safeUser.getAccess();
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					  put(LogMessage.ACTION, REMOVE_USER_FROM_SDB).
				      put(LogMessage.MESSAGE, String.format ("Start trying to remove user from SDB [%s]", safeUser.getPath())).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				      build()));
		} catch (IOException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION,REMOVE_USER_FROM_SDB).
					put(LogMessage.MESSAGE, "Exception occurred while creating requestMap from input jsonstr").
					put(LogMessage.RESPONSE,e.getMessage()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}

		String userName = requestMap.get("username");
		userName = (userName !=null) ? userName.toLowerCase() : userName;
		if (StringUtils.isEmpty(userName)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"username can't be empty\"]}");
		}
		String path = requestMap.get("path");
		boolean canDeletePermission = ControllerUtil.canAddPermission(path, token);
		if(ControllerUtil.isValidSafePath(path) && canDeletePermission){
			String folders[] = path.split("[/]+");

			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			String s_policy = "s_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
						s_policy += folders[index];
					}
					else {
						r_policy += folders[index] +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
						s_policy += folders[index] +"_";
					}
				}
			}
			Response userResponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/userpass/read", "{\"username\":\"" + userName + "\"}",
						token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/ldap/users", "{\"username\":\"" + userName + "\"}", token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)){
				// OIDC implementation changes
				ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, userDetails, true);
				if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
					if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, REMOVE_USER_FROM_SDB)
								.put(LogMessage.MESSAGE,"Trying to fetch OIDC user policies, failed")
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));

						// Trying to remove the orphan entries if exists
						Map<String, String> params = new HashMap<String, String>();
						params.put("type", "users");
						params.put("name", userName);
						params.put("path", path);
						params.put("access", TVaultConstants.DELETE);
						Response metadataResponse = ControllerUtil.updateMetadata(params, token);
						if (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
							log.debug(
									JSONUtil.getJSON(ImmutableMap.<String, String>builder()
											.put(LogMessage.USER,
													ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
											.put(LogMessage.ACTION, REMOVE_USER_FROM_SDB)
											.put(LogMessage.MESSAGE, String.format("Successfully removed user[%s] from [%s]",safeUser.getUsername(),safeUser.getPath()))
											.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
											.put(LogMessage.APIURL,
													ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
											.build()));
						} else {
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, REMOVE_USER_FROM_SDB)
									.put(LogMessage.MESSAGE, "Error occurred while removing of dangling user associations")
									.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
									.body("{\"messages\":[\"User configuration failed. Please try again\"]}");
						}
					}
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
				}
				oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
				oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
				userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
				userResponse.setHttpstatus(responseEntity.getStatusCode());
			}
			String responseJson="";
			String groups="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
				responseJson = userResponse.getResponse();	
				try {
					if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						currentpolicies.addAll(oidcEntityResponse.getPolicies());
					} else {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
						if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
						}
					}
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);

				String policiesString = StringUtils.join(policies, ",");
				String currentpoliciesString = StringUtils.join(currentpolicies, ",");

				Response ldapConfigresponse = new Response();
				if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
					ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, token);
				} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, token);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)){
					// OIDC Implementation : Entity Update
					try {
						ldapConfigresponse = oidcUtil.updateOIDCEntity(policies,
								oidcEntityResponse.getEntityName());
                        oidcUtil.renewUserToken(userDetails.getClientToken());
					} catch (Exception e) {
						log.error(e);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, REMOVE_USER_FROM_SDB)
								.put(LogMessage.MESSAGE, "Exception while updating the identity")
								.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					}

				}
				if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					Map<String,String> params = new HashMap<String,String>();
					params.put("type", "users");
					params.put("name",userName);
					params.put("path",path);
					params.put("access",TVaultConstants.DELETE);
					Response metadataResponse = ControllerUtil.updateMetadata(params,token);
					if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION,REMOVE_USER_FROM_SDB)
								.put(LogMessage.MESSAGE, String.format("Successfully removed user[%s] from [%s]",safeUser.getUsername(),safeUser.getPath()))
								.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");		
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
						if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, REMOVE_USER_FROM_SDB)
									.put(LogMessage.MESSAGE, String.format("Successfully removed user[%s] from [%s]",safeUser.getUsername(),safeUser.getPath()))
									.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
							return ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");
						}
						else {
							log.debug("Meta data update failed");
							log.debug((metadataResponse!=null)?metadataResponse.getResponse():TVaultConstants.EMPTY);
							if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
								ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,
										currentpoliciesString, token);
							} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
								ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString,
										groups, token);
							} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)){
								// OIDC changes
								try {
									ldapConfigresponse = oidcUtil.updateOIDCEntity(currentpolicies,
											oidcEntityResponse.getEntityName());
                                    oidcUtil.renewUserToken(userDetails.getClientToken());
								} catch (Exception e2) {
									log.error(e2);
									log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
											.put(LogMessage.USER,
													ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
											.put(LogMessage.ACTION, REMOVE_USER_FROM_SDB)
											.put(LogMessage.MESSAGE,
													"Exception while updating the identity")
											.put(LogMessage.STACKTRACE, Arrays.toString(e2.getStackTrace()))
											.put(LogMessage.APIURL,
													ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
											.build()));
								}
							}
							if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
								log.debug("Reverting user policy uupdate");
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");
							}else{
								log.debug("Reverting user policy update failed");
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Contact Admin \"]}");
							}
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
				params.put("access",TVaultConstants.DELETE);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER,
									ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, REMOVE_USER_FROM_SDB)
							.put(LogMessage.MESSAGE, String.format("Successfully removed user[%s] from [%s]",safeUser.getUsername(),safeUser.getPath()))
							.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
							.put(LogMessage.APIURL,
									ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
				}else{
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, REMOVE_USER_FROM_SDB).
							put(LogMessage.MESSAGE, "Error occurred while removing of dangling user associations").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed. Please try again\"]}");
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}

	/**
	 * Removes an associated group from LDAP
	 * @param token
	 * @param safeGroup
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> removeGroupFromSafe(String token, SafeGroup safeGroup, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION,REMOVE_GROUP_FROM_SDB).
				put(LogMessage.MESSAGE, String.format ("Start trying to remove Group [%s] from SDB folder [%s]", safeGroup.getGroupname(),safeGroup.getPath())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		ObjectMapper objMapper = new ObjectMapper();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
		}	

		String groupName = safeGroup.getGroupname();
		String path = safeGroup.getPath();
		if(ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)){
			// check for this group is associated to this safe
			String safeMetadataPath = METADATA + path;
			Response metadataReadResponse = reqProcessor.process("/read","{\"path\":\""+safeMetadataPath+"\"}",token);
			Map<String, Object> responseMap = null;
			if(HttpStatus.OK.equals(metadataReadResponse.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, REMOVE_GROUP_FROM_SDB).
						put(LogMessage.MESSAGE,"Got metadata info").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				responseMap = ControllerUtil.parseJson(metadataReadResponse.getResponse());
				if(responseMap.isEmpty()) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, REMOVE_GROUP_FROM_SDB).
							put(LogMessage.MESSAGE, String.format ("Error Fetching existing safe info [%s]", path)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified\"]}");
				}
			}
			else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, REMOVE_GROUP_FROM_SDB).
						put(LogMessage.MESSAGE, String.format ("Error Fetching existing safe info [%s]", path)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified\"]}");
			}

			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			Map<String,Object> groupsData = (Map<String,Object>)metadataMap.get(TVaultConstants.GROUPS);

			if (groupsData == null || !groupsData.containsKey(groupName)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, REMOVE_GROUP_FROM_SDB).
						put(LogMessage.MESSAGE, String.format ("Group [%s] is not associated to Safe [%s]", groupName, path)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove group from safe. Group association to safe not found\"]}");
			}

			String[] folders = path.split("[/]+");

			String readPolicy = "r_";
			String writePolicy = "w_";
			String denyPolicy = "d_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						readPolicy += folders[index];
						writePolicy += folders[index];
						denyPolicy += folders[index];
					}
					else {
						readPolicy += folders[index]  +"_";
						writePolicy += folders[index] +"_";
						denyPolicy += folders[index] +"_";
					}
				}
			}
			Response userResponse = new Response();
			//OIDC changes
			if( TVaultConstants.LDAP.equals(vaultAuthMethod)){
				userResponse = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
			}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				//call read api with groupname
				oidcGroup= oidcUtil.getIdentityGroupDetails(groupName, token);
				if (oidcGroup != null) {
					userResponse.setHttpstatus(HttpStatus.OK);
					userResponse.setResponse(oidcGroup.getPolicies().toString());
				} else {
					userResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
				}
			}
			
			String responseJson="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
				responseJson = userResponse.getResponse();	
				try {
					//OIDC Changes
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						currentpolicies.addAll(oidcGroup.getPolicies());
					}
					
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);
				policies.remove(readPolicy);
				policies.remove(writePolicy);
				policies.remove(denyPolicy);
				String policiesString = StringUtils.join(policies, ",");
				String currentpoliciesString = StringUtils.join(currentpolicies, ",");
				Response ldapConfigresponse = new Response();
				//OIDC Changes
				if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, policiesString, token);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, policies, currentpolicies,
							oidcGroup.getId());
					oidcUtil.renewUserToken(userDetails.getClientToken());
				}
				if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
						|| ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) { 
					Map<String,String> params = new HashMap<>();
					params.put("type", "groups");
					params.put("name",groupName);
					params.put("path",path);
					params.put("access","delete");
					Response metadataResponse = ControllerUtil.updateMetadata(params,token);
					if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, REMOVE_GROUP_FROM_SDB).
								put(LogMessage.MESSAGE, String.format ("Group [%s] is successfully removed from Safe [%s]", safeGroup.getGroupname(),safeGroup.getPath())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");		
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
						if (metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, REMOVE_GROUP_FROM_SDB).
									put(LogMessage.MESSAGE, String.format ("Group [%s] is successfully removed from Safe [%s]", safeGroup.getGroupname(),safeGroup.getPath())).
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");	
						}
						else {
							log.debug("Meta data update failed");
							log.debug((null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY);
							//OIDC Changes
							if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
								ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString,
										token);
							} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
								ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, currentpolicies,
										currentpolicies, oidcGroup.getId());
								oidcUtil.renewUserToken(userDetails.getClientToken());
							}
							if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
								log.debug("Reverting user policy update");
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Please try again\"]}");
							}else{
								log.debug("Reverting Group policy update failed");
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Contact Admin \"]}");
							}
						}
					}		
				}else{
					String ssoToken = oidcUtil.getSSOToken();
					if (!StringUtils.isEmpty(ssoToken)) {
						String objectId = oidcUtil.getGroupObjectResponse(ssoToken, groupName);
						if (objectId == null || StringUtils.isEmpty(objectId)) {
							return deleteOrphanGroupEntriesForSafe(token, safeGroup, groupName, path);
						}
					}
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Try Again\"]}");
				}	
			}else{
				return deleteOrphanGroupEntriesForSafe(token, safeGroup, groupName, path);
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}

	/**
	 * Method to delete orphan group entries if exists for Safe
	 * @param token
	 * @param safeGroup
	 * @param groupName
	 * @param path
	 * @return
	 */
	private ResponseEntity<String> deleteOrphanGroupEntriesForSafe(String token, SafeGroup safeGroup, String groupName,
			String path) {
		// Trying to remove the orphan entries if exists
		Map<String,String> params = new HashMap<>();
		params.put("type", "groups");
		params.put("name",groupName);
		params.put("path",path);
		params.put("access","delete");
		Response metadataResponse = ControllerUtil.updateMetadata(params,token);
		if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "Remove Group to SDB").
					put(LogMessage.MESSAGE, String.format ("Group [%s] is successfully removed from Safe [%s]", safeGroup.getGroupname(),safeGroup.getPath())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"Message\":\"Group not available or deleted from AD, removed the group assignment and permissions \"}");
		}else{
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Try again \"]}");
		}
	}

	/**
	 * Adds AWS Configuration to Safe
	 * @param token
	 * @param SafeawsConfiguration
	 * @return
	 */
	public ResponseEntity<String> addAwsRoleToSafe(String token, AWSRole awsRole) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION,ADD_AWS_ROLE_TO_SDB).
				put(LogMessage.MESSAGE, String.format ("Start trying to Add AwsRole [%s] to Safe [%s]", awsRole.getRole(),awsRole.getPath())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		String jsonstr = JSONUtil.getJSON(awsRole);
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
		}
		if(!ControllerUtil.areAWSRoleInputsValid(requestMap)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		String role = (String)requestMap.get("role");
		String path = (String)requestMap.get("path");
		String access = (String)requestMap.get("access");

		role = (role !=null) ? role.toLowerCase() : role;
		path = (path != null) ? path.toLowerCase() : path;
		access = (access != null) ? access.toLowerCase(): access;

		boolean canAddAWSRole = ControllerUtil.canAddPermission(path, token);
		if(canAddAWSRole){
			String folders[] = path.split("[/]+");

			String policyPrefix ="";
			switch (access){
			case TVaultConstants.READ_POLICY: policyPrefix = "r_"; break ;
			case TVaultConstants.WRITE_POLICY: policyPrefix = "w_" ;break;
			case TVaultConstants.DENY_POLICY: policyPrefix = "d_" ;break;
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
			String auth_type = "ec2";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			String policiesString = "";
			String currentpoliciesString = "";

			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
				responseJson = roleResponse.getResponse();	
				try {
					JsonNode policiesArry =objMapper.readTree(responseJson).get("policies");
					for(JsonNode policyNode : policiesArry){
						currentpolicies.add(policyNode.asText());
					}
					auth_type = objMapper.readTree(responseJson).get("auth_type").asText();
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);
				policies.remove(r_policy);
				policies.remove(w_policy);
				policies.remove(d_policy);
				policies.add(policy);
				policiesString = StringUtils.join(policies, ",");
				currentpoliciesString = StringUtils.join(currentpolicies, ",");
			}else{
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either AWS role doesn't exists or you don't have enough permission to add this AWS role to Safe\"]}");
			}
			Response ldapConfigresponse = null;
			if (TVaultConstants.IAM.equals(auth_type)) {
				ldapConfigresponse = awsiamAuthService.configureAWSIAMRole(role,policiesString,token);
			}
			else {
				ldapConfigresponse = awsAuthService.configureAWSRole(role,policiesString,token);
			}
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "aws-roles");
				params.put("name",role);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, ADD_AWS_ROLE_TO_SDB).
							put(LogMessage.MESSAGE, String.format ("AwsRole [%s] is successfully associated to Safe [%s] and permission is [%s]", awsRole.getRole(),awsRole.getPath(),awsRole.getAccess())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");		
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
					if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, ADD_AWS_ROLE_TO_SDB).
								put(LogMessage.MESSAGE, String.format ("AwsRole [%s] is successfully associated to Safe [%s] and permission is [%s]", awsRole.getRole(),awsRole.getPath(),awsRole.getAccess())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");	
					}
					else {
						log.debug("Meta data update failed");
						log.debug((null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY);
						ldapConfigresponse = awsAuthService.configureAWSRole(role,currentpoliciesString,token);
						if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
							log.debug("Reverting user policy uupdate");
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
						}else{
							log.debug("Reverting user policy update failed");
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Contact Admin \"]}");
						}
					}
				}		
			}else{
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Try Again\"]}");
			}	
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	/**
	 * Removes AWS role from Safe
	 * @param token
	 * @param awsRole
	 * @param detachOnly
	 * @return
	 */
	public ResponseEntity<String> removeAWSRoleFromSafe(String token, AWSRole awsRole, boolean detachOnly, UserDetails userDetails){
		String jsonstr = JSONUtil.getJSON(awsRole);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				  put(LogMessage.ACTION,REMOVE_AWS_ROLE_FROM_SDB).
			      put(LogMessage.MESSAGE, String.format ("Start trying to delete AWS Role from SDB [%s]", awsRole.getPath())).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			      build()));
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

			// check of this role is associated to this safe
			String safeMetadataPath = METADATA + path;
			Response metadataReadResponse = reqProcessor.process("/read","{\"path\":\""+safeMetadataPath+"\"}",token);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
					put(LogMessage.MESSAGE, "Completed metadata info fetching").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			Map<String, Object> responseMap = null;
			if(HttpStatus.OK.equals(metadataReadResponse.getHttpstatus())) {
				responseMap = ControllerUtil.parseJson(metadataReadResponse.getResponse());
				if(responseMap.isEmpty()) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
							put(LogMessage.MESSAGE, String.format ("Error Fetching existing safe info [%s]", path)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified\"]}");
				}
			}
			else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
						put(LogMessage.MESSAGE, String.format ("Error Fetching existing safe info [%s]", path)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified\"]}");
			}

			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			Map<String,Object> awsroles = (Map<String,Object>)metadataMap.get(TVaultConstants.AWS_ROLES);

			if (awsroles == null || !awsroles.containsKey(role)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
						put(LogMessage.MESSAGE, String.format ("AWS role [%s] is not associated to Safe [%s]", awsRole.getRole(),awsRole.getPath())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove AWS role from safe. AWS role association to safe not found\"]}");
			}

			return removeAWSRoleAssociationFromSafe(token, awsRole, userDetails, objMapper, role, path);
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					  put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
				      put(LogMessage.MESSAGE, "Delete AWS Role from SDB failed").
				      put(LogMessage.RESPONSE, INVALID_PATH).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}

	/**
	 * Method to remove the AWS role association from safe.
	 * @param token
	 * @param awsRole
	 * @param userDetails
	 * @param objMapper
	 * @param role
	 * @param path
	 * @return
	 */
	private ResponseEntity<String> removeAWSRoleAssociationFromSafe(String token, AWSRole awsRole,
			UserDetails userDetails, ObjectMapper objMapper, String role, String path) {
		// delete mode, delete aws role as part of detachment of role from SDB.
		Response permissionResponse = ControllerUtil.canDeleteRole(awsRole.getRole(), token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH);
		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(permissionResponse.getHttpstatus()) || HttpStatus.UNAUTHORIZED.equals(permissionResponse.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+permissionResponse.getResponse()+"\"]}");
		}

		String folders[] = path.split("[/]+");
		String policyPostfix = folders[0].toLowerCase() + "_" + folders[1];

		String readPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.READ_POLICY))
				.append(policyPostfix).toString();
		String writePolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.WRITE_POLICY))
				.append(policyPostfix).toString();
		String denyPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.DENY_POLICY))
				.append(policyPostfix).toString();
		String ownerPolicy = new StringBuilder()
				.append(TVaultConstants.SVC_ACC_POLICIES_PREFIXES.getKey(TVaultConstants.SUDO_POLICY))
				.append(policyPostfix).toString();

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
				put(LogMessage.MESSAGE, String.format ("Safe AWS Role Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, ownerPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		Response roleResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+role+"\"}",token);
		String responseJson="";
		String authType = TVaultConstants.EC2;
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
			responseJson = roleResponse.getResponse();
			try {
				JsonNode policiesArry =objMapper.readTree(responseJson).get("policies");
				for(JsonNode policyNode : policiesArry){
					currentpolicies.add(policyNode.asText());
				}
				authType = objMapper.readTree(responseJson).get(TVaultConstants.AUTH_TYPE).asText();
			} catch (IOException e) {
		        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
		                put(LogMessage.MESSAGE, e.getMessage()).
		                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                build()));
			}
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		} else{
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either AWS role doesn't exist or you don't have enough permission to remove this AWS role from Safe\"]}");
		}

		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
				put(LogMessage.MESSAGE, "Remove AWS Role from Safe -  policy :" + policiesString + " is being configured." ).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		Response awsRoleConfigresponse = null;
		if (TVaultConstants.IAM.equals(authType)) {
			awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(role,policiesString,token);
		}
		else {
			awsRoleConfigresponse = awsAuthService.configureAWSRole(role,policiesString,token);
		}

		return removeMetadataForAWSRoleRemoveFromSafe(token, role, path, authType, currentpoliciesString,
				awsRoleConfigresponse);
	}

	/**
	 * Method to remove the AWS role from safe metadata
	 * @param token
	 * @param role
	 * @param path
	 * @param authType
	 * @param currentpoliciesString
	 * @param awsRoleConfigresponse
	 * @return
	 */
	private ResponseEntity<String> removeMetadataForAWSRoleRemoveFromSafe(String token, String role, String path,
			String authType, String currentpoliciesString, Response awsRoleConfigresponse) {
		if(awsRoleConfigresponse != null && (awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.OK))){
			Map<String,String> params = new HashMap<>();
			params.put("type", "aws-roles");
			params.put("name",role);
			params.put("path",path);
			params.put("access","delete");
			Response metadataResponse = ControllerUtil.updateMetadata(params,token);
			if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
						put(LogMessage.MESSAGE, String.format("AWS Role [%s] is successfully removed from Safe [%s].", role,path)).
						put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from Safe\"]}");
			}
			return revertAWSRoleRemovalForSafe(token, role, authType, currentpoliciesString, metadataResponse);
		}
		else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove AWS Role from the Safe\"]}");
		}
	}

	/**
	 * Method to revert the AWS role removal process if metadata update failed
	 * @param token
	 * @param role
	 * @param authType
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> revertAWSRoleRemovalForSafe(String token, String role, String authType,
			String currentpoliciesString, Response metadataResponse) {
		Response awsRoleConfigresponse = null;
		if (TVaultConstants.IAM.equals(authType)) {
			awsRoleConfigresponse = awsiamAuthService.configureAWSIAMRole(role,currentpoliciesString,token);
		}
		else {
			awsRoleConfigresponse = awsAuthService.configureAWSRole(role,currentpoliciesString,token);
		}
		if(awsRoleConfigresponse != null && awsRoleConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
					put(LogMessage.MESSAGE, "Reverting, AWS Role policy update success").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, REMOVE_AWS_ROLE_FROM_SDB).
					put(LogMessage.MESSAGE, "Reverting AWS Role policy update failed").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Contact Admin \"]}");
		}
	}


	/**
	 * Delete a folder
	 * @param token
	 * @param path
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> deletefolder(String token, String path, UserDetails userDetails, Boolean validSafe){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, DELETE_SDB).
				put(LogMessage.MESSAGE, String.format ("Start trying to Delete SDB folder [%s]", path)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		validSafe = !validSafe?isValidSafe(token, path) : validSafe;
		if(ControllerUtil.isPathValid(path) && validSafe){
			Response response = new Response();
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				String folders[] = path.split("[/]+");
				String r_policy = "r_";
				String w_policy = "w_";
				String d_policy = "d_";
				String s_policy = "s_";

				if (folders.length > 0) {
					for (int index = 0; index < folders.length; index++) {
						if (index == folders.length -1 ) {
							r_policy += folders[index];
							w_policy += folders[index];
							d_policy += folders[index];
							s_policy += folders[index];
						}
						else {
							r_policy += folders[index]  +"_";
							w_policy += folders[index] +"_";
							d_policy += folders[index] +"_";
							s_policy += folders[index] +"_";
						}
					}
				}

				reqProcessor.process("/access/delete","{\"accessid\":\""+r_policy+"\"}",token);
				reqProcessor.process("/access/delete","{\"accessid\":\""+w_policy+"\"}",token);
				reqProcessor.process("/access/delete","{\"accessid\":\""+d_policy+"\"}",token);
				reqProcessor.process("/access/delete","{\"accessid\":\""+s_policy+"\"}",token);

				String _path = METADATA+path;

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
					Map<String,String> approles = (Map<String, String>)metadataMap.get("app-roles");
					// always add safeowner to the users list whose policy should be updated
					String onwerId = (String) metadataMap.get("ownerid");
					if (!StringUtils.isEmpty(onwerId) && users !=null) {
						users.put(onwerId, "sudo");
					}
					ControllerUtil.updateUserPolicyAssociationOnSDBDelete(path,users,token,userDetails);
					ControllerUtil.updateGroupPolicyAssociationOnSDBDelete(path,groups,token,userDetails);
					ControllerUtil.deleteAwsRoleOnSDBDelete(path,awsroles,token);
					updateApprolePolicyAssociationOnSDBDelete(path, approles, token);
				}
				ControllerUtil.recursivedeletesdb("{\"path\":\""+_path+"\"}",token,response);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, DELETE_SDB).
						put(LogMessage.MESSAGE, "SDB Folder Deletion completed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				Response versionCreationResponse = safeUtils.updateActivityInfo(token, path, userDetails, TVaultConstants.DELETE_FOLDER_ACTION, null, null);
				if (HttpStatus.NO_CONTENT.equals(versionCreationResponse.getHttpstatus())) {
					log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "createNestedfolder").
							put(LogMessage.MESSAGE, String.format ("Created version folder for [%s]", path)).
							put(LogMessage.STATUS, versionCreationResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				else {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "createNestedfolder").
							put(LogMessage.MESSAGE, String.format ("Failed to create version folder for [%s]", path)).
							put(LogMessage.STATUS, versionCreationResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");

			}else{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, DELETE_SDB).
						put(LogMessage.MESSAGE, "SDB Folder Deletion Completed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else if(ControllerUtil.isValidDataPath(path) && validSafe) {
			Response response = new Response();
			ControllerUtil.recursivedeletesdb("{\"path\":\""+path+"\"}",token,response);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, DELETE_SDB).
						put(LogMessage.MESSAGE, "SDB Folder Deletion Completed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Folder deleted\"]}");
			}else{
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Delete Folder").
						put(LogMessage.MESSAGE, "SDB Folder Deletion Completed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, DELETE_SDB).
					put(LogMessage.MESSAGE, "SDB Folder Deletion failed").
					put(LogMessage.RESPONSE, INVALID_PATH).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Either invalid path specified or access denied for the user to delete the folder/subfolder\"]}");
		}
	}
	
	 /**
     * Approle policy update as part of offboarding
     * @param path
     * @param acessInfo
     * @param token
     */
    private void updateApprolePolicyAssociationOnSDBDelete(String path, Map<String,String> acessInfo, String token) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "updateApprolePolicyAssociationOnSDBDelete").
                put(LogMessage.MESSAGE, "Start trying updateApprolePolicyAssociationOnSDBDelete").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        if(acessInfo!=null) {
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
						r_policy += folders[index] +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}	

            Set<String> approles = acessInfo.keySet();
            ObjectMapper objMapper = new ObjectMapper();
            for(String approleName : approles) {
                Response roleResponse = reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"" + approleName + "\"}", token);
                String responseJson = "";
                List<String> policies = new ArrayList<>();
                List<String> currentpolicies = new ArrayList<>();
                if (HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
                    responseJson = roleResponse.getResponse();
                    try {
                        JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
						if (null != policiesArry) {
							for (JsonNode policyNode : policiesArry) {
								currentpolicies.add(policyNode.asText());
							}
						}
                    } catch (IOException e) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, "updateApprolePolicyAssociationOnSDBDelete").
								put(LogMessage.MESSAGE, String.format ("%s, Approle removal as part of offboarding Safe failed.", approleName)).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
                    }
                    policies.addAll(currentpolicies);
                    policies.remove(r_policy);
                    policies.remove(w_policy);
                    policies.remove(d_policy);

                    String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
                    log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateApprolePolicyAssociationOnSDBDelete").
                            put(LogMessage.MESSAGE, "Current policies :" + policiesString + " is being configured").
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                    appRoleService.configureApprole(approleName, policiesString, token);
                }
            }
        }
    }

	/**
	 * Read from safe Recursively
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getFoldersRecursively(String token, String path, Integer limit, Integer offset) {
		String _path = "";
		if( TVaultConstants.APPS.equals(path)||TVaultConstants.SHARED.equals(path)||TVaultConstants.USERS.equals(path)){
			_path = METADATA+path;
		}else{
			_path = path;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "getFoldersRecursively").
				put(LogMessage.MESSAGE, String.format ("Trying to get fodler recursively [%s]", path)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		Response response = reqProcessor.process("/sdb/list","{\"path\":\""+_path+"\"}",token);
		List<String> safeList = new ArrayList<>();
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			String responseJson = response.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			try {
				JsonNode safeArray = objMapper.readTree(responseJson).get("keys");
				if (null != safeArray) {
					for(JsonNode safe : safeArray){
						safeList.add(safe.asText());
					}
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getFoldersRecursively").
						put(LogMessage.MESSAGE, "Failed to extract safe list from response").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
		List<String> filterListWithoutVersionFolder = safeList.stream().filter(f -> !f.startsWith(TVaultConstants.VERSION_FOLDER_PREFIX)).collect(Collectors.toList());

		int totalCount = filterListWithoutVersionFolder.size();
		limit = (limit == null)?totalCount:limit;
		offset = (offset == null)?0:offset;

		List<String> filterList = filterListWithoutVersionFolder.stream().skip(offset).limit(limit).collect(Collectors.toList());
		Map<String, Object> safesMap = new HashMap<String, Object>();
		safesMap.put("keys", filterList.toArray());
		safesMap.put("total", totalCount);
		safesMap.put("next", (totalCount - (filterList.size()+ offset)>0?totalCount - (filterList.size() + offset):-1));

		log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "getFoldersRecursively").
				put(LogMessage.MESSAGE, "getFoldersRecursively completed").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		return ResponseEntity.status(response.getHttpstatus()).body(JSONUtil.getJSON(safesMap));
	}

	/**
	 * Create folder
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> createNestedfolder(String token, String path, UserDetails userDetails) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "createNestedfolder").
				put(LogMessage.MESSAGE, String.format ("Trying to createNestedfolder [%s]", path)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		path = (path != null) ? path.toLowerCase(): path;


		if(ControllerUtil.isPathValid(path) && 	isValidSafe(token, path)){
			String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
			Response response = reqProcessor.process("/sdb/createfolder",jsonStr,token);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createNestedfolder").
					put(LogMessage.MESSAGE, "createNestedfolder completed").
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
				// create version folder
				Response versionCreationResponse = safeUtils.updateActivityInfo(token, path, userDetails, TVaultConstants.CREATE_ACTION, null, null);
				if (HttpStatus.NO_CONTENT.equals(versionCreationResponse.getHttpstatus())) {
					log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "createNestedfolder").
							put(LogMessage.MESSAGE, String.format ("Created version folder for [%s]", path)).
							put(LogMessage.STATUS, versionCreationResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				else {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "createNestedfolder").
							put(LogMessage.MESSAGE, String.format ("Failed to create version folder for [%s]", path)).
							put(LogMessage.STATUS, versionCreationResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Folder created \"]}");
			}
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "createNestedfolder").
					put(LogMessage.MESSAGE, "createNestedfolder completed").
					put(LogMessage.RESPONSE, INVALID_PATH).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
	}

	/**
	 * Associate approle to Safe
	 * @param token
	 * @param jsonstr
	 * @return
	 */
	public ResponseEntity<String> associateApproletoSDB(String token, SafeAppRoleAccess safeAppRoleAccess) {
		String jsonstr = JSONUtil.getJSON(safeAppRoleAccess);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION,ADD_APPROLE_TO_SDB).
				put(LogMessage.MESSAGE, String.format ("Start trying to associate AppRole to SDB [%s]", safeAppRoleAccess.getPath())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		Map<String,Object> requestMap = ControllerUtil.parseJson(jsonstr);
		if(!ControllerUtil.areSafeAppRoleInputsValid(requestMap)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}

		if (Arrays.asList(TVaultConstants.MASTER_APPROLES).contains(safeAppRoleAccess.getRole_name())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any safe\"]}");
		}
		String approle = requestMap.get("role_name").toString().toLowerCase();
		String path = requestMap.get("path").toString();
		String access = requestMap.get("access").toString();

		boolean canAddAppRole = ControllerUtil.canAddPermission(path, token);
		if(canAddAppRole){

			log.info("Associate approle to SDB -  path :" + path + "valid" );

			String folders[] = path.split("[/]+");

			String policy ="";

			switch (access){
				case "read": policy = "r_" + folders[0].toLowerCase() + "_" + folders[1] ; break ;
				case "write": policy = "w_"  + folders[0].toLowerCase() + "_" + folders[1] ;break;
				case "deny": policy = "d_"  + folders[0].toLowerCase() + "_" + folders[1] ;break;
			}
			String policyPostfix = folders[0].toLowerCase() + "_" + folders[1];
			Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+approle+"\"}",token);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
					put(LogMessage.MESSAGE, "Fetching approle metadata info").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			String responseJson="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
				responseJson = roleResponse.getResponse();
				ObjectMapper objMapper = new ObjectMapper();
				try {
					JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
					if (null != policiesArry) {
						for(JsonNode policyNode : policiesArry){
							currentpolicies.add(policyNode.asText());
						}
					}
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);

				policies.remove("r_"+policyPostfix);
				policies.remove("w_"+policyPostfix);
				policies.remove("d_"+policyPostfix);

			} else {
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either Approle doesn't exists or you don't have enough permission to add this approle to Safe\"]}");
			}
			if("".equals(policy)){
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}
			policies.add(policy);
			String policiesString = StringUtils.join(policies, ",");
			String currentpoliciesString = StringUtils.join(currentpolicies, ",");
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION,ADD_APPROLE_TO_SDB).
					put(LogMessage.MESSAGE, "Associate approle to SDB -  policy :" + policiesString + " is being configured" ).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			//Call controller to update the policy for approle
			Response approleControllerResp = appRoleService.configureApprole(approle,policiesString,token);
			if(HttpStatus.OK.equals(approleControllerResp.getHttpstatus()) || (HttpStatus.NO_CONTENT.equals(approleControllerResp.getHttpstatus()))) {

				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
						put(LogMessage.MESSAGE, "Associate approle to SDB -  policy :" + policiesString + " is associated").
						put(LogMessage.STATUS, approleControllerResp.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "app-roles");
				params.put("name",approle);
				params.put("path",path);
				params.put("access",access);
				Response metadataResponse = ControllerUtil.updateMetadata(params,token);
				if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
							put(LogMessage.MESSAGE, String.format ("add AppRole [%s] to Safe [%s] permission [%s] is successful", safeAppRoleAccess.getRole_name(),safeAppRoleAccess.getPath(),safeAppRoleAccess.getAccess())).
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
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
					if(HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
								put(LogMessage.MESSAGE, String.format ("add AppRole [%s] to Safe [%s] permission [%s] is successful", safeAppRoleAccess.getRole_name(),safeAppRoleAccess.getPath(),safeAppRoleAccess.getAccess())).
								put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :" + approle + " is successfully associated with SDB\"]}");
					}
					else {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
								put(LogMessage.MESSAGE, "AppRole configuration failed.").
								put(LogMessage.RESPONSE, metadataResponse.getResponse()).
								put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						//Trying to revert the metadata update in case of failure
						approleControllerResp = appRoleService.configureApprole(approle,currentpoliciesString,token);
						if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
									put(LogMessage.MESSAGE, "Reverting user policy update failed").
									put(LogMessage.RESPONSE, metadataResponse.getResponse()).
									put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
						}else{
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
									put(LogMessage.MESSAGE, "Reverting user policy update failed").
									put(LogMessage.RESPONSE, metadataResponse.getResponse()).
									put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Contact Admin \"]}");
						}
					}
				}

			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
						put(LogMessage.MESSAGE, "Association of AppRole to SDB failed").
						put(LogMessage.RESPONSE, approleControllerResp.getResponse()).
						put(LogMessage.STATUS, approleControllerResp.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				log.error( "Associate Approle" +approle + "to sdb FAILED");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle :" + approle + " failed to be associated with SDB\"]}");
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, ADD_APPROLE_TO_SDB).
					put(LogMessage.MESSAGE, "Association of AppRole to SDB failed").
					put(LogMessage.RESPONSE, INVALID_PATH).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle :" + approle + " failed to be associated with SDB.. Invalid Path specified\"]}");
		}
	}

	public ResponseEntity<String> removeApproleFromSafe(String token, String jsonstr) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION,REMOVE_APPROLE_FROM_SDB ).
				put(LogMessage.MESSAGE, "Start trying to delete approle from SDB.").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;
		try {
			requestMap = objMapper.readValue(jsonstr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			log.error(e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request. please check the request json\"]}");
		}

		String role = requestMap.get("role_name");
		String path = requestMap.get("path");

		if (ControllerUtil.isValidSafePath(path) && ControllerUtil.isValidSafe(path, token)) {
			String folders[] = path.split("[/]+");
			String policyPostfix = folders[0].toLowerCase() + "_" + folders[1];
			Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+role+"\"}",token);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION,REMOVE_APPROLE_FROM_SDB ).
					put(LogMessage.MESSAGE, String.format ("Start reading approle metadata info", jsonstr)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			String responseJson="";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();
			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
				responseJson = roleResponse.getResponse();
				try {
					JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
					if (null != policiesArry) {
						for (JsonNode policyNode : policiesArry) {
							currentpolicies.add(policyNode.asText());
						}
					}
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);
				policies.remove("r_"+policyPostfix);
				policies.remove("w_"+policyPostfix);
				policies.remove("d_"+policyPostfix);
			}
			else {
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either Approle doesn't exists or you don't have enough permission to remove this approle from Safe\"]}");
			}

			String policiesString = StringUtils.join(policies, ",");
			String currentpoliciesString = StringUtils.join(currentpolicies, ",");
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, REMOVE_APPROLE_FROM_SDB).
					put(LogMessage.MESSAGE, "Remove approle from SDB -  policy :" + policiesString + " is being configured" ).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			//Call controller to update the policy for approle
			Response approleControllerResp = appRoleService.configureApprole(role,policiesString,token);
			if(HttpStatus.OK.equals(approleControllerResp.getHttpstatus()) || (HttpStatus.NO_CONTENT.equals(approleControllerResp.getHttpstatus()))) {

				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, REMOVE_APPROLE_FROM_SDB).
						put(LogMessage.MESSAGE, "Removed approle from SDB -  policy :" + policiesString).
						put(LogMessage.STATUS, approleControllerResp.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));

					Map<String, String> params = new HashMap<>();
					params.put("type", "app-roles");
					params.put("name", role);
					params.put("path", path);
					params.put("access", "delete");
					Response metadataResponse = ControllerUtil.updateMetadata(params, token);
					if (metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
								put(LogMessage.ACTION, REMOVE_APPROLE_FROM_SDB).
								put(LogMessage.MESSAGE, String.format ("AppRole [%s] is successfully removed from Safe [%s]",role,path)).
								put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
								build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle is successfully removed(if existed) from Safe\"]}");
					} else {
						String safeType = ControllerUtil.getSafeType(path);
						String safeName = ControllerUtil.getSafeName(path);
						List<String> safeNames = ControllerUtil.getAllExistingSafeNames(safeType, token);
						String newPath = path;
						if (safeNames != null) {

							for (String existingSafeName : safeNames) {
								if (existingSafeName.equalsIgnoreCase(safeName)) {
									// It will come here when there is only one valid safe
									newPath = safeType + "/" + existingSafeName;
									break;
								}
							}

						}
						params.put("path", newPath);
						metadataResponse = ControllerUtil.updateMetadata(params, token);
						if (metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
									put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
									put(LogMessage.ACTION, REMOVE_APPROLE_FROM_SDB).
									put(LogMessage.MESSAGE, String.format ("AppRole [%s] successfully removed from Safe [%s]",role,path)).
									put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
									put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
									build()));
							return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");
						} else {
							approleControllerResp = appRoleService.configureApprole(role,currentpoliciesString,token);
							if(HttpStatus.OK.equals(approleControllerResp.getHttpstatus()) || (HttpStatus.NO_CONTENT.equals(approleControllerResp.getHttpstatus()))) {
								log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
										put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
										put(LogMessage.ACTION, REMOVE_APPROLE_FROM_SDB).
										put(LogMessage.MESSAGE, "Remove Approle from SDB failed").
										put(LogMessage.RESPONSE, (null != metadataResponse) ? metadataResponse.getResponse() : TVaultConstants.EMPTY).
										put(LogMessage.STATUS, (null != metadataResponse) ? metadataResponse.getHttpstatus().toString() : TVaultConstants.EMPTY).
										put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
										build()));
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
							}
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration revoke failed.Please try again\"]}");
						}
					}
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, REMOVE_APPROLE_FROM_SDB).
						put(LogMessage.MESSAGE, "Remove Approle from SDB failed").
						put(LogMessage.RESPONSE, "Role configuration failed.Please try again").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, REMOVE_APPROLE_FROM_SDB).
					put(LogMessage.MESSAGE, "Remove Approle from SDB failed").
					put(LogMessage.RESPONSE, INVALID_PATH).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}

	}

	/**
	 * Get check if the safe exists or not
	 * @param userToken
	 * @param path
	 * @param userDetails
	 * @return
	 */
	private boolean isValidSafe(String userToken, String path) {
		try {
			String w_policy = "w_"+ ControllerUtil.getSafeType(path) + "_" + ControllerUtil.getSafeName(path);
			VaultTokenLookupDetails  vaultTokenLookupDetails = tokenValidator.getVaultTokenLookupDetails(userToken);
			String[] policies = vaultTokenLookupDetails.getPolicies();
			if (ArrayUtils.isNotEmpty(policies) && Arrays.asList(policies).contains(w_policy)) {
				return true;
			}
			return false;
		} catch (TVaultValidationException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "Check valid safe").
					put(LogMessage.MESSAGE, "isValidSafe checking faild").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return false;
		}

	}
}
