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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeUser;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.utils.AuthorizationUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.SafeUtils;


@Component
public class  SelfSupportService {
	
	@Autowired
	private SafesService safesService;
	
	@Autowired
	private AuthorizationUtils authorizationUtils;
	
	@Autowired
	private SafeUtils safeUtils;
	
	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	public static final String READ_POLICY="read";
	public static final String WRITE_POLICY="write";
	public static final String DENY_POLICY="deny";
	public static final String SUDO_POLICY="sudo";
	
	private static Logger log = LogManager.getLogger(SelfSupportService.class);

	/**
	 * Creates a safe by the user with least privileges, Requires an AppRole which can perform Safe Creation 
	 * (Sufficient access to the paths such as shared or metadata/shared, etc)
	 * @param token
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String> createSafe(UserDetails userDetails, String userToken, Safe safe) {
		
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
			if (safe != null && safe.getSafeBasicDetails() != null) {
				safe.getSafeBasicDetails().setOwnerid(userDetails.getUsername());
			}
			ResponseEntity<String> safe_creation_response = safesService.createSafe(token, safe);
			if (HttpStatus.OK.equals(safe_creation_response.getStatusCode() )) {
				// Associate admin user to the safe...
				SafeUser safeUser = new SafeUser();
				safeUser.setAccess("sudo");
				safeUser.setPath(safe.getPath());
				safeUser.setUsername(userDetails.getUsername());
				safesService.addUserToSafe(token, safeUser);
			}
			return safe_creation_response;
		}
	}
	/**
	 * 
	 * @param userDetails
	 * @param userToken
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> addUserToSafe(UserDetails userDetails, String userToken, SafeUser safeUser) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.addUserToSafe(token, safeUser);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return safesService.addUserToSafe(token, safeUser);
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
			boolean isAuthorized = authorizationUtils.isAuthorized(token, safeType, safeName);
			if (isAuthorized) {
				token = userDetails.getSelfSupportToken();
				return safesService.getSafe(token, path);
			}
			else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"errors\":[\"Not authorized to get Safe information\"]}");
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
		if (userDetails.isAdmin()) {
			return safesService.removeUserFromSafe(token, safeUser);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return safesService.removeUserFromSafe(token, safeUser);
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
			String[] safes = safeUtils.getManagedSafesFromPolicies(userDetails.getPolicies(), path);
			Map<String, String[]> safesMap = new HashMap<String, String[]>();
			safesMap.put("keys", safes);
			return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safesMap));
		}
	}
}
