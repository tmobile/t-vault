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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.getADServiceAccounts.value}", notes = "${ServiceAccountsControllerV2.getADServiceAccounts.notes}")
	@GetMapping(value="/v2/ad/serviceaccounts", produces="application/json")
	public ResponseEntity<ADServiceAccountObjects> getADServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("serviceAccountName" ) String serviceAccountName, @RequestParam(value="excludeOnboarded", defaultValue="true") boolean excludeOnboarded ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getADServiceAccounts(token, userDetails, serviceAccountName, excludeOnboarded);
	}
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.getServiceAccounts.value}", notes = "${ServiceAccountsControllerV2.getServiceAccounts.notes}")
	@GetMapping(value="/v2/serviceaccounts", produces="application/json")
	public ResponseEntity<String> getServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getOnboardedServiceAccounts(token, userDetails);
	}
	@ApiOperation(value = "${ServiceAccountsControllerV2.getServiceAccountDetails.value}", notes = "${ServiceAccountsControllerV2.getServiceAccountDetails.notes}")
	@GetMapping(value="/v2/serviceaccounts/{service_account_name}", produces="application/json")
	public ResponseEntity<String> getServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @PathVariable("service_account_name" ) String svcAccName){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getOnboarderdServiceAccount(token, svcAccName, userDetails);
	}
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.onboardServiceAccount.value}", notes = "${ServiceAccountsControllerV2.onboardServiceAccount.notes}")
	@PostMapping(value="/v2/serviceaccounts/onboard", produces="application/json")
	public ResponseEntity<String> onboardServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid ServiceAccount serviceAccount ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
	}

	@ApiOperation(value = "${ServiceAccountsControllerV2.addUserToServiceAccount.value}", notes = "${ServiceAccountsControllerV2.addUserToServiceAccount.notes}")
	@PostMapping(value="/v2/serviceaccounts/user", produces="application/json")
	public ResponseEntity<String> addUserToSvcAcc( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody ServiceAccountUser serviceAccountUser ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
	}
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.removeUserServiceAccount.value}", notes = "${ServiceAccountsControllerV2.removeUserServiceAccount.notes}")
	@DeleteMapping(value="/v2/serviceaccounts/user", produces="application/json")
	public ResponseEntity<String> removeUserServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody ServiceAccountUser serviceAccountUser ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
	}
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.resetPassword.value}", notes = "${ServiceAccountsControllerV2.resetPassword.notes}")
	@GetMapping(value="/v2/serviceaccounts/password/reset", produces="application/json")
	public ResponseEntity<String> resetSvcAccPwd( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam("serviceAccountName" ) String serviceAccountName ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.resetSvcAccPassword(token, serviceAccountName, userDetails);
	}
	
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
	@ApiOperation(value = "${ServiceAccountsControllerV2.createRole.value}", notes = "${ServiceAccountsControllerV2.createRole.notes}")
	@PostMapping(value="/v2/serviceaccounts/aws/role",consumes="application/json",produces="application/json")
	public ResponseEntity<String> createRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody AWSLoginRole awsLoginRole) throws TVaultValidationException {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.createRole(userDetails, token, awsLoginRole);
	}

	/**
	 * Method to create aws iam role
	 * @param token
	 * @param awsiamRole
	 * @return
	 */
	@ApiOperation(value = "${SelfSupportController.createIamRole.value}", notes = "${SelfSupportController.createIamRole.notes}")
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
}
