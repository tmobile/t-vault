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
package com.tmobile.cso.vault.api.v2.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tmobile.cso.vault.api.service.ServiceAccountsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.List;

@RestController
@CrossOrigin
@Api( description = "Manage Service Accounts Passwords", position = 13)
public class ServiceAccountsControllerV2 {

	@Autowired
	private ServiceAccountsService serviceAccountsService;
	/**
	 * Gets the list of service account from AD
	 * @param request
	 * @param token
	 * @param serviceAccountName
	 * @param excludeOnboarded
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.getADServiceAccounts.value}", notes = "${ServiceAccountsControllerV2.getADServiceAccounts.notes}")
	@GetMapping(value="/v2/ad/serviceaccounts", produces="application/json")
	public ResponseEntity<ADServiceAccountObjects> getADServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("serviceAccountName" ) String serviceAccountName, @RequestParam(value="excludeOnboarded", defaultValue="true") boolean excludeOnboarded ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getADServiceAccounts(token, userDetails, serviceAccountName, excludeOnboarded);
	}
	/**
	 * Gets the list of service accounts onboarded for password rotation
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.getServiceAccounts.value}", notes = "${ServiceAccountsControllerV2.getServiceAccounts.notes}")
	@GetMapping(value="/v2/serviceaccounts", produces="application/json")
	public ResponseEntity<String> getServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getOnboardedServiceAccounts(token, userDetails);
	}
	/**
	 * Gets the details of an onboarded service account
	 * @param request
	 * @param token
	 * @param svcAccName
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.getServiceAccountDetails.value}", notes = "${ServiceAccountsControllerV2.getServiceAccountDetails.notes}")
	@GetMapping(value="/v2/serviceaccounts/{service_account_name}", produces="application/json")
	public ResponseEntity<String> getServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @PathVariable("service_account_name" ) String svcAccName){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getOnboarderdServiceAccount(token, svcAccName, userDetails);
	}
	/**
	 * Onbaords a service account for password rotation
	 * @param request
	 * @param token
	 * @param serviceAccount
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.onboardServiceAccount.value}", notes = "${ServiceAccountsControllerV2.onboardServiceAccount.notes}")
	@PostMapping(value="/v2/serviceaccounts/onboard", produces="application/json")
	public ResponseEntity<String> onboardServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid ServiceAccount serviceAccount ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
	}
	/**
	 * Grants an user service account permission
	 * @param request
	 * @param token
	 * @param serviceAccountUser
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.addUserToServiceAccount.value}", notes = "${ServiceAccountsControllerV2.addUserToServiceAccount.notes}")
	@PostMapping(value="/v2/serviceaccounts/user", produces="application/json")
	public ResponseEntity<String> addUserToSvcAcc( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody ServiceAccountUser serviceAccountUser ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails, false);
	}
	/**
	 * Removes permission for a user from the service account
	 * @param request
	 * @param token
	 * @param serviceAccountUser
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.removeUserServiceAccount.value}", notes = "${ServiceAccountsControllerV2.removeUserServiceAccount.notes}")
	@DeleteMapping(value="/v2/serviceaccounts/user", produces="application/json")
	public ResponseEntity<String> removeUserServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody ServiceAccountUser serviceAccountUser ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
	}
	/**
	 * To reset service account password
	 * @param request
	 * @param token
	 * @param serviceAccountName
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.resetPassword.value}", notes = "${ServiceAccountsControllerV2.resetPassword.notes}")
	@PutMapping(value="/v2/serviceaccounts/password", produces="application/json")
	public ResponseEntity<String> resetSvcAccPwd( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("serviceAccountName" ) String serviceAccountName ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.resetSvcAccPassword(token, serviceAccountName, userDetails);
	}
	/**
	 * To read service account password
	 * @param request
	 * @param token
	 * @param serviceAccountName
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.readPassword.value}", notes = "${ServiceAccountsControllerV2.readPassword.notes}")
	@GetMapping(value="/v2/serviceaccounts/password", produces="application/json")
	public ResponseEntity<String> readSvcAccPwd( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("serviceAccountName" ) String serviceAccountName ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.readSvcAccPassword(token, serviceAccountName, userDetails);
	}
	/**
	 * Offboard a service account from password rotation
	 * @param request
	 * @param token
	 * @param serviceAccount
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.offboardServiceAccount.value}", notes = "${ServiceAccountsControllerV2.offboardServiceAccount.notes}")
	@PostMapping(value="/v2/serviceaccounts/offboard", produces="application/json")
	public ResponseEntity<String> offboardServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody OnboardedServiceAccount serviceAccount ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.offboardServiceAccount(token, serviceAccount, userDetails);
	}

    /**
     * Add group to Service Account
     * @param request
     * @param token
     * @param serviceAccountGroup
     * @return
     */
	@ApiOperation(value = "${ServiceAccountsControllerV2.addGroupToServiceAccount.value}", notes = "${ServiceAccountsControllerV2.addGroupToServiceAccount.notes}")
	@PostMapping(value="/v2/serviceaccounts/group", produces="application/json")
	public ResponseEntity<String> addGroupToSvcAcc( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody ServiceAccountGroup serviceAccountGroup ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
	}

