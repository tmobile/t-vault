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
@RequestMapping(value="/access")
@CrossOrigin
@Api(description = "Manage Vault Policies", position = 1)
public class AccessController {
	
	@Autowired
	private RequestProcessor reqProcessor;
			
	/***
	 * Method to create a vault policy
	 * @param token
	 * @param jsonStr : path and policy details
	 * @return : HttpStatus 200 on successful creation of policy
	 * Sample output
	 *  	{"Messages":["Policy created"]}
	 */
	
	@PostMapping(value="/create",consumes="application/json",produces="application/json")
	public ResponseEntity<String> createPolicy(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		Response response = reqProcessor.process("/access/create",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.OK))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access Policy created \"]}");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/***
	 * Method to update an existing vault policy
	 * @param token
	 * @param jsonStr : path and policy details
	 * @return : HttpStatus 200 on successful creation of policy
	 * Sample output
	 *  	{"Message":"Policy update "}
	 */
	@PostMapping(value="/update",consumes="application/json",produces="application/json")
	public ResponseEntity<String> updatePolicy(@RequestHeader(value="vault-token") String token,  @RequestBody String jsonStr){
		
		Response response = reqProcessor.process("/access/update",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Policy updated \"]}");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	@GetMapping(value="",produces="application/json")
	public ResponseEntity<String> listAllPolices(@RequestHeader(value="vault-token") String token){
		
		Response response = reqProcessor.process("/access/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	@GetMapping(value="/{accessid}",produces="application/json")
	public ResponseEntity<String> getPolicyInfo(@RequestHeader(value="vault-token") String token,@PathVariable("accessid" ) String accessid){
		
		if(null == accessid || "".equals(accessid)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing accessid \"]}");
		}
		
		Response response = reqProcessor.process("/access","{\"accessid\":\""+accessid+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	
	/**
	 * Method to delete a policy 
	 * 
	 * @param token
	 * @param accessid
	 * @return HttpStatus 200 if policy is deleted
	 */
	
	@DeleteMapping(value="/delete/{accessid}",produces="application/json")
	public ResponseEntity<String> deletePolicyInfo(@RequestHeader(value="vault-token") String token,@PathVariable("accessid" ) String accessid){
		
		if(null == accessid || "".equals(accessid)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing accessid \"]}");
		}
		Response response = reqProcessor.process("/access/delete","{\"accessid\":\""+accessid+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access is deleted\"]}");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	
	
}
