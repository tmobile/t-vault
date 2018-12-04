package com.tmobile.cso.vault.api.utils;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.model.VaultTokenLookupDetails;
import com.tmobile.cso.vault.api.process.Response;
@Component
public class TokenUtils {

	private Logger log = LogManager.getLogger(TokenUtils.class);
	
	@Autowired
	private PolicyUtils policyUtils;
	
	public TokenUtils() {
		// TODO Auto-generated constructor stub
	}

	public String generatePowerToken(String token) {
		String roleName = "vault-power-user-role"; 
		Response appRoleRes = ControllerUtil.reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+roleName+"\"}",token); //TODO: How to get this role
		String role_id = ((LinkedHashMap<String, String>)ControllerUtil.parseJson(appRoleRes.getResponse()).get("data")).get("role_id");
	
		Response secidRes = ControllerUtil.reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+roleName+"\"}",token);
		String secret_id = ((LinkedHashMap<String, String>)ControllerUtil.parseJson(secidRes.getResponse()).get("data")).get("secret_id");
		AppRoleIdSecretId appRoleIdSecretId = new AppRoleIdSecretId(role_id, secret_id);
		
		Response loginRes = ControllerUtil.reqProcessor.process("/auth/approle/login",JSONUtil.getJSON(appRoleIdSecretId),"");
		String powerToken = ((LinkedHashMap<String, String>)ControllerUtil.parseJson(loginRes.getResponse()).get("auth")).get("client_token");
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

	/**
	 * Does the lookup of vault token and gets the details that are looked up
	 * @param token
	 * @return
	 */
	public VaultTokenLookupDetails getVaultTokenLookupDetails(String token) {
		Response response = ControllerUtil.reqProcessor.process("/auth/tvault/lookup","{}", token);
		VaultTokenLookupDetails lookupDetails = null;
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			try {
				lookupDetails = new VaultTokenLookupDetails();
				ObjectMapper objMapper = new ObjectMapper();
				String username = objMapper.readTree(response.getResponse()).get("username").asText();
				String[] policies = policyUtils.getPoliciesAsArray(objMapper, response.getResponse());
				lookupDetails.setUsername(username);
				lookupDetails.setPolicies(policies);
				lookupDetails.setToken(token);
			} catch (IOException e) {
				// TODO LOG ERROR?
				e.printStackTrace();
			}
		}
		return lookupDetails;
	}

	/**
	 * Get the name of the user from the given client token
	 * @param token
	 * @return
	 */
	public String getLoggedinUsername(String token) {
		ObjectMapper objMapper = new ObjectMapper();
		String username ="";
		Response response = ControllerUtil.reqProcessor.process("/auth/tvault/lookup","{}", token);	
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			try {
				username = objMapper.readTree(response.getResponse()).get("username").asText();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return username;
		}
		return username;
	}

	/**
	 * Gets the list of policies owned by the given user (based on client token lookup)
	 * @param token
	 * @return
	 */
	public String[] getCurrentPolicies(String token) {
		ObjectMapper objMapper = new ObjectMapper();
		Response response = ControllerUtil.reqProcessor.process("/auth/tvault/lookup","{}", token);	
		String currentpolicies[] = null;
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			String responseJson = response.getResponse();	
			try {
				currentpolicies = policyUtils.getPoliciesAsArray(objMapper, responseJson);
			} catch (IOException e) {
				ControllerUtil.log.error(e);
			}
		}
		return currentpolicies;
	}
}
