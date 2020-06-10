//=========================================================================
//Copyright 2020 T-Mobile, US
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//See the readme.txt file for additional language around disclaimer of warranties.
//=========================================================================
package com.tmobile.cso.vault.api.v2.controller;


import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.process.CertResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.CertManagerLoginRequest;
import com.tmobile.cso.vault.api.model.SSLCertificateRequest;
import com.tmobile.cso.vault.api.service.SSLCertificateService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api( description = "SSL Certificate  Management Controller", position = 15)
public class SSLCertificateController {

	@Autowired
	private SSLCertificateService sslCertificateService;

	/**
	 * To authenticate with Certificate Lifecycle Manager
	 * @param certManagetLoginRequest
	 * @return
	 */
	@ApiOperation(value = "${SSLCertificateController.login.value}", notes = "${SSLCertificateController.login.notes}")
	@PostMapping(value="/v2/auth/sslcert/login",produces="application/json")
	public ResponseEntity<String> authenticate(@RequestBody CertManagerLoginRequest certManagetLoginRequest) throws Exception {
		return sslCertificateService.authenticate(certManagetLoginRequest);
	}
	/**
	 * To Create SSL Certificate
	 * @param sslCertificateRequest
	 * @return
	 */
	@ApiOperation(value = "${SSLCertificateController.sslcreate.value}", notes = "${SSLCertificateController.sslcreate.notes}")
	@PostMapping(value="/v2/sslcert",consumes="application/json",produces="application/json")
	public ResponseEntity<CertResponse> generateSSLCertificate(@RequestBody SSLCertificateRequest sslCertificateRequest)  {
		return sslCertificateService.generateSSLCertificate(sslCertificateRequest);
	}



}