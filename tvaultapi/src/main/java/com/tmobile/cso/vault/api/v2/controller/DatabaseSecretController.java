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

package com.tmobile.cso.vault.api.v2.controller;

import com.tmobile.cso.vault.api.service.DatabaseSecretService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin
@Api(description = "Manage AWS Secrets", position = 13)
public class DatabaseSecretController {
	
	@Autowired
	DatabaseSecretService databaseSecretService;

	/**
	 * Read MySql database temporary credentials.
	 * @param token
	 * @param role_name
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.getTemporaryCredentials.value}", notes = "${DatabaseSecretController.getTemporaryCredentials.notes}")
	@GetMapping(value="/v2/database/role/credentials/{role_name}",produces="application/json")
	public ResponseEntity<String> getTemporaryCredentials(@RequestHeader(value="vault-token") String token, @PathVariable String role_name){
		return databaseSecretService.getTemporaryCredentials(role_name, token);
	}
}
