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

package com.tmobile.cso.vault.api.utils;

import java.util.Base64;
import java.util.LinkedHashMap;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
@Component
public class TokenUtils {

	@Autowired
	private RequestProcessor reqProcessor;
	
	private Logger log = LogManager.getLogger(TokenUtils.class);
	
	@Value("${vault.auth.method}")
	private String vaultAuthMethod;
	
	@Value("${selfservice.username}")
	private String selfserviceUsername;
	
	@Value("${selfservice.password}")
	private String selfservicePassword;

	@Value("${selfservice.tokengenerator}")
	private String selfServiceTokenGenerator;

	public TokenUtils() {

	}

	public String getSelfServiceToken() {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "getSelfServiceToken").
				put(LogMessage.MESSAGE, String.format ("Trying to generate SelfServiceToken")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (TVaultConstants.APPROLE.equals(selfServiceTokenGenerator)) {
			return getSelfServiceTokenWithAppRole();
		}
		String selfServiceToken = null;
		String tvaultSelfServiceUsername = new String(Base64.getDecoder().decode(selfserviceUsername));
		String tvaultSelfServicePassword = new String(Base64.getDecoder().decode(selfservicePassword));
		
		UserLogin userLogin = new UserLogin(tvaultSelfServiceUsername, tvaultSelfServicePassword);
		String jsonStr = JSONUtil.getJSON(userLogin);
		Response response = null;
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			response = reqProcessor.process("/auth/ldap/login",jsonStr,"");	
		}
		else {
			// Default to userpass
			response = reqProcessor.process("/auth/userpass/login",jsonStr,"");
		}
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getSelfServiceToken").
					put(LogMessage.MESSAGE, String.format ("SelfService token successfully created")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			String res = response.getResponse();
			if (!StringUtils.isEmpty(res)) {
				LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>)ControllerUtil.parseJson(response.getResponse());
				selfServiceToken = (String) responseMap.get("client_token");
			}
		}else{
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getSelfServiceToken").
					put(LogMessage.MESSAGE, String.format ("SelfService token failed")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return selfServiceToken;
	}

	/**
	 * Gets the Self Service token with AppRole using role_id and secret_id
	 * @return
	 */
	public String getSelfServiceTokenWithAppRole() {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "getSelfServiceTokenWithAppRole").
				put(LogMessage.MESSAGE, String.format ("Trying to generate SelfServiceTokenWithAppRole")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		String selfServiceToken = null;
		String role_id = new String(Base64.getDecoder().decode(selfserviceUsername));
		String secret_id = new String(Base64.getDecoder().decode(selfservicePassword));
		if (ControllerUtil.getSscred() != null) {
			role_id = new String(Base64.getDecoder().decode(ControllerUtil.getSsUsername()));
			secret_id = new String(Base64.getDecoder().decode(ControllerUtil.getSsPassword()));
		}
		AppRoleIdSecretId approleLogin = new AppRoleIdSecretId();
		approleLogin.setRole_id(role_id);
		approleLogin.setSecret_id(secret_id);
		String jsonStr = JSONUtil.getJSON(approleLogin);

		Response response  = reqProcessor.process("/auth/approle/login",jsonStr,"");

		if(HttpStatus.OK.equals(response.getHttpstatus())){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getSelfServiceTokenWithAppRole").
					put(LogMessage.MESSAGE, String.format ("SelfService token successfully created using AppRole")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			String res = response.getResponse();
			if (!StringUtils.isEmpty(res)) {
				LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>)ControllerUtil.parseJson(response.getResponse());
				if (responseMap.get("auth") != null) {
					selfServiceToken = (String)((LinkedHashMap<String, Object>) responseMap.get("auth")).get("client_token");
				}
			}
		}else{
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getSelfServiceTokenWithAppRole").
					put(LogMessage.MESSAGE, String.format ("SelfService token failed")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return selfServiceToken;
	}

	/**
	 * To revoke token
	 * @param token
	 */
	public void revokePowerToken(String token) {
		Response response = ControllerUtil.getReqProcessor().process("/auth/tvault/revoke","{}", token);
		if (HttpStatus.NO_CONTENT.equals(response.getHttpstatus()) || HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "revokePowerToken").
					put(LogMessage.MESSAGE, String.format ("SelfService token successfully revoked")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		else {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "revokePowerToken").
					put(LogMessage.MESSAGE, String.format ("SelfService token revoke failed")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
	}
}
