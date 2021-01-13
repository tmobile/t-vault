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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;


@Component
public class  SelfSupportService {
	
	@Autowired
	private SafesService safesService;
	
	@Autowired
	private PolicyUtils policyUtils;
	
	@Autowired
	private AuthorizationUtils authorizationUtils;
	
	@Autowired
	private SafeUtils safeUtils;

	@Autowired
	private AWSAuthService awsAuthService;

	@Autowired
	private AWSIAMAuthService awsiamAuthService;

	@Autowired
	private AppRoleService appRoleService;

	@Autowired
	private OIDCUtil oidcUtil;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	@Value("${safe.quota:20}")
	private String safeQuota;

	@Autowired
	private RequestProcessor reqProcessor;

	@Autowired
	private TokenUtils tokenUtils;

	@Autowired
	private DirectoryService directoryService;

	private static Logger log = LogManager.getLogger(SelfSupportService.class);
	private static final String PATHSTR = "{\"path\":\"";

	/**
	 * Creates a safe by the user with least privileges, Requires an AppRole which can perform Safe Creation 
	 * (Sufficient access to the paths such as shared or metadata/shared, etc)
	 * @param token
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String> createSafe(UserDetails userDetails, Safe safe) {
		
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.createSafe(token, safe);
		}
		else {
			// Assign the owner (Infer from logged in user?)
			// Create proper policies
			// Assign the policies
			// Modify should work the same
			// Delete safe - clean up of all items, paths, permissions, policies
			token = userDetails.getSelfSupportToken();
			if (!isSafeValid(safe)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
			}
			// check the user safe limit
			if (isSafeQuotaReached(token, userDetails.getUsername(), ControllerUtil.getSafeType(safe.getPath()), userDetails)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"You have reached the limit of number of allowed safes that can be created\"]}");
			}
			if (safe != null && safe.getSafeBasicDetails() != null) {
				safe.getSafeBasicDetails().setOwnerid(userDetails.getUsername());
			}
			ResponseEntity<String> safe_creation_response = safesService.createSafe(token, safe);
			if (HttpStatus.OK.equals(safe_creation_response.getStatusCode() )) {
				// Associate admin user to the safe...
				SafeUser safeUser = new SafeUser();
				safeUser.setAccess(TVaultConstants.SUDO_POLICY);
				safeUser.setPath(safe.getPath());
				safeUser.setUsername(userDetails.getUsername());
				safesService.addUserToSafe(token, safeUser, userDetails, true);
			}
			return safe_creation_response;
		}
	}

	/**
	 * Check whether the safe info in request is valid
	 * @param safe
	 * @return
	 */
	private boolean isSafeValid(Safe safe) {
		return safe != null && safe.getPath() != null && !safe.getPath().equals("");
	}

	/**
	 * Check whether the user safe limit reached
	 * @param token
	 * @param username
	 * @param path
	 * @return
	 */
	private boolean isSafeQuotaReached(String token, String username, String path, UserDetails userDetails) {
		String[] policies = policyUtils.getCurrentPolicies(token, username, userDetails);
		String[] safes = safeUtils.getManagedSafes(policies, path);
		if (safes.length >= Integer.parseInt(safeQuota)) {
			return true;
		}
		return false;
	}


