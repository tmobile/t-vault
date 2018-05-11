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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.model.TVaultError;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.model.UserpassUser;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;

@Component
public class  UserPassService {

	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;
	/**
	 * To create user
	 * @param token
	 * @param user
	 * @return
	 */
	public ResponseEntity<String> createUser(String token, UserpassUser user){
		String jsonStr = JSONUtil.getJSON(user);
		Response response = reqProcessor.process("/auth/userpass/create", jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Username User created\"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To get user info
	 * @param token
	 * @param username
	 * @return
	 */
	public ResponseEntity<String> readUser(String token, String username){
		Response response = reqProcessor.process("/auth/userpass/read","{\"username\":\""+username+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To delete user
	 * @param token
	 * @param username
	 * @return
	 */
	public ResponseEntity<String> deleteUser(String token, String username){
		UserLogin user = new UserLogin();
		user.setUsername(username);
		String jsonStr = JSONUtil.getJSON(user);
		Response response = reqProcessor.process("/auth/userpass/delete",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Username User deleted\"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To update password
	 * @param token
	 * @param user
	 * @return
	 */
	public ResponseEntity<String> updatePassword( String token, UserpassUser user){
		String jsonStr = JSONUtil.getJSON(user);
		Response response = reqProcessor.process("/auth/userpass/update",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Password for the user updated\"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	/**
	 * To get list of users
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listUsers(String token){
		Response response = reqProcessor.process("/auth/userpass/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	

	}
	/**
	 * To login using userpass auth
	 * @param user
	 * @return
	 */
	public ResponseEntity<String> login(UserLogin user){
		String jsonStr = JSONUtil.getJSON(user);
		Response response = reqProcessor.process("/auth/userpass/login",jsonStr,"");
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
}