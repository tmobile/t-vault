package com.tmobile.cso.vault.api.v2.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.AzureServiceAccount;
import com.tmobile.cso.vault.api.model.AzureServiceAccountOffboardRequest;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.service.AzureServicePrinicipalAccountsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api( description = "Manage Azure Service Prinicipal and Secrets", position = 21)
public class AzureServicePrinicipalAccountsController {
	
	private static final String USER_DETAILS_STRING="UserDetails";
	
	@Autowired
	private AzureServicePrinicipalAccountsService azureServicePrinicipalAccountsService;
	
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.onboardAzureServiceAccount.value}", notes = "${AzureServicePrinicipalAccountsController.onboardAzureServiceAccount.notes}")
	@PostMapping(value="/v2/azureserviceaccounts/onboard", produces="application/json")
	public ResponseEntity<String> onboardAzureServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid AzureServiceAccount azureServiceAccount ){
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.onboardAzureServiceAccount(token, azureServiceAccount, userDetails);
	}
	
	/**
	 * Get the list of azure service prinicipal for users with permissions.
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.getAzureServicePrinicipalList.value}", notes = "${AzureServicePrinicipalAccountsController.getAzureServicePrinicipalList.notes}",hidden = false)
	@GetMapping (value="/v2/azureserviceaccounts/list",produces="application/json")
	public ResponseEntity<String> getAzureServicePrinicipalList(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.getAzureServicePrinicipalList(userDetails);
	}
	
	/**
	 * Read secrets from vault
	 * @param token
	 * @param path
	 * @param fetchOption
	 * @return
	 * @throws IOException 
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.readFolders.value}", notes = "${AzureServicePrinicipalAccountsController.readFolders.notes}", hidden = true)
	@GetMapping(value = "/v2/azureserviceaccounts/folders/secrets", produces = "application/json")
	public ResponseEntity<String> readFolders(@RequestHeader(value = "vault-token") String token,
			@RequestParam("path") String path) throws IOException {
		return azureServicePrinicipalAccountsService.readFolders(token, path);
	}
	
	/**
	 * Get azure service account detail with secretkey
	 * @param request
	 * @param token
	 * @param azure_svc_name
	 * @param folderName
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.getIAMServiceAccountSecretKey.value}", notes = "${AzureServicePrinicipalAccountsController.getIAMServiceAccountSecretKey.notes}", hidden = true)
	@GetMapping(value = "/v2/azureserviceaccounts/secrets/{azure_svc_name}/{folderName}", produces = "application/json")
	public ResponseEntity<String> getAzureServiceAccountSecretKey(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("azure_svc_name") String azureServiceAccountName,
			@PathVariable("folderName") String folderName) {
		return azureServicePrinicipalAccountsService.getAzureServiceAccountSecretKey(token, azureServiceAccountName, folderName);
	}
	
	/**
	 * Read Secret
	 * @param token
	 * @param azureSvcName
	 * @param accessKey
	 * @return
	 * @throws IOException 
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.readSecret.value}", notes = "${AzureServicePrinicipalAccountsController.readSecret.notes}", hidden = false)
	@GetMapping(value = "/v2/azureserviceaccounts/secret/{azure_svc_name}/{accessKey}", produces = "application/json")
	public ResponseEntity<String> readSecret(@RequestHeader(value = "vault-token") String token,
			@PathVariable("azure_svc_name") String azureSvcName,
			@PathVariable("accessKey") String accessKey) throws IOException {
		return azureServicePrinicipalAccountsService.readSecret(token, azureSvcName, accessKey);
	}
	
	/**
	 * Offboard Azure service account.
	 * @param request
	 * @param token
	 * @param azureServiceAccountOffboardRequest
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.offboardAzureServiceAccount.value}", notes = "${AzureServicePrinicipalAccountsController.offboardAzureServiceAccount.notes}")
	@PostMapping(value="/v2/azureserviceaccounts/offboard", produces="application/json")
	public ResponseEntity<String> offboardAzureServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AzureServiceAccountOffboardRequest azureServiceAccountOffboardRequest ){
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.offboardAzureServiceAccount(token, azureServiceAccountOffboardRequest, userDetails);
	}

}
