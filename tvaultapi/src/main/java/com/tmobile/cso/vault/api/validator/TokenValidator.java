package com.tmobile.cso.vault.api.validator;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.VaultTokenLookupDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.AuthorizationUtils;
import com.tmobile.cso.vault.api.utils.CommonUtils;
import com.tmobile.cso.vault.api.utils.PolicyUtils;

@Component
public class TokenValidator {
	
	@Autowired
	private RequestProcessor reqProcessor;
	
	@Autowired
	private AuthorizationUtils authorizationUtils;
	
	@Autowired
	private CommonUtils commonUtils;
	
	@Autowired
	private PolicyUtils policyUtils;
	
	public TokenValidator() {
	}
	/**
	 * Does the lookup of vault token and gets the details that are looked up
	 * @param token
	 * @return
	 */
	public VaultTokenLookupDetails getVaultTokenLookupDetails(String token) throws TVaultValidationException{
		Response response = reqProcessor.process("/auth/tvault/lookup","{}", token);
		VaultTokenLookupDetails lookupDetails = null;
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			try {
				lookupDetails = new VaultTokenLookupDetails();
				ObjectMapper objMapper = new ObjectMapper();
				ObjectNode objNode = (ObjectNode) objMapper.readTree(response.getResponse());
				lookupDetails.setUsername(objNode.get("username").asText());
				String[] policies = commonUtils.getPoliciesAsArray(objMapper, response.getResponse());
				lookupDetails.setPolicies(policies);
				lookupDetails.setToken(token);
				lookupDetails.setValid(true);
				lookupDetails.setAdmin(authorizationUtils.containsAdminPolicies(Arrays.asList(policies),  policyUtils.getAdminPolicies()));
			} catch (IOException e) {
				throw new TVaultValidationException(e);
			}
		}
		else if(HttpStatus.FORBIDDEN.equals(response.getHttpstatus())){
			throw new TVaultValidationException(
					String.format("Can't perform the required operation. Possible reasons: 1. Invalid/expired client token or 2. Insufficient permissions. Actual Status: [%s], Reason [%s]", response.getHttpstatus(), response.getResponse()));
		}
		else {
			throw new TVaultValidationException(
					String.format("Can't perform the required operation. Actual Status: [%s], Reason [%s]", response.getHttpstatus(), response.getResponse()));
		}
		return lookupDetails;
	}
}
