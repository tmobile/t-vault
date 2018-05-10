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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

import io.swagger.annotations.Api;


@RestController
@RequestMapping(value="/auth/userpass")
@CrossOrigin
@Api(description = "Manage Userpass Authentication", position = 8)
public class UserPassController {
	
	private Logger log = LogManager.getLogger(UserPassController.class);
	@Autowired
	private RequestProcessor reqProcessor;

	
	/**
	 * CREATE USER
	 * @param token
	 * @param username
	 * @return
	 */
	@PostMapping(value="/create", consumes="application/json", produces="application/json")
	public ResponseEntity<String> createUser(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		log.info("creating user");
		
		Response response = reqProcessor.process("/auth/userpass/create", jsonStr,token);

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Username User created\"]}");
				
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}

	
	/**
	 * READ USER
	 * @param token
	 * @param username
	 * @return
	 */
	@GetMapping(value="/read/{username}",produces="application/json")
	public ResponseEntity<String> readUser(@RequestHeader(value="vault-token") String token, @PathVariable("username" ) String username){

			log.info("Read user");

			Response response = reqProcessor.process("/auth/userpass/read","{\"username\":\""+username+"\"}",token);
		
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	
	/**
	 * DELETEUSER
	 * @param token
	 * @param username
	 * @return
	 */
	@DeleteMapping(value="/delete",produces="application/json")
	public ResponseEntity<String> deleteUser(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		log.info("delete user");

		Response response = reqProcessor.process("/auth/userpass/delete",jsonStr,token);
	
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Username User deleted\"]}");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	/**
	 * UPDATE PASSWORD
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@PostMapping(value="/update",produces="application/json")
	public ResponseEntity<String> updatePassword(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		log.info("update user");

		Response response = reqProcessor.process("/auth/userpass/update",jsonStr,token);
		
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Password for the user updated\"]}");
		
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	/**
	 * 
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@GetMapping(value="/list",produces="application/json")
	public ResponseEntity<String> listUsers(@RequestHeader(value="vault-token") String token){

		log.info("Listing users");

		Response response = reqProcessor.process("/auth/userpass/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	

	}
	
	/**
	 * 
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@PostMapping(value="/login",produces="application/json")	
	public ResponseEntity<String> login(@RequestBody String jsonStr){
		
		log.info("login user {}", jsonStr);
		
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

