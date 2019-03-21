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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.ADServiceAccountObjects;
import com.tmobile.cso.vault.api.model.OnboardedServiceAccount;
import com.tmobile.cso.vault.api.model.ServiceAccount;
import com.tmobile.cso.vault.api.model.ServiceAccountUser;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.service.ServiceAccountsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api( description = "Manage Service Accounts Passwords", position = 13)
public class ServiceAccountsControllerV2 {

	@Autowired
	private ServiceAccountsService serviceAccountsService;
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.getADServiceAccounts.value}", notes = "${ServiceAccountsControllerV2.getADServiceAccounts.notes}")
	@GetMapping(value="/v2/ad/serviceaccounts", produces="application/json")
	public ResponseEntity<ADServiceAccountObjects> getADServiceAccounts(@RequestParam("serviceAccountName" ) String serviceAccountName ){
		return serviceAccountsService.getADServiceAccounts(serviceAccountName);
	}
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.getServiceAccounts.value}", notes = "${ServiceAccountsControllerV2.getServiceAccounts.notes}")
	@GetMapping(value="/v2/serviceaccounts", produces="application/json")
	public ResponseEntity<String> getServiceAccounts(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.getServiceAccounts(token, userDetails);
	}
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.onboardServiceAccount.value}", notes = "${ServiceAccountsControllerV2.onboardServiceAccount.notes}")
	@PostMapping(value="/v2/serviceaccounts/onboard", produces="application/json")
	public ResponseEntity<String> onboardServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid ServiceAccount serviceAccount ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
	}

	@ApiOperation(value = "${ServiceAccountsControllerV2.addUserToServiceAccount.value}", notes = "${ServiceAccountsControllerV2.addUserToServiceAccount.notes}")
	@PostMapping(value="/v2/serviceaccounts/user", produces="application/json")
	public ResponseEntity<String> addUserToSvcAcc( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody ServiceAccountUser serviceAccountUser ){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
	}
	
	@ApiOperation(value = "${ServiceAccountsControllerV2.removeUserServiceAccount.value}", notes = "${ServiceAccountsControllerV2.removeUserServiceAccount.notes}")
	@DeleteMapping(value="/v2/serviceaccounts/user", produces="application/json")
	public ResponseEntity<String> removeUserServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody ServiceAccountUser serviceAccountUser ){
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
}
