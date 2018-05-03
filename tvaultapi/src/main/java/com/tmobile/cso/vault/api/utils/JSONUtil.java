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

package com.tmobile.cso.vault.api.utils;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
public class JSONUtil {

	/**
	 * Converts an object to a JSON 
	 * @param obj
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String getJSON(Object obj)  {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return "{}";
		}
	}
	
	/**
	 * Creates an Object from JSON String
	 * @param jsonStr
	 * @param className
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static Object getObj(String jsonStr, Class<?> className) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		//JSON from String to Object
		return mapper.readValue(jsonStr, className);
	}
}
