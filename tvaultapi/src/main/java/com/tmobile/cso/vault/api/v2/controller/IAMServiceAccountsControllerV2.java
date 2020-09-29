package com.tmobile.cso.vault.api.v2.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.service.IAMServiceAccountsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api( description = "Manage IAM Service Accounts", position = 19)
public class IAMServiceAccountsControllerV2 {

	
	@Autowired
	private IAMServiceAccountsService iamServiceAccountsService;
	
	public static final String USER_DETAILS_STRING="UserDetails";
	
	
	/**
	 * Gets the list of iam service accounts onboarded for password rotation
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${IAMServiceAccountsControllerV2.getOnboardedIAMServiceAccounts.value}", notes = "${IAMServiceAccountsControllerV2.getOnboardedIAMServiceAccounts.notes}")
	@GetMapping(value="/v2/iamserviceaccounts", produces="application/json")
	public ResponseEntity<String> getOnboardedIAMServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return iamServiceAccountsService.getOnboardedIAMServiceAccounts(token, userDetails);
	}
	
	/**
	 * Get the list of iam service accounts for users with permissions.
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${IAMServiceAccountsControllerV2.getIAMServiceAccountsList.value}", notes = "${IAMServiceAccountsControllerV2.getIAMServiceAccountsList.notes}",hidden = false)
	@GetMapping (value="/v2/iamserviceaccounts/list",produces="application/json")
	public ResponseEntity<String> getIAMServiceAccountsList(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return iamServiceAccountsService.getIAMServiceAccountsList(userDetails, token);
	}
	
	/**
	 * Get iam service account detail from metadata
	 * 
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${IAMServiceAccountsControllerV2.getIAMServiceAccountDetail.value}", notes = "${IAMServiceAccountsControllerV2.getIAMServiceAccountDetail.notes}", hidden = false)
	@GetMapping(value = "/v2/iamserviceaccounts/{iam_svc_name}", produces = "application/json")
	public ResponseEntity<String> getIAMServiceAccountDetail(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("iam_svc_name") String iamsvcname){
		return iamServiceAccountsService.getIAMServiceAccountDetail(token, iamsvcname);
	}
	
	
	/**
	 * Get iam service account detail with secretkey
	 * 
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${IAMServiceAccountsControllerV2.getIAMServiceAccountSecretKey.value}", notes = "${IAMServiceAccountsControllerV2.getIAMServiceAccountSecretKey.notes}", hidden = false)
	@GetMapping(value = "/v2/iamserviceaccounts/secrets/{iam_svc_name}", produces = "application/json")
	public ResponseEntity<String> getIAMServiceAccountSecretKey(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("iam_svc_name") String iamsvcname){
		return iamServiceAccountsService.getIAMServiceAccountSecretKey(token, iamsvcname);
	}
	
	
	
	
}
