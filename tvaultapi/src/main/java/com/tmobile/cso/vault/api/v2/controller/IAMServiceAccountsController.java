/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */
package com.tmobile.cso.vault.api.v2.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.IAMServiceAccount;
import com.tmobile.cso.vault.api.model.IAMServiceAccountGroup;
import com.tmobile.cso.vault.api.model.IAMServiceAccountUser;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.service.IAMServiceAccountsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api( description = "Manage IAM Service Account and Secrets", position = 20)
public class IAMServiceAccountsController {

	@Autowired
	private IAMServiceAccountsService iamServiceAccountsService;

	/**
	 * Onbaords a IAM service account for password rotation
	 * @param request
	 * @param token
	 * @param iamServiceAccount
	 * @return
	 */
	@ApiOperation(value = "${IAMServiceAccountsController.onboardIAMServiceAccount.value}", notes = "${IAMServiceAccountsController.onboardIAMServiceAccount.notes}")
	@PostMapping(value="/v2/iamserviceaccounts/onboard", produces="application/json")
	public ResponseEntity<String> onboardIAMServiceAccount( HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid IAMServiceAccount iamServiceAccount ){
		UserDetails userDetails = (UserDetails) request.getAttribute("UserDetails");
		return iamServiceAccountsService.onboardIAMServiceAccount(token, iamServiceAccount, userDetails);
	}

	/**
	 * Add user to IAM service account.
	 * @param iamServiceAccountUser
	 * @returnC
	 */
	@PostMapping(value="/v2/iamserviceaccounts/user",produces="application/json")
	@ApiOperation(value = "${IAMServiceAccountsController.addUserToIAMServiceAccount.value}", notes = "${IAMServiceAccountsController.addUserToIAMServiceAccount.notes}")
	public ResponseEntity<String> addUserToIAMServiceAccount(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid IAMServiceAccountUser iamServiceAccountUser){
	   UserDetails userDetails = (UserDetails) request.getAttribute("UserDetails");
	   return iamServiceAccountsService.addUserToIAMServiceAccount(token, userDetails, iamServiceAccountUser, false);
	}

	/**
	 * Add Group to IAM service account.
	 * @param request
	 * @param token
	 * @param iamServiceAccountGroup
	 * @return
	 */
	@PostMapping(value="/v2/iamserviceaccounts/group",produces="application/json")
	@ApiOperation(value = "${IAMServiceAccountsController.addGroupToIAMServiceAccount.value}", notes = "${IAMServiceAccountsController.addGroupToIAMServiceAccount.notes}")
	public ResponseEntity<String> addGroupToIAMServiceAccount(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestBody @Valid IAMServiceAccountGroup iamServiceAccountGroup){
	   UserDetails userDetails = (UserDetails) request.getAttribute("UserDetails");
	   return iamServiceAccountsService.addGroupToIAMServiceAccount(token, iamServiceAccountGroup, userDetails);
	}

}
