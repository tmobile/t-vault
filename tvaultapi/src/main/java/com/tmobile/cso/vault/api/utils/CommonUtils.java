// =========================================================================
// Copyright 2019 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License")
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
import java.util.ArrayList;
import java.util.Iterator;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
@Component
public class CommonUtils {

	public CommonUtils() {
		//Empty constructor
	}
	/**
	 * Converts policies string part of response received from Vault Rest call to string array
	 * @param objMapper
	 * @param policyJson
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public String[] getPoliciesAsArray(ObjectMapper objMapper, String policyJson) throws JsonProcessingException, IOException{
		ArrayList<String> policies = new ArrayList<String>();
		JsonNode policiesNode = objMapper.readTree(policyJson).get("policies");
		if (policiesNode.isContainerNode()) {
			Iterator<JsonNode> elementsIterator = policiesNode.elements();
		       while (elementsIterator.hasNext()) {
		    	   JsonNode element = elementsIterator.next();
		    	   policies.add(element.asText());
		       }
		}
		else {
			policies.add(policiesNode.asText());
		}

		// get identity policies
		JsonNode identityPoliciesNode = objMapper.readTree(policyJson).get("identity_policies");
		if (identityPoliciesNode.isContainerNode()) {
			Iterator<JsonNode> elementsIterator = identityPoliciesNode.elements();
			while (elementsIterator.hasNext()) {
				JsonNode element = elementsIterator.next();
				policies.add(element.asText());
			}
		}
		else {
			policies.add(identityPoliciesNode.asText());
		}

		return policies.toArray(new String[policies.size()]);
	}
}
