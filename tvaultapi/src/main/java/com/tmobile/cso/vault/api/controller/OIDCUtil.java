package com.tmobile.cso.vault.api.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.HttpUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.service.DirectoryService;

@Component
public class OIDCUtil {
	
	public OIDCUtil() {

	}
	@Autowired
	HttpUtils httpUtils;

	@Autowired
	TokenUtils tokenUtils;

	@Value("${sso.azure.resourceendpoint}")
	private String ssoResourceEndpoint;

	@Value("${sso.azure.groupsendpoint}")
	private String ssoGroupsEndpoint;

	@Value("${sso.azure.userendpoint}")
	private String ssoGetUserEndpoint;

	@Value("${sso.azure.usergroups}")
	private String ssoGetUserGroups;

	@Value("${SSLCertificateController.sprintmail.text}")
	private String sprintMailTailText;

	@Value("${sso.admin.groupName.Pattern}")
	private String ssoGroupPattern;

	public static final Logger log = LogManager.getLogger(OIDCUtil.class);
	
	@Autowired
	private RequestProcessor reqProcessor;
	
	@Autowired
	private DirectoryService directoryService;
	
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
	 * 
	 * @param token
	 * @param groupName
	 * @param policies
	 * @param currentPolicies
	 * @param id
	 * @return
	 */
	public Response updateGroupPolicies(String token, String groupName, List<String> policies,
			List<String> currentPolicies, String id) {

		OIDCIdentityGroupRequest oidcIdentityGroupRequest = new OIDCIdentityGroupRequest();
		oidcIdentityGroupRequest.setName(groupName);
		oidcIdentityGroupRequest.setType(TVaultConstants.EXTERNAL_TYPE);
		// Delete Group Alias By ID
		Response response = deleteGroupAliasByID(token, id);
		if (response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
			// Delete Group By Name
			Response deleteGroupResponse = deleteGroupByName(token, groupName);
			if (deleteGroupResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
				oidcIdentityGroupRequest.setPolicies(policies);
				String canonicalID = updateIdentityGroupByName(token, oidcIdentityGroupRequest);
				String mountAccessor = fetchMountAccessorForOidc(token);
				// Object Id call object Api
				String ssoToken = getSSOToken();
				String objectId = getGroupObjectResponse(ssoToken, groupName);
				if (!StringUtils.isEmpty(canonicalID) && !StringUtils.isEmpty(mountAccessor)
						&& !StringUtils.isEmpty(objectId)) {
					// Update Group Alias
					GroupAliasRequest groupAliasRequest = new GroupAliasRequest();
					groupAliasRequest.setCanonical_id(canonicalID);
					groupAliasRequest.setMount_accessor(mountAccessor);
					groupAliasRequest.setName(objectId);
					return createGroupAlias(token, groupAliasRequest);
				}
			}
		}
		oidcIdentityGroupRequest.setPolicies(currentPolicies);
		updateIdentityGroupByName(token, oidcIdentityGroupRequest);
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
		return reqProcessor.process("/identity/group/name/delete", "{\"name\":\"" + name + "\"}", token);
	}

