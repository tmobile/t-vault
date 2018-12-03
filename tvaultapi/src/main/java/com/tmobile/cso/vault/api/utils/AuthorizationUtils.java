package com.tmobile.cso.vault.api.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.VaultTokenLookupDetails;
@Component
public class AuthorizationUtils {

	
	@Autowired
	private TokenUtils tokenUtils;
	
	@Autowired
	private SafeUtils safeUtils;
	
	@Autowired
	private PolicyUtils policyUtils;
	
	public AuthorizationUtils() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Checks whether the given user can edit the given safe.
	 * @param token
	 * @param safeType
	 * @param safeName
	 * @return
	 */
	public boolean isAuthorized(String token, String safeType, String safeName) {
		boolean authorized = false;
		String powerToken = tokenUtils.generatePowerToken(token);
		VaultTokenLookupDetails loggedinUserLookupDetails = tokenUtils.getVaultTokenLookupDetails(token);
		String[] loggedinUserPolicies = loggedinUserLookupDetails.getPolicies();
		
		Safe targetSafeMetadata = safeUtils.getSafeMetaDataWithAppRoleElevation(token, safeType, safeName);
		
		if (targetSafeMetadata == null) {
			// There is no safe
			authorized = false;
			return authorized;
		}
		ArrayList<String> policiesTobeChecked = policyUtils.getPoliciesTobeCheked(safeType, safeName);
		for (String policyTobeChecked: policiesTobeChecked) {
			String policyKeyTobeChecked = new StringBuffer().append(safeType).toString();
			authorized = isAuthorized(loggedinUserPolicies, policyTobeChecked, policyKeyTobeChecked, powerToken);
			if (authorized) {
				break;
			}
		}
		
		return authorized;
	}

	/**
	 * 
	 * @param currentUserPolicies
	 * @param lookupPolicyName
	 * @param lookupPolicyKey
	 * @param powerToken
	 * @return
	 */
	private boolean isAuthorized(String[] currentUserPolicies, String lookupPolicyName, String lookupPolicyKey, String powerToken) {
		boolean authorized = false;
		for (String currentPolicy: currentUserPolicies) {
			if (currentPolicy.startsWith(lookupPolicyName)) {
				authorized = isAuthorized(currentPolicy, lookupPolicyName, lookupPolicyKey, powerToken);
				break;
			}
		}
		return authorized;
	}
	
	/**
	 * 
	 * @param currentPolicy
	 * @param lookupPolicyName
	 * @param lookupPolicyKey
	 * @param powerToken
	 * @return
	 */
	private boolean isAuthorized(String currentPolicy, String lookupPolicyName, String lookupPolicyKey, String powerToken) {
		boolean authorized = false;
		// Open the policy and check whether there is real policy entry...
		LinkedHashMap<String, LinkedHashMap<String, Object>> capabilitiesMap = policyUtils.getPolicyInfo(currentPolicy, powerToken);
		for (Map.Entry<String, LinkedHashMap<String, Object>> entry : capabilitiesMap.entrySet()) {
		    String capKey = entry.getKey();
		    LinkedHashMap<String, Object> value = entry.getValue();
		    // now work with key and value...
		    if (capKey.startsWith(lookupPolicyKey)) {
		    	for (Map.Entry<String, Object> valEntry : value.entrySet()) {
		    		if (valEntry.getValue() instanceof String) {
			    		//String valKey = valEntry.getKey();
			    		String capability = valEntry.getValue().toString();
			    		if (capability.toLowerCase().startsWith("write") || capability.toLowerCase().startsWith("sudo")) {
							authorized = true;
							break;
			    		}
		    		}
		    		else if (valEntry.getValue() instanceof ArrayList<?>) {
		    			ArrayList<Object> capabilities = (ArrayList<Object>)valEntry.getValue();
			    		if (capabilities.contains("create")|| capabilities.contains("update") || capabilities.contains("delete") ) {
							authorized = true;
							break;
			    		}
		    		}
		    	}
		    }
		    if (authorized) {
		    	break;
		    }
		}
		return authorized;
	}
	/**
	 * Checks whether policies passed contains any admin policies
	 * @param policies
	 * @return
	 */
	public boolean containsAdminPolicies(List<String> policies, List<String> adminPolicies) {
		List<String> commonPolicies = (List<String>) CollectionUtils.intersection(policies, adminPolicies);
		return commonPolicies.size() > 0 ? true: false;
	}
}
