// =========================================================================
// Copyright 2018 T-Mobile, US
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

package com.tmobile.cso.vault.api.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

import io.swagger.annotations.Api;

@RestController
@RequestMapping(value="/auth/aws")
@CrossOrigin
@Api(description = "Manage AWS Authentication", position = 3)
public class AWSAuthController {
	
	@Autowired
	private RequestProcessor reqProcessor;
	
	/**
	 * Method to authenticate using aws ec2 pkcs7 document and app role
	 * 
	 * @param jsonStr : role and pkcs7 
	 * @return clinet token and nonce
	 * 
	 * {"client_token":"01a9b77f-2166-01bb-e152-06b1123c1df8",
	 *   "nonce":"MIAGCSqGSIb3DQEHAqCAMIACAQExCzAJBgUrDgMCGgUAMIAGC"
	 * }
	 */
	
	@PostMapping(value="/login",consumes="application/json",produces="application/json")
	public ResponseEntity<String> authenticateLdap( @RequestBody String jsonStr){
		if(jsonStr.toLowerCase().contains("nonce")){
			return ResponseEntity.badRequest().body("{\"errors\":[\"Not a valid request. Parameter 'nonce' is not expected \"]}");
		}
		
		String nonce= "";
		try {
			nonce = new ObjectMapper().readTree(jsonStr).at("/pkcs7").toString().substring(1,50);
		} catch (IOException e) {
			// Log exception
			e.printStackTrace();
			return ResponseEntity.badRequest().body("{\"errors\":[\"Not valid request. Check params \"]}");
		}
		String noncejson = "{\"nonce\":\""+nonce+"\",";
		jsonStr = noncejson + jsonStr.substring(1);
		
		Response response = reqProcessor.process("/auth/aws/login",jsonStr,"");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Method to create an aws app role 
	 * @param token 
	 * @param jsonStr : role name, aws bound params, policies
 	 * @return httpStatus 200 on success
 	 * 
 	 * Sample return value
 	 * 		{"Messages":["Role created"]}
	 */
	@PostMapping(value="/roles/create",consumes="application/json",produces="application/json")
	public ResponseEntity<String> createRole(@RequestHeader(value="vault-token") String token,
																	@RequestBody String jsonStr) {
		boolean validInputs = false;
		try {
			validInputs = ControllerUtil.areAWSEC2RoleInputsValid(jsonStr);
		} catch (TVaultValidationException e1) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\""+e1.getMessage()+ "\"]}");
		}
		if (!validInputs) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Invalid Inputs \"]}");
		}
		
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String roleName = "" ;
		
		try {
			JsonNode root = objMapper.readTree(jsonStr);
			roleName = root.get("role").asText();
			roleName = (roleName != null) ? roleName.toLowerCase() : roleName;
			if(root.get("policies") != null)
				latestPolicies = root.get("policies").asText();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Response response = reqProcessor.process("/auth/aws/roles/create",jsonStr,token);
		
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ // Role created with policies. Need to update SDB metadata too.
			response = ControllerUtil.updateMetaDataOnConfigChanges(roleName, "roles", currentPolicies, latestPolicies, token);
			if(!HttpStatus.OK.equals(response.getHttpstatus()))
				return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"AWS Role configured\",\""+response.getResponse()+"\"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
	}
	
	/**
	 * Method to update an aws app role. 
	 * @param token
	 * @param jsonStr role name, aws bound params, policies
	 * @return
	 *  Sample return value
 	 * 		{"Messages":["Role updated"]}
	 */
	
	@PostMapping(value="/roles/update",consumes="application/json",produces="application/json")
	public ResponseEntity<String> updateRole(@RequestHeader(value="vault-token") String token,
																	@RequestBody String jsonStr){
		
		boolean validInputs = false;
		try {
			validInputs = ControllerUtil.areAWSEC2RoleInputsValid(jsonStr);
		} catch (TVaultValidationException e1) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\""+e1.getMessage()+ "\"]}");
		}
		if (!validInputs) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Invalid Inputs \"]}");
		}
		
		ObjectMapper objMapper = new ObjectMapper();
		String currentPolicies = "";
		String latestPolicies = "";
		String roleName = "" ;
		
		try {
			JsonNode root = objMapper.readTree(jsonStr);
			roleName = root.get("role").asText();
			if(root.get("policies") != null)
				latestPolicies = root.get("policies").asText();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Response awsResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+roleName+"\"}",token);
		String responseJson="";	
		
		if(HttpStatus.OK.equals(awsResponse.getHttpstatus())){
			responseJson = awsResponse.getResponse();	
			try {
				Map<String,Object> responseMap; 
				responseMap = objMapper.readValue(responseJson, new TypeReference<Map<String, Object>>(){});
				@SuppressWarnings("unchecked")
				List<String> policies  = (List<String>) responseMap.get("policies");
				currentPolicies = policies.stream().collect(Collectors.joining(",")).toString();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Update failed . AWS Role does not exist \"]}");
		}
		
		Response response = reqProcessor.process("/auth/aws/roles/delete",jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			response = reqProcessor.process("/auth/aws/roles/update",jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				response = ControllerUtil.updateMetaDataOnConfigChanges(roleName, "aws-roles", currentPolicies, latestPolicies, token);
				if(!HttpStatus.OK.equals(response.getHttpstatus()))
					return ResponseEntity.status(response.getHttpstatus()).body("{\"messages\":[\"AWS Role configured\",\""+response.getResponse()+"\"]}");
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		
	}
	/**
	 * Deleting an existing role.
	 * 
	 * @param token
	 * @param jsonStr
	 * @return
	 */
	
	@DeleteMapping(value="/roles/delete/{role}",produces="application/json")
	public ResponseEntity<String> deleteRole(@RequestHeader(value="vault-token") String token,
									@PathVariable("role" ) String role){
		
		Response response = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role deleted \"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	
	
	/**
	 * Method to fetch information for an aws approle.
	 * @param token
	 * @param role
	 * @return
	 * 
	 * Sample return
	 * {"bound_ami_id":"testami",,"bound_account_id":"","bound_iam_instance_profile_arn":"","bound_iam_role_arn":""
	 * 		"policies":["default","testpolicy","testpolicy1"]
	 * }
	 */
	
	@GetMapping(value="/roles/{role}",produces="application/json")
	public ResponseEntity<String> fetchRole(@RequestHeader(value="vault-token") String token,
				@PathVariable("role") String role){
		
		String jsoninput= "{\"role\":\""+role+"\"}";
		Response response = reqProcessor.process("/auth/aws/roles",jsoninput,token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	@GetMapping(value="/roles",produces="application/json")
	public ResponseEntity<String> listRoles(@RequestHeader(value="vault-token") String token){
		
		Response response = reqProcessor.process("/auth/aws/roles/list","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
}
