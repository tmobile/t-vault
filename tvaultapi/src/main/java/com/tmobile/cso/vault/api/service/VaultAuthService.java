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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.AuthorizationUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;

@Component
public class  VaultAuthService {

	@Autowired
	private RequestProcessor reqProcessor;
	
	@Autowired
	private AuthorizationUtils authorizationUtils;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;
	
	private static Logger log = LogManager.getLogger(VaultAuthService.class);

	/**
	 * Logs a user in to TVault using ldap or userpass authentication methods
	 * @param jsonStr
	 * @return
	 */
	private ResponseEntity<String> login(String jsonStr) {
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
			if (HttpStatus.BAD_REQUEST.equals(response.getHttpstatus())) {
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\": [\"User Authentication failed\", \"Invalid username or password. Please retry again after correcting username or password.\"]}");
			}
			else if (HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {
				return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\": [\"User Authentication failed\", \"This may be due to vault services are down or vault services are not reachable\"]}");
			}
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Username Authentication Failed.\"]}");
		}
	}
	/**
	 * Logs a user in to TVault using ldap or userpass authentication methods
	 * @param user
	 * @return
	 */
	public ResponseEntity<String> login(UserLogin user) {
		String jsonStr = JSONUtil.getJSON(user);
		return login(jsonStr);
	}
	/**
	 * Renews vault token for a given user token
	 * @param jsonStr
	 * @return
	 */
	public ResponseEntity<String> renew(String token) {
		Response response = reqProcessor.process("/auth/tvault/renew","{}", token);	
 		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Self renewal of token Failed.\"]}");
		}
	}
	
	/**
	 * Looks up the Login details from Vault for a given user token
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> lookup( String token){
		Response response = reqProcessor.process("/auth/tvault/lookup","{}", token);	
 		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Token Lookup Failed.\"]}");
		}
	}
	/**
	 * Logs the user out from Vault based on given user token
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> revoke(String token){
		Response response = reqProcessor.process("/auth/tvault/revoke","{}", token);	
 		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Token revoke Failed.\"]}");
		}
	}

}
