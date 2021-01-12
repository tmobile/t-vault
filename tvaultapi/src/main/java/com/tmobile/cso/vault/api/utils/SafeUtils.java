/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */

package com.tmobile.cso.vault.api.utils;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.Response;
@Component
public class SafeUtils {
	private Logger log = LogManager.getLogger(SafeUtils.class);

	@Autowired
	private RequestProcessor reqProcessor;

	public SafeUtils() {
		// empty constructor
	}

	/**
	 * Gets the admin/sudo policies for the given user...
	 * @param policiesNode
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public List<String> getPoliciesForManagedSafes(JsonNode policiesNode) throws JsonProcessingException, IOException {
		List<String> adminPolicies = new ArrayList<>();
		if ((!ObjectUtils.isEmpty(policiesNode)) && (policiesNode.isContainerNode())) {
			// Policies is supposed to be a container node.			
				Iterator<JsonNode> elementsIterator = policiesNode.elements();
			       while (elementsIterator.hasNext()) {
			    	   JsonNode element = elementsIterator.next();
			    	   String policy = element.asText();
			    	   if (!StringUtils.isEmpty(policy) && policy.startsWith("s_")) {
			    		   adminPolicies.add(element.asText());
			    	   }
			       }
			
		}
		return adminPolicies;
	}
	/**
	 * Gets the list of safes from policies for a given safeType
	 * @param policies
	 * @param safeType
	 * @return
	 */
	public String[] getManagedSafes(String[] policies, String safeType) {
		List<String> safes = new ArrayList<>();
		if (policies != null) {
			for (String policy: policies) {
				if (policy.startsWith("s_")) {
					String[] _policies = policy.split("_");
					if (_policies[1].equals(safeType)) {
						String [] safeNameArray = Arrays.copyOfRange(_policies, 2, _policies.length);
						String safeName = StringUtils.arrayToDelimitedString(safeNameArray, "_");
						safes.add(safeName);
					}
				}
			}
		}
		return safes.toArray(new String[safes.size()]);
	}
	/**
	 * Checks whether a user can be added
	 * @param userDetails
	 * @param safeUser
	 * @return
	 */
	public boolean canAddOrRemoveUser(UserDetails userDetails, SafeUser safeUser, String action) {
		String token = userDetails.getSelfSupportToken();
		String path = safeUser.getPath();
		String safeType = ControllerUtil.getSafeType(path);
		String safeName = ControllerUtil.getSafeName(path);
		
		if (StringUtils.isEmpty(safeType) || StringUtils.isEmpty(safeName)) {
			return false;
		}
		if (userDetails.isAdmin()) {
			token = userDetails.getClientToken();
		}
		Safe safeMetaData = getSafeMetaData(token, safeType, safeName);
		if (safeMetaData == null) {
			return false;
		}
		String safeOwnerid = safeMetaData.getSafeBasicDetails().getOwnerid();		
		
		if (userDetails.isAdmin()) {			
			boolean isAdmin = checkForAdmin(safeOwnerid,safeUser,action);			
			return isAdmin;
		}
		else {			
			boolean isNonAdmin = checkForNonAdmin(safeOwnerid,safeUser,action,userDetails);			
			return isNonAdmin;			
		}
	}
	/**
	 * Gets the metadata associated with a given safe, requires an AppRole token which can perform this operation
	 * or token which has safeadmin capabilities
	 * @param token
	 * @param safeType
	 * @param safeName
	 * @return
	 */
	public Safe getSafeMetaData(String token, String safeType, String safeName){
		String metaDataPath = "metadata/" + safeType + '/' + safeName;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "Get Info").
				put(LogMessage.MESSAGE, String.format ("Trying to get Info for [%s]", metaDataPath)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		// Elevation is required in case user does not have access to the path.
		Response response = ControllerUtil.getReqProcessor().process("/sdb","{\"path\":\""+metaDataPath+"\"}",token);
		// Create the Safe bean
		Safe safe = null;
		if(response != null && HttpStatus.OK.equals(response.getHttpstatus())){
			try {
				ObjectMapper objMapper = new ObjectMapper();
				JsonNode dataNode = objMapper.readTree(response.getResponse()).get("data");
				safe = getSafeInfo(dataNode);
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						  put(LogMessage.ACTION, "getSafeMetaDataWithAppRoleElevation").
					      put(LogMessage.MESSAGE, "Error while trying to get details about the safe").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					      build()));			
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, "Get Info").
				put(LogMessage.MESSAGE, "Getting Info completed").
				put(LogMessage.STATUS, response != null ? response.getHttpstatus().toString() : "").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		return safe;
	}
	/**
	 * Gets 
	 * @param response
	 * @return
	 */
	public Safe getSafeInfo (JsonNode dataNode) {
		Safe safe = new Safe();
		SafeBasicDetails safeBasicDetails = new SafeBasicDetails();
		safe.setSafeBasicDetails(safeBasicDetails);
		safe.getSafeBasicDetails().setName(dataNode.get("name").asText());
		safe.getSafeBasicDetails().setDescription(dataNode.get("description").asText());
		safe.getSafeBasicDetails().setOwner(dataNode.get("owner").asText());
		String applicationName=null;
		if (null != dataNode.get("appName")) {
			applicationName = dataNode.get("appName").asText();
		}
		safe.getSafeBasicDetails().setAppName(applicationName);
		String apptag=null;
		if (null != dataNode.get("applicationTag")) {
			apptag = dataNode.get("applicationTag").asText();
		}
		safe.getSafeBasicDetails().setApplicationTag(apptag);
		String ownerId = null;
		if (null != dataNode.get("ownerid")) {
			ownerId = dataNode.get("ownerid").asText();
		}
		safe.getSafeBasicDetails().setOwnerid(ownerId);
		return safe;
	}
	
	private boolean checkForAdmin(String safeOwnerid,SafeUser safeUser,String action) {
		if (StringUtils.isEmpty(safeOwnerid)) {
			// Null or empty user for owner
			// Existing safes will not have ownerid
			// Safes created by safeadmins will not have ownerid
			return true;
		}
		else {
			// There is some owner assigned to the safe
			if (safeOwnerid.equalsIgnoreCase(safeUser.getUsername())) {
				// Safeadmin is trying to add the owner of the safe as some user with some permission
				// Safeadmin can add read or write permission to safeowner
				if (TVaultConstants.READ_POLICY.equals(safeUser.getAccess()) || TVaultConstants.WRITE_POLICY.equals(safeUser.getAccess()) || (null==safeUser.getAccess() && action.equals(TVaultConstants.REMOVE_USER))) {
					// safeadmin or the safeowner himself can set read/write permission to the safeowner
					return true;
				}
				return false;
			}
			else {
				// Safeadmin is trying to add a user, who is non-owner of the safe with read/write/deny
				return true;
			}
		}
	}
	
	private boolean checkForNonAdmin(String safeOwnerid,SafeUser safeUser,String action, UserDetails userDetails) {
		// Prevent the owner of the safe to be denied...
					if (userDetails.getUsername() != null && userDetails.getUsername().equalsIgnoreCase(safeOwnerid)) {
						// This user is owner of the safe...
						if (safeUser.getUsername().equalsIgnoreCase(safeOwnerid)) {
							if (TVaultConstants.READ_POLICY.equals(safeUser.getAccess()) || TVaultConstants.WRITE_POLICY.equals(safeUser.getAccess()) || (null==safeUser.getAccess() && action.equals(TVaultConstants.REMOVE_USER))) {
								// safeowner himself can set read/write permission to the safeowner
								return true;
							}
							return false;
						}
						return true;
					}
					// other normal users will not have permission as they are not the owner
					return false;
	}

	/**
	 * To create version folder on a folder creation in safe
	 * @param token
	 * @param path
	 * @param userDetails
	 * @param isPartOfFolderCreation
	 * @return
	 */
	public Response createVersionFolder(String token, String path, UserDetails userDetails, boolean isPartOfFolderCreation, List<String> modifiedKeys) {
		if (ControllerUtil.isPathValid(path)) {
			String actualFolderName = path.substring(path.lastIndexOf('/') + 1);
			String versionFolderName = TVaultConstants.VERSION_FOLDER_PREFIX + actualFolderName;
			String versionFolderPath = path.substring(0, path.lastIndexOf('/')) + "/" + versionFolderName;

			Response readVersionFolderResponse = reqProcessor.process("/read","{\"path\":\""+versionFolderPath+"\"}",token);
			FolderVersionData currentVersionData = null;
			if (readVersionFolderResponse.getHttpstatus().equals(HttpStatus.OK)) {
				ObjectMapper objectMapper = new ObjectMapper();
				try {
					currentVersionData = objectMapper.readValue(readVersionFolderResponse.getResponse(), new TypeReference<FolderVersionData>() {});
				} catch (IOException e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "createVersionFolder").
							put(LogMessage.MESSAGE, String.format("Failed to extract Folder version info for [%s}", path)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
			}

			FolderVersion folderVersionData = getFolderVersionData(path, userDetails, isPartOfFolderCreation, currentVersionData!=null?currentVersionData.getData():null, modifiedKeys);

			String versionJsonStr ="{\"path\":\""+versionFolderPath +"\",\"data\":"+ JSONUtil.getJSON(folderVersionData)+"}";
			if (!readVersionFolderResponse.getHttpstatus().equals(HttpStatus.OK)) {
				// No version folder exists. Creating one
				log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "createVersionFolder").
						put(LogMessage.MESSAGE, String.format("Creating version folder for [%s}", path)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return reqProcessor.process("/sdb/createfolder",versionJsonStr,token);
			}
			else {
				// Version folder already exists. Writing to it.
				log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "createVersionFolder").
						put(LogMessage.MESSAGE, String.format("Version folder exists for [%s}. Adding details to existing version folder", path)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return reqProcessor.process("/write",versionJsonStr,token);
			}
		}
		Response failedResponse = new Response();
		failedResponse.setResponse("{\"errors\":[\"Invalid path\"]}");
		return failedResponse;
	}

	/**
	 * To get folder version information
	 * @param path
	 * @param userDetails
	 * @param isPartOfFolderCreation
	 * @return
	 */
	private FolderVersion getFolderVersionData(String path, UserDetails userDetails, boolean isPartOfFolderCreation, FolderVersion currentFolderVersionData, List<String> modifiedKeys) {
		Long modifiedAt = new Date().getTime();
		String modifiedBy = userDetails.getEmail();
		FolderVersion folderVersionData = new FolderVersion();
		folderVersionData.setFolderPath(path);
		folderVersionData.setFolderModifiedAt(modifiedAt);
		folderVersionData.setFolderModifiedBy(modifiedBy);

		if (!isPartOfFolderCreation) {
			// if part of folder creation, there might exist version info for this folder
			List<SecretVersionDetails> newSecretVersionDetails = new ArrayList<>();
			newSecretVersionDetails.add(new SecretVersionDetails(modifiedAt, modifiedBy));

			Map<String, List<SecretVersionDetails>> currentSecretVersionDetails = new HashMap<>();
			if (currentFolderVersionData != null && currentFolderVersionData.getSecretVersions() != null) {
				currentSecretVersionDetails = currentFolderVersionData.getSecretVersions();
			}

			for (int i=0; i< modifiedKeys.size(); i++) {
				currentSecretVersionDetails.put(modifiedKeys.get(i), newSecretVersionDetails);
			}
			folderVersionData.setSecretVersions(currentSecretVersionDetails);
		}
		return folderVersionData;
	}
}
