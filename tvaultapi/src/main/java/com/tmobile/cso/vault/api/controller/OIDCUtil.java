package com.tmobile.cso.vault.api.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
		Map<String, String> metaDataParams = null;
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data")
				.getAsJsonObject(TVaultConstants.OIDC+"/");
		
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
			if (m.getKey().equals(TVaultConstants.POLICIES) && (m.getValue() != null || m.getValue() != "")) {
				String policy = m.getValue().toString().replace("[", "").replace("]", "").replaceAll("\\s", "");
				List<String> policies = new ArrayList<>(Arrays.asList(policy.split(",")));
				oidcEntityResponse.setPolicies(policies);
			}
		}
		return oidcEntityResponse;
	}

}
