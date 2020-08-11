package com.tmobile.cso.vault.api.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.OIDCGroup;
import com.tmobile.cso.vault.api.utils.HttpUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.model.DirectoryUser;
import com.tmobile.cso.vault.api.model.GroupAliasRequest;
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
import com.tmobile.cso.vault.api.model.OIDCIdentityGroupRequest;
import com.tmobile.cso.vault.api.model.OIDCLookupEntityRequest;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.service.DirectoryService;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class OIDCUtil {

	@Autowired
	HttpUtils httpUtils;

	@Value("${sso.azure.resourceendpoint}")
	private String ssoResourceEndpoint;

	@Value("${sso.azure.groupsendpoint}")
	private String ssoGroupsEndpoint;

	private static RequestProcessor reqProcessor;
	public static final Logger log = LogManager.getLogger(OIDCUtil.class);
	
	@Autowired
	private RequestProcessor reqProcessor;
	
	@Autowired
	private DirectoryService directoryService;
	
	public static final Logger log = LogManager.getLogger(OIDCUtil.class);
	
	/**
	 * Fetch mount accessor id from oidc mount
	 * @param response
	 * @return
	 */
	public String fetchMountAccessorForOidc(String token) {
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
	
	/**
	 * Get Entity LookUp Response
	 * @param authMountResponse
	 * @return
	 */
	public OIDCEntityResponse getEntityLookUpResponse(String authMountResponse) {
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
	 * Update Group Policies
	 * @param token
	 * @param policies
	 * @param groupName
	 * @return
	 */
	public Response updateGroupPolicies(String token, List<String> policies, String groupName) {

		// Delete Group By Name
		Response response = deleteGroupByName(token, groupName);
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			OIDCIdentityGroupRequest oidcIdentityGroupRequest = new OIDCIdentityGroupRequest();
			oidcIdentityGroupRequest.setName(groupName);
			oidcIdentityGroupRequest.setPolicies(policies);
			oidcIdentityGroupRequest.setType(TVaultConstants.EXTERNAL_TYPE);
			String canonicalID = updateIdentityGroupByName(token, oidcIdentityGroupRequest);
			if (canonicalID != null) {
				String mountAccessor = fetchMountAccessorForOidc(token);
				// Object Id call object Api
				String objectId = "";
				GroupAliasRequest groupAliasRequest = new GroupAliasRequest();
				groupAliasRequest.setCanonical_id(canonicalID);
				groupAliasRequest.setMount_accessor(mountAccessor);
				groupAliasRequest.setName(objectId);
				return createGroupAlias(token, groupAliasRequest);
			} else {
				response.setHttpstatus(HttpStatus.BAD_REQUEST);
				return response;
			}
		}
		response.setHttpstatus(HttpStatus.BAD_REQUEST);
		return response;
	}
	
	/**
	 * Delete Group By Name
	 * @param token
	 * @param name
	 * @return
	 */
	public Response deleteGroupByName(String token, String name) {
		return reqProcessor.process("/identity/group/name", "{\"name\":\"" + name + "\"}", token);
	}

	/**
	 * To get identity group details.
	 * @param groupName
	 * @param token
	 * @return
	 */
	public static OIDCGroup getIdentityGroupDetails(String groupName, String token) {
		Response response = reqProcessor.process("/identity/group/name", "{\"group\":\""+groupName+"\"}", token);
		if(HttpStatus.OK.equals(response.getHttpstatus())) {
			String responseJson = response.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			List<String> policies = new ArrayList<>();
			try {
				OIDCGroup oidcGroup = new OIDCGroup();
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

	/**
	 * To get SSO token.
	 * @return
	 */
	public String getSSOToken() {
		JsonParser jsonParser = new JsonParser();
		HttpClient httpClient = httpUtils.getHttpClient();
		String accessToken = "";
		if (httpClient == null) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getSSOToken").
					put(LogMessage.MESSAGE, "Failed to initialize httpClient").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return null;
		}
		String api = ControllerUtil.getOidcADLoginUrl();
		HttpPost postRequest = new HttpPost(api);
		postRequest.addHeader("Content-type", TVaultConstants.HTTP_CONTENT_TYPE_URL_ENCODED);
		postRequest.addHeader("Accept",TVaultConstants.HTTP_CONTENT_TYPE_JSON);

		List<NameValuePair> form = new ArrayList<>();
		form.add(new BasicNameValuePair("grant_type", "client_credentials"));
		form.add(new BasicNameValuePair("client_id",  ControllerUtil.getOidcClientId()));
		form.add(new BasicNameValuePair("client_secret",  ControllerUtil.getOidcClientSecret()));
		form.add(new BasicNameValuePair("resource",  ssoResourceEndpoint));
		UrlEncodedFormEntity entity;

		try {
			entity = new UrlEncodedFormEntity(form);
		} catch (UnsupportedEncodingException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getSSOToken").
					put(LogMessage.MESSAGE, "Failed to encode entity").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return null;
		}

		postRequest.setEntity(entity);
		String output;
		StringBuilder jsonResponse = new StringBuilder();

		try {
			HttpResponse apiResponse = httpClient.execute(postRequest);
			if (apiResponse.getStatusLine().getStatusCode() != 200) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getSSOToken").
						put(LogMessage.MESSAGE, "Failed to get sso token").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return null;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
			while ((output = br.readLine()) != null) {
				jsonResponse.append(output);
			}
			JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
			if (!responseJson.isJsonNull() && responseJson.has("access_token")) {
				accessToken = responseJson.get("access_token").getAsString();
			}
			return accessToken;
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getSSOToken").
					put(LogMessage.MESSAGE, "Failed to parse SSO response").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return null;
	}

	/**
	 * To get object id for a group.
	 *
	 * @param ssoToken
	 * @param groupName
	 * @return
	 */
	public String getGroupObjectResponse(String ssoToken, String groupName)  {
		JsonParser jsonParser = new JsonParser();
		HttpClient httpClient = httpUtils.getHttpClient();
		String groupObjectId = null;
		if (httpClient == null) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getGroupObjectResponse").
					put(LogMessage.MESSAGE, "Failed to initialize httpClient").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return null;
		}

		String filterSearch = "$filter=displayName%20eq%20'"+groupName+"'";
		String api = ssoGroupsEndpoint + filterSearch;
		HttpGet getRequest = new HttpGet(api);
		getRequest.addHeader("accept", TVaultConstants.HTTP_CONTENT_TYPE_JSON);
		getRequest.addHeader("Authorization", "Bearer " + ssoToken);
		String output = "";
		StringBuilder jsonResponse = new StringBuilder();

		try {
			HttpResponse apiResponse = httpClient.execute(getRequest);
			if (apiResponse.getStatusLine().getStatusCode() != 200) {
				return null;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
			while ((output = br.readLine()) != null) {
				jsonResponse.append(output);
			}

			JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
			if (responseJson != null && responseJson.has("value")) {
				JsonArray vaulesArray = responseJson.get("value").getAsJsonArray();
				if (vaulesArray.size() > 0) {
					groupObjectId = vaulesArray.get(0).getAsJsonObject().get("id").getAsString();
				}
			}
			return groupObjectId;
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getGroupObjectResponse").
					put(LogMessage.MESSAGE, String.format ("Failed to parse group object api response")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return null;
	}
     /*
	 * Update Identity Group By Name
	 * @param token
	 * @param oidcIdentityGroupRequest
	 * @return
	 */
	public String updateIdentityGroupByName(String token, OIDCIdentityGroupRequest oidcIdentityGroupRequest) {
		String jsonStr = JSONUtil.getJSON(oidcIdentityGroupRequest);
		Response response = reqProcessor.process("/identity/group/name/update", jsonStr, token);

		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			Map<String, String> metaDataParams = null;
			JsonParser jsonParser = new JsonParser();
			JsonObject data = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
			if (data != null) {
				JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");

				metaDataParams = new Gson().fromJson(object.toString(), Map.class);

				String canonicalId = "";
				for (Map.Entry m : metaDataParams.entrySet()) {
					if ("id".equals(m.getKey())) {
						canonicalId = m.getValue().toString();
						break;
					}
				}
				return canonicalId;
			}
		}
		return null;

	}
	
	/**
	 * Create Group Alias
	 * @param token
	 * @param groupAliasRequest
	 * @return
	 */
	public Response createGroupAlias(String token, GroupAliasRequest groupAliasRequest) {
		String jsonStr = JSONUtil.getJSON(groupAliasRequest);
		return reqProcessor.process("/identity/group-alias", jsonStr, token);
	}
	
	
	/**
	 * Common method to Fetch OIDC entity details
	 * 
	 * @param token
	 * @param username
	 * @return
	 */
	public ResponseEntity<OIDCEntityResponse> oidcFetchEntityDetails(String token, String username) {
		String mountAccessor = fetchMountAccessorForOidc(token);
		if (!StringUtils.isEmpty(mountAccessor)) {
			ResponseEntity<DirectoryObjects> response = directoryService.searchByCorpId(username);
			String aliasName = "";
			Object[] results = response.getBody().getData().getValues();
			for (Object tp : results) {
				aliasName = ((DirectoryUser) tp).getUserEmail();
			}
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setAlias_name(aliasName);
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
			return entityLookUp(token, oidcLookupEntityRequest);
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new OIDCEntityResponse());
	}
	
	/**
	 * Entity Lookup 
	 * @param token
	 * @param oidcLookupEntityRequest
	 * @return
	 */
	public ResponseEntity<OIDCEntityResponse> entityLookUp(String token,
			OIDCLookupEntityRequest oidcLookupEntityRequest) {
		String jsonStr = JSONUtil.getJSON(oidcLookupEntityRequest);
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		Response response = reqProcessor.process("/identity/lookup/entity", jsonStr, token);
		if (response.getHttpstatus().equals(HttpStatus.OK)) {
			oidcEntityResponse = getEntityLookUpResponse(response.getResponse());
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "entityLookUp")
					.put(LogMessage.MESSAGE, "Successfully received entity lookup")
					.put(LogMessage.STATUS, response.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(response.getHttpstatus()).body(oidcEntityResponse);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "entityLookUp").put(LogMessage.MESSAGE, "Failed entity Lookup")
					.put(LogMessage.STATUS, response.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(response.getHttpstatus()).body(oidcEntityResponse);
		}
	}
}
