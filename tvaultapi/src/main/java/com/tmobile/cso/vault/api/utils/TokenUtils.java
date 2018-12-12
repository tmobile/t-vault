package com.tmobile.cso.vault.api.utils;

import java.util.Base64;
import java.util.LinkedHashMap;

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
	

	public TokenUtils() {

	}

	public String generatePowerToken(String token) {
		String roleName = "vault-power-user-role"; 
		Response appRoleRes = ControllerUtil.reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+roleName+"\"}",token); //TODO: How to get this role
		String role_id = null;
		if (appRoleRes.getResponse() != null && ((LinkedHashMap<String, String>)ControllerUtil.parseJson(appRoleRes.getResponse()).get("data")) != null) {
			role_id = ((LinkedHashMap<String, String>)ControllerUtil.parseJson(appRoleRes.getResponse()).get("data")).get("role_id");
		}
		
		Response secidRes = ControllerUtil.reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+roleName+"\"}",token);
		String secret_id = ((LinkedHashMap<String, String>)ControllerUtil.parseJson(secidRes.getResponse()).get("data")).get("secret_id");
		String powerToken = null;
		if (role_id != null && secret_id != null) {
			AppRoleIdSecretId appRoleIdSecretId = new AppRoleIdSecretId(role_id, secret_id);
			
			Response loginRes = ControllerUtil.reqProcessor.process("/auth/approle/login",JSONUtil.getJSON(appRoleIdSecretId),"");
			powerToken = ((LinkedHashMap<String, String>)ControllerUtil.parseJson(loginRes.getResponse()).get("auth")).get("client_token");
		}
		return powerToken;
		
	}
	
	public String getSelfServiceToken() {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "getSelfServiceToken").
				put(LogMessage.MESSAGE, String.format ("Trying to generate SelfServiceToken")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		String selfServiceToken = null;
		String tvaultSelfServiceUsername = new String(Base64.getDecoder().decode(selfserviceUsername));
		String tvaultSelfServicePassword = new String(Base64.getDecoder().decode(selfservicePassword));
		
		UserLogin userLogin = new UserLogin(tvaultSelfServiceUsername, tvaultSelfServicePassword);
		String jsonStr = JSONUtil.getJSON(userLogin);
		Response response = null;
		if ("ldap".equals(vaultAuthMethod)) {
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
	 * To revoke token
	 * @param token
	 */
	public void revokePowerToken(String token) {
		//TODO: Error handling, etc
		ControllerUtil.reqProcessor.process("/auth/tvault/revoke","{}", token);
	}


}
