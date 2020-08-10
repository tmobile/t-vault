package com.tmobile.cso.vault.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.OIDCGroup;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

@Component
public class OIDCUtil {
	
	private static RequestProcessor reqProcessor;
	public static final Logger log = LogManager.getLogger(OIDCUtil.class);
	
	@Autowired(required = true)
	public void setreqProcessor(RequestProcessor reqProcessor) {
		OIDCUtil.reqProcessor = reqProcessor;
	}

	/**
	 * Method to get requestProcessor
	 * @return
	 */
	public static RequestProcessor getReqProcessor() {
		return OIDCUtil.reqProcessor;
	}
	
	/**
	 * Fetch mount accessor id from oidc mount
	 * @param response
	 * @return
	 */
	public static String fetchMountAccessorForOidc(String token) {
		Response response = reqProcessor.process("/sys/list", "{}", token);
		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			Map<String, String> metaDataParams = null;
			JsonParser jsonParser = new JsonParser();
			JsonObject data = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
			if (data != null) {
				JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data")
						.getAsJsonObject(TVaultConstants.OIDC + "/");

				metaDataParams = new Gson().fromJson(object.toString(), Map.class);

				String accessor = "";
				for (Map.Entry m : metaDataParams.entrySet()) {
					if (m.getKey().equals(TVaultConstants.ALIAS_MOUNT_ACCESSOR)) {
						accessor = m.getValue().toString();
						break;
					}
				}
				return accessor;
			}
		}
		return null;
	}
	
	public static OIDCEntityResponse getEntityLookUpResponse(String authMountResponse) {
		Map<String, String> metaDataParams = null;
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(authMountResponse)).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		for (Map.Entry m : metaDataParams.entrySet()) {
			if (m.getKey().equals(TVaultConstants.ENTITY_NAME)) {
				oidcEntityResponse.setEntityName(m.getValue().toString());
			}
			if (m.getKey().equals(TVaultConstants.POLICIES) && m.getValue() != null && m.getValue() != "") {
				String policy = m.getValue().toString().replace("[", "").replace("]", "").replaceAll("\\s", "");
				List<String> policies = new ArrayList<>(Arrays.asList(policy.split(",")));
				oidcEntityResponse.setPolicies(policies);
			}
		}
		return oidcEntityResponse;
	}

	/**
	 * To get identity group details.
	 * @param groupName
	 * @param token
	 * @return
	 */
	public static OIDCGroup getIdentityGroupDetails(String groupName, String token) {
		Response response = reqProcessor.process("/identity/group/name", "{\"group\":\""+groupName+"\"}", token);
		OIDCGroup oidcGroup = new OIDCGroup();
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			String responseJson = response.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			List<String> policies = new ArrayList<>();
			try {
				oidcGroup.setId(objMapper.readTree(responseJson).get("id").asText());
				JsonNode policiesArry = objMapper.readTree(responseJson).get("policies");
				for (JsonNode policyNode : policiesArry) {
					policies.add(policyNode.asText());
				}
				oidcGroup.setPolicies(policies);
				return oidcGroup;
			}catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getIdentityGroupDetails").
						put(LogMessage.MESSAGE, String.format ("Failed to get identity group details for [%s]", groupName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
		}
		return null;
	}
}
