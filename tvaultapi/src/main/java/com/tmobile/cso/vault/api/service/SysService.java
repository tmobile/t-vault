// =========================================================================
// Copyright 2019 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License")
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

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.Unseal;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class  SysService {

	@Value("${vault.port}")
	private String vaultPort;
	
	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger log = LogManager.getLogger(SysService.class);

	/**
	 * Returns the health of TVault system
	 * @return
	 */
	public ResponseEntity<String> checkVaultHealth() {
		// Try with https first
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Check Health").
			      put(LogMessage.MESSAGE, "Trying to get health of the Vault Server").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/health","{}","");
		if(HttpStatus.OK.equals(response.getHttpstatus()) || HttpStatus.TOO_MANY_REQUESTS.equals(response.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Healthy.All OK\"]}");
		} else {
			// If no response for https, then try http
			response = reqProcessor.process("/v2/health","{}","");
			if(HttpStatus.OK.equals(response.getHttpstatus()) || HttpStatus.TOO_MANY_REQUESTS.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Check Health").
					      put(LogMessage.MESSAGE, "Getting Vault Health information completed successfully").
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Healthy.All OK\"]}");
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Check Health").
					      put(LogMessage.MESSAGE, "Getting Vault Health information failed.").
					      put(LogMessage.RESPONSE, response.getResponse()).
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"Not OK \"]}");
			}
		}
	}
	
	/**
	 * Checks vault seal status
	 * @return
	 */
	public ResponseEntity<String> checkVaultSealStatus() {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Check Seal-status").
			      put(LogMessage.MESSAGE, "Trying to get Seal-Status of Vault Server").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/seal-status","{}","");
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":"+response.getResponse()+"}");
		} else {
			// If no response for http, then try https
			response = reqProcessor.process("/v2/seal-status","{}","");
			if(HttpStatus.OK.equals(response.getHttpstatus())) {
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":"+response.getResponse()+"}");
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "Check Seal-status").
					      put(LogMessage.MESSAGE, "Getting Vault Seal-Status failed.").
					      put(LogMessage.RESPONSE, response.getResponse()).
					      put(LogMessage.STATUS, response.getHttpstatus().toString()).
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));
				return ResponseEntity.status(response.getHttpstatus()).body("{\"Error\":[\"Unable to get Seal-Status information\"]}");
			}
		}
	}
	/**
	 * Unseals the vault
	 * @param unseal
	 * @return
	 */
	public ResponseEntity<String> unseal (Unseal unseal){
		String jsonStr = JSONUtil.getJSON(unseal);
		jsonStr = jsonStr.substring(0,jsonStr.lastIndexOf("}"));
		jsonStr = jsonStr+ ",\"port\":\""+vaultPort+"\"}";
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Unseal Vault").
			      put(LogMessage.MESSAGE, "Trying to unseal Vault Server").
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		// Try with https first
		Response response = reqProcessor.process("/unseal",jsonStr,"");
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Unseal Vault").
			      put(LogMessage.MESSAGE, "Unsealing Vault server completed").
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else {
			if(HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {
				// Try with http now
				response = reqProcessor.process("/v2/unseal",jsonStr,"");
			}
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}

	}
	
	public ResponseEntity<String> unsealProgress (String serverip){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Unseal Vault Progress").
			      put(LogMessage.MESSAGE, String.format("Unseal Vault Progress for [%s]", serverip)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		Response response = reqProcessor.process("/unseal-progress","{\"serverip\":\""+serverip+"\",\"port\":\""+vaultPort+"\"}","");
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Unseal Vault").
			      put(LogMessage.MESSAGE, String.format("Unsealing Vault server completed for [%s]", serverip)).
			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		else {
			if(HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {
				response = reqProcessor.process("/v2/unseal-progress","{\"serverip\":\""+serverip+"\",\"port\":\""+vaultPort+"\"}","");
			}
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
}
