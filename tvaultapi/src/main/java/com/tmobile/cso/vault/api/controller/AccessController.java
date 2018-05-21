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

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

import io.swagger.annotations.Api;


@RestController
@RequestMapping(value="/access")
@CrossOrigin
@Api(description = "Manage Vault Policies", position = 1)
public class AccessController {
	
	private Logger log = LogManager.getLogger(AccessController.class);
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
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Create Policy").
			      put(LogMessage.MESSAGE, String.format("Trying to create policy [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/access/create",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.OK)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Create Policy").
				      put(LogMessage.MESSAGE, "Policy creation completed successfully").
				      put(LogMessage.RESULT, response.getResponse()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access Policy created \"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Create Policy").
			      put(LogMessage.MESSAGE, "Policy creation failed").
			      put(LogMessage.RESULT, response.getResponse()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
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
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Update Policy").
			      put(LogMessage.MESSAGE, String.format("Trying to update policy [%s]", jsonStr)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/access/update",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Update Policy").
				      put(LogMessage.MESSAGE, "Policy Update completed successfully").
				      put(LogMessage.RESULT, response.getResponse()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Policy updated \"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Update Policy").
			      put(LogMessage.MESSAGE, "Policy Update failed").
			      put(LogMessage.RESULT, response.getResponse()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	@GetMapping(value="",produces="application/json")
	public ResponseEntity<String> listAllPolices(@RequestHeader(value="vault-token") String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "List Policies").
			      put(LogMessage.MESSAGE, "Trying to get all policies").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/access/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	@GetMapping(value="/{accessid}",produces="application/json")
	public ResponseEntity<String> getPolicyInfo(@RequestHeader(value="vault-token") String token,@PathVariable("accessid" ) String accessid){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Get Policy information").
			      put(LogMessage.MESSAGE, String.format("Trying to get policy information for [%s]", accessid)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if(null == accessid || "".equals(accessid)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing accessid \"]}");
		}
		
		Response response = reqProcessor.process("/access","{\"accessid\":\""+accessid+"\"}",token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Get Policy information").
			      put(LogMessage.MESSAGE, "Getting policy information failed").
			      put(LogMessage.RESULT, response.getResponse()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
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
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete Policy").
			      put(LogMessage.MESSAGE, String.format("Trying to delete policy [%s]", accessid)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if(null == accessid || "".equals(accessid)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing accessid \"]}");
		}
		Response response = reqProcessor.process("/access/delete","{\"accessid\":\""+accessid+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Delete Policy").
				      put(LogMessage.MESSAGE, "Policy information deleted succssfully").
				      put(LogMessage.RESULT, response.getResponse()).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access is deleted\"]}");
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Delete Policy").
			      put(LogMessage.MESSAGE, "Deletion of Policy information failed").
			      put(LogMessage.RESULT, response.getResponse()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	
	
}
