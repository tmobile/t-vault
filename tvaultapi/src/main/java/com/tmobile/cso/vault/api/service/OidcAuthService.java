// =========================================================================
// Copyright 2020 T-Mobile, US
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.OidcRequest;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.AuthorizationUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class OidcAuthService {

	@Autowired
	private RequestProcessor reqProcessor;
	
	@Autowired
	private AuthorizationUtils authorizationUtils;

	@Autowired
	private VaultAuthService vaultAuthService;

	@Value("${selfservice.enable}")
	private boolean isSSEnabled;

	@Value("${ad.passwordrotation.enable}")
	private boolean isAdPswdRotationEnabled;

	private static Logger log = LogManager.getLogger(OidcAuthService.class);

	/**
	 * To get OIDC auth url.
	 * @param token
	 * @param oidcRequest
	 * @return
	 */
	public ResponseEntity<String> getAuthUrl(String token, OidcRequest oidcRequest) {
		String jsonStr = JSONUtil.getJSON(oidcRequest);
		Response response = reqProcessor.process("/auth/oidc/oidc/auth_url",jsonStr,token);
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getAuthUrl").
					put(LogMessage.MESSAGE, "Successfully retrieved OIDC auth url").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getAuthUrl").
					put(LogMessage.MESSAGE, String.format ("Failed to get OIDC auth url [%s]", response.getResponse())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Failed to get OIDC auth url\"]}");
		}
	}

	/**
	 * To get vault token with OIDC callback state and code.
	 * @param token
	 * @param state
	 * @param code
	 * @return
	 */
	public ResponseEntity<String> processCallback(String token, String state, String code) {
		String pathStr = "?code="+code+"&state="+state;
		Response response = reqProcessor.process("/auth/oidc/oidc/callback","{\"path\":\""+pathStr+"\"}",token);
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			Map<String, Object> responseMap = null;
			try {
				responseMap = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "processCallback").
						put(LogMessage.MESSAGE, "Failed to getresponse map from callback response").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			if(responseMap!=null && responseMap.get("access")!=null) {
				Map<String,Object> access = (Map<String,Object>)responseMap.get("access");
				access = vaultAuthService.filterDuplicateSafePermissions(access);
				access = vaultAuthService.filterDuplicateSvcaccPermissions(access);
				responseMap.put("access", access);
				// set SS, AD password rotation enable status
				Map<String,Object> feature = new HashMap<>();
				feature.put(TVaultConstants.SELFSERVICE, isSSEnabled);
				feature.put(TVaultConstants.ADAUTOROTATION, isAdPswdRotationEnabled);
				responseMap.put("feature", feature);
				response.setResponse(JSONUtil.getJSON(responseMap));
			}

			log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "processCallback").
					put(LogMessage.MESSAGE, "Successfully retrieved token from OIDC login").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "processCallback").
				put(LogMessage.MESSAGE, "Failed to get token from OIDC login").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
}
