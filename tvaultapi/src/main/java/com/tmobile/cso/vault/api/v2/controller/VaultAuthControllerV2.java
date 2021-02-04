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

package com.tmobile.cso.vault.api.v2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.service.VaultAuthService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


@RestController
@CrossOrigin
@Api(description = "Manage Vault Authentication", position = 19)
public class VaultAuthControllerV2 {
    @Value("${vault.auth.method}")
    private String vaultAuthMethod;
	
	@Autowired
	private VaultAuthService vaultAuthService;
	
	/**
	 * Login to TVault
	 * @param user
	 * @returnC
	 */
	@PostMapping(value="/v2/auth/tvault/login",produces="application/json")
	@ApiOperation(value = "${VaultAuthControllerV2.login.value}", notes = "${VaultAuthControllerV2.login.notes}", hidden = true)
	public ResponseEntity<String> login(@RequestBody UserLogin user){
		return vaultAuthService.login(user);
	}
	
	/**
	 * To renew token
	 * @param token
	 * @return
	 */
	@GetMapping(value="/v2/auth/tvault/renew",produces="application/json")
	@ApiOperation(value = "${VaultAuthControllerV2.renew.value}", notes = "${VaultAuthControllerV2.renew.notes}", hidden = true)
	public ResponseEntity<String> renew(@RequestHeader(value="vault-token") String token){
		return vaultAuthService.renew(token);
	}
	
	/**
	 * To Lookup token details
	 * @param token
	 * @return
	 */
	@PostMapping(value="/v2/auth/tvault/lookup",produces="application/json")	
	@ApiOperation(value = "${VaultAuthControllerV2.lookup.value}", notes = "${VaultAuthControllerV2.lookup.notes}")
	public ResponseEntity<String> lookup(@RequestHeader(value="vault-token") String token){
		return vaultAuthService.lookup(token);
	}
	/**
	 * To revoke a token
	 * @param token
	 * @return
	 */
	@GetMapping(value="/auth/tvault/revoke",produces="application/json")
	@ApiOperation(value = "${VaultAuthControllerV2.revoke.value}", notes = "${VaultAuthControllerV2.revoke.notes}", hidden = true)
	@ApiResponses(value={
		@ApiResponse(code = 204, message = "Revoked Successfully")
	})
	public ResponseEntity<String> revoke(@RequestHeader(value="vault-token") String token){
		return vaultAuthService.revoke(token);
	}
	
}

