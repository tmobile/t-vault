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

package com.tmobile.cso.vault.api.process;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.config.ApiConfig;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class RequestValidator {
	@Autowired
	private RestProcessor restProcessor;
	private Logger log = LogManager.getLogger(RequestValidator.class);
	private final String[] mountPaths = {"apps","shared","users"};
	public Message validate(final ApiConfig apiConfig,final Map<String, Object> requestParams,String token){
		Message msg = new Message();
		switch (apiConfig.getApiEndPoint()){
			case "/access/create":{
				boolean duplicate = checkforDuplicatePolicy(requestParams, token);
				if(duplicate){
					msg.setMsgTxt("Existing access id. Use '/access/update' to update");
					msg.setMsgType(MSG_TYPE.ERR);
				}
				break;
		
			}
			case "/auth/aws/roles/create":{
				boolean duplicate = checkforDuplicateAwsGroup(requestParams, token);
				if(duplicate){
					msg.setMsgTxt("Existing role. Use '/auth/aws/roles/update' if needed");
					msg.setMsgType(MSG_TYPE.ERR);
				}
				break;
			}
			case "/auth/aws/iam/role/create":{
				boolean duplicate = checkforDuplicateAwsGroup(requestParams, token);
				if(duplicate){
					msg.setMsgTxt("Existing role. Use '/auth/aws/iam/roles/update' if needed");
					msg.setMsgType(MSG_TYPE.ERR);
				}
				break;
			}
			case "/sdb/create":{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Validate").
				      put(LogMessage.MESSAGE, String.format ("Checking for duplicate safe  [%s]", JSONUtil.getJSON(requestParams))).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
				boolean duplicate = checkforDuplicateSDB(requestParams, token);
				if(duplicate){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Validate").
						      put(LogMessage.MESSAGE, "Existing safe").
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					msg.setMsgTxt("The given safe already exists.");
					msg.setMsgType(MSG_TYPE.ERR);
				}
				break;
			}
			case "/sdb/createfolder":{
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "Validate").
				      put(LogMessage.MESSAGE, String.format ("Checking for duplicate folder  [%s]", JSONUtil.getJSON(requestParams))).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
				boolean duplicate = checkforDuplicateFolder(requestParams, token);
				if(duplicate){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							  put(LogMessage.ACTION, "Validate").
						      put(LogMessage.MESSAGE, "Existing folder").
						      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						      build()));
					msg.setMsgTxt("Existing folder");
					msg.setMsgType(MSG_TYPE.ERR);
				}
				break;
			}
		}
		return msg;
	}

	private boolean checkforDuplicatePolicy(Map<String, Object> requestParams,String token){
		if(requestParams.get("accessid")!= null){
			String policyName = requestParams.get("accessid").toString();
			ResponseEntity<String> valutResponse = restProcessor.get("/sys/policy/"+policyName, token);
			if(valutResponse.getStatusCode().equals(HttpStatus.NOT_FOUND)){
				return false;
			}
			return true;
		}
		return false;
	}
	
	private boolean checkforDuplicateAwsGroup(Map<String, Object> requestParams,String token){	
		if(requestParams.get("role") !=null){
			String role = requestParams.get("role").toString();
			ResponseEntity<String> valutResponse = restProcessor.get("/auth/aws/role/"+role, token);
			if(HttpStatus.OK.equals(valutResponse.getStatusCode())){
				return true;
			}
			return false;
		}
		return false;
	}
	
	private boolean checkforDuplicateSDB(Map<String, Object> requestParams,String token){	
		if(requestParams.get("data") !=null){
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) requestParams.get("data");
			String sdbName = (String) map.get("name");
			HashMap<String, List<String>> allSafeNames = ControllerUtil.getAllExistingSafeNames(token);
			for (Map.Entry<String, List<String>> entry : allSafeNames.entrySet()) {
				List<String> safeNames = entry.getValue();
				for (String safeName: safeNames) {
					// Note: SafeName is duplicate if it is found in any type (Shared/User/Apps). Hence no need to compare by prefixing with SafeType
					if (sdbName.equalsIgnoreCase(safeName)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	private boolean checkforDuplicateFolder(Map<String, Object> requestParams,String token){	
		if(requestParams.get("path") !=null){
			String path = requestParams.get("path").toString().toLowerCase();
			ResponseEntity<String> valutResponse = restProcessor.get("/"+path, token);
			if(valutResponse.getStatusCode().equals(HttpStatus.OK)){
				return true;
			}
			return false;
		}
		return false;
	}
}
