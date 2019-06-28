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

package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.swing.text.html.HTML;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AWSSecretService {

	@Autowired
	private RequestProcessor reqProcessor;

	private static Logger logger = LogManager.getLogger(AWSSecretService.class);

	public ResponseEntity<String> createAWSRole(AWSDynamicRoleRequest awsDynamicRoleRequest, String token) {

		AWSTempRole awsTempRole = new AWSTempRole();
		awsTempRole.setName(awsDynamicRoleRequest.getName());
		awsTempRole.setCredential_type("iam_user");

		String[] actions = awsDynamicRoleRequest.getPermisisons().split(",");
		String[] resources = awsDynamicRoleRequest.getResources().split(",");

		Statement policyStatement = new Statement("Allow", actions, resources);

		PolicyDocument policyDocument = new PolicyDocument("2012-10-17",policyStatement);
		awsTempRole.setPolicy_document(policyDocument.toString());

		String roleString = JSONUtil.getJSON(awsTempRole);

		Response response = reqProcessor.process("/aws/roles/",roleString,token);
		if (HttpStatus.OK.equals(response.getHttpstatus()) || HttpStatus.NO_CONTENT.equals(response.getHttpstatus())) {
			// @todo create policy
			return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"Vault AWS Role created successfully\"]}");
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	public ResponseEntity<String> getTemporaryCredentials(String role_name, String token) {

		Response response = reqProcessor.process("/aws/creds/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	public ResponseEntity<String> deleteAWSRole(String role_name, String token) {
		Response response = reqProcessor.process("/aws/roles/","{\"role_name\":\""+role_name+"\"}",token);
		// @todo delete policy
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
}