	/**
	 * Adds a user to a safe
	 * @param userDetails
	 * @param userToken
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> addUserToSafe(UserDetails userDetails, String userToken, SafeUser safeUser) {
		boolean canAddUser = safeUtils.canAddOrRemoveUser(userDetails, safeUser, TVaultConstants.ADD_USER);
		if (canAddUser) {
			if (userDetails.isAdmin()) {
				return safesService.addUserToSafe(userDetails.getClientToken(), safeUser, userDetails, false);
			}
			else {
				return safesService.addUserToSafe(userDetails.getSelfSupportToken(), safeUser, userDetails, false);
			}
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to add users to this safe\"]}");
		}
	}
	/**
	 * Get SDB Info
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getInfo(UserDetails userDetails, String userToken, String path){
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.getInfo(token, path);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to get this safe info\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.getInfo(token, path);
		}
	}
	/**
	 * Gets safe information as power user
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getSafe(UserDetails userDetails, String userToken, String path) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.getSafe(token, path);
		}
		else {
			String safeType = ControllerUtil.getSafeType(path);
			String safeName = ControllerUtil.getSafeName(path);
			if (StringUtils.isEmpty(safeType) || StringUtils.isEmpty(safeName)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path specified\"]}");
			}
			String powerToken = userDetails.getSelfSupportToken();
			String username = userDetails.getUsername();
			Safe safeMetaData = safeUtils.getSafeMetaData(powerToken, safeType, safeName);
			String[] latestPolicies = policyUtils.getCurrentPolicies(powerToken, username, userDetails);
			ArrayList<String> policiesTobeChecked =  policyUtils.getPoliciesTobeCheked(safeType, safeName);
			boolean isAuthorized = authorizationUtils.isAuthorized(userDetails, safeMetaData, latestPolicies, policiesTobeChecked, false);
			if (isAuthorized) {
				token = userDetails.getSelfSupportToken();
				return safesService.getSafe(token, path);
			}
			else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"errors\":[\"Access denied: no permission to read this safe information\"]}");
			}
		}
	}
	/**
	 * Removes user from safe as PowerUser
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> removeUserFromSafe(UserDetails userDetails, String userToken, SafeUser safeUser) {
		String token = userDetails.getClientToken();
		boolean isAuthorized = safeUtils.canAddOrRemoveUser(userDetails, safeUser, TVaultConstants.REMOVE_USER);
		if (!isAuthorized) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"errors\":[\"Access denied: no permission to remove users from this safe\"]}");
		}
		if (userDetails.isAdmin()) {
			return safesService.removeUserFromSafe(token, safeUser, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return safesService.removeUserFromSafe(token, safeUser, userDetails);
		}
	}
	/**
	 * Read from safe Recursively
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getFoldersRecursively(UserDetails userDetails, String userToken, String path) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.getFoldersRecursively(token, path);
		}
		else {
			// List of safes based on current user
			String[] policies = policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails);
			String[] safes = safeUtils.getManagedSafes(policies, path);
			Map<String, String[]> safesMap = new HashMap<String, String[]>();
			safesMap.put("keys", safes);
			return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safesMap));
		}
	}
	/**
	 * isAuthorized
	 * @param token
	 * @param safeName
	 * @return
	 */
	public ResponseEntity<String> isAuthorized (UserDetails userDetails, String path) {
		if (!ControllerUtil.isPathValid(path)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path specified\"]}");
		}
		String safeType = ControllerUtil.getSafeType(path);
		String safeName = ControllerUtil.getSafeName(path);
		if (StringUtils.isEmpty(safeType) || StringUtils.isEmpty(safeName)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path specified\"]}");
		}
		String powerToken = userDetails.getSelfSupportToken();
		String username = userDetails.getUsername();
		Safe safeMetaData = safeUtils.getSafeMetaData(powerToken, safeType, safeName);
		if (safeMetaData == null) {
			return ResponseEntity.status(HttpStatus.OK).body("false");
		}
		String[] latestPolicies = policyUtils.getCurrentPolicies(powerToken, username, userDetails);
		ArrayList<String> policiesTobeChecked =  policyUtils.getPoliciesTobeCheked(safeType, safeName);
		boolean isAuthorized = authorizationUtils.isAuthorized(userDetails, safeMetaData, latestPolicies, policiesTobeChecked, false);
		return ResponseEntity.status(HttpStatus.OK).body(String.valueOf(isAuthorized));
	}

	/**
	 * Update a safe by the user with least privileges, Requires an AppRole which can perform Safe updation
	 * (Sufficient access to the paths such as shared or metadata/shared, etc)
	 * @param userToken
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String> updateSafe(UserDetails userDetails, String userToken, Safe safe) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.updateSafe(token, safe);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, safe.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update this safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			ResponseEntity<String> safe_creation_response = safesService.updateSafe(token, safe);
			return safe_creation_response;
		}
	}
	/**
	 * Delete a safe by the user with least privileges, Requires an AppRole which can perform Safe Deletion
	 * (Sufficient access to the paths such as shared or metadata/shared, etc)
	 * @param userDetails
	 * @param userToken
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> deletefolder(UserDetails userDetails, String userToken, String path) {
		if (userDetails.isAdmin()) {
			//Taking self service token for safe deletion by admin to avoid the read/deny restriction to admin
			return safesService.deletefolder(userDetails.getSelfSupportToken(), path, userDetails);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to delete this safe\"]}");
			}
			ResponseEntity<String> safe_creation_response = safesService.deletefolder(userDetails.getSelfSupportToken(), path,userDetails);
			return safe_creation_response;
		}
	}
	/**
	 * Adds a group to a safe
	 * @param userDetails
	 * @param userToken
	 * @param safeGroup
	 * @return
	 */
	public ResponseEntity<String> addGroupToSafe(UserDetails userDetails, String userToken, SafeGroup safeGroup) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.addGroupToSafe(token, safeGroup, userDetails);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, safeGroup.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add group to the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.addGroupToSafe(token, safeGroup, userDetails);
		}
	}
	/**
	 * Removes a group from safe
	 * @param userDetails
	 * @param userToken
	 * @param safeGroup
	 * @return
	 */
	public ResponseEntity<String> removeGroupFromSafe(UserDetails userDetails, String userToken, SafeGroup safeGroup) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.removeGroupFromSafe(token, safeGroup, userDetails);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, safeGroup.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove group from the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.removeGroupFromSafe(token, safeGroup, userDetails);
		}
	}

	/**
	 * Add AWS role to safe
	 * @param userDetails
	 * @param userToken
	 * @param awsRole
	 * @return
	 */
	public ResponseEntity<String> addAwsRoleToSafe(UserDetails userDetails, String userToken, AWSRole awsRole) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.addAwsRoleToSafe(token, awsRole);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, awsRole.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add AWS role to the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.addAwsRoleToSafe(token, awsRole);
		}
	}

	/**
	 * Remove or detach AWS role from safe
	 * @param userDetails
	 * @param userToken
	 * @param awsRole
	 * @param detachOnly
	 * @return
	 */
	public ResponseEntity<String> removeAWSRoleFromSafe(UserDetails userDetails, String userToken, AWSRole awsRole, boolean detachOnly) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.removeAWSRoleFromSafe(token, awsRole, detachOnly, userDetails);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, awsRole.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Either AWS role doesn't exist or you don't have enough permission to remove this AWS role from Safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.removeAWSRoleFromSafe(token, awsRole, detachOnly, userDetails);
		}
	}

	/**
	 * Associate approle to safe
	 * @param userDetails
	 * @param token
	 * @param jsonstr
	 * @return
	 */
	public ResponseEntity<String> associateApproletoSDB(UserDetails userDetails, String userToken, SafeAppRoleAccess safeAppRoleAccess) {
		String jsonstr = JSONUtil.getJSON(safeAppRoleAccess);
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.associateApproletoSDB(token, safeAppRoleAccess);
		}
		else {
			Map<String,Object> requestMap = ControllerUtil.parseJson(jsonstr);
			if(!ControllerUtil.areSafeAppRoleInputsValid(requestMap)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
			}
			String path = requestMap.get("path").toString();
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add Approle to the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.associateApproletoSDB(token, safeAppRoleAccess);
		}
	}

	/**
	 * Delete app role from safe
	 * @param userDetails
	 * @param userToken
	 * @param jsonstr
	 * @return
	 */
	public ResponseEntity<String> deleteApproleFromSDB(UserDetails userDetails, String userToken, SafeAppRoleAccess safeAppRoleAccess) {
//		if (TVaultConstants.SELF_SERVICE_APPROLE_NAME.equals(safeAppRoleAccess.getRole_name())) {
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to delete this approle\"]}");
//		}
		if (Arrays.asList(TVaultConstants.MASTER_APPROLES).contains(safeAppRoleAccess.getRole_name())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: no permission to delete this approle\"]}");
		}
		String jsonstr = JSONUtil.getJSON(safeAppRoleAccess);
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.removeApproleFromSafe(token, jsonstr);
		}
		else {
			Map<String,Object> requestMap = ControllerUtil.parseJson(jsonstr);
			if (ObjectUtils.isEmpty(requestMap.get("role_name")) || ObjectUtils.isEmpty(requestMap.get("path"))) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid role name or path\"]}");
			}
			String path = requestMap.get("path").toString();
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove approle from the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.removeApproleFromSafe(token, jsonstr);
		}
	}

	/**
	 * Create aws role
	 * @param userDetails
	 * @param userToken
	 * @param awsLoginRole
	 * @param path
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createRole(UserDetails userDetails, String userToken, AWSLoginRole awsLoginRole, String path) throws TVaultValidationException {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return awsAuthService.createRole(token, awsLoginRole, userDetails);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to create AWS role\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return awsAuthService.createRole(token, awsLoginRole, userDetails);
		}
	}

	/**
	 * Update aws role
	 * @param userDetails
	 * @param userToken
	 * @param awsLoginRole
	 * @param path
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> updateRole(UserDetails userDetails, String userToken, AWSLoginRole awsLoginRole, String path) throws TVaultValidationException {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return awsAuthService.updateRole(token, awsLoginRole);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update AWS role\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return awsAuthService.updateRole(token, awsLoginRole);
		}
	}

	/**
	 * Create aws iam role
	 * @param userDetails
	 * @param userToken
	 * @param awsiamRole
	 * @param path
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createIAMRole(UserDetails userDetails, String userToken, AWSIAMRole awsiamRole, String path) throws TVaultValidationException {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return awsiamAuthService.createIAMRole(awsiamRole, token, userDetails);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to create AWS IAM role\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return awsiamAuthService.createIAMRole(awsiamRole, token, userDetails);
		}
	}

	/**
	 * Update aws iam role
	 * @param userDetails
	 * @param userToken
	 * @param awsiamRole
	 * @param path
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> updateIAMRole(UserDetails userDetails, String userToken, AWSIAMRole awsiamRole, String path) throws TVaultValidationException {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return awsiamAuthService.updateIAMRole(token, awsiamRole);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update AWS IAM role\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return awsiamAuthService.updateIAMRole(token, awsiamRole);
		}
	}
	/**
	 * Gets the map of all existing safe names
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getAllSafeNames(UserDetails userDetails) {
		HashMap<String, List<String>> safeNames = ControllerUtil.getAllExistingSafeNames(userDetails.getSelfSupportToken());
		if (MapUtils.isEmpty(safeNames)) {
			return ResponseEntity.status(HttpStatus.OK).body("No safes are available");
		}
		else {
			return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safeNames));
		}
	}

	/**
	 * Create AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> createAppRole(String userToken, AppRole appRole, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return appRoleService.createAppRole(token, appRole, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return appRoleService.createAppRole(token, appRole, userDetails);
		}
	}

	/**
	 * Delete AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> deleteAppRole(String userToken, AppRole appRole, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return appRoleService.deleteAppRole(token, appRole, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return appRoleService.deleteAppRole(token, appRole, userDetails);
		}
	}
	
	/**
	 * Get safes having read/write permission
	 * @param userDetails
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getSafes(UserDetails userDetails, String userToken) {
		oidcUtil.renewUserToken(userDetails.getClientToken());
		String token = userDetails.getClientToken();
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);

		policies = filterPoliciesBasedOnPrecedence(Arrays.asList(policies));

		List<Map<String, String>> safeListUsers = new ArrayList<>();
		List<Map<String, String>> safeListShared = new ArrayList<>();
		List<Map<String, String>> safeListApps = new ArrayList<>();
		Map<String, List<Map<String, String>>> safeList = new HashMap<>();
		if (policies != null) {
			for (String policy: policies) {
				Map<String, String> safePolicy = new HashMap<>();
				String[] _policies = policy.split("_", -1);
				if (_policies.length >= 3) {
					String[] policyName = Arrays.copyOfRange(_policies, 2, _policies.length);
					String safeName = String.join("_", policyName);
					String safeType = _policies[1];

					if (policy.startsWith("r_")) {
						safePolicy.put(safeName, "read");
					} else if (policy.startsWith("w_")) {
						safePolicy.put(safeName, "write");
					}
					else if (policy.startsWith("d_")) {
						safePolicy.put(safeName, "deny");
					}
					if (!safePolicy.isEmpty()) {
						if (safeType.equals(TVaultConstants.USERS)) {
							safeListUsers.add(safePolicy);
						} else if (safeType.equals(TVaultConstants.SHARED)) {
							safeListShared.add(safePolicy);
						} else if (safeType.equals(TVaultConstants.APPS)) {
							safeListApps.add(safePolicy);
						}
					}
				}
			}
			safeList.put(TVaultConstants.USERS, safeListUsers);
			safeList.put(TVaultConstants.SHARED, safeListShared);
			safeList.put(TVaultConstants.APPS, safeListApps);
		}
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safeList));
	}

	/**
	 * Filter safe policies based on policy precedence.
	 * @param policies
	 * @return
	 */
	private String [] filterPoliciesBasedOnPrecedence(List<String> policies) {
		List<String> filteredList = new ArrayList<>();
		for (int i = 0; i < policies.size(); i++ ) {
			String policyName = policies.get(i);
			String[] _policy = policyName.split("_", -1);
			if (_policy.length >= 3) {
				String itemName = policyName.substring(1);
				List<String> matchingPolicies = filteredList.stream().filter(p->p.substring(1).equals(itemName)).collect(Collectors.toList());
				if (!matchingPolicies.isEmpty()) {
					/* deny has highest priority. Read and write are additive in nature
						Removing all matching as there might be duplicate policies from user and groups
					*/
					if (policyName.startsWith("d_") || (policyName.startsWith("w_") && !matchingPolicies.stream().anyMatch(p-> p.equals("d"+itemName)))) {
						filteredList.removeAll(matchingPolicies);
						filteredList.add(policyName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("d"+itemName))) {
						// policy is read and deny already in the list. Then deny has precedence.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("d"+itemName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("w"+itemName))) {
						// policy is read and write already in the list. Then write has precedence.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("w"+itemName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("r"+itemName)) || matchingPolicies.stream().anyMatch(p-> p.equals("s"+itemName))) {
						// policy is read and read already in the list. Then remove all duplicates read and add single read permission for that safe.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("r"+itemName);
					}
				}
				else {
					filteredList.add(policyName);
				}
			}
		}
		return filteredList.toArray(new String[0]);
	}

	/**
	 * Read all approle names
	 * @param userToken
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRoles(String userToken, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return appRoleService.readAppRoles(token);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return appRoleService.readAppRoles(token);
		}
	}
	
	/**
	 * List all approle names
	 * @param userToken
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> listAppRoles(String userToken, UserDetails userDetails) {
		return appRoleService.listAppRoles(userToken, userDetails);
	}

	/**
	 * Read approle info
	 * @param userToken
	 * @param rolename
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRole(String userToken, String rolename, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return appRoleService.readAppRole(token, rolename);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return appRoleService.readAppRole(token, rolename);
		}
	}
	/**
	 * Read role_id for a given AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRoleRoleId(String userToken, String rolename, UserDetails userDetails) {
		return appRoleService.readAppRoleRoleId(userToken, rolename, userDetails);
	}
	/**
	 * Read secret_id for a given AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRoleSecretId(String userToken, String rolename, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		return appRoleService.readAppRoleSecretId(token, rolename, userDetails);
	}
	/**
	 * Read approle details
	 * @param userToken
	 * @param rolename
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRoleDetails(String userToken, String rolename, UserDetails userDetails) {
		return appRoleService.readAppRoleDetails(userToken, rolename, userDetails);
	}
	/**
	 * To get/read Accessors of all SecretIds issued against an AppRole
	 * @param userToken
	 * @param rolename
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readSecretIdAccessors(String userToken, String rolename, UserDetails userDetails) {
		return appRoleService.readSecretIdAccessors(userToken, rolename, userDetails);
	}
	/**
	 * Delete SecretIds for a given AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> deleteSecretIds(String userToken, AppRoleAccessorIds appRoleAccessorIds, UserDetails userDetails) {
		return appRoleService.deleteSecretIds(userToken, appRoleAccessorIds, userDetails);
	}
	/**
	 * Update AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> updateAppRole(String userToken, AppRole appRole, UserDetails userDetails) {
		return appRoleService.updateAppRole(userToken, appRole, userDetails);
	}

	/**
	 * Transfer safe ownership to a user.
	 * @param token
	 * @param safeTransferRequest
	 * @return
	 */
	public ResponseEntity<String> transferSafe(String token, SafeTransferRequest safeTransferRequest, UserDetails userDetails) {

		String powerToken = token;
		if (!userDetails.isAdmin()) {
			powerToken = tokenUtils.getSelfServiceToken();
		}
		String path = safeTransferRequest.getSafeType() + '/' + safeTransferRequest.getSafeName();

		//get current owner NT id
		Safe safeMetaData = safeUtils.getSafeMetaData(powerToken, safeTransferRequest.getSafeType(), safeTransferRequest.getSafeName());
		if (safeMetaData == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Either Safe doesn't exist or you don't have enough permission to access this safe\"]}");
		}
		String currentOwnerNtid = safeMetaData.getSafeBasicDetails().getOwnerid();

		if (userDetails.isAdmin() || (currentOwnerNtid != null && currentOwnerNtid.equalsIgnoreCase(userDetails.getUsername()))) {
			String newOwnerEmail = safeTransferRequest.getNewOwnerEmail();

			String newOwnerNtid = directoryService.getNtidForUser(newOwnerEmail);

			if (StringUtils.isEmpty(newOwnerNtid)) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "transferSafe").
						put(LogMessage.MESSAGE, String.format("Failed to get NTid for [%s]", newOwnerEmail)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid email provided for new owner\"]}");
			}

			if (newOwnerNtid.equalsIgnoreCase(currentOwnerNtid)) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "transferSafe").
						put(LogMessage.MESSAGE, String.format("New owner email id [%s] should not be same as current owner email id [%s]", newOwnerEmail, currentOwnerNtid)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"New owner email id should not be same as current owner email id\"]}");
			}

			boolean hasCurrentOwner = true;
			if (StringUtils.isEmpty(currentOwnerNtid) || TVaultConstants.NULL_STRING.equalsIgnoreCase(currentOwnerNtid) || currentOwnerNtid.equalsIgnoreCase(TVaultConstants.APPROLE)) {
				hasCurrentOwner = false;
			}
			if (hasCurrentOwner) {
				// remove current owner sudo permission from safe
				SafeUser safeUser = new SafeUser(path, currentOwnerNtid, TVaultConstants.SUDO_POLICY);
				ResponseEntity<String> removeUserResponse = removeSudoUserFromSafe(powerToken, safeUser, userDetails);
				if (!HttpStatus.OK.equals(removeUserResponse.getStatusCode())) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "transferSafe").
							put(LogMessage.MESSAGE, String.format("Failed to remove current owner [%s] from safe [%s]", currentOwnerNtid, path)).
							put(LogMessage.STATUS, removeUserResponse.getStatusCode().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove current owner from safe\"]}");
				}
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "transferSafe").
						put(LogMessage.MESSAGE, String.format("Removed current owner [%s] from safe [%s]", currentOwnerNtid, path)).
						put(LogMessage.STATUS, removeUserResponse.getStatusCode().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			} else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "transferSafe").
						put(LogMessage.MESSAGE, String.format("Current owner NTid not available in safe metadata for [%s]", path)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}

			// add sudo permission to new user to this safe
			SafeUser safeUser = new SafeUser(path, newOwnerNtid, TVaultConstants.SUDO_POLICY);
			ResponseEntity<String> addUserResponse = safesService.addUserToSafe(powerToken, safeUser, userDetails, true);
			if (!HttpStatus.OK.equals(addUserResponse.getStatusCode())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "transferSafe").
						put(LogMessage.MESSAGE, String.format("Failed to add new owner [%s] to safe [%s]", newOwnerNtid, path)).
						put(LogMessage.STATUS, addUserResponse.getStatusCode().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to add new owner to the safe\"]}");
			}
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "transferSafe").
					put(LogMessage.MESSAGE, String.format("New owner added to the safe [%s]", path)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));


			// Update metadata with new owner information
			String safeMetadataPath = "metadata/" + path;
			Response response = reqProcessor.process("/read", "{\"path\":\"" + safeMetadataPath + "\"}", powerToken);
			Map<String, Object> responseMap = null;
			if (HttpStatus.OK.equals(response.getHttpstatus())) {
				responseMap = ControllerUtil.parseJson(response.getResponse());
				if (responseMap.isEmpty()) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error fetching safe metadata\"]}");
				}

				responseMap.put("path", safeMetadataPath);
				((Map<String, Object>) responseMap.get("data")).put("ownerid", newOwnerNtid);
				((Map<String, Object>) responseMap.get("data")).put("owner", newOwnerEmail);

				String metadataJson = ControllerUtil.convetToJson(responseMap);
				response = reqProcessor.process("/sdb/update", metadataJson, powerToken);
				if (response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "transferSafe").
							put(LogMessage.MESSAGE, "Safe transfer successful").
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe transfer successful \"]}");
				} else {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "transferSafe").
							put(LogMessage.MESSAGE, "Safe transfer failed").
							put(LogMessage.RESPONSE, response.getResponse()).
							put(LogMessage.STATUS, response.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Safe transfer failed\"]}");
				}
			}
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Safe transfer failed. Error fetching safe metadata\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "transferSafe").
					put(LogMessage.MESSAGE, String.format("Access denied to transfer this safe [%s]", path)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: No permission to transfer this safe. Only Owner and admin users can transfer safes\"]}");
		}
	}

	/**
	 * Removes an sudo user from Safe
	 * @param token
	 * @param safeUser
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> removeSudoUserFromSafe(String token, SafeUser safeUser, UserDetails userDetails) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		ObjectMapper objMapper = new ObjectMapper();
		String userName = safeUser.getUsername();
		userName = (userName != null) ? userName.toLowerCase() : userName;
		if (StringUtils.isEmpty(userName)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"User configuration failed. Invalid user\"]}");
		}
		String path = safeUser.getPath();
		if (ControllerUtil.isValidSafePath(path)) {
			String sudoPolicy = "s_" + ControllerUtil.getSafeType(path) + "_" + ControllerUtil.getSafeName(path);
			String readPolicy = "r_" + ControllerUtil.getSafeType(path) + "_" + ControllerUtil.getSafeName(path);
			String writePolicy = "w_" + ControllerUtil.getSafeType(path) + "_" + ControllerUtil.getSafeName(path);
			String denyPolicy = "d_" + ControllerUtil.getSafeType(path) + "_" + ControllerUtil.getSafeName(path);

			Response userResponse = new Response();
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/userpass/read", "{\"username\":\"" + userName + "\"}",
						token);
			} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				userResponse = reqProcessor.process("/auth/ldap/users", "{\"username\":\"" + userName + "\"}", token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				// OIDC implementation changes
				ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, userDetails, true);
				if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
				}
				oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
				oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
				userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
				userResponse.setHttpstatus(responseEntity.getStatusCode());
			}
			String responseJson = "";
			String groups = "";
			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if (HttpStatus.OK.equals(userResponse.getHttpstatus())) {
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
				policies.remove(sudoPolicy);

				String policiesString = StringUtils.join(policies, ",");
				String currentpoliciesString = StringUtils.join(currentpolicies, ",");

				Response ldapConfigresponse = new Response();
				if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
					ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, token);
				} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, token);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					// OIDC Implementation : Entity Update
					try {
						ldapConfigresponse = oidcUtil.updateOIDCEntity(policies,
								oidcEntityResponse.getEntityName());
						oidcUtil.renewUserToken(userDetails.getClientToken());
					} catch (Exception e) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "removeSudoUserFromSafe")
								.put(LogMessage.MESSAGE, String.format("Exception while updating the identity for user [%s", userName))
								.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					}

				}
				if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					// Updating metadata only when there is no read/write/deny permission exists to current owner
					if (currentpolicies.contains(readPolicy) || currentpolicies.contains(writePolicy) || currentpolicies.contains(denyPolicy)) {
						log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "removeSudoUserFromSafe")
								.put(LogMessage.MESSAGE, String.format("Successfully removed user[%s] from [%s]. No metadata update is required as metadata is updated with read/write/deny permission", safeUser.getUsername(), safeUser.getPath()))
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");
					}
					Map<String, String> params = new HashMap<>();
					params.put("type", "users");
					params.put("name", userName);
					params.put("path", path);
					params.put("access", TVaultConstants.DELETE);
					Response metadataResponse = ControllerUtil.updateMetadata(params, token);
					if (metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
						log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "removeSudoUserFromSafe")
								.put(LogMessage.MESSAGE, String.format("Successfully removed user[%s] from [%s]", safeUser.getUsername(), safeUser.getPath()))
								.put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString())
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");
					} else {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "removeSudoUserFromSafe")
								.put(LogMessage.MESSAGE, "Meta data update failed")
								.put(LogMessage.STATUS, metadataResponse != null ? metadataResponse.getHttpstatus().toString() : "")
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
						if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
							ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,
									currentpoliciesString, token);
						} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString,
									groups, token);
						} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
							// OIDC changes
							try {
								ldapConfigresponse = oidcUtil.updateOIDCEntity(currentpolicies,
										oidcEntityResponse.getEntityName());
								oidcUtil.renewUserToken(userDetails.getClientToken());
							} catch (Exception e2) {
								log.error(e2);
								log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
										.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
										.put(LogMessage.ACTION, "removeSudoUserFromSafe")
										.put(LogMessage.MESSAGE, "Exception while updating the identity")
										.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
										.build()));
							}
						}
						if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, "removeSudoUserFromSafe")
									.put(LogMessage.MESSAGE, "Reverting user policy update")
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");
						} else {
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, "removeSudoUserFromSafe")
									.put(LogMessage.MESSAGE, "Reverting user policy update failed")
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed. Reverting user policy updation failed\"]}");
						}
					}
				}
				else {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");
				}
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid user\"]}");
			}
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
		}
	}
	
	/**
	 * List AWS and EC2 Roles
	 * @param token
	 * @param userDetails
	 * @return
	 */	
	public ResponseEntity<String> listRoles(String token, UserDetails userDetails){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "listRoles").
			      put(LogMessage.MESSAGE, "Trying to get list of AWS roles").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		String path = TVaultConstants.AWS_USERS_METADATA_MOUNT_PATH + "/" + userDetails.getUsername();
		Response response = new Response();
		if (userDetails.isAdmin()) {
			return awsAuthService.listRoles(token);
		}
		else {
			response = reqProcessor.process("/auth/aws/rolesbyuser/list",PATHSTR+path+"\"}",userDetails.getSelfSupportToken());
		}
		
		if (response!=null && HttpStatus.OK.equals(response.getHttpstatus())) {			
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else if (response!=null && HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"keys\":[]}");
		}		
		return ResponseEntity.status(response==null?null:response.getHttpstatus()).body(response==null?null:response.getResponse());	
	}
	/**
	 * Create AWS EC2 role
	 * @param userDetails
	 * @param userToken
	 * @param awsLoginRole
	 * @return
	 * @throws TVaultValidationException
	 */
	
	public ResponseEntity<String> createAwsec2Role(UserDetails userDetails, String token, AWSLoginRole awsLoginRole) throws TVaultValidationException {
		String accesstoken;
		if (userDetails.isAdmin()) {
			 accesstoken = userDetails.getClientToken();
		}
		else {
			accesstoken = userDetails.getSelfSupportToken();
		}
		return awsAuthService.createRole(accesstoken, awsLoginRole, userDetails);
	}
	/**
	 * Create AWS IAM role
	 * @param userDetails
	 * @param userToken
	 * @param awsLoginRole
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createAwsiamRole(UserDetails userDetails, String token, AWSIAMRole awsiamRole) throws TVaultValidationException {
		String accesstoken;
		if (userDetails.isAdmin()) {
			accesstoken = userDetails.getClientToken();
		}
		else {
			accesstoken = userDetails.getSelfSupportToken();
		}
		return awsiamAuthService.createIAMRole(awsiamRole, accesstoken, userDetails);
	}
}
