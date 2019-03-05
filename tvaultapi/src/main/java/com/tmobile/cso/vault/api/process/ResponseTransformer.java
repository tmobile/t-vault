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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tmobile.cso.vault.api.config.ApiConfig;
@Component
public  class ResponseTransformer {
	
	@Autowired
	RestProcessor restprocessor;
	
	public void transform(ApiConfig apiConfig,Map<String, Object> responseparams,String token){
		switch (apiConfig.getApiEndPoint()){
			case "/auth/ldap/login":
			case "/auth/userpass/login":
			case "/auth/approle/login":
			case "/auth/aws/login":{
				fetchSDBPaths(responseparams,token);
				break;
			}
			case "/sdb/list": {
				removeDuplicateNames(responseparams);
				break;
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void removeDuplicateNames(Map<String, Object> responseparams) {
		
		List<String> keys = (List<String>)((Map<String,Object>) responseparams.get("data")).get("keys");	
		keys = keys.stream().map(s -> s.replace("/", "")).distinct().collect(Collectors.toList());
		((Map<String,Object>) responseparams.get("data")).put("keys", keys);
	
	}

	private  void fetchSDBPaths (Map<String, Object> responseparams,String token){
		
		@SuppressWarnings("unchecked")
		List<String> policies = (List<String>)((Map<String,Object>) responseparams.get("auth")).get("policies");
		
		Map<String,Object> accessMap = new HashMap<String,Object>();
		for(String policy : policies){
			String policySplit[] = policy.split("_");
			if(policySplit.length >=3 && ("r".equals(policySplit[0]) || "w".equals(policySplit[0])|| "d".equals(policySplit[0]))){
				String mount = policySplit[1];
				
				List<String> folderNameParts = new ArrayList<String>();
				for(int i=2;i<policySplit.length;i++){
					folderNameParts.add(policySplit[i]);
				}
				
				String folder = folderNameParts.stream().collect(Collectors.joining("_"));
				
				Map<String,String> access = new HashMap<String,String>();
				
				@SuppressWarnings("unchecked")
				List<Map<String,String>> accesses = (List<Map<String,String>>) accessMap.get(mount);
				if(accesses == null) {
					accesses = new ArrayList<Map<String,String>>();
					accessMap.put(mount, accesses);
				}
				
				switch (policySplit[0]) {
					case "r": access.put(folder,"read"); break;
					case "w": access.put(folder,"write"); break;
					case "d": access.put(folder,"deny"); break;
				}
				accesses.add(access);
			}
		}
		responseparams.put("access", accessMap);
		if(policies.contains("safeadmin")){
			responseparams.put("admin", "yes");
		}else{
			responseparams.put("admin", "no");
		}
		
	}
}
