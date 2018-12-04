package com.tmobile.cso.vault.api.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeBasicDetails;
import com.tmobile.cso.vault.api.process.Response;
@Component
public class SafeUtils {
	private Logger log = LogManager.getLogger(SafeUtils.class);
	
	@Autowired
	private TokenUtils tokenUtils;
	
	public SafeUtils() {
	}
	/**
	 * Gets 
	 * @param response
	 * @return
	 */
	public Safe getSafeInfo (JsonNode dataNode) {
		Safe safe = new Safe();
		SafeBasicDetails safeBasicDetails = new SafeBasicDetails();
		safe.setSafeBasicDetails(safeBasicDetails);
		safe.getSafeBasicDetails().setName(dataNode.get("name").asText());
		safe.getSafeBasicDetails().setDescription(dataNode.get("description").asText());
		safe.getSafeBasicDetails().setOwner(dataNode.get("owner").asText());
		safe.getSafeBasicDetails().setOwnerid(dataNode.get("ownerid").asText());
		return safe;
	}
	/**
	 * Gets the admin/sudo policies for the given user...
	 * @param policiesNode
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public List<String> getPoliciesForManagedSafes(JsonNode policiesNode) throws JsonProcessingException, IOException {
		List<String> adminPolicies = new ArrayList<String>();
		if (!ObjectUtils.isEmpty(policiesNode)) {
			// Policies is supposed to be a container node.
			if (policiesNode.isContainerNode()) {
				Iterator<JsonNode> elementsIterator = policiesNode.elements();
			       while (elementsIterator.hasNext()) {
			    	   JsonNode element = elementsIterator.next();
			    	   String policy = element.asText();
			    	   if (!StringUtils.isEmpty(policy) && policy.startsWith("s_")) {
			    		   adminPolicies.add(element.asText());
			    	   }
			       }
			}
		}
		return adminPolicies;
	}
	/**
	 * Gets the list of safes from policies for a given path
	 * @param policies
	 * @param path
	 * @return
	 */
	public String[] getManagedSafesFromPolicies(String[] policies, String path) {
		List<String> safes = new ArrayList<String>();
		if (policies != null) {
			for (String policy: policies) {
				if (policy.startsWith("s_")) {
					String[] _policies = policy.split("_");
					if (_policies[1].equals(path)) {
						safes.add(_policies[2]);
					}
				}
			}
		}
		return safes.toArray(new String[safes.size()]);
	}
	/**
	 * Gets the metadata associated with a given safe
	 * @param token
	 * @param safeType
	 * @param safeName
	 * @return
	 */
	public Safe getSafeMetaDataWithAppRoleElevation(String token, String safeType, String safeName){
		String _path = "metadata/" + safeType + "/" + safeName;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Get Info").
				put(LogMessage.MESSAGE, String.format ("Trying to get Info for [%s]", _path)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		// Elevation is required in case user does not have access to the path.
		// If there is a need for elevation, can we say the user is not authorized?
		String powerToken = tokenUtils.generatePowerToken(token);
		Response response = ControllerUtil.reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",powerToken);
		// Create the Safe bean
		Safe safe = null;
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			try {
				ObjectMapper objMapper = new ObjectMapper();
				JsonNode dataNode = objMapper.readTree(response.getResponse().toString()).get("data");
				safe = getSafeInfo(dataNode);
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						  put(LogMessage.ACTION, "getSafeMetaDataWithAppRoleElevation").
					      put(LogMessage.MESSAGE, "Error while trying to get details about the safe").
					      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					      build()));			
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Get Info").
				put(LogMessage.MESSAGE, "Getting Info completed").
				put(LogMessage.STATUS, response.getHttpstatus().toString()).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return safe;
	}
}
