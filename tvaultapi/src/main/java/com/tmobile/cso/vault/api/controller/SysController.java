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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

import io.swagger.annotations.Api;


@RestController
@CrossOrigin
@Api(description = "Manage Vault System", position = 7)
public class SysController {
	@Value("${vault.port}")
	private String vaultPort;
	
	@Autowired
	private RequestProcessor reqProcessor;
		
	@GetMapping(value ="/health" ,produces="application/json")
	public ResponseEntity<String> checkHealth (){
		Response response = reqProcessor.process("/health","{}","");
		if(HttpStatus.OK.equals(response.getHttpstatus()) // initialized, unsealed, and active
				|| HttpStatus.TOO_MANY_REQUESTS.equals(response.getHttpstatus()) // unsealed and standby
			//	|| HttpStatus.SERVICE_UNAVAILABLE.equals(response.getHttpstatus()) // sealed .. Needed for unseal API to work. Now sealed is unhealthy
			 ) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Healthy.All OK\"]}");
		}else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Not OK \"]}");
		}
	}
	
	@PostMapping(value ="/unseal" , consumes = "application/json" ,produces="application/json")
	public ResponseEntity<String> unseal (@RequestBody String jsonStr){
		jsonStr = jsonStr.substring(0,jsonStr.lastIndexOf("}"));
		jsonStr = jsonStr+ ",\"port\":\""+vaultPort+"\"}";
		Response response = reqProcessor.process("/unseal",jsonStr,"");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	@GetMapping(value ="/unseal-progress" ,produces="application/json")
	public ResponseEntity<String> unsealProgress (@RequestParam("serverip" ) String serverip){
		System.out.println(serverip);
		Response response = reqProcessor.process("/unseal-progress","{\"serverip\":\""+serverip+"\",\"port\":\""+vaultPort+"\"}","");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
}