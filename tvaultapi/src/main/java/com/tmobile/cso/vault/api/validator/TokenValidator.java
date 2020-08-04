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

package com.tmobile.cso.vault.api.validator;

import java.io.IOException;
import java.util.Arrays;

import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.model.DirectoryUser;
import com.tmobile.cso.vault.api.service.DirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.util.StringUtils;

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

	@Autowired
	DirectoryService directoryService;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;
	
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
				if ("oidc".equals(vaultAuthMethod) && objNode.get("display_name") != null) {
					// display_name is in format oidc-<email>
					String email = objNode.get("display_name").asText().substring(5);
					if (!StringUtils.isEmpty(email)) {
						lookupDetails.setEmail(email);
						ResponseEntity<DirectoryObjects> directoryObjectsResponseEntity = directoryService.searchByUPN(email);
						if (directoryObjectsResponseEntity != null && HttpStatus.OK.equals(directoryObjectsResponseEntity.getStatusCode())) {
							Object[] adUser = directoryObjectsResponseEntity.getBody().getData().getValues();
							if (adUser.length > 0) {
								DirectoryUser directoryUser = (DirectoryUser) adUser[0];
								lookupDetails.setUsername(directoryUser.getUserName());
							}
						}
					}
				}
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
