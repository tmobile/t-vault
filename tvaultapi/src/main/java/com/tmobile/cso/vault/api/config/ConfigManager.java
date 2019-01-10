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

package com.tmobile.cso.vault.api.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.NoApiConfigFoundException;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigManager {
	protected static Map<String,ApiConfig> apiConfigMap = new HashMap<String,ApiConfig>();
	private static Logger log = LogManager.getLogger(ConfigManager.class);
	static {
		try{
			ObjectMapper objectMapper = new ObjectMapper();
			TypeFactory typeFactory = objectMapper.getTypeFactory();
			InputStream jsonData = ConfigManager.class.getResourceAsStream("api_config.json");
			List<ApiConfig> apiConfigList = objectMapper.readValue(jsonData,typeFactory.constructCollectionType(List.class, ApiConfig.class));
			apiConfigList.forEach((apiconfig) -> {apiConfigMap.put(apiconfig.getApiEndPoint(),apiconfig);
													apiconfig.tempVaultEndpoint= apiconfig.apiEndPoint;
				});
		}catch(Exception e){
			log.debug(e.getMessage());
		}
	}
	
	public static ApiConfig lookUpApiConfig(String apiEndPoint) throws NoApiConfigFoundException {
		
		if (apiConfigMap.get(apiEndPoint) == null)
			throw new NoApiConfigFoundException(apiEndPoint);
		return apiConfigMap.get(apiEndPoint);
	}

}
