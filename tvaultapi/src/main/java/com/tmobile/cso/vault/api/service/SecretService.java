// =========================================================================
// Copyright 2020 T-Mobile, US
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.CommonUtils;
import com.tmobile.cso.vault.api.utils.SafeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class  SecretService {

	@Value("${vault.port}")
	private String vaultPort;

	@Value("${secretcount.safelist.limit}")
	private int safeListLimit;

	@Autowired
	private RequestProcessor reqProcessor;

	@Autowired
	private CommonUtils commonUtils;

	@Autowired
	private SafeUtils safeUtils;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger log = LogManager.getLogger(SecretService.class);
	/**
	 * To read secret from vault
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> readFromVault(String token, String path){
	    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "Read Secret").
                put(LogMessage.MESSAGE, String.format("Trying to read secret [%s]", path)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
              Response response = reqProcessor.process("/read","{\"path\":\""+path+"\"}",token);
              log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, "Read Secret").
                        put(LogMessage.MESSAGE, String.format("Reading secret [%s] completed succssfully", path)).
                        put(LogMessage.STATUS, response.getHttpstatus().toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                        build()));
              return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Write a secret into vault
	 * @param token
	 * @param secret
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> write(String token, Secret secret, UserDetails userDetails,String deleteFlag){
		String jsonStr = JSONUtil.getJSON(secret);
		String path="";
		try {
			path = new ObjectMapper().readTree(jsonStr).at("/path").asText();
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Write Secret").
				      put(LogMessage.MESSAGE, String.format("Trying to write secret [%s]", path)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			jsonStr = ControllerUtil.addDefaultSecretKey(jsonStr);
			if (!ControllerUtil.areSecretKeysValid(jsonStr)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request.Check json data\"]}");
			}
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request.Check json data\"]}");
		}
		if(ControllerUtil.isPathValid(path) && !path.contains(TVaultConstants.VERSION_FOLDER_PREFIX)){
		    // Check if the user has explicit write permission. Safe owners (implicit write permission) will be denied from write operation
			if (!hasExplicitWritePermission(token, userDetails, secret.getPath())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Write Secret").
						put(LogMessage.MESSAGE, String.format("Deleting secret [%s] failed", path)).
						put(LogMessage.RESPONSE, "No permisison to write secret in this safe").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"No permisison to write secret in this safe\"]}");
			}
			Response readResponse = reqProcessor.process("/read","{\"path\":\""+secret.getPath()+"\"}",token);
			Response response = reqProcessor.process("/write",jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
				if (!StringUtils.isEmpty("deleteFlag") && deleteFlag.equalsIgnoreCase("true")){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Write Secret").
							put(LogMessage.MESSAGE, String.format("Deleting secret [%s] completed successfully", path)).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
					}
				// write version information to version folder
				Response updateVersionInfoResponse = saveVersionInfo(token, path, userDetails, secret, readResponse);
				if(updateVersionInfoResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Write Secret").
							put(LogMessage.MESSAGE, String.format("Version info updated for [%s]", path)).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				else if (updateVersionInfoResponse.getHttpstatus().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Write Secret").
							put(LogMessage.MESSAGE, String.format("No changes made to the secrets in [%s]", path)).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				else {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Write Secret").
							put(LogMessage.MESSAGE, String.format("Failed to update version info for [%s]", path)).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Write Secret").
				      put(LogMessage.MESSAGE, String.format("Writing secret [%s] failed", path)).
				      put(LogMessage.RESPONSE, response.getResponse()).
				      put(LogMessage.STATUS, response.getHttpstatus().toString()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Write Secret").
				      put(LogMessage.MESSAGE, String.format("Writing secret [%s] failed", path)).
				      put(LogMessage.RESPONSE, "Invalid path").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
	}

	/**
	 * Write a secret into vault
	 * @param token
	 * @param secret
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> write(String token, Secret secret, UserDetails userDetails){
		String jsonStr = JSONUtil.getJSON(secret);
		String path="";
		try {
			path = new ObjectMapper().readTree(jsonStr).at("/path").asText();
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Write Secret").
					put(LogMessage.MESSAGE, String.format("Trying to write secret [%s]", path)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			jsonStr = ControllerUtil.addDefaultSecretKey(jsonStr);
			if (!ControllerUtil.areSecretKeysValid(jsonStr)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request.Check json data\"]}");
			}
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request.Check json data\"]}");
		}
		if(ControllerUtil.isPathValid(path) && !path.contains(TVaultConstants.VERSION_FOLDER_PREFIX)){
			// Check if the user has explicit write permission. Safe owners (implicit write permission) will be denied from write operation
			if (!hasExplicitWritePermission(token, userDetails, secret.getPath())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Write Secret").
						put(LogMessage.MESSAGE, String.format("Writing secret [%s] failed", path)).
						put(LogMessage.RESPONSE, "No permisison to write secret in this safe").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"No permisison to write secret in this safe\"]}");
			}
			Response readResponse = reqProcessor.process("/read","{\"path\":\""+secret.getPath()+"\"}",token);
			Response response = reqProcessor.process("/write",jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Write Secret").
							put(LogMessage.MESSAGE, String.format("Writing secret [%s] completed succssfully", path)).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				// write version information to version folder
				Response updateVersionInfoResponse = saveVersionInfo(token, path, userDetails, secret, readResponse);
				if(updateVersionInfoResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Write Secret").
							put(LogMessage.MESSAGE, String.format("Version info updated for [%s]", path)).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				else if (updateVersionInfoResponse.getHttpstatus().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Write Secret").
							put(LogMessage.MESSAGE, String.format("No changes made to the secrets in [%s]", path)).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				else {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Write Secret").
							put(LogMessage.MESSAGE, String.format("Failed to update version infofor [%s]", path)).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Write Secret").
					put(LogMessage.MESSAGE, String.format("Writing secret [%s] failed", path)).
					put(LogMessage.RESPONSE, response.getResponse()).
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Write Secret").
					put(LogMessage.MESSAGE, String.format("Writing secret [%s] failed", path)).
					put(LogMessage.RESPONSE, "Invalid path").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
	}

	/**
	 * To save version information on secret creation
	 * @param token
	 * @param path
	 * @param userDetails
	 * @return
	 */
	private Response saveVersionInfo(String token, String path, UserDetails userDetails, Secret secret, Response readResponse) {
		List<String> modifiedKeys = getChangedSecretKeys(secret, readResponse);
		List<String> deletedKeys = getDeletedSecretKeys(secret, readResponse);
		Response versionCreationResponse = new Response();
		if (modifiedKeys.size() >0 || deletedKeys.size() >0) {
			versionCreationResponse = safeUtils.createVersionFolder(token, path, userDetails, false, modifiedKeys, deletedKeys);
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
		}
		else {
			versionCreationResponse.setHttpstatus(HttpStatus.UNPROCESSABLE_ENTITY);
			versionCreationResponse.setResponse("{\"errors\":[\"No changes made to secrets\"]}");
		}
		return versionCreationResponse;
	}

	/**
	 * To get the list of deleted secrets in the write secret request
	 * @param secret
	 * @param readResponse
	 * @return
	 */
	private List<String> getDeletedSecretKeys(Secret secret, Response readResponse) {
		ObjectMapper objectMapper = new ObjectMapper();
		List<String> deletedSecretKeys = new ArrayList<>();
		if (readResponse.getHttpstatus().equals(HttpStatus.OK)) {
			try {
				Secret oldSecret = objectMapper.readValue(readResponse.getResponse(),	new TypeReference<Secret>() {});
				if (oldSecret.getDetails() != null && oldSecret.getDetails().size() > 0) {
					for (Map.Entry<String, String> entry : oldSecret.getDetails().entrySet()) {
						String key = entry.getKey();
						String value = entry.getValue();
						if (!secret.getDetails().containsKey(key)) {
							deletedSecretKeys.add(key);
						}
					}
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getDeletedSecretKeys").
						put(LogMessage.MESSAGE, String.format("Failed to read current secret for [%s}", secret.getPath())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
		return deletedSecretKeys;
	}

	/**
	 * To get the list of changed secrets in the write secret request
	 * @param secret
	 * @param readResponse
	 * @return
	 */
	private List<String> getChangedSecretKeys(Secret secret, Response readResponse) {
		ObjectMapper objectMapper = new ObjectMapper();
		List<String> modifiedSecretKeys = new ArrayList<>();
		if (readResponse.getHttpstatus().equals(HttpStatus.OK)) {
			try {
				Secret oldSecret = objectMapper.readValue(readResponse.getResponse(),	new TypeReference<Secret>() {});
				if (oldSecret.getDetails() != null && oldSecret.getDetails().size() > 0) {
					for (Map.Entry<String, String> entry : secret.getDetails().entrySet()) {
						String key = entry.getKey();
						String value = entry.getValue();
						if (!oldSecret.getDetails().containsKey(key) || !value.equals(oldSecret.getDetails().get(entry.getKey()))) {
							modifiedSecretKeys.add(key);
						}
					}
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getChangedSecretKeys").
						put(LogMessage.MESSAGE, String.format("Failed to read current secret for [%s}", secret.getPath())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
		return modifiedSecretKeys;
	}

	/**
	 * To check if user has explicit write permission
	 * @param token
	 * @param userName
	 * @param path
	 * @return
	 */
	private boolean hasExplicitWritePermission(String token, UserDetails userDetails, String path) {
		String policy = "w_"+ ControllerUtil.getSafeType(path) + "_" + ControllerUtil.getSafeName(path);
		if (Arrays.stream(userDetails.getPolicies()).anyMatch(policy::equals)) {
			return true;
		}
		return false;
	}

	/**
	 * Delete secret from vault
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> deleteFromVault(String token, String path){
		if(ControllerUtil.isValidDataPath(path)){
			//if(ControllerUtil.isValidSafe(path,token)){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete Secret").
				      put(LogMessage.MESSAGE, String.format("Trying to delete secret [%s]", path)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
				Response response = reqProcessor.process("/delete","{\"path\":\""+path+"\"}",token);
				if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Delete Secret").
						      put(LogMessage.MESSAGE, String.format("Deleting secret [%s] completed", path)).
						      put(LogMessage.STATUS, response.getHttpstatus().toString()).
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secrets deleted\"]}");
				}
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			//}else{
			//	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid safe\"]}");
			//}
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete Secret").
				      put(LogMessage.MESSAGE, String.format("Deleting secret [%s] failed", path)).
				      put(LogMessage.RESPONSE, "Invalid path").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
	}
	/**
	 * Read vault folders and secrets recursively
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> readFromVaultRecursive(String token, String path){
		Response response = new Response();
		SafeNode safeNode = new SafeNode();
		safeNode.setId(path);
		if (ControllerUtil.isValidSafePath(path)) {
			safeNode.setType(TVaultConstants.SAFE);
		}
		else {
			safeNode.setType(TVaultConstants.FOLDER);
		}
		ControllerUtil.recursiveRead("{\"path\":\""+path+"\"}",token,response, safeNode);
		ObjectMapper mapper = new ObjectMapper();
		try {
			String res = mapper.writeValueAsString(safeNode);
			return ResponseEntity.status(response.getHttpstatus()).body(res);
		} catch (JsonProcessingException e) {
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	/**
	 * Read Folder and Secrets for a given folder
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> readFoldersAndSecrets(String token, String path){
		Response response = new Response();
		SafeNode safeNode = new SafeNode();
		safeNode.setId(path);
		if (ControllerUtil.isValidSafePath(path)) {
			safeNode.setType(TVaultConstants.SAFE);
		}
		else {
			safeNode.setType(TVaultConstants.FOLDER);
		}
		ControllerUtil.getFoldersAndSecrets("{\"path\":\""+path+"\"}",token,response, safeNode);
		ObjectMapper mapper = new ObjectMapper();
		try {
			String res = mapper.writeValueAsString(safeNode);
			return ResponseEntity.status(response.getHttpstatus()).body(res);
		} catch (JsonProcessingException e) {
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}

	/**
	 * Get total secret count in T-Vault
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getSecretCount(String token, String safeType, int offset) {
		if (!isAuthorizedToGetSecretCount(token)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getSecretCount").
					put(LogMessage.MESSAGE, "Access Denied: No enough permission to access this API").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access Denied: No enough permission to access this API\"]}");
		}

		if (StringUtils.isEmpty(safeType) || offset < 0) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getSecretCount").
					put(LogMessage.MESSAGE, "Invalid path or offset").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path or offset\"]}");
		}
		SecretCount secretCount = new SecretCount();
		String safePath = safeType;

		Response response = new Response();

		SafeNode safeNode = new SafeNode();
		safeNode.setId(safePath);
		safeNode.setType(TVaultConstants.SAFE);
		log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "getSecretCount").
				put(LogMessage.MESSAGE, "Trying to get safe nodes in the given path").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		List<String> safePathList = getSafeListInPath(token, safePath);
		List<String> offsetSafePahtList = safePathList.stream().skip(offset).limit(safeListLimit).collect(Collectors.toList());
		int safeTypeSecretTotalCount = 0;
		Map<String, Integer> safeSecretCount = new HashMap<>();

		for(String path: offsetSafePahtList) {
			safeNode = ControllerUtil.recursiveReadForCount("{\"path\":\""+path+"\"}",token,response, path, TVaultConstants.SAFE);

			for (int i=0;i< safeNode.getChildren().size(); i++) {
				SafeNode safe = safeNode.getChildren().get(i);
				int count = getSecretCountInSafe(safe.getChildren(), safe.getParentId());
				safeSecretCount.put(ControllerUtil.getSafeName(safe.getId()), count>0?count:0);
				safeTypeSecretTotalCount+=(count>0?count:0);
			}
			if (safeNode.getChildren().size() == 0) {
				safeSecretCount.put(ControllerUtil.getSafeName(safeNode.getId()), 0);
			}
		}

		secretCount.setSafeSecretCount(safeSecretCount);
		secretCount.setTotalSecretCount(safeTypeSecretTotalCount);
		secretCount.setTotalSafes(safePathList.size());
		secretCount.setNext(-1);
		if (secretCount.getSafeSecretCount().size() > 0 && secretCount.getSafeSecretCount().size() == safeListLimit && secretCount.getSafeSecretCount().size() + offset < secretCount.getTotalSafes()) {
			secretCount.setNext(offset + safeListLimit);
		}
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(secretCount));
	}

	/**
	 * To get the safe path list for a path.
	 * @param token
	 * @param safePath
	 * @return
	 */
	private List<String> getSafeListInPath(String token, String safePath) {
		ObjectMapper objMapper =  new ObjectMapper();
		List<String> safePathList = new ArrayList<>();
		Response lisresp = reqProcessor.process("/sdb/list", "{\"path\":\""+safePath+"\"}", token);
		if(HttpStatus.OK.equals(lisresp.getHttpstatus())){
			try {
				JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
				int i = 1;
				for(JsonNode node : folders){
					safePathList.add(safePath + "/" + node.asText());
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getSafeListInPath").
						put(LogMessage.MESSAGE, String.format ("Failed to get safe list for [%s]", safePath)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
		Collections.sort(safePathList);
		return safePathList;
	}

	/**
	 * To get secret count in a safeNode
	 * @param safeNode
	 * @param patentId
	 * @return
	 */
	private int getSecretCountInSafe(List<SafeNode> safeNode, String patentId) {
		int count = 0;
		for (int i=0;i< safeNode.size(); i++) {
			SafeNode safe = safeNode.get(i);
			if (safe.getType().equalsIgnoreCase("secret") && !safe.getParentId().equalsIgnoreCase(patentId) && !safe.getId().contains(TVaultConstants.VERSION_FOLDER_PREFIX)) {
				try {
					Secret data = (Secret)JSONUtil.getObj(safe.getValue(), Secret.class);
					count += data.getDetails().size();
				} catch (IOException e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "getSecretCountInSafe").
							put(LogMessage.MESSAGE, "Error getting Safe Object").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
			}
			else if (safe.getType().equalsIgnoreCase("folder") && safe.getChildren().size()>0) {
				count += getSecretCountInSafe(safe.getChildren(), patentId);
			}
		}
		return count;
	}

	/**
	 * To check if authorized to get secret count.
	 * @param token
	 * @return
	 */
	private boolean isAuthorizedToGetSecretCount(String token) {
		ObjectMapper objectMapper = new ObjectMapper();
		List<String> currentPolicies = new ArrayList<>();
		Response response = reqProcessor.process("/auth/tvault/lookup","{}", token);
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			String responseJson = response.getResponse();
			try {
				currentPolicies = Arrays.asList(commonUtils.getPoliciesAsArray(objectMapper, responseJson));
				if (currentPolicies.contains(TVaultConstants.ROOT_POLICY)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "isAuthorizedToGetSecretCount")
							.put(LogMessage.MESSAGE, "The Token has required policies to get total secret count.")
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
					return true;
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "isAuthorizedToGetSecretCount")
						.put(LogMessage.MESSAGE,
								"Failed to parse policies from token")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "isAuthorizedToGetSecretCount")
				.put(LogMessage.MESSAGE, "The Token does not have required policies to get total secret count.")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return false;
	}

	/**
	 * To get folder last change details
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getFolderVersionInfo(String token, String path) {
		ObjectMapper objMapper =  new ObjectMapper();
		List<String> folderSecretList = getFolderSecretList(token, path, objMapper);
		List<FolderVersion> folderVersionList = getVersionSafeNode(token, path, objMapper, folderSecretList, true);

		List<String> folderSecretListFromParent;
		List<FolderVersion> folderVersionListFromParent;
		String parentPath = path.substring(0, path.lastIndexOf('/'));
		if (!TVaultConstants.USERS.equals(parentPath) && !TVaultConstants.SHARED.equals(parentPath) && !TVaultConstants.APPS.equals(parentPath)) {
			folderSecretListFromParent = getFolderSecretList(token, parentPath, objMapper);
			folderSecretListFromParent = folderSecretListFromParent.stream().filter(f-> path.equals(parentPath + "/" + f)).collect(Collectors.toList());
			folderVersionListFromParent = getVersionSafeNode(token, parentPath, objMapper, folderSecretListFromParent, false);
			if (folderVersionListFromParent.size() > 0) {
				folderVersionList.addAll(folderVersionListFromParent);
			}
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			String res = mapper.writeValueAsString(folderVersionList);
			return ResponseEntity.status(HttpStatus.OK).body(res);
		} catch (JsonProcessingException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get last changed details for this folder\"]}");
		}
	}

	private List<String> getFolderSecretList(String token, String path, ObjectMapper objMapper) {
		List<String> folderList = new ArrayList<>();
 		String jsonstr = "{\"path\":\"" + path + "\"}";
		// Get the list of folders
		Response lisresp = reqProcessor.process("/sdb/list", jsonstr, token);
		if (!HttpStatus.NOT_FOUND.equals(lisresp.getHttpstatus()) && !HttpStatus.FORBIDDEN.equals(lisresp.getHttpstatus())) {
			if (!lisresp.getResponse().contains("errors")) {
				try {
					JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
					for(JsonNode node : folders){
						folderList.add(node.asText());
					}
					folderList = folderList.stream().filter(f -> !f.startsWith(TVaultConstants.VERSION_FOLDER_PREFIX)).collect(Collectors.toList());
				} catch (IOException e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "getVersionSafeNode").
							put(LogMessage.MESSAGE, String.format ("Unable to get folder list for [%s]", path)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}
			}
		}
		return folderList;
	}

	private List<FolderVersion> getVersionSafeNode(String token, String path, ObjectMapper objMapper, List<String> folderList, boolean excludeChild) {

		List<FolderVersion> folderVersionlist = new ArrayList<>();
		for (String folder: folderList) {
			String versionPath = getVersionFolderPath(path + "/" + folder);

			// get version data from version folder
			String jsonstr = "{\"path\":\"" + versionPath + "\"}";
			Response versionResp = reqProcessor.process("/read", jsonstr, token);
			FolderVersionData folderVersiondata = null;
			if (HttpStatus.OK.equals(versionResp.getHttpstatus())) {
				try {
					folderVersiondata = objMapper.readValue(versionResp.getResponse(), new TypeReference<FolderVersionData>() {});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (folderVersiondata != null) {
				if (excludeChild) {
					folderVersiondata.getData().setSecretVersions(null);
				}
				folderVersionlist.add(folderVersiondata.getData());
			}
		}
		return folderVersionlist;
	}

	/**
	 * To get version folder name from path
	 * @param path
	 * @return
	 */
	private String getVersionFolderPath(String path) {
		String versionFolderName = TVaultConstants.VERSION_FOLDER_PREFIX + path.substring(path.lastIndexOf('/') + 1);
		String verionPath = path.substring(0, path.lastIndexOf('/')) + "/" + versionFolderName;
		return verionPath;
	}
}
