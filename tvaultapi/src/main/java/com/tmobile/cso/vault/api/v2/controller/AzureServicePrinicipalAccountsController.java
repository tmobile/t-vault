package com.tmobile.cso.vault.api.v2.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.tmobile.cso.vault.api.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.AzureServiceAccount;
import com.tmobile.cso.vault.api.model.AzureServiceAccountAWSRole;
import com.tmobile.cso.vault.api.model.AzureServiceAccountGroup;
import com.tmobile.cso.vault.api.model.AzureServiceAccountOffboardRequest;
import com.tmobile.cso.vault.api.model.AzureServiceAccountUser;
import com.tmobile.cso.vault.api.model.IAMServiceAccountGroup;
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
	 * Get the list of azure service principal for users with permissions.
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
	@GetMapping(value = "/v2/azureserviceaccounts/secret/{azure_svc_name}/{secretKey}", produces = "application/json")
	public ResponseEntity<String> readSecret(@RequestHeader(value = "vault-token") String token,
			@PathVariable("azure_svc_name") String azureSvcName,
			@PathVariable("secretKey") String secretKey) throws IOException {
		return azureServicePrinicipalAccountsService.readSecret(token, azureSvcName, secretKey);
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
	/**
	 * Gets the list of azure service principal onboarded
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.getOnboardedAsureServicePrincipal.value}", notes = "${AzureServicePrinicipalAccountsController.getOnboardedAsureServicePrincipal.notes}",hidden = false)
	@GetMapping(value="/v2/azureserviceaccounts", produces="application/json")
	public ResponseEntity<String> getOnboardedIAMServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.getOnboardedAzureServiceAccounts(token, userDetails);
	}

	/**
	 * Add user to Azure Service Principal
	 * @param azureServiceAccountUser
	 * @return
	 */
	@PostMapping(value="/v2/azureserviceaccounts/user",produces="application/json")
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.addUserToAzureServicePrincipal.value}", notes = "${AzureServicePrinicipalAccountsController.addUserToAzureServicePrincipal.notes}")
	public ResponseEntity<String> addUserToAzureServicePrincipal(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid AzureServiceAccountUser azureServiceAccountUser){
	   UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
	   return azureServicePrinicipalAccountsService.addUserToAzureServiceAccount(token, userDetails, azureServiceAccountUser, false);
	}
	/**
	 * Removes permission for a user from the Azure service account
	 * @param request
	 * @param token
	 * @param azureServiceAccountUser
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.removeUserFromAzureServiceAccount.value}", notes = "${AzureServicePrinicipalAccountsController.removeUserFromAzureServiceAccount.notes}")
	@DeleteMapping(value="/v2/azureserviceaccounts/user", produces="application/json")
	public ResponseEntity<String> removeUserFromAzureServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody AzureServiceAccountUser azureServiceAccountUser ){
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.removeUserFromAzureServiceAccount(token, azureServiceAccountUser, userDetails);
	}
	/**
	 * Method to create an aws app role
	 * @param token
	 * @param awsLoginRole
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.createAWSRole.value}", notes = "${AzureServicePrinicipalAccountsController.createAWSRole.notes}")
	@PostMapping(value="/v2/azureserviceaccounts/aws/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> createAWSRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSLoginRole awsLoginRole) throws TVaultValidationException {
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.createAWSRole(userDetails, token, awsLoginRole);
	}

	/**
	 * Method to create aws iam role
	 * @param token
	 * @param awsiamRole
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.createIamRole.value}", notes = "${AzureServicePrinicipalAccountsController.createIamRole.notes}")
	@PostMapping(value="/v2/azureserviceaccounts/aws/iam/role",produces="application/json")
	public ResponseEntity<String> createIAMRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSIAMRole awsiamRole) throws TVaultValidationException{
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.createIAMRole(userDetails, token, awsiamRole);
	}

	/**
	 * Adds AWS role to Azure Service Account
	 *
	 * @param token
	 * @param azureServiceAccountAWSRole
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.addAwsRoleToAzureSvcacc.value}", notes = "${AzureServicePrinicipalAccountsController.addAwsRoleToAzureSvcacc.notes}")
	@PostMapping(value = "/v2/azureserviceaccounts/role", produces = "application/json")
	public ResponseEntity<String> addAwsRoleToAzureSvcacc(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token,
			@Valid @RequestBody AzureServiceAccountAWSRole azureServiceAccountAWSRole) {
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.addAwsRoleToAzureSvcacc(userDetails, token, azureServiceAccountAWSRole);
	}

	/**
	 * Get Azure Service Principal detail from metadata
	 * @param request
	 * @param token
	 * @param azureSvcName
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.getAzureServicePrincipalDetail.value}", notes = "${AzureServicePrinicipalAccountsController.getAzureServicePrincipalDetail.notes}", hidden = true)
	@GetMapping(value = "/v2/azureserviceaccounts/{azure_svc_name}", produces = "application/json")
	public ResponseEntity<String> getAzureServicePrincipalDetail(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @PathVariable("azure_svc_name") String azureSvcName){
		return azureServicePrinicipalAccountsService.getAzureServicePrincipalDetail(token, azureSvcName);
	}
	/**
	 * Remove AWS role from Azure Service Account
	 * 
	 * @param token
	 * @param azureServiceAccountAWSRole
	 * @return
	 */
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.removeAwsRoleToAzureSvcacc.value}", notes = "${AzureServicePrinicipalAccountsController.removeAwsRoleToAzureSvcacc.notes}" ,hidden = false)
	@DeleteMapping(value = "/v2/azureserviceaccounts/role", produces = "application/json")
	public ResponseEntity<String> removeAwsRoleToAzureSvcacc(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token,
			@Valid @RequestBody AzureServiceAccountAWSRole azureServiceAccountAWSRole) {
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.removeAwsRoleFromAzureSvcacc(userDetails, token, azureServiceAccountAWSRole);
	}

	/**
	 * Add Group to Azure Service Principal.
	 * @param request
	 * @param token
	 * @param azureServiceAccountGroup
	 * @return
	 */
	@PostMapping(value="/v2/azureserviceaccounts/group",produces="application/json")
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.addGroupToAzureServiceAccount.value}", notes = "${AzureServicePrinicipalAccountsController.addGroupToAzureServiceAccount.notes}")
	public ResponseEntity<String> addGroupToAzureServiceAccount(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid AzureServiceAccountGroup azureServiceAccountGroup){
	   UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
	   return azureServicePrinicipalAccountsService.addGroupToAzureServiceAccount(token, azureServiceAccountGroup, userDetails);
	}

	/**
	 * Activate Azure Service Principal.
	 * @param request
	 * @param token
	 * @param servicePrinicipalName
	 * @return
	 */
	@PostMapping(value="/v2/azureserviceaccounts/activateAzureServicePrinicipal",produces="application/json")
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.activateAzureServicePrinicipal.value}", notes = "${AzureServicePrinicipalAccountsController.activateAzureServicePrinicipal.notes}")
	public ResponseEntity<String> activateAzureServicePrinicipal(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("servicePrinicipalName") String servicePrinicipalName){
		UserDetails userDetails = (UserDetails) request.getAttribute(USER_DETAILS_STRING);
		return azureServicePrinicipalAccountsService.activateAzureServicePrinicipal(token, userDetails, servicePrinicipalName);
	}

	/**
	 * Rotate Azure Service Principal secret by secretKeyId.
	 * @param request
	 * @param token
	 * @param azureServicePrinicipalRotateRequest
	 * @return
	 */
	@PostMapping(value="/v2/azureserviceaccounts/rotate",produces="application/json")
	@ApiOperation(value = "${AzureServicePrinicipalAccountsController.rotateSecret.value}", notes = "${AzureServicePrinicipalAccountsController.rotateSecret.notes}")
	public ResponseEntity<String> rotateSecret(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid AzureServicePrinicipalRotateRequest azureServicePrinicipalRotateRequest){
		return azureServicePrinicipalAccountsService.rotateSecret(token, azureServicePrinicipalRotateRequest);
	}
}
