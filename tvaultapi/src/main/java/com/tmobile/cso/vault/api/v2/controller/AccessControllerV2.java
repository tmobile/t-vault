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

package com.tmobile.cso.vault.api.v2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.AccessPolicy;
import com.tmobile.cso.vault.api.service.AccessService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@RestController
@CrossOrigin
@Api(description = "Manage Vault Policies", position = 11)
public class AccessControllerV2 {
	
	@Autowired
	private AccessService accessService;
	
	/**
	 * Creates vault policy
	 * @param token
	 * @param accessPolicy
	 * @return
	 */
	@ApiOperation(value = "${AccessControllerV2.createPolicy.value}", notes = "${AccessControllerV2.createPolicy.notes}")
	@PostMapping(value="/v2/access",consumes="application/json",produces="application/json")
	public ResponseEntity<String> createPolicy(@RequestHeader(value="vault-token") String token, @RequestBody AccessPolicy accessPolicy){
		return accessService.createPolicy(token, accessPolicy);
	}
	/**
	 * Updates an existing vault policy
	 * @param token
	 * @param accessPolicy
	 * @return
	 */
	@ApiOperation(value = "${AccessControllerV2.updatePolicy.value}", notes = "${AccessControllerV2.updatePolicy.notes}")
	@PutMapping(value="/v2/access",consumes="application/json",produces="application/json")
	public ResponseEntity<String> updatePolicy(@RequestHeader(value="vault-token") String token,  @RequestBody AccessPolicy accessPolicy){
		return accessService.updatePolicy(token, accessPolicy);
	}
	/**
	 * List all policies
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${AccessControllerV2.listAllPolices.value}", notes = "${AccessControllerV2.listAllPolices.notes}")
	@GetMapping(value="/v2/access",produces="application/json")
	public ResponseEntity<String> listAllPolices(@RequestHeader(value="vault-token") String token){
		return accessService.listAllPolices(token);
	}
	/**
	 * To get details of a policy
	 * @param token
	 * @param accessid
	 * @return
	 */
	@ApiOperation(value = "${AccessControllerV2.getPolicyInfo.value}", notes = "${AccessControllerV2.getPolicyInfo.notes}")
	@GetMapping(value="/v2/access/{accessid}",produces="application/json")
	public ResponseEntity<String> getPolicyInfo(@RequestHeader(value="vault-token") String token, @PathVariable("accessid" ) String accessid){
		return accessService.getPolicyInfo(token, accessid);
	}
	/**
	 * To delete a policy
	 * @param token
	 * @param accessid
	 * @return
	 */
	@ApiOperation(value = "${AccessControllerV2.deletePolicyInfo.value}", notes = "${AccessControllerV2.deletePolicyInfo.notes}")
	@DeleteMapping(value="/v2/access/{accessid}",produces="application/json")
	public ResponseEntity<String> deletePolicyInfo(@RequestHeader(value="vault-token") String token,@PathVariable("accessid" ) String accessid){
		return accessService.deletePolicyInfo(token, accessid);
	}
}
