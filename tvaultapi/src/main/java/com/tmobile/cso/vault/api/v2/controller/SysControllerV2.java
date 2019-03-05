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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.Unseal;
import com.tmobile.cso.vault.api.service.SysService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@RestController
@CrossOrigin
@Api(description = "Manage Vault System", position = 17)
public class SysControllerV2 {
	@Value("${vault.port}")
	private String vaultPort;
	
	@Autowired
	private SysService sysService;

	/**
	 * To check the vault health
	 * @return
	 */
	@ApiOperation(value = "${SysControllerV2.checkVaultHealth.value}", notes = "${SysControllerV2.checkVaultHealth.notes}")
	@GetMapping(value ="/v2/health" ,produces="application/json")
	public ResponseEntity<String> checkVaultHealth(){
		return sysService.checkVaultHealth();
	}
	/**
	 * To check the vault seal status
	 * @return
	 */
	@ApiOperation(value = "${SysControllerV2.checkSealStatus.value}", notes = "${SysControllerV2.checkSealStatus.notes}")
	@GetMapping(value ="/v2/seal-status" ,produces="application/json")
	public ResponseEntity<String> checkVaultSealStatus(){
		return sysService.checkVaultSealStatus();
	}
	/**
	 * To unseal
	 * @param jsonStr
	 * @return
	 */
	@ApiOperation(value = "${SysControllerV2.unseal.value}", notes = "${SysControllerV2.unseal.notes}")
	@PostMapping(value ="/v2/unseal" , consumes = "application/json" ,produces="application/json")
	public ResponseEntity<String> unseal (@RequestBody Unseal unseal){
		return sysService.unseal(unseal);
	}
	/**
	 * To know unseal progress
	 * @param serverip
	 * @return
	 */
	@ApiOperation(value = "${SysControllerV2.unsealProgress.value}", notes = "${SysControllerV2.unsealProgress.notes}")
	@GetMapping(value ="/v2/unseal-progress" ,produces="application/json")
	public ResponseEntity<String> unsealProgress (@RequestParam("serverip" ) String serverip){
		return sysService.unsealProgress(serverip);
	}
}