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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.config.ApiConfig;
@Component
public  class RequestTransformer {
	private static Logger log = LogManager.getLogger(RequestTransformer.class);
	public void transform(ApiConfig apiConfig,Map<String, Object> requestParams){
		switch (apiConfig.getApiEndPoint()){
			case "/access/create":
			case "/access/update":{
				String policyJson = createPolicyJson(requestParams);
				requestParams.remove("access");
				requestParams.put("rules",policyJson);
				break;
			}
		}
		
	}
	private  String createPolicyJson (Map<String, Object> requestParams){
		
		ObjectMapper objMapper =  new ObjectMapper();
		String accessinfo ="";
		Map<String,String> pathPolicyMap   = new HashMap<String,String>();
		try {
			accessinfo = objMapper.writeValueAsString(requestParams.get("access"));
			pathPolicyMap = objMapper.readValue(accessinfo, new TypeReference<Map<String, String>>(){});
		} catch (IOException e1) {
			log.error(e1);
		}	
		Map<String,Object> pathMap = new HashMap<String,Object>();
		pathPolicyMap.forEach((path,policy)-> {
			Map<String,String> policyMap = new HashMap<String,String>();
			policyMap.put("policy",policy);
			pathMap.put(path.toLowerCase(), policyMap);
		});
				
		Map<String,Object> pathsMap = new HashMap<String,Object> ();
		pathsMap.put("path", pathMap);
		try{
			return new ObjectMapper().writeValueAsString(pathsMap);
		} catch (JsonProcessingException e) {
			log.error(e);
		}
		return null;
	}
}
