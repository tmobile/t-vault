package com.tmobile.cso.vault.api.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.Response;
@Component
public class PolicyUtils {

	private Logger log = LogManager.getLogger(PolicyUtils.class);
	
	public PolicyUtils() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Get the details for the given policy
	 * @param policyName
	 * @param token
	 * @return
	 */
	public LinkedHashMap<String, LinkedHashMap<String, Object>> getPolicyInfo( String policyName, String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Get Policy information").
				put(LogMessage.MESSAGE, String.format("Trying to get policy information for [%s]", policyName)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if(StringUtils.isEmpty(policyName)){
			return null;
		}
		
		Response response = ControllerUtil.reqProcessor.process("/access","{\"accessid\":\""+policyName+"\"}",token);
		String policyJson = response.getResponse().toString();
		//TODO: Properly handle null/empty cases...
		LinkedHashMap<String, LinkedHashMap<String, Object>> capabilitiesMap = (LinkedHashMap<String, LinkedHashMap<String, Object>>) ControllerUtil.parseJson(ControllerUtil.parseJson(policyJson).get("rules").toString()).get("path");
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Get Policy information").
				put(LogMessage.MESSAGE, "Getting policy information Complete").
				put(LogMessage.STATUS, response.getHttpstatus().toString()).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return capabilitiesMap ;
	}

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
	
	/**
	 * Gets the list of policies to be checked for a given safe
	 * @param safeType
	 * @param safeName
	 * @return
	 */
	public ArrayList<String> getPoliciesTobeCheked(String safeType, String safeName) {
		ArrayList<String> policiesTobeChecked = new ArrayList<String>();
		policiesTobeChecked.addAll(getAdminPolicies()); 
		policiesTobeChecked.addAll(getSudoPolicies(safeType, safeName));
		return policiesTobeChecked;
	}
	/**
	 * To get the list of admin policies
	 * @return
	 */
	public ArrayList<String> getAdminPolicies() {
		ArrayList<String> adminPolicies = new ArrayList<String>();
		// TODO: Currently this list is based on the hard coded policy name, may be, this needs to externalized
		adminPolicies.add("safeadmin");
		return adminPolicies;
	}
	
	/**
	 * 
	 * @return
	 */
	private ArrayList<String> getSudoPolicies(String safeType, String safeName) {
		ArrayList<String> sudoPolicies = new ArrayList<String>();
		sudoPolicies.add(new StringBuffer().append("s_").append(safeType).append("_").append(safeName).toString());
		return sudoPolicies;
	}

}