	/**
	 * To get identity group details.
	 * @param groupName
	 * @param token
	 * @return
	 */
	public OIDCGroup getIdentityGroupDetails(String groupName, String token) {
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
					String cloudGroupId = null;
					String onPremGroupId = null;
					for (int i=0;i<vaulesArray.size();i++) {
						JsonObject adObject = vaulesArray.get(i).getAsJsonObject();
						// Filter out the duplicate groups by skipping groups created from onprem. Taking group with onPremisesSyncEnabled == null
						if (adObject.has("onPremisesSyncEnabled")) {
							if (adObject.get("onPremisesSyncEnabled").isJsonNull()) {
								cloudGroupId = adObject.get("id").getAsString();
								break;
							}
							else if (adObject.get("onPremisesSyncEnabled").getAsBoolean()) {
								onPremGroupId = adObject.get("id").getAsString();
							}
						}
					}
					groupObjectId = (cloudGroupId!=null)?cloudGroupId:onPremGroupId;
					if (groupObjectId == null) {
						JsonObject adObject = vaulesArray.get(0).getAsJsonObject();
						groupObjectId = adObject.get("id").getAsString();
					}
				}
			}
			return groupObjectId;
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getGroupObjectResponse").
					put(LogMessage.MESSAGE, "Failed to parse group object api response").
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
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<OIDCEntityResponse> oidcFetchEntityDetails(String token, String username, UserDetails userDetails) {
		String mountAccessor = fetchMountAccessorForOidc(token);
		if (!StringUtils.isEmpty(mountAccessor)) {
			ResponseEntity<DirectoryObjects> response = directoryService.searchByCorpId(username);
			String aliasName = "";
			Object[] results = response.getBody().getData().getValues();
			for (Object tp : results) {
				if (((DirectoryUser) tp).getUserName().equalsIgnoreCase(username)) {
					aliasName = ((DirectoryUser) tp).getUserEmail();
					break;
				}
			}
			if(StringUtils.isEmpty(aliasName)){
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new OIDCEntityResponse());
			}
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setAlias_name(aliasName);
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);

            // Get polices from user entity. This will have only user policies.
            ResponseEntity<OIDCEntityResponse> entityResponseResponseEntity = entityLookUp(token, oidcLookupEntityRequest);
            if (!HttpStatus.OK.equals(entityResponseResponseEntity.getStatusCode())) {
				// Create entity alias for the user
				OidcEntityAliasRequest oidcEntityAliasRequest = new OidcEntityAliasRequest(aliasName, mountAccessor);
				Response createEntityAliasResponse = createEntityAlias(token, oidcEntityAliasRequest);
				if (HttpStatus.OK.equals(createEntityAliasResponse.getHttpstatus())) {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "oidcFetchEntityDetails").
							put(LogMessage.MESSAGE, String.format("Created entity alias for [%s]", aliasName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					// fetching entity lookup again
					entityResponseResponseEntity = entityLookUp(token, oidcLookupEntityRequest);
				} else {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "oidcFetchEntityDetails").
							put(LogMessage.MESSAGE, String.format("Failed to created entity alias for [%s]", aliasName)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
					return ResponseEntity.status(createEntityAliasResponse.getHttpstatus()).body(new OIDCEntityResponse());
				}
			}
			List<String> combinedPolicyList = new ArrayList<>();
            List<String> policies = entityResponseResponseEntity.getBody().getPolicies();
            if (policies!=null) {
				combinedPolicyList.addAll(policies);
			}

			// if permission adding to current user, then take token policies also.
			List<String> policiesFromToken;
            if (userDetails != null && username.equalsIgnoreCase(userDetails.getUsername())) {
				// Get policies from token. This will have all the policies from user and group except the user polices updated to the entity.
				policiesFromToken = tokenLookUp(userDetails.getClientToken());
				combinedPolicyList.addAll(policiesFromToken);
			}

            List<String> policiesWithOutDuplicates
                    = combinedPolicyList.stream().distinct().collect(Collectors.toList());
            entityResponseResponseEntity.getBody().setPolicies(policiesWithOutDuplicates);
            return entityResponseResponseEntity;
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
	
	/**
	 * Delete Group Alias By ID
	 * @param token
	 * @param id
	 * @return
	 */
	public Response deleteGroupAliasByID(String token, String id) {
		return reqProcessor.process("/identity/group-alias/id/delete", "{\"id\":\"" + id + "\"}", token);
	}

	/**
	 * Update Entity by name
	 *
	 * @param policies
	 * @param entityName
	 * @return
	 */
	public Response updateOIDCEntity(List<String> policies, String entityName) {
		OIDCEntityRequest oidcEntityRequest = new OIDCEntityRequest();
		oidcEntityRequest.setPolicies(policies);
		oidcEntityRequest.setDisabled(Boolean.FALSE);
		oidcEntityRequest.setName(entityName);
		Map<String, String> metaData = new HashMap<>();
		oidcEntityRequest.setMetadata(metaData);
		String selfServiceSupportToken = tokenUtils.getSelfServiceTokenWithAppRole();
		return updateEntityByName(selfServiceSupportToken, oidcEntityRequest);
	}

	/**
	 * Update Entity By Name
	 * @param token
	 * @param oidcEntityRequest
	 * @return
	 */
	public Response updateEntityByName(String token, OIDCEntityRequest oidcEntityRequest){
		String jsonStr = JSONUtil.getJSON(oidcEntityRequest);
		return reqProcessor.process("/identity/entity/name/update", jsonStr, token);
	}

    /**
     * To renew user token after oidc policy update.
     *
     * @param token
     * @return
     */
    public void renewUserToken(String token) {
        Response renewResponse = reqProcessor.process("/auth/tvault/renew", "{}", token);
        if (HttpStatus.OK.equals(renewResponse.getHttpstatus())) {
            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "Add Group to SDB").
                    put(LogMessage.MESSAGE, "Successfully renewd user token after group policy update").
                    put(LogMessage.STATUS, (null != renewResponse) ? renewResponse.getHttpstatus().toString() : TVaultConstants.EMPTY).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "Add Group to SDB").
                    put(LogMessage.MESSAGE, "Reverting user policy update failed").
                    put(LogMessage.STATUS, (null != renewResponse) ? renewResponse.getHttpstatus().toString() : TVaultConstants.EMPTY).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
    }


    /**
     * Get Entity LookUp Response
     *
     * @param authMountResponse
     * @return
     */
    public List<String> getPoliciedFromTokenLookUp(String authMountResponse) {
        Map<String, String> metaDataParams = null;
        JsonParser jsonParser = new JsonParser();
        JsonObject object = ((JsonObject) jsonParser.parse(authMountResponse));
        metaDataParams = new Gson().fromJson(object.toString(), Map.class);
        List<String> policies = new ArrayList<>();
        for (Map.Entry m : metaDataParams.entrySet()) {
            if (m.getKey().equals(TVaultConstants.IDENTITY_POLICIES) && m.getValue() != null && m.getValue() != "") {
                String policy = m.getValue().toString().replace("[", "").replace("]", "").replaceAll("\\s", "");
                policies = new ArrayList<>(Arrays.asList(policy.split(",")));
                break;
            }
        }
        return policies;
    }

    /**
     * Entity Lookup
     *
     * @param token
     * @return
     */
    public List<String> tokenLookUp(String token) {
        List<String> policies = new ArrayList<>();
        Response response = reqProcessor.process("/auth/tvault/lookup", "{}", token);
        if (response.getHttpstatus().equals(HttpStatus.OK)) {
            policies = getPoliciedFromTokenLookUp(response.getResponse());
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "tokenLookUp")
                    .put(LogMessage.MESSAGE, "Successfully received token lookup")
                    .put(LogMessage.STATUS, response.getHttpstatus().toString())
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
            return policies;
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "tokenLookUp").put(LogMessage.MESSAGE, "Failed token Lookup")
                    .put(LogMessage.STATUS, response.getHttpstatus().toString())
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
            return policies;
        }
    }

	/**
	 * To create entity alias.
	 * @param token
	 * @param oidcEntityAliasRequest
	 * @return
	 */
	public Response createEntityAlias(String token, OidcEntityAliasRequest oidcEntityAliasRequest) {
		String jsonStr = JSONUtil.getJSON(oidcEntityAliasRequest);
		return reqProcessor.process("/identity/entity-alias", jsonStr, token);
	}
	
	/**
	 * getUserName
	 * @param email
	 * @return
	 */
	public String getUserName(String email){
		ResponseEntity<DirectoryObjects> responseEntity = directoryService.searchByUPN(email);
		String userName = "";
		Object[] results = responseEntity.getBody().getData().getValues();
		for (Object tp : results) {
			userName = ((DirectoryUser) tp).getUserName();
		}
		return userName;
	}

	/**
	 * To get groups from AAD.
	 *
	 * @param ssoToken
	 * @param groupName
	 * @return
	 */
	public List<DirectoryGroup> getGroupsFromAAD(String ssoToken, String groupName) {

		JsonParser jsonParser = new JsonParser();
		HttpClient httpClient = httpUtils.getHttpClient();
		List<DirectoryGroup> allGroups = new ArrayList<>();
		if (httpClient == null) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getGroupsFromAAD").
					put(LogMessage.MESSAGE, "Failed to initialize httpClient").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return allGroups;
		}

		String filterSearch = "$filter=startsWith%28displayName%2C'"+groupName+"'%29";
		String api = ssoGroupsEndpoint + filterSearch;
		HttpGet getRequest = new HttpGet(api);
		getRequest.addHeader("accept", TVaultConstants.HTTP_CONTENT_TYPE_JSON);
		getRequest.addHeader("Authorization", "Bearer " + ssoToken);
		String output = "";
		StringBuilder jsonResponse = new StringBuilder();

		try {
			HttpResponse apiResponse = httpClient.execute(getRequest);
			if (apiResponse.getStatusLine().getStatusCode() == 200) {
				BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
				while ((output = br.readLine()) != null) {
					jsonResponse.append(output);
				}

				JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
				if (responseJson != null && responseJson.has("value")) {
					JsonArray vaulesArray = responseJson.get("value").getAsJsonArray();
					if (vaulesArray.size() > 0) {
						Set<String> groupNamesSet = new HashSet<>();
						// Adding to set to remove duplicates
						for (int i=0;i<vaulesArray.size();i++) {
							JsonObject adObject = vaulesArray.get(i).getAsJsonObject();
							groupNamesSet.add(adObject.get("displayName").getAsString());
						}
						for (String group: groupNamesSet) {
							DirectoryGroup directoryGroup = new DirectoryGroup();
							directoryGroup.setDisplayName(group);
							directoryGroup.setGroupName(group);
							directoryGroup.setEmail(null);
							allGroups.add(directoryGroup);
						}
					}
				}
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "getGroupsFromAAD").
						put(LogMessage.MESSAGE, String.format("Retrieved %d group(s) from AAD", allGroups.size())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return allGroups;
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getGroupsFromAAD").
					put(LogMessage.MESSAGE, "Failed to retrieve groups from AAD").
					put(LogMessage.STATUS, String.valueOf(apiResponse.getStatusLine().getStatusCode())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getGroupsFromAAD").
					put(LogMessage.MESSAGE, "Failed to parse AAD groups api response").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return allGroups;
	}

	/**
	 * Method to get the Id from AAd by email and access token
	 *
	 * @param accessToken
	 * @param userEmail
	 * @return
	 */
	public String getIdOfTheUser(String accessToken, String userEmail) {
		String userId = null;
		JsonParser jsonParser = new JsonParser();
		HttpClient httpClient = httpUtils.getHttpClient();
		if (httpClient == null) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ID_USER_STRING)
					.put(LogMessage.MESSAGE, "Failed to initialize httpClient")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return null;
		}

		String filterSearch = "";
		if (userEmail.toLowerCase().endsWith(sprintMailTailText)) {
			filterSearch = "?$filter=startsWith%28mail%2C'" + userEmail + "'%29";
		} else {
			filterSearch = userEmail;
		}

		String api = ssoGetUserEndpoint + filterSearch;

		HttpGet getRequest = new HttpGet(api);
		getRequest.addHeader("Authorization", "Bearer " + accessToken);

		StringBuilder jsonResponse = new StringBuilder();

		try {
			HttpResponse apiResponse = httpClient.execute(getRequest);
			if (apiResponse.getStatusLine().getStatusCode() == 200) {
				userId = parseAndGetIdFromAADResponse(userId, jsonParser, jsonResponse, apiResponse);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.GET_ID_USER_STRING)
						.put(LogMessage.MESSAGE, String.format("Retrieved %s user id from AAD", userId))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return userId;
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ID_USER_STRING)
					.put(LogMessage.MESSAGE, "Failed to retrieve user id from AAD")
					.put(LogMessage.STATUS, String.valueOf(apiResponse.getStatusLine().getStatusCode()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ID_USER_STRING)
					.put(LogMessage.MESSAGE, "Failed to parse AAD user api response")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return userId;
	}

	/**
	 * @param userId
	 * @param jsonParser
	 * @param jsonResponse
	 * @param apiResponse
	 * @return
	 * @throws IOException
	 */
	private String parseAndGetIdFromAADResponse(String userId, JsonParser jsonParser, StringBuilder jsonResponse,
			HttpResponse apiResponse) throws IOException {
		String output = "";
		BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
		while ((output = br.readLine()) != null) {
			jsonResponse.append(output);
		}

		JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
		if (responseJson != null && responseJson.has("id")) {
			userId = responseJson.get("id").getAsString();
		}
		return userId;
	}

	/**
	 * To get all self service groups from AAD.
	 *
	 * @param ssoToken
	 * @param groupName
	 * @return
	 */
	public List<String> getSelfServiceGroupsFromAADById(String accessToken, String userAADId, String userName) {

		JsonParser jsonParser = new JsonParser();
		HttpClient httpClient = httpUtils.getHttpClient();
		List<String> allGroups = new ArrayList<>();
		if (httpClient == null) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_SELF_SERVICE_GROUPS_STRING)
					.put(LogMessage.MESSAGE, "Failed to initialize httpClient")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return allGroups;
		}
		String api = ssoGetUserEndpoint + userAADId + ssoGetUserGroups;
		HttpGet getRequest = new HttpGet(api);
		getRequest.addHeader("Authorization", "Bearer " + accessToken);
		String output = "";
		StringBuilder jsonResponse = new StringBuilder();

		try {
			HttpResponse apiResponse = httpClient.execute(getRequest);
			if (apiResponse.getStatusLine().getStatusCode() == 200) {
				BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
				while ((output = br.readLine()) != null) {
					jsonResponse.append(output);
				}
				JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
				if (responseJson != null && responseJson.has("value")) {
					JsonArray vaulesArray = responseJson.get("value").getAsJsonArray();
					if (vaulesArray.size() > 0) {
						Set<String> groupNamesSet = getGroupNameFromJsonArray(vaulesArray);

						if (!isUserSelfServiceAdmin(groupNamesSet)) {
							log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
									.put(LogMessage.ACTION, SSLCertificateConstants.GET_SELF_SERVICE_GROUPS_STRING)
									.put(LogMessage.MESSAGE, "No self-service groups available")
									.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
									.build()));

							return allGroups;
						}

						getMatchedSelfServiceGroups(allGroups, groupNamesSet);
					}
				}
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.GET_SELF_SERVICE_GROUPS_STRING)
						.put(LogMessage.MESSAGE,
								String.format("Retrieved %d group(s) from AAD for user %s", allGroups.size(), userName))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return allGroups;
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_SELF_SERVICE_GROUPS_STRING)
					.put(LogMessage.MESSAGE, "Failed to retrieve groups from AAD")
					.put(LogMessage.STATUS, String.valueOf(apiResponse.getStatusLine().getStatusCode()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_SELF_SERVICE_GROUPS_STRING)
					.put(LogMessage.MESSAGE, "Failed to parse AAD groups api response")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return allGroups;
	}

	/**
	 * @param vaulesArray
	 * @return
	 */
	private Set<String> getGroupNameFromJsonArray(JsonArray vaulesArray) {
		Set<String> groupNamesSet = new HashSet<>();
		// Adding to set to remove duplicates
		for (int i = 0; i < vaulesArray.size(); i++) {
			JsonObject adObject = vaulesArray.get(i).getAsJsonObject();
			groupNamesSet.add(adObject.get("displayName").getAsString());
		}
		return groupNamesSet;
	}

	/**
	 * @param allGroups
	 * @param groupNamesSet
	 */
	private void getMatchedSelfServiceGroups(List<String> allGroups, Set<String> groupNamesSet) {
		Set<String> filteredGroupSet = getMatchedSelfServiceGroups(groupNamesSet);
		for (String group : filteredGroupSet) {
			group = StringUtils.substringBetween(group, "r_selfservice_", "_admin");
			allGroups.add(group);
		}
	}

	/**
	 * @param userMemberGroups
	 * @return
	 */
	private boolean isUserSelfServiceAdmin(Set<String> userMemberGroups) {
		boolean match = false;
		if (!userMemberGroups.isEmpty()) {
			match = userMemberGroups.stream().anyMatch(str -> str.trim().matches(ssoGroupPattern));
		}
		return match;
	}

	/**
	 * @param allGroups
	 * @return
	 */
	private Set<String> getMatchedSelfServiceGroups(Set<String> allGroups) {
		Pattern pattern = Pattern.compile(ssoGroupPattern);
		return allGroups.stream().filter(pattern.asPredicate()).collect(Collectors.toSet());
	}
}
