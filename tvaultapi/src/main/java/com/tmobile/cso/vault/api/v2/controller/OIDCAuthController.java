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

package com.tmobile.cso.vault.api.v2.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.tmobile.cso.vault.api.model.*;
import io.swagger.annotations.ApiOperation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tmobile.cso.vault.api.service.OIDCAuthService;

import io.swagger.annotations.Api;

@RestController
@CrossOrigin
@Api( description = "OIDC Authentication Controller", position = 15)
public class OIDCAuthController {
	
	@Autowired
	private OIDCAuthService oidcAuthService;
	
	/**
	 * Get Authentication Mounts
	 * @param request
	 * @param token
	 * @return
	 */
	@GetMapping(value = "/v2/sys/list", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.getAuthenticationMounts.value}", notes = "${OIDCAuthController.getAuthenticationMounts.notes}")
	public ResponseEntity<String> getAuthenticationMounts(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token) {
		return oidcAuthService.getAuthenticationMounts(token);
	}
	
	/**
	 * Entity lookup from identity engine
	 * @param request
	 * @param token
	 * @param oidcLookupEntityRequest
	 * @return
	 */
	@PostMapping(value = "/v2/identity/lookup/entity", produces = "application/json")
	@ApiOperation(hidden = true,  value = "${OIDCAuthController.entityLookUp.value}", notes = "${OIDCAuthController.entityLookUp.notes}")
	public ResponseEntity<OIDCEntityResponse> entityLookUp(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token,
			@Valid @RequestBody OIDCLookupEntityRequest oidcLookupEntityRequest) {
		return oidcAuthService.entityLookUp(token, oidcLookupEntityRequest);
	}
	
	/**
	 * Create Group Entity LookUp
	 * @param request
	 * @param token
	 * @param oidcLookupEntityRequest
	 * @return
	 */
	@PostMapping(value = "/v2/identity/lookup/group", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.groupEntityLookUp.value}", notes = "${OIDCAuthController.groupEntityLookUp.notes}")
	public ResponseEntity<String> groupEntityLookUp(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token,
			@Valid @RequestBody OIDCLookupEntityRequest oidcLookupEntityRequest) {
		return oidcAuthService.groupEntityLookUp(token, oidcLookupEntityRequest);
	}
	
	/**
	 * Read Entity Alias By ID
	 * @param request
	 * @param token
	 * @param id
	 * @return
	 */
	@GetMapping(value = "/v2/identity/entity-alias/id/{id}", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.readEntityAliasById.value}", notes = "${OIDCAuthController.readEntityAliasById.notes}")
	public ResponseEntity<String> readEntityAliasById(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("id") String id) {
		return oidcAuthService.readEntityAliasById(token,id);
	}
	
	/**
	 * Read Entity By Name
	 * @param request
	 * @param token
	 * @param entityName
	 * @return
	 */
	@GetMapping(value = "/v2/identity/entity/name/{name}", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.readEntityByName.value}", notes = "${OIDCAuthController.readEntityByName.notes}")
	public ResponseEntity<String> readEntityByName(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("name") String entityName) {
		return oidcAuthService.readEntityByName(token,entityName);
	}
	
	/**
	 * Update Entity by Name
	 * @param request
	 * @param token
	 * @param entityName
	 * @param oidcEntityRequest
	 * @return
	 */
	@PostMapping(value = "/v2/identity/entity/name/{name}", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.updateEntityByName.value}", notes = "${OIDCAuthController.updateEntityByName.notes}")
	public ResponseEntity<String> updateEntityByName(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("name") String entityName,
			@Valid @RequestBody OIDCEntityRequest oidcEntityRequest) {
		return oidcAuthService.updateEntityByName(token, oidcEntityRequest);
	}
	
	/**
	 * Read Group Alias By ID
	 * @param request
	 * @param token
	 * @param id
	 * @return
	 */
	@GetMapping(value = "/v2/identity/group-alias/id/{id}", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.readGroupAliasById.value}", notes = "${OIDCAuthController.readGroupAliasById.notes}")
	public ResponseEntity<String> readGroupAliasById(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("id") String id) {
		return oidcAuthService.readGroupAliasById(token, id);
	}
	
	/**
	 * Update Identity Group By Name
	 * @param request
	 * @param token
	 * @param entityName
	 * @param oidcIdentityGroupRequest
	 * @return
	 */
	@PostMapping(value = "/v2/identity/group/name/{name}", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.updateIdentityGroupByName.value}", notes = "${OIDCAuthController.updateIdentityGroupByName.notes}")
	public ResponseEntity<String> updateIdentityGroupByName(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("name") String entityName,
			@Valid @RequestBody OIDCIdentityGroupRequest oidcIdentityGroupRequest) {
		return oidcAuthService.updateIdentityGroupByName(token, oidcIdentityGroupRequest);
	}
	
	/**
	 * Delete Group By Name
	 * @param request
	 * @param token
	 * @param name
	 * @return
	 */
	@DeleteMapping(value = "/v2/identity/group/name/{name}", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.deleteGroupByName.value}", notes = "${OIDCAuthController.deleteGroupByName.notes}")
	public ResponseEntity<String> deleteGroupByName(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("name") String name) {
		return oidcAuthService.deleteGroupByName(token, name);
	}
	
