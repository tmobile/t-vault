// =========================================================================
// Copyright 2020 T-Mobile, US
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

import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.service.WorkloadDetailsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@Api( description = "Get Workload details", position = 14)
public class WorkloadDetailsController {

	@Autowired
	private WorkloadDetailsService workloadDetailsService;

	/**
	 * To get application list from Workload endpoint
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${WorkloadDetailsController.getApprolesFromCwm.value}", notes = "${WorkloadDetailsController.getApprolesFromCwm.notes}")
	@GetMapping(value="/v2/serviceaccounts/cwm/approles", produces="application/json")
	public ResponseEntity<String> getWorkloadDetails(HttpServletRequest request, @RequestHeader(value="vault-token") String token){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return workloadDetailsService.getWorkloadDetails(token, userDetails);
	}

	/**
	 * To get application details by selected app name
	 * @param request
	 * @param token
	 * @param appName
	 * @return
	 */
	@ApiOperation(value = "${WorkloadDetailsController.getApprolesFromCwm.appname.value}", notes = "$" +
			"{WorkloadDetailsController.getApprolesFromCwm.appname.notes}")
	@GetMapping(value="/v2/serviceaccounts/cwm/appdetails/appname", produces="application/json")
	public ResponseEntity<String> getWorkloadDetailsByAPIName(HttpServletRequest request,
															  @RequestHeader(value="vault-token") String token, @RequestParam(
			"appName" ) String appName){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return workloadDetailsService.getWorkloadDetailsByAppName(appName);
	}
}
