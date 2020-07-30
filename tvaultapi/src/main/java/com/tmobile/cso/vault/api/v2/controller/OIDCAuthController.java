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
	public ResponseEntity<String> entityLookUp(HttpServletRequest request,
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
	public ResponseEntity<String> updateEntityByName(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("name") String entityName,
			@Valid @RequestBody OIDCEntityRequest oidcEntityRequest) {
		return oidcAuthService.updateEntityByName(token, oidcEntityRequest, entityName);
	}
	
	/**
	 * Read Group Alias By ID
	 * @param request
	 * @param token
	 * @param id
	 * @return
	 */
	@GetMapping(value = "/v2/identity/group-alias/id/{id}", produces = "application/json")
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
	public ResponseEntity<String> updateIdentityGroupByName(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("name") String entityName,
			@Valid @RequestBody OIDCIdentityGroupRequest oidcIdentityGroupRequest) {
		return oidcAuthService.updateIdentityGroupByName(token, oidcIdentityGroupRequest, entityName);
	}
	
	/**
	 * Delete Group By Name
	 * @param request
	 * @param token
	 * @param name
	 * @return
	 */
	@DeleteMapping(value = "/v2/identity/group/name/{name}", produces = "application/json")
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
	@ApiOperation(value = "${VaultAuthControllerV2.login.value}", notes = "${VaultAuthControllerV2.login.notes}")
	public ResponseEntity<String> getAuthUrl(@RequestBody OidcRequest oidcRequest, @RequestHeader(value="vault-token") String token){
		return oidcAuthService.getAuthUrl(oidcRequest);
	}

	/**
	 * Process OIDC callback.
	 * @param token
	 * @param state
	 * @param code
	 * @return
	 */
	@GetMapping(value="/v2/auth/oidc/callback",produces="application/json")
	@ApiOperation(value = "${VaultAuthControllerV2.renew.value}", notes = "${VaultAuthControllerV2.renew.notes}")
	public ResponseEntity<String> processOIDCCallback(@RequestHeader(value="vault-token") String token, @RequestParam(name="state", required = false) String state, @RequestParam(name="code", required = false) String code){
		return oidcAuthService.processOIDCCallback(state, code);

	}

}
