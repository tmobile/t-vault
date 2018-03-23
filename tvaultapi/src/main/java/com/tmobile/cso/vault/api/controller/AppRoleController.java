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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;


@RestController
@RequestMapping(value="/auth/approle")
@CrossOrigin

public class AppRoleController {
	
	private Logger log = LogManager.getLogger(AppRoleController.class);
	@Autowired
	private RequestProcessor reqProcessor;
	private SDBController sdbController = new SDBController();

	
	/**
	 * CREATE APPROLE
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@PostMapping(value="/createAppRole", consumes="application/json", produces="application/json")
	public ResponseEntity<String> createAppRole(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		log.info("creating AppRole");
		
		Response response = reqProcessor.process("/auth/approle/role/create", jsonStr,token);

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created\"]}");
				
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}

	/**
	 * ASSOCIATE APPROLE TO SDB
	 * @param token
	 * @param jsonStr
	 * @return
	 */

	@PostMapping(value="/associateApprole",produces="application/json")
	public ResponseEntity<String> associateApprole(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){

			log.info("Associate AppRole to SDB - Approle Controller ");

			ResponseEntity<String> response = sdbController.associateApproletoSDB(token,jsonStr);

			log.info("Associate AppRole to SDB - After SDBController call..");

			if(response.getStatusCode().equals(HttpStatus.NO_CONTENT))
				
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
		
			else if(response.getStatusCode().equals(HttpStatus.OK))
			
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
			else
				
				return ResponseEntity.status(response.getStatusCode()).body(response.toString());	
	}
	
	/**
	 * READ APPROLE
	 * @param token
	 * @param rolename
	 * @return
	 */
	@GetMapping(value="/readAppRole/{role_name}",produces="application/json")
	public ResponseEntity<String> readAppRole(@RequestHeader(value="vault-token") String token, @PathVariable("role_name" ) String rolename){

			log.info("Read AppRole");

			Response response = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+rolename+"\"}",token);
		
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	

	/**
	 * READ APPROLE ROLEID
	 * @param token
	 * @param rolename
	 * @return
	 */
	@GetMapping(value="/readAppRoleRoleId/{role_name}",produces="application/json")
	public ResponseEntity<String> readAppRoleRoleId(@RequestHeader(value="vault-token") String token, @PathVariable("role_name" ) String rolename){

			log.info("Read AppRole RoleID");

			Response response = reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+rolename+"\"}",token);
		
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	

	
	/**
	 * DELETE APPROLE
	 * @param token
	 * @param username
	 * @return
	 */
	@DeleteMapping(value="/deleteAppRole",produces="application/json")
	public ResponseEntity<String> deleteAppRole(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		log.info("delete AppRole");

		Response response = reqProcessor.process("/auth/approle/role/delete",jsonStr,token);
	
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted\"]}");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}

	
	/**
	 * CREATE SECRETID FOR APPROLE
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@PostMapping(value="/createSecretId", consumes="application/json", produces="application/json")
	public ResponseEntity<String> createsecretId(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		log.info("creating SecretId for AppRole");
		
		Response response = reqProcessor.process("/auth/approle/secretid/create", jsonStr,token);

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret ID created for AppRole\"]}");
				
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}

	
	

	/**
	 * READ SECRETID FOR APPROLE
	 * @param token
	 * @param rolename
	 * @return
	 */
	@GetMapping(value="/readSecretId/{role_name}",produces="application/json")
	public ResponseEntity<String> readSecretId(@RequestHeader(value="vault-token") String token, @PathVariable("role_name" ) String rolename){

			log.info("Read SecretID");

			Response response = reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+rolename+"\"}",token);
		
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	

	/**
	 * DELETE SECRET FOR APPROLE
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@PostMapping(value="/deleteSecretId",produces="application/json")
	public ResponseEntity<String> deleteSecretId(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		log.info("Delete SecretId");

		Response response = reqProcessor.process("/auth/approle/secret/delete",jsonStr,token);
	
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SecretId for AppRole deleted\"]}");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}

	
	
	@PostMapping(value="/login",produces="application/json")	
	public ResponseEntity<String> login(@RequestBody String jsonStr){
		
		log.info("login Approle");
		
		Response response = reqProcessor.process("/auth/approle/login",jsonStr,"");

		log.info("Approle login HTTP STATUSCODE  :" + response.getHttpstatus());
		
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		
		}else{
		
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Approle Login Failed.\"]}" + "HTTP STATUSCODE  :" + response.getHttpstatus());
		
		}

	}
	
}
