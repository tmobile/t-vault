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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.model.AccessPolicy;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;

@Component
public class  AccessService {

	@Value("${vault.port}")
	private String vaultPort;

	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger logger = LogManager.getLogger(AccessService.class);
	/**
	 * Creates Vault policy
	 * @param token
	 * @param accessPolicy
	 * @return
	 */
	public ResponseEntity<String> createPolicy(String token, AccessPolicy accessPolicy){
		String jsonStr = JSONUtil.getJSON(accessPolicy);
		Response response = reqProcessor.process("/access/create",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.OK)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access Policy created \"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Updates Vault policy
	 * @param token
	 * @param accessPolicy
	 * @return
	 */
	public ResponseEntity<String> updatePolicy( String token,  AccessPolicy accessPolicy){
		String jsonStr = JSONUtil.getJSON(accessPolicy);
		Response response = reqProcessor.process("/access/update",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Policy updated \"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * List all vault policies
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listAllPolices(String token){
		Response response = reqProcessor.process("/access/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * To get details about a policy
	 * @param token
	 * @param accessid
	 * @return
	 */
	public ResponseEntity<String> getPolicyInfo(String token, String accessid){
		if(null == accessid || "".equals(accessid)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing accessid \"]}");
		}
		Response response = reqProcessor.process("/access","{\"accessid\":\""+accessid+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * To delete a policy
	 * @param token
	 * @param accessid
	 * @return
	 */
	public ResponseEntity<String> deletePolicyInfo(String token, String accessid){
		if(null == accessid || "".equals(accessid)){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing accessid \"]}");
		}
		Response response = reqProcessor.process("/access/delete","{\"accessid\":\""+accessid+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access is deleted\"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
}