	/**
	 * Delete Group Alias By ID
	 * @param request
	 * @param token
	 * @param id
	 * @return
	 */
	@DeleteMapping(value = "/v2/identity/group-alias/id/{id}", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.deleteGroupAliasByID.value}", notes = "${OIDCAuthController.deleteGroupAliasByID.notes}")
	public ResponseEntity<String> deleteGroupAliasByID(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("id") String id) {
		return oidcAuthService.deleteGroupAliasByID(token, id);
	}
	
	/**
	 * Create Group Alias
	 * @param request
	 * @param token
	 * @param groupAliasRequest
	 * @return
	 */
	@PostMapping(value = "/v2/identity/group-alias", produces = "application/json")
	@ApiOperation(hidden = true, value = "${OIDCAuthController.createGroupAlias.value}", notes = "${OIDCAuthController.createGroupAlias.notes}")
	public ResponseEntity<String> createGroupAlias(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token,
			@Valid @RequestBody GroupAliasRequest groupAliasRequest) {
		return oidcAuthService.createGroupAlias(token, groupAliasRequest);
	}

	/**
	 * Login to TVault
	 * @returnC
	 */
	@PostMapping(value="/v2/auth/oidc/auth_url",produces="application/json")
	@ApiOperation(value = "${OIDCAuthController.getAuthUrl.value}", notes = "${OIDCAuthController.getAuthUrl.notes}")
	public ResponseEntity<String> getAuthUrl(@RequestBody OidcRequest oidcRequest){
		return oidcAuthService.getAuthUrl(oidcRequest);
	}

	/**
	 * Process OIDC callback.
	 * @param state
	 * @param code
	 * @return
	 */
	@GetMapping(value="/v2/auth/oidc/callback",produces="application/json")
	@ApiOperation(value = "${OIDCAuthController.processOIDCCallback.value}", notes = "${OIDCAuthController.processOIDCCallback.notes}")
	public ResponseEntity<String> processOIDCCallback(@RequestParam(name="state", required = false) String state, @RequestParam(name="code", required = false) String code){
		return oidcAuthService.processOIDCCallback(state, code);

	}

	/**
	 * To get group object id from Azure AD.
	 * @param groupName
	 * @return
	 */
	@GetMapping(value="/v2/azure/group/{name}/objectid",produces="application/json")
	@ApiOperation(value = "${OIDCAuthController.getGroupObjectIdFromAD.value}", notes = "${OIDCAuthController.getGroupObjectIdFromAD.notes}", hidden=true)
	public ResponseEntity<String> getGroupObjectIdFromAzure(@PathVariable("name") String groupName){
		return oidcAuthService.getGroupObjectIdFromAzure(groupName);

	}

	/**
	 * To get identity group details.
	 * @param groupName
	 * @return
	 */
	@GetMapping(value="/v2/identity/group/{name}",produces="application/json")
	@ApiOperation(value = "${OIDCAuthController.getIdentityGroupDetails.value}", notes = "${OIDCAuthController.getIdentityGroupDetails.notes}", hidden=true)
	public ResponseEntity<String> getIdentityGroupDetails(HttpServletRequest request,
		@RequestHeader(value = "vault-token") String token, @PathVariable("name") String groupName){
		return oidcAuthService.getIdentityGroupDetails(groupName, token);
	}
	
	/**
	 * To get User name.
	 * @param request
	 * @return
	 */
	@GetMapping(value = "/v2/username", produces = "application/json")
	@ApiOperation(value = "${OIDCAuthController.getUserName.value}", notes = "${OIDCAuthController.getUserName.notes}", hidden = true)
	public ResponseEntity<String> getUserName(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return oidcAuthService.getUserName(userDetails);
	}

	/**
	 * To search group in AAD.
	 * @param groupName
	 * @return
	 */
	@GetMapping(value="/v2/azure/groups",produces="application/json")
	@ApiOperation(value = "${OIDCAuthController.searchGroupsInAzure.value}", notes = "${OIDCAuthController.searchGroupsInAzure.notes}")
	public ResponseEntity<DirectoryObjects> searchGroupInAzureAD(@RequestParam("name") String groupName){
		return oidcAuthService.searchGroupInAzureAD(groupName);

	}
	/**
	 * To search groupEmail in AAD.
	 * @param email
	 * @return
	 */
	@GetMapping(value="/v2/azure/email",produces="application/json")
	@ApiOperation(value = "${OIDCAuthController.searchGroupEmailInAzureAD.value}", notes = "${OIDCAuthController.searchGroupEmailInAzureAD.notes}")
	public ResponseEntity<DirectoryObjects> searchGroupEmailInAzureAD(@RequestParam("mail") String email){
		return oidcAuthService.searchGroupEmailInAzureAD(email);
	}
	
	/**
	 * To fetch the build details
	 * @return
	 */
	@GetMapping(value="/v2/auth/build",produces="application/json")
	@ApiOperation(value = "${OIDCAuthController.getBuildDetails.value}", notes = "${OIDCAuthController.getBuildDetails.notes}")
	public ResponseEntity<BuildDetails> getBuildDetails(){
		return oidcAuthService.getBuildDetails();
	}
	
	
}
