// =========================================================================
// Copyright 2020 T-Mobile, US
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

package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.TokenUtils;
import com.tmobile.cso.vault.api.utils.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Component
public class OIDCAuthService {

    @Autowired
    private RequestProcessor reqProcessor;
    
    @Autowired
    private OIDCUtil oidcUtil;

    @Value("${selfservice.enable}")
    private boolean isSSEnabled;

    @Value("${ad.passwordrotation.enable}")
    private boolean isAdPswdRotationEnabled;

    @Value("${sso.azure.resourceendpoint}")
    private String ssoResourceEndpoint;

    @Value("${sso.azure.groupsendpoint}")
    private String ssoGroupsEndpoint;

    private static Logger log = LogManager.getLogger(OIDCAuthService.class);

	/**
	 * Get Authentication Mounts
	 *
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getAuthenticationMounts(String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "List Auth Methods").put(LogMessage.MESSAGE, "Trying to get all auth Methods")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String mountAccessor = oidcUtil.fetchMountAccessorForOidc(token);
		return ResponseEntity.status(HttpStatus.OK).body(mountAccessor);
	}
    /**
     * Entity Lookup from identity engine
     * @param token
     * @param oidcLookupEntityRequest
     * @return
     */
	public ResponseEntity<OIDCEntityResponse> entityLookUp(String token,
			OIDCLookupEntityRequest oidcLookupEntityRequest) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "Entity Lookup from identity engine")
				.put(LogMessage.MESSAGE, "Trying to Lookup entity from identity engine")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return oidcUtil.entityLookUp(token, oidcLookupEntityRequest);
	}
	
    /**
     * Group Entity Lookup from identity engine
     * @param token
     * @param oidcLookupEntityRequest
     * @return
     */
    public ResponseEntity<String> groupEntityLookUp(String token, OIDCLookupEntityRequest oidcLookupEntityRequest) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
                .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                .put(LogMessage.ACTION, "Group Entity Lookup from identity engine").put(LogMessage.MESSAGE, "Trying to Lookup group entity from identity engine")
                .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

        String jsonStr = JSONUtil.getJSON(oidcLookupEntityRequest);
        Response response = reqProcessor.process("/identity/lookup/group", jsonStr, token);
        return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
    }
    /**
     * Read Entity Alias By ID
     * @param token
     * @param id
     * @return
     */
    public ResponseEntity<String> readEntityAliasById(String token, String id) {
        log.debug(
                JSONUtil.getJSON(
                        ImmutableMap.<String, String> builder()
                                .put(LogMessage.USER,
                                        ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                                .put(LogMessage.ACTION, "Read Entity Alias By ID")
                                .put(LogMessage.MESSAGE, "Trying to read Entity Alias").put(LogMessage.APIURL,
                                ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                                .build()));
        Response response = reqProcessor.process("/identity/entity-alias/id", "{\"id\":\"" + id + "\"}", token);
        return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
    }
    /**
     * Read Entity By Name
     * @param token
     * @param entityName
     * @return
     */
    public ResponseEntity<String> readEntityByName(String token, String entityName) {
        log.debug(
                JSONUtil.getJSON(
                        ImmutableMap.<String, String> builder()
                                .put(LogMessage.USER,
                                        ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                                .put(LogMessage.ACTION, "Read Entity By Name")
                                .put(LogMessage.MESSAGE, "Trying to read Entity").put(LogMessage.APIURL,
                                ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                                .build()));
        Response response = reqProcessor.process("/identity/entity/name", "{\"name\":\"" + entityName + "\"}", token);
        return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
    }

    /**
     * Update Entity By Name
     * @param token
     * @param oidcEntityRequest
     * @return
     */
	public ResponseEntity<String> updateEntityByName(String token, OIDCEntityRequest oidcEntityRequest) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "Update Entity By Name")
				.put(LogMessage.MESSAGE, "Trying to update entity by name")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		Response response = oidcUtil.updateEntityByName(token, oidcEntityRequest);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

    /**
     * Update Identity Group By Name
     * @param token
     * @param oidcIdentityGroupRequest
     * @return
     */
    public ResponseEntity<String> updateIdentityGroupByName(String token,
                                                            OIDCIdentityGroupRequest oidcIdentityGroupRequest) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
                .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                .put(LogMessage.ACTION, "Update Identity Group By Name")
                .put(LogMessage.MESSAGE, "Trying to update identity group entity by name")
                .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		String canonicalID = oidcUtil.updateIdentityGroupByName(token, oidcIdentityGroupRequest);
		if (canonicalID != null) {
			return ResponseEntity.status(HttpStatus.OK).body(canonicalID);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to get Canonical ID\"]}");
		}
    }

    /**
     * Group Alias By Id
     * @param token
     * @param id
     * @return
     */
    public ResponseEntity<String> readGroupAliasById(String token, String id) {
        log.debug(
                JSONUtil.getJSON(
                        ImmutableMap.<String, String> builder()
                                .put(LogMessage.USER,
                                        ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                                .put(LogMessage.ACTION, "Read Group Alias By Id")
                                .put(LogMessage.MESSAGE, "Trying to get Group Alias By Id").put(LogMessage.APIURL,
                                ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                                .build()));
        Response response = reqProcessor.process("/identity/group-alias/id", "{\"id\":\"" + id + "\"}", token);
        return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
    }

    /**
     * Read Group Alias By Id
     * @param token
     * @param name
     * @return
     */
    public ResponseEntity<String> deleteGroupByName(String token, String name) {
        log.debug(
                JSONUtil.getJSON(
                        ImmutableMap.<String, String> builder()
                                .put(LogMessage.USER,
                                        ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                                .put(LogMessage.ACTION, "Read Group Alias By Id")
                                .put(LogMessage.MESSAGE, "Trying to read Group Alias By Id").put(LogMessage.APIURL,
                                ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                                .build()));
		Response response = oidcUtil.deleteGroupByName(token, name);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
    }

    /**
     * Delete Group Alias By Id
     * @param token
     * @param id
     * @return
     */
    public ResponseEntity<String> deleteGroupAliasByID(String token, String id) {
        log.debug(
                JSONUtil.getJSON(
                        ImmutableMap.<String, String> builder()
                                .put(LogMessage.USER,
                                        ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                                .put(LogMessage.ACTION, "Delete group alias By Id")
                                .put(LogMessage.MESSAGE, "Trying to read Group Alias By Id").put(LogMessage.APIURL,
                                ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                                .build()));
        Response response = oidcUtil.deleteGroupAliasByID(token, id);
        return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
    }

    /**
     * Create Group Alias
     * @param token
     * @param groupAliasRequest
     * @return
     */
    public ResponseEntity<String> createGroupAlias(String token, GroupAliasRequest groupAliasRequest) {
        log.debug(
                JSONUtil.getJSON(
                        ImmutableMap.<String, String> builder()
                                .put(LogMessage.USER,
                                        ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                                .put(LogMessage.ACTION, "Create Group Alias")
                                .put(LogMessage.MESSAGE, "Trying to create Group Alias").put(LogMessage.APIURL,
                                ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                                .build()));
        Response response = oidcUtil.createGroupAlias(token, groupAliasRequest);
        return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
    }

    /**
     * To get OIDC auth url.
     * @param oidcRequest
     * @return
     */
    public ResponseEntity<String> getAuthUrl(OidcRequest oidcRequest) {
        String jsonStr = JSONUtil.getJSON(oidcRequest);
        Response response = reqProcessor.process("/auth/oidc/oidc/auth_url",jsonStr, "");
        if(HttpStatus.OK.equals(response.getHttpstatus())){
            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getAuthUrl").
                    put(LogMessage.MESSAGE, "Successfully retrieved OIDC auth url").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
        }else{
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getAuthUrl").
                    put(LogMessage.MESSAGE, String.format ("Failed to get OIDC auth url [%s]", response.getResponse())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Failed to get OIDC auth url\"]}");
        }
    }

    /**
     * To get vault token with OIDC callback state and code.
     * @param state
     * @param code
     * @return
     */
    public ResponseEntity<String> processOIDCCallback(String state, String code) {

        String pathStr = "?code="+code+"&state="+state;
        Response response = reqProcessor.process("/auth/oidc/oidc/callback","{\"path\":\""+pathStr+"\"}", "");
        if(HttpStatus.OK.equals(response.getHttpstatus())){
            Map<String, Object> responseMap = null;
            try {
                responseMap = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
            } catch (IOException e) {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "processCallback").
                        put(LogMessage.MESSAGE, "Failed to getresponse map from callback response").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }
            if(responseMap!=null && responseMap.get("access")!=null) {
                Map<String,Object> access = (Map<String,Object>)responseMap.get("access");
                access = ControllerUtil.filterDuplicateSafePermissions(access);
                access = ControllerUtil.filterDuplicateSvcaccPermissions(access);
                responseMap.put("access", access);
                // set SS, AD password rotation enable status
                Map<String,Object> feature = new HashMap<>();
                feature.put(TVaultConstants.SELFSERVICE, isSSEnabled);
                feature.put(TVaultConstants.ADAUTOROTATION, isAdPswdRotationEnabled);
                responseMap.put("feature", feature);
                response.setResponse(JSONUtil.getJSON(responseMap));
            }

            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "processCallback").
                    put(LogMessage.MESSAGE, "Successfully retrieved token from OIDC login").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "processCallback").
                put(LogMessage.MESSAGE, "Failed to get token from OIDC login").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
    }

    /**
     * To get group object id from Azure AD.
     * @param groupName
     * @return
     */
    public ResponseEntity<String> getGroupObjectIdFromAzure(String groupName) {
        String ssoToken = oidcUtil.getSSOToken();
        if (!StringUtils.isEmpty(ssoToken)) {
            String objectId = oidcUtil.getGroupObjectResponse(ssoToken, groupName);
            if (objectId != null) {
                return ResponseEntity.status(HttpStatus.OK).body("{\"data\":{\"objectId\": \""+objectId+"\"}}");
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"errors\":[\"Group not found in Active Directory\"]}");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to get SSO token for Azure AD access\"]}");
    }

    /**
     * To get identity group details.
     * @param groupName
     * @return
     */
    public ResponseEntity<String> getIdentityGroupDetails(String groupName, String token) {
        OIDCGroup oidcGroup = oidcUtil.getIdentityGroupDetails(groupName, token);
        if (oidcGroup != null) {
            return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(oidcGroup));
        }
        else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"errors\":[\"Group not found\"]}");
        }
    }
    
	/**
	 * Get UserName
	 * 
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getUserName(UserDetails userDetails) {
		 String userName = userDetails.getUsername().toLowerCase();
		 String useremail = userDetails.getEmail();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "getUserName")
				.put(LogMessage.MESSAGE,
						String.format(
								"User Successfully logged in into application and user name = "
										+ "[%s] email = [%s] admin = [%s]",
								userDetails.getUsername(), userDetails.getEmail(), userDetails.isAdmin()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return ResponseEntity.status(HttpStatus.OK).body("{\"data\":{\"username\":\"" + userName
				+ "\",\"useremail\":\"" + useremail + "\"}}");

	}

    /**
     * To search group in AAD.
     * @param groupName
     * @return
     */
    public ResponseEntity<DirectoryObjects> searchGroupInAzureAD(String groupName) {
        String ssoToken = oidcUtil.getSSOToken();
        DirectoryObjects groups = new DirectoryObjects();
        if (!StringUtils.isEmpty(ssoToken)) {
            List<DirectoryGroup> allGroups = oidcUtil.getGroupsFromAAD(ssoToken, groupName);

            DirectoryObjectsList groupsList = new DirectoryObjectsList();
            groupsList.setValues(allGroups.toArray(new DirectoryGroup[allGroups.size()]));
            groups.setData(groupsList);
        }
        return ResponseEntity.status(HttpStatus.OK).body(groups);
    }
    /**
     * To search groupEmail in AAD.
     * @param email
     * @return
     */
    public ResponseEntity<DirectoryObjects> searchGroupEmailInAzureAD(String email) {
        String ssoToken = oidcUtil.getSSOToken();
        DirectoryObjects groups = new DirectoryObjects();
        if (!StringUtils.isEmpty(ssoToken)) {
            List<DirecotryGroupEmail> allGroups = oidcUtil.getGroupsEmailFromAAD(ssoToken, email);

            DirectoryObjectsList groupsList = new DirectoryObjectsList();
            groupsList.setValues(allGroups.toArray(new DirecotryGroupEmail[allGroups.size()]));
            groups.setData(groupsList);
        }
        return ResponseEntity.status(HttpStatus.OK).body(groups);
    }
    
    /**
     * To get build details.
     * @return
     */
    public ResponseEntity<BuildDetails> getBuildDetails(){
    	
    	 ClassLoader classLoader = getClass().getClassLoader();
         URL resource = classLoader.getResource("build_variables.txt");
         BuildDetails details = new BuildDetails();
         try {
         if (resource == null) {
             throw new IllegalArgumentException("file not found! " + "build_variables.txt");
         } else {
        	 File ssFile =  new File(resource.toURI());
		log.debug("Trying to read build details file");
		
			if (ssFile.exists()) {
				
				Scanner sc = new Scanner(ssFile);
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					if (line.startsWith("version")) {
						String version = line.substring("version=".length(), line.length());
						log.debug("Successfully read version: from sscred file");
						details.setVersion(version);
					}
					else if (line.startsWith("date")) {
						String date = line.substring("date=".length(), line.length());
						log.debug("Successfully read password: from sscred file");
						details.setBuildDate(date);
					}
					
				}
				sc.close();
			}
		}
         }catch (Exception e) {
			log.error(String.format("Unable to read sscred file: [%s]", e.getMessage()));
		}
    	return ResponseEntity.status(HttpStatus.OK).body(details);
    }
}
