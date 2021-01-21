// =========================================================================
// Copyright 2019 T-Mobile, US
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
package com.tmobile.cso.vault.api.v2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.service.DirectoryService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@CrossOrigin
@Api(description = "Manage Directory Service User Information")

public class DirectoryServiceControllerV2 {

	@Autowired
	private DirectoryService directoryService;

	@ApiOperation(value = "${DirectoryServiceControllerV2.searchByUPN.value}", notes = "${DirectoryServiceControllerV2.searchByUPN.notes}")
	
	@GetMapping(value="/v2/ldap/users",produces="application/json")
	public ResponseEntity<DirectoryObjects> searchByUPN(@ApiParam(name="UserPrincipalName", defaultValue="") 
	@RequestParam(name="UserPrincipalName", defaultValue="") String userPrincipalName ){
		return directoryService.searchByUPN(userPrincipalName);
	}

	@ApiOperation(value = "${DirectoryServiceControllerV2.searchByCorpId.value}", notes = "${DirectoryServiceControllerV2.searchByCorpId.notes}")
	@GetMapping(value="/v2/ldap/corpusers",produces="application/json")
	public ResponseEntity<DirectoryObjects> searchByCorpId(@ApiParam(name="CorpId", defaultValue="") 
	@RequestParam(name="CorpId", defaultValue="") String corpId ){
		return directoryService.searchByCorpId(corpId);
	}
	
	@ApiOperation(value = "${DirectoryServiceControllerV2.searchByGroup.value}", notes = "${DirectoryServiceControllerV2.searchByGroup.notes}")
	@GetMapping(value="/v2/ldap/groups",produces="application/json")
	public ResponseEntity<DirectoryObjects> searchByGroup(@ApiParam(name="groupName", defaultValue="") 
	@RequestParam(name="groupName", defaultValue="") String groupName ){
		return directoryService.searchByGroupName(groupName);
	}

	@ApiOperation(value = "${DirectoryServiceControllerV2.searchByDisplayName.value}", notes = "${DirectoryServiceControllerV2.searchByDisplayName.notes}")
	@GetMapping(value="/v2/ldap/ntusers",produces="application/json")
	public ResponseEntity<DirectoryObjects> searchUserInGSM(@ApiParam(name="displayName", defaultValue="")
	@RequestParam(name="displayName", defaultValue="") String displayName ){
		return directoryService.searchByDisplayNameAndId(displayName);
	}

	@ApiOperation(value = "${DirectoryServiceControllerV2.getAllUsersDetailByNtIds.value}", notes = "${DirectoryServiceControllerV2.getAllUsersDetailByNtIds.notes}")
	@GetMapping(value="/v2/ldap/getusersdetail/{userNames}",produces="application/json")
	public ResponseEntity<DirectoryObjects> getAllUsersDetailByNtIds(@PathVariable("userNames") String userNames ){
		return directoryService.getAllUsersDetailByNtIds(userNames);
	}

	@ApiOperation(value = "${DirectoryServiceControllerV2.searchByEmailInCorpAD.value}", notes = "${DirectoryServiceControllerV2.searchByEmailInCorpAD.notes}", hidden=true)
	@GetMapping(value="/v2/corp/users",produces="application/json")
	public ResponseEntity<DirectoryObjects> searchByEmailInCorpAD(@ApiParam(name="email", defaultValue="")
														@RequestParam(name="email", defaultValue="") String email ){
		return directoryService.searchByEmailInCorp(email);
	}

	@ApiOperation(value = "${DirectoryServiceControllerV2.searchByUPNInGsmAndCorp.value}", notes = "${DirectoryServiceControllerV2.searchByUPNInGsmAndCorp.notes}", hidden=true)
	@GetMapping(value="/v2/tmo/users",produces="application/json")
	public ResponseEntity<DirectoryObjects> searchByUPNInGsmAndCorp(@ApiParam(name="UserPrincipalName", defaultValue="")
														@RequestParam(name="UserPrincipalName", defaultValue="") String userPrincipalName ){
		return directoryService.searchByUPNInGsmAndCorp(userPrincipalName);
	}
}
