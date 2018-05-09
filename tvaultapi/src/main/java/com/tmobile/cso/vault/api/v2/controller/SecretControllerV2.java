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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.service.SecretService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@RestController
@CrossOrigin
@Api(description = "Manage Secrets", position = 15)
public class SecretControllerV2 {
	
	@Autowired
	private SecretService secretService;
	/**
	 * Read secrets from vault
	 * @param token
	 * @param path
	 * @return
	 */
	@ApiOperation(value = "${SecretControllerV2.readFromVault.value}", notes = "${SecretControllerV2.readFromVault.notes}")
	@GetMapping(value="/v2/read",produces= "application/json")
	public ResponseEntity<String> readFromVault(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		return secretService.readFromVault(token, path);
	}
	/**
	 * Write secrets into vault
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${SecretControllerV2.write.value}", notes = "${SecretControllerV2.write.notes}")
	@PostMapping(value="/v2/write",consumes="application/json",produces="application/json")
	public ResponseEntity<String> write(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		return secretService.write(token, jsonStr);
	}
	/**
	 * Delete secrets from vault
	 * @param token
	 * @param path
	 * @return
	 */
	@ApiOperation(value = "${SecretControllerV2.deleteFromVault.value}", notes = "${SecretControllerV2.deleteFromVault.notes}")
	@DeleteMapping(value="/v2/delete",produces="application/json")
	public ResponseEntity<String> deleteFromVault(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		return secretService.deleteFromVault(token, path);
	}
	/**
	 * Reads the given sdb path/folder recursively
	 * @param token
	 * @param path
	 * @return
	 */
	@ApiOperation(value = "${SecretControllerV2.readFromVaultRecursive.value}", notes = "${SecretControllerV2.readFromVaultRecursive.notes}")
	@GetMapping(value="/v2/readfull",produces= "application/json")
	public ResponseEntity<String> readFromVaultRecursive(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		return secretService.readFromVaultRecursive(token, path);
	}
	/**
	 * Reads the contents of a safe/folder, includes both folders and secrets
	 * @param token
	 * @param path
	 * @return
	 */
	@ApiOperation(value = "${SecretControllerV2.readFoldersAndSecrets.value}", notes = "${SecretControllerV2.readFoldersAndSecrets.notes}")
	@GetMapping(value="/v2/readAll",produces= "application/json")
	public ResponseEntity<String> readFoldersAndSecrets(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		return secretService.readFoldersAndSecrets(token, path);
	}
}

