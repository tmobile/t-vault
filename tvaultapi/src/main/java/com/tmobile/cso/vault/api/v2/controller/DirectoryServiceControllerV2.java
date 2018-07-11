package com.tmobile.cso.vault.api.v2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

	@ApiOperation(value = "${DirectoryServiceControllerV2.searchByGroup.value}", notes = "${DirectoryServiceControllerV2.searchByGroup.notes}")
	@GetMapping(value="/v2/ldap/groups",produces="application/json")
	public ResponseEntity<DirectoryObjects> searchByGroup(@ApiParam(name="groupName", defaultValue="") 
	@RequestParam(name="groupName", defaultValue="") String groupName ){
		return directoryService.searchByGroupName(groupName);
	}

	// Another API to search based on Group
	// objectclass is to be set as group
}
