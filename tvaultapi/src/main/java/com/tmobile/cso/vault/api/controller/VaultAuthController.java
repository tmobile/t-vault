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

package com.tmobile.cso.vault.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

import io.swagger.annotations.Api;


@RestController
@RequestMapping(value="/auth/tvault")
@CrossOrigin
@Api(description = "Manage Vault Authentication", position = 9)
public class VaultAuthController {
        @Value("${vault.auth.method}")
        private String vaultAuthMethod;
	
	@Autowired
	private RequestProcessor reqProcessor;

	
	@PostMapping(value="/login",produces="application/json")	
	public ResponseEntity<String> login(@RequestBody String jsonStr){
		
		Response response = null;

		if ("ldap".equals(vaultAuthMethod)) {
			response = reqProcessor.process("/auth/ldap/login",jsonStr,"");	
		}
		else {
			// Default to userpass
			response = reqProcessor.process("/auth/userpass/login",jsonStr,"");
		}
		
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Username Authentication Failed.\"]}");
		}

	}
	/**
	 * To renew token
	 * @param token
	 * @return
	 */
	@PostMapping(value="/renew",produces="application/json")	
	public ResponseEntity<String> renew(@RequestHeader(value="vault-token") String token){
		Response response = reqProcessor.process("/auth/tvault/renew","{}", token);	
 		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Self renewal of token Failed.\"]}");
		}

	}
	
	/**
	 * To Lookup token details
	 * @param token
	 * @return
	 */
	@PostMapping(value="/lookup",produces="application/json")	
	public ResponseEntity<String> lookup(@RequestHeader(value="vault-token") String token){
		Response response = reqProcessor.process("/auth/tvault/lookup","{}", token);	
 		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Token Lookup Failed.\"]}");
		}

	}
	
	
	/**
	 * To revoke a token
	 * @param token
	 * @return
	 */
	@PostMapping(value="/revoke",produces="application/json")	
	public ResponseEntity<String> revoke(@RequestHeader(value="vault-token") String token){
		Response response = reqProcessor.process("/auth/tvault/revoke","{}", token);	
 		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Token revoke Failed.\"]}");
		}

	}
	
}