    /**
     * Remove group from Service Account
     * @param request
     * @param token
     * @param serviceAccountGroup
     * @return
     */
    @ApiOperation(value = "${ServiceAccountsControllerV2.removeGroupFromServiceAccount.value}", notes = "${ServiceAccountsControllerV2.removeGroupFromServiceAccount.notes}")
    @DeleteMapping(value="/v2/serviceaccounts/group", produces="application/json")
    public ResponseEntity<String> removeGroupFromSvcAcc( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody ServiceAccountGroup serviceAccountGroup ){
        UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
        return serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
    }

    /**
     * Add approle to Service Account
     * @param request
     * @param token
     * @param serviceAccountApprole
     * @return
     */
    @ApiOperation(value = "${ServiceAccountsControllerV2.associateApprole.value}", notes = "${ServiceAccountsControllerV2.associateApprole.notes}")
    @PostMapping(value="/v2/serviceaccounts/approle",consumes="application/json",produces="application/json")
    public ResponseEntity<String>associateApproletoSvcAcc(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody ServiceAccountApprole serviceAccountApprole) {
        UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
        return serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);
    }

	/**
	 * Remove approle from Service Account
	 * @param request
	 * @param token
	 * @param serviceAccountApprole
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.removeApprole.value}", notes = "${ServiceAccountsControllerV2.removeApprole.notes}")
	@DeleteMapping(value="/v2/serviceaccounts/approle",consumes="application/json",produces="application/json")
	public ResponseEntity<String>removeApproleFromSvcAcc(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody ServiceAccountApprole serviceAccountApprole) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.removeApproleFromSvcAcc(userDetails, token, serviceAccountApprole);
	}

	/**
	 * Get metadata for service account
	 * @param request
	 * @param token
	 * @param path
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.getServiceAccountsMeta.value}", notes = "${ServiceAccountsControllerV2.getServiceAccountsMeta.notes}", hidden=true)
	@GetMapping(value="/v2/serviceaccounts/meta", produces="application/json")
	public ResponseEntity<String> getServiceAccountMeta(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("path" ) String path){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getServiceAccountMeta(token, userDetails, path);
	}

	/**
	 * Method to create an aws app role
	 * @param token
	 * @param awsLoginRole
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.createAWSRole.value}", notes = "${ServiceAccountsControllerV2.createAWSRole.notes}")
	@PostMapping(value="/v2/serviceaccounts/aws/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> createAWSRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSLoginRole awsLoginRole) throws TVaultValidationException {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.createAWSRole(userDetails, token, awsLoginRole);
	}

	/**
	 * Method to create aws iam role
	 * @param token
	 * @param awsiamRole
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.createIamRole.value}", notes = "${ServiceAccountsControllerV2.createIamRole.notes}")
	@PostMapping(value="/v2/serviceaccounts/aws/iam/role",produces="application/json")
	public ResponseEntity<String> createIAMRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSIAMRole awsiamRole) throws TVaultValidationException{
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.createIAMRole(userDetails, token, awsiamRole);
	}

	/**
	 * Adds AWS role to Service Account
	 * @param token
	 * @param serviceAccountAWSRole
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.addAWSRole.value}", notes = "${ServiceAccountsControllerV2.addAWSRole.notes}")
	@PostMapping (value="/v2/serviceaccounts/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> addAwsRoleToSvcacc(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody ServiceAccountAWSRole serviceAccountAWSRole){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.addAwsRoleToSvcacc(userDetails, token, serviceAccountAWSRole);
	}

	/**
	 * Remove AWS role from Safe and delete the role
	 * @param token
	 * @param serviceAccountAWSRole
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.removeAWSRole.value}", notes = "${ServiceAccountsControllerV2.removeAWSRole.notes}")
	@DeleteMapping (value="/v2/serviceaccounts/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> removeAWSRoleFromSvcacc(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody ServiceAccountAWSRole serviceAccountAWSRole){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.removeAWSRoleFromSvcacc(userDetails, token, serviceAccountAWSRole);
	}

	/**
	 * Update onboarded service account
	 * @param request
	 * @param token
	 * @param serviceAccount
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.updateOnboardServiceAccount.value}", notes = "${ServiceAccountsControllerV2.updateOnboardServiceAccount.notes}")
	@PutMapping(value="/v2/serviceaccounts/onboard", produces="application/json")
	public ResponseEntity<String> updateOnboardedServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid ServiceAccount serviceAccount ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.updateOnboardedServiceAccount(token, serviceAccount, userDetails);
	}

	/**
	 * Change service account owner
	 * @param token
	 * @param serviceAccountName
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.transferSvcAccountOwner.value}", notes = "${ServiceAccountsControllerV2.transferSvcAccountOwner.notes}")
	@PostMapping (value="/v2/serviceaccounts/transfer",produces="application/json")
	public ResponseEntity<String> transferSvcAccountOwner(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("serviceAccountName" ) String serviceAccountName){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.transferSvcAccountOwner(userDetails, token, serviceAccountName);
	}
	
	/**
	 * 
	 * @param request
	 * @param token
	 * @param serviceAccountName
	 * @return
	 */
	@ApiOperation(value = "${ServiceAccountsControllerV2.getServiceAccountsList.value}", notes = "${ServiceAccountsControllerV2.getServiceAccountsList.notes}",hidden = true)
	@GetMapping (value="/v2/serviceaccounts/list",produces="application/json")
	public ResponseEntity<String> getServiceAccountsList(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getServiceAccounts(userDetails, token);
	}


}
