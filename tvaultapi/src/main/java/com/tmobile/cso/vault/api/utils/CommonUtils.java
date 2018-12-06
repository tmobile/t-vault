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
		return policies.toArray(new String[policies.size()]);
	}
}
