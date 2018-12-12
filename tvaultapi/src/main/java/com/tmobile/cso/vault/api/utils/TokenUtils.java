package com.tmobile.cso.vault.api.utils;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.process.Response;
@Component
public class TokenUtils {

	private Logger log = LogManager.getLogger(TokenUtils.class);
	
	public TokenUtils() {
		// TODO Auto-generated constructor stub
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
	/**
	 * To revoke token
	 * @param token
	 */
	public void revokePowerToken(String token) {
		//TODO: Error handling, etc
		ControllerUtil.reqProcessor.process("/auth/tvault/revoke","{}", token);
	}


}
