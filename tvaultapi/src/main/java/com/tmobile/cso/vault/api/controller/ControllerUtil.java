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

package com.tmobile.cso.vault.api.controller;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import com.tmobile.cso.vault.api.model.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
@Component
public final class ControllerUtil {
	
	private static RequestProcessor reqProcessor;
	public static final Logger log = LogManager.getLogger(ControllerUtil.class);

	@Value("${vault.auth.method}")
    private String tvaultAuthMethod;

	private static String vaultAuthMethod;
	
	@Value("${vault.secret.key.whitelistedchars:[a-z0-9_]+}")
    private String secretKeyWhitelistedCharacters;
	
	@Value("${vault.approle.name.whitelistedchars:[a-z0-9_]+}")
	private String approleWhitelistedCharacters;
	
	@Value("${vault.sdb.name.whitelistedchars:[a-z0-9_-]+}")
	private String sdbNameWhitelistedCharacters; 
	
	private static String secretKeyAllowedCharacters;
	
	private static String approleAllowedCharacters;
	
	private static String sdbNameAllowedCharacters="[a-z0-9_-]+";
	
	private final static String[] mountPaths = {"apps","shared","users"};
	private final static String[] permissions = {"read", "write", "deny", "sudo"};
	
	private static final String SAFELIST= "/sdb/list";
	private static final String PATHSTR= "{\"path\":\"";
	private static final String READSTR= "/read";
	private static final String USERNAMESTR= "username";
	private static final String POLICYSTR= "policies";
	private static final String UPDATEMETADATASTR= "updateMetadata";
	private static final String ACCESSTR= "access";
	private static final String METADATASTR = "metadata/";
	private static final String DELETESTR = "delete";
	private static final String DATASTR = "\",\"data\":";
	private static final String WRITESTR = "/write";
	private static final String UPDATEPOLICYONDELETE = "updateUserPolicyAssociationOnSDBDelete";
	private static final String FAILMESGSTR = "Failed to convert [%s] to lowercase.";
	private static final String ROLESTR = "Role is required.";
	private static final String CREATEMETADATASTR = "createMetadata";
	private static final String USERSTR = "username:";
	private static final String PASSWORDSTR = "password:";
	private static final String NCLMUSERNAMESTR = "nclmusername:";
	private static final String NCLMPASSWORDSTR = "nclmpassword:";
	private static final String CWMTOKENSTR = "cwmToken:";
	private static final String OIDCCLIENTNAMESTR = "OIDC_CLIENT_NAME=";
	private static final String OIDCCLIENTIDSTR = "OIDC_CLIENT_ID=";
	private static final String OIDCCLIENTSECRETSTR = "OIDC_CLIENT_SECRET=";
	private static final String BOUNDAUDIENCE = "BOUND_AUDIENCES=";
	private static final String OIDCURL = "OIDC_DISCOVERY_URL=";
	private static final String ADLOGINURL = "AD_LOGIN_URL=";
	private static final String EMAILSTR = ".t-mobile.com";
	private static final String INTERNALEXTERNAL = "internal|external";
	
	@Value("${selfservice.ssfilelocation}")
    private String sscredLocation;
	private static String sscredFileLocation;

	private static String ssUsername;
	private static String ssPassword;


	private static SSCred sscred = null;

	//NCLM Details
	private static String nclmUsername;
	private static String nclmPassword;

	//Workload token
	private static String cwmToken;

	private static String oidcClientName;
	private static String oidcClientId;
	private static String oidcClientSecret;
	private static String oidcBoundAudiences;
	private static String oidcDiscoveryUrl;
	private static String oidcADLoginUrl;
	private static OIDCCred oidcCred = null;

	private static String iamUsername;
	private static String iamPassword;
	private static IAMPortalCred iamPortalCred = null;
	
	private static OIDCUtil oidcUtil;
	private static final String ERROR_STRING= "{\"errors\":[\"Unexpected error :\"";

	@PostConstruct
	private void initStatic () {
		vaultAuthMethod = this.tvaultAuthMethod;
		secretKeyAllowedCharacters = this.secretKeyWhitelistedCharacters;
		approleAllowedCharacters = this.approleWhitelistedCharacters;
		sdbNameAllowedCharacters = this.sdbNameWhitelistedCharacters;
		sscredFileLocation = this.sscredLocation;
		readSSCredFile(sscredFileLocation, true);
		readOIDCCredFile(sscredFileLocation, true);
		readIAMPortalCredFile(sscredFileLocation, true);
	}

	@Autowired(required = true)
	public void setreqProcessor(RequestProcessor reqProcessor) {
		ControllerUtil.reqProcessor = reqProcessor;
	}

	/**
	 * Method to get requestProcessor
	 * @return
	 */
	public static RequestProcessor getReqProcessor() {
		return ControllerUtil.reqProcessor;
	}

	public static OIDCUtil getOidcUtil() {
		return ControllerUtil.oidcUtil;
	}
	
	@Autowired(required = true)
	public void setOidcUtil(OIDCUtil oidcUtil) {
		ControllerUtil.oidcUtil = oidcUtil;
	}

	public static void recursivedeletesdb(String jsonstr,String token,  Response responseVO){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, TVaultConstants.RECURSIVE_DELETE_SDB).
				put(LogMessage.MESSAGE, "Trying recursive delete...").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		ObjectMapper objMapper =  new ObjectMapper();
		String path = TVaultConstants.EMPTY;
		try {
			path = objMapper.readTree(jsonstr).at("/path").asText();
		} catch (IOException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, TVaultConstants.RECURSIVE_DELETE_SDB).
					put(LogMessage.MESSAGE, String.format ("recursivedeletesdb failed for [%s]", e.getMessage())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			responseVO.setSuccess(false);
			responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
			responseVO.setResponse(ERROR_STRING+e.getMessage() +"\"]}");
		}
		
		Response lisresp = reqProcessor.process(SAFELIST,jsonstr,token);
		if(HttpStatus.NOT_FOUND.equals(lisresp.getHttpstatus())){
			Response resp = reqProcessor.process("/delete",jsonstr,token);
			responseVO.setResponse(resp.getResponse());
			responseVO.setHttpstatus(resp.getHttpstatus());
		}else if ( HttpStatus.FORBIDDEN.equals(lisresp.getHttpstatus())){
			responseVO.setResponse(lisresp.getResponse());
			responseVO.setHttpstatus(lisresp.getHttpstatus());
			return;
		}else{
			try {
				 JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
				 for(JsonNode node : folders){
					recursivedeletesdb (PATHSTR+path+"/"+node.asText()+"\"}" ,token,responseVO);
				 }
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, TVaultConstants.RECURSIVE_DELETE_SDB).
						put(LogMessage.MESSAGE, String.format ("recursivedeletesdb failed for [%s]", e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				responseVO.setSuccess(false);
				responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				responseVO.setResponse(ERROR_STRING+e.getMessage() +"\"]}");
			}
			recursivedeletesdb (PATHSTR+path+"\"}" ,token,responseVO);
		}
	}
	
	/**
	 * Gets path from jsonstr
	 * @param objMapper
	 * @param jsonstr
	 * @param responseVO
	 * @return
	 */
	private static String getPath(ObjectMapper objMapper, String jsonstr, Response responseVO) {

		String path = TVaultConstants.EMPTY;
		try {
			path = objMapper.readTree(jsonstr).at("/path").asText();
		} catch (IOException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getPath").
					put(LogMessage.MESSAGE, String.format ("getPath failed for [%s]", e.getMessage())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			responseVO.setSuccess(false);
			responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
			responseVO.setResponse(ERROR_STRING+e.getMessage() +"\"]}");
		}
		return path;
	}
	

	/**
	 * Recursively reads the folders/secrets for a given path
	 * @param jsonstr
	 * @param token
	 * @param responseVO
	 * @param safeNode
	 */
	public static void recursiveRead(String jsonstr,String token,  Response responseVO, SafeNode safeNode){
		ObjectMapper objMapper =  new ObjectMapper();
		String path = getPath(objMapper, jsonstr, responseVO);
		/* Read the secrets for the given path */
		Response secresp = reqProcessor.process(READSTR,jsonstr,token);
		if (HttpStatus.OK.equals(secresp.getHttpstatus())) {
			responseVO.setResponse(secresp.getResponse());
			responseVO.setHttpstatus(secresp.getHttpstatus());
			SafeNode sn = new SafeNode();
			sn.setId(path);
			sn.setValue(secresp.getResponse());
			if (!TVaultConstants.SAFE.equals(safeNode.getType())) {
				sn.setType(TVaultConstants.SECRET);
				sn.setParentId(safeNode.getId());
				safeNode.addChild(sn);
			}
			else {
				safeNode.setValue(secresp.getResponse());
			}
		}
		/* Read the folders for the given path */
		Response lisresp = reqProcessor.process(SAFELIST,jsonstr,token);
		if(HttpStatus.NOT_FOUND.equals(lisresp.getHttpstatus())){
			Response resp = reqProcessor.process(READSTR,jsonstr,token);
			responseVO.setResponse(resp.getResponse());
			responseVO.setHttpstatus(resp.getHttpstatus());
			return;
		}else if ( HttpStatus.FORBIDDEN.equals(lisresp.getHttpstatus())){
			responseVO.setResponse(lisresp.getResponse());
			responseVO.setHttpstatus(lisresp.getHttpstatus());
			return;
		}else{
			if (!lisresp.getResponse().contains("errors")) {
				try {
					JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
					for(JsonNode node : folders){
						if (!node.asText().startsWith(TVaultConstants.VERSION_FOLDER_PREFIX)) {
							jsonstr = PATHSTR + path + "/" + node.asText() + "\"}";
							SafeNode sn = new SafeNode();
							sn.setId(path + "/" + node.asText());
							sn.setValue(path + "/" + node.asText());
							sn.setType(TVaultConstants.FOLDER);
							sn.setParentId(safeNode.getId());
							safeNode.addChild(sn);
							/* Recursively read the folders for the given folder/sub folders */
							recursiveRead(jsonstr, token, responseVO, sn);
						}
					}

				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "recursiveRead").
							put(LogMessage.MESSAGE, String.format ("recursiveRead failed for [%s]", e.getMessage())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					responseVO.setSuccess(false);
					responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
					responseVO.setResponse(ERROR_STRING+e.getMessage() +"\"]}");
				}
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "recursiveRead").
						put(LogMessage.MESSAGE, "Unable to recursively read the given path").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				responseVO.setSuccess(false);
				responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				responseVO.setResponse("{\"errors\":[\"Unable to recursively read the given path :"+jsonstr +"\"]}");
			}
		}
	}

	/**
	 * Recursively reads the folders/secrets for a given path to get the secret count.
	 * @param jsonstr
	 * @param token
	 * @param responseVO
	 * @param _path
	 * @param type
	 * @return
	 */
	public static SafeNode recursiveReadForCount(String jsonstr,String token,  Response responseVO, String _path, String type){
		SafeNode safeNode = new SafeNode();
		safeNode.setId(_path);
		safeNode.setType(type);
		ObjectMapper objMapper =  new ObjectMapper();
		String path = getPath(objMapper, jsonstr, responseVO);
		/* Read the secrets for the given path */
		Response secresp = reqProcessor.process(READSTR,jsonstr,token);
		if (HttpStatus.OK.equals(secresp.getHttpstatus())) {
			responseVO.setResponse(secresp.getResponse());
			responseVO.setHttpstatus(secresp.getHttpstatus());
			SafeNode sn = new SafeNode();
			sn.setId(path);
			sn.setValue(secresp.getResponse());
			if (!TVaultConstants.SAFE.equals(safeNode.getType())) {
				sn.setType(TVaultConstants.SECRET);
				sn.setParentId(safeNode.getId());
				safeNode.addChild(sn);
			}
			else {
				safeNode.setValue(secresp.getResponse());
			}
		}
		/* Read the folders for the given path */
		Response lisresp = reqProcessor.process(SAFELIST,jsonstr,token);
		if(HttpStatus.NOT_FOUND.equals(lisresp.getHttpstatus())){
			Response resp = reqProcessor.process(READSTR,jsonstr,token);
			responseVO.setResponse(resp.getResponse());
			responseVO.setHttpstatus(resp.getHttpstatus());
			return safeNode;
		}else if ( HttpStatus.FORBIDDEN.equals(lisresp.getHttpstatus())){
			responseVO.setResponse(lisresp.getResponse());
			responseVO.setHttpstatus(lisresp.getHttpstatus());
			return safeNode;
		}else{
			if (!lisresp.getResponse().contains("errors")) {
				try {
					JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
					for(JsonNode node : folders){
						jsonstr = PATHSTR+path+"/"+node.asText()+"\"}";
						SafeNode sn ;
						/* Recursively read the folders for the given folder/sub folders */
						sn = recursiveReadForCount( jsonstr,token,responseVO, path+"/"+node.asText(), TVaultConstants.FOLDER);
						sn.setId(path+"/"+node.asText());
						sn.setValue(path+"/"+node.asText());
						sn.setType(TVaultConstants.FOLDER);
						sn.setParentId(safeNode.getId());
						safeNode.addChild(sn);
					}
					return safeNode;
				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "recursiveRead").
							put(LogMessage.MESSAGE, String.format ("recursiveRead failed for [%s]", e.getMessage())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					responseVO.setSuccess(false);
					responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
					responseVO.setResponse(ERROR_STRING+e.getMessage() +"\"]}");
				}
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "recursiveRead").
						put(LogMessage.MESSAGE, String.format ("Unable to recursively read the given path [%s]",jsonstr)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				responseVO.setSuccess(false);
				responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				responseVO.setResponse("{\"errors\":[\"Unable to recursively read the given path :"+jsonstr +"\"]}");
			}
		}
		return safeNode;
	}

	/**
	 * Gets the folders and secrets for a given path
	 * @param jsonstr
	 * @param token
	 * @param responseVO
	 * @param safeNode
	 */
	public static void getFoldersAndSecrets(String jsonstr,String token,  Response responseVO, SafeNode safeNode){
		ObjectMapper objMapper =  new ObjectMapper();
		String path = getPath(objMapper, jsonstr, responseVO);
		/* Read the secrets for the given path */
		Response secresp = reqProcessor.process(READSTR,jsonstr,token);
		responseVO.setResponse(secresp.getResponse());
		responseVO.setHttpstatus(secresp.getHttpstatus());
		boolean secretsExist = false;
		if (HttpStatus.OK.equals(secresp.getHttpstatus())) {
			SafeNode sn = new SafeNode();
			sn.setId(path);
			sn.setValue(secresp.getResponse());
			if (!TVaultConstants.SAFE.equals(safeNode.getType())) {
				secretsExist = true;
				sn.setType(TVaultConstants.SECRET);
				sn.setParentId(safeNode.getId());
				safeNode.addChild(sn);
			}
			else {
				safeNode.setValue(secresp.getResponse());
			}
		}

		/* Read the folders for the given path */
		Response lisresp = reqProcessor.process(SAFELIST,jsonstr,token);
		if(HttpStatus.NOT_FOUND.equals(lisresp.getHttpstatus())){
			if (!secretsExist) {
				// No secrets and no folders
				if (TVaultConstants.SAFE.equals(safeNode.getType())) {
					responseVO.setResponse(TVaultConstants.EMPTY_JSON);
					responseVO.setHttpstatus(HttpStatus.OK);
				}
				else {
					responseVO.setResponse(lisresp.getResponse());
					responseVO.setHttpstatus(lisresp.getHttpstatus());
				}
			}
			return;
		}else if ( HttpStatus.FORBIDDEN.equals(lisresp.getHttpstatus())){
			responseVO.setResponse(lisresp.getResponse());
			responseVO.setHttpstatus(lisresp.getHttpstatus());
			return;
		}else{
			if (!lisresp.getResponse().contains("errors")) {
				try {
					JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
					for(JsonNode node : folders){
						if (!node.asText().startsWith(TVaultConstants.VERSION_FOLDER_PREFIX)) {
							jsonstr = PATHSTR+path+"/"+node.asText()+"\"}";
							SafeNode sn = new SafeNode();
							sn.setId(path+"/"+node.asText());
							sn.setValue(path+"/"+node.asText());
							sn.setType(TVaultConstants.FOLDER);
							sn.setParentId(safeNode.getId());
							safeNode.addChild(sn);
						}
					}
					responseVO.setSuccess(true);
					responseVO.setHttpstatus(HttpStatus.OK);

				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "getFoldersAndSecrets").
							put(LogMessage.MESSAGE, String.format ("Unable to getFoldersAndSecrets [%s]", e.getMessage())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					responseVO.setSuccess(false);
					responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
					responseVO.setResponse(ERROR_STRING+e.getMessage() +"\"]}");
				}
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "getFoldersAndSecrets").
						put(LogMessage.MESSAGE, String.format ("Unable to read the given path [%s]",jsonstr)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				responseVO.setSuccess(false);
				responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				responseVO.setResponse("{\"errors\":[\"Unable to read the given path :"+jsonstr +"\"]}");
			}
		}
	}
	public static Response configureLDAPUser(String userName,String policies,String groups,String token ){
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "configureLDAPUser").
				put(LogMessage.MESSAGE, String.format ("Trying configureLDAPUse with username [%s] policies [%s] and groups [%s] ", userName, policies, groups)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureUserMap = new HashMap<String,String>();
		configureUserMap.put(USERNAMESTR, userName);
		configureUserMap.put(POLICYSTR, policies);
		configureUserMap.put("groups", groups);
		String ldapUserConfigJson ="";
		try {
			ldapUserConfigJson = objMapper.writeValueAsString(configureUserMap);
		} catch (JsonProcessingException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "configureLDAPUser").
					put(LogMessage.MESSAGE, String.format ("Unable to create ldapUserConfigJson [%s] with username [%s] policies [%s] and groups [%s] ", e.getMessage(), userName, policies, groups)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return reqProcessor.process("/auth/ldap/users/configure",ldapUserConfigJson,token);
	}

	public static Response configureUserpassUser(String userName,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureUserMap = new HashMap<String,String>();
		configureUserMap.put(USERNAMESTR, userName);
		configureUserMap.put(POLICYSTR, policies);
		String userpassUserConfigJson =TVaultConstants.EMPTY;
		try {
			userpassUserConfigJson = objMapper.writeValueAsString(configureUserMap);
		} catch (JsonProcessingException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "configureUserpassUser").
					put(LogMessage.MESSAGE, String.format ("Unable to create userpassUserConfigJson [%s] with userName [%s] policies [%s] ", e.getMessage(), userName, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));

		}
		return reqProcessor.process("/auth/userpass/updatepolicy",userpassUserConfigJson,token);
	}
	public static Response configureLDAPGroup(String groupName,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureGrouMap = new HashMap<String,String>();
		configureGrouMap.put("groupname", groupName);
		configureGrouMap.put(POLICYSTR, policies);
		String ldapConfigJson ="";
		try {
			ldapConfigJson = objMapper.writeValueAsString(configureGrouMap);
		} catch (JsonProcessingException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "configureLDAPGroup").
					put(LogMessage.MESSAGE, String.format ("Unable to create ldapConfigJson [%s] with groupName [%s] policies [%s] ", e.getMessage(), groupName, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return reqProcessor.process("/auth/ldap/groups/configure",ldapConfigJson,token);
	}
	
	public static Response configureAWSRole(String roleName,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureRoleMap = new HashMap<String,String>();
		configureRoleMap.put("role", roleName);
		configureRoleMap.put(POLICYSTR, policies);
		String awsConfigJson ="";
		try {
			awsConfigJson = objMapper.writeValueAsString(configureRoleMap);
		} catch (JsonProcessingException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "configureAWSRole").
					put(LogMessage.MESSAGE, String.format ("Unable to create awsConfigJson [%s] with roleName [%s] policies [%s] ", e.getMessage(), roleName, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return reqProcessor.process("/auth/aws/roles/update",awsConfigJson,token);
	}
	
	public static Response configureAWSIAMRole(String roleName,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureRoleMap = new HashMap<String,String>();
		configureRoleMap.put("role", roleName);
		configureRoleMap.put(POLICYSTR, policies);
		String awsConfigJson ="";
		try {
			awsConfigJson = objMapper.writeValueAsString(configureRoleMap);
		} catch (JsonProcessingException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "configureAWSIAMRole").
					put(LogMessage.MESSAGE, String.format ("Unable to create awsConfigJson with message [%s] for roleName [%s] policies [%s] ", e.getMessage(), roleName, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return reqProcessor.process("/auth/aws/iam/roles/update",awsConfigJson,token);
	}

	
	
	public static Response updateMetadata(Map<String,String> params,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, UPDATEMETADATASTR).
				put(LogMessage.MESSAGE, "Trying to upate metadata with params").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		String _type = params.get("type");
		String name = params.get("name");
		String access = params.get(ACCESSTR);
		String path = params.get("path");
		path = METADATASTR+path;
		
		ObjectMapper objMapper = new ObjectMapper();
		String pathjson =PATHSTR+path+"\"}";
		// Read info for the path
		Response metadataResponse = reqProcessor.process(READSTR,pathjson,token);
		Map<String,Object> _metadataMap = null;
		if(HttpStatus.OK.equals(metadataResponse.getHttpstatus())){
			try {
				_metadataMap = objMapper.readValue(metadataResponse.getResponse(), new TypeReference<Map<String,Object>>() {});
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATEMETADATASTR).
						put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for type [%s], name [%s], access [%s] and path [%s] message [%s]", _type, name, access, path, e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			
			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>) _metadataMap.get("data");
			
			@SuppressWarnings("unchecked")
			Map<String,String> dataMap = (Map<String,String>) metadataMap.get(_type);
			if(dataMap == null) { dataMap = new HashMap<String,String>(); metadataMap.put(_type, dataMap);}
			
			dataMap.remove(name);
			if(!DELETESTR.equals(access))
				dataMap.put(name, access);
			
			String metadataJson = "";
			try {
				metadataJson = objMapper.writeValueAsString(metadataMap);
			} catch (JsonProcessingException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATEMETADATASTR).
						put(LogMessage.MESSAGE, String.format ("Error in creating metadataJson for type [%s], name [%s], access [%s] and path [%s] with message [%s]", _type, name, access, path, e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			
			String writeJson =  PATHSTR+path+DATASTR+ metadataJson +"}";
			metadataResponse = reqProcessor.process(WRITESTR,writeJson,token);
			return metadataResponse;
		}
		return null;
	}
	
	public static Response updateMetaDataOnConfigChanges(String name, String type,String currentPolicies, String latestPolicies, String token){
		
		List<String> _currentPolicies = Arrays.asList(currentPolicies.split(","));
		List<String> _latestpolicies = Arrays.asList(latestPolicies.split(","));
		List<String> _new = new ArrayList<String>();
		List<String> _del = new ArrayList<String>();
		for(String currPolicy : _currentPolicies){
			if(!_latestpolicies.contains(currPolicy)){
				_del.add(currPolicy);
			}
		}
		
		for(String latest : _latestpolicies){
			if(!_currentPolicies.contains(latest)){
				_new.add(latest);
			}
		}
		
		Map<String,String> sdbAccessMap = new HashMap<String,String>();
		
		for(String policy : _new){
			String policyInfo[] = policy.split("_");
			if(policyInfo.length==3){
				String access ="" ;
				switch(policyInfo[0]) {
					case "r" : 	access = TVaultConstants.READ_POLICY; break;
					case "w" : 	access = TVaultConstants.WRITE_POLICY; break;
					default:	access= TVaultConstants.DENY_POLICY ;break;
				}
				String path = policyInfo[1]+"/"+policyInfo[2];
				sdbAccessMap.put(path, access);
			}
		}
		for(String policy : _del){
			String policyInfo[] = policy.split("_");
			if(policyInfo.length==3){
				String path = policyInfo[1]+"/"+policyInfo[2];
				if(!sdbAccessMap.containsKey(path)){
					sdbAccessMap.put(path, DELETESTR);
				}
			}
		}		
		Response response;
		response = getupdateMetaDataResponse(sdbAccessMap,type,name,token);			
		return response;
	}
	
	private static Response getupdateMetaDataResponse(Map<String,String> sdbAccessMap,String type,String name, String token) {
		Iterator<Entry<String,String>> itr = sdbAccessMap.entrySet().iterator();
		List<String> failed = new ArrayList<String>();
		while(itr.hasNext()){
			Entry<String,String> entry = itr.next();
			Map<String,String> params = new HashMap<String,String>();
			params.put("type", type);
			params.put("name", name);
			params.put("path", entry.getKey());
			params.put(ACCESSTR, entry.getValue());
			Response rsp = updateMetadata(params, token);
			if(rsp == null || !HttpStatus.NO_CONTENT.equals(rsp.getHttpstatus())){
				failed.add(entry.getKey());
			}
		}
		Response response = new Response();
		if(failed.size()==0){
			response.setHttpstatus(HttpStatus.OK);
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "updateMetaDataOnConfigChanges").
					put(LogMessage.MESSAGE, "updateMetaDataOnConfigChanges failed ").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			response.setHttpstatus(HttpStatus.MULTI_STATUS);
			response.setResponse("Meta data update failed for "+failed.toString() );
		}
		return response;
	}

	/**
	 * Update metadata on service account update
	 * @param params
	 * @param token
	 * @return
	 */
	public static Response updateMetadataOnSvcUpdate(String path, ServiceAccount serviceAccount, String token) {
		String _path = METADATASTR + path;
		ObjectMapper objMapper = new ObjectMapper();
		String pathjson =PATHSTR+_path+"\"}";

		Response metadataResponse = reqProcessor.process(READSTR,pathjson,token);
		Map<String,Object> _metadataMap = null;
		if(HttpStatus.OK.equals(metadataResponse.getHttpstatus())){
			try {
				_metadataMap = objMapper.readValue(metadataResponse.getResponse(), new TypeReference<Map<String,Object>>() {});
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATEMETADATASTR).
						put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for type service account update, name [%s], and path [%s] message [%s]", serviceAccount.getName(), _path, e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}

			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>) _metadataMap.get("data");

			metadataMap.put("adGroup", serviceAccount.getAdGroup());
			metadataMap.put("appName", serviceAccount.getAppName());
			metadataMap.put("appID", serviceAccount.getAppID());
			metadataMap.put("appTag", serviceAccount.getAppTag());
			if (serviceAccount.getOwner() != null && !serviceAccount.getOwner().equals(TVaultConstants.EMPTY) && !metadataMap.get("managedBy").equals(serviceAccount.getOwner())) {
				metadataMap.put("managedBy", serviceAccount.getOwner());
			}
			String metadataJson = "";
			try {
				metadataJson = objMapper.writeValueAsString(metadataMap);
			} catch (JsonProcessingException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATEMETADATASTR).
						put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for type service account update, name [%s], and path [%s] message [%s]", serviceAccount.getName(), _path, e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}

			String writeJson =  PATHSTR+_path+DATASTR+ metadataJson +"}";
			metadataResponse = reqProcessor.process(WRITESTR,writeJson,token);
			return metadataResponse;
		}
		return null;

	}
	
	/**
	 * Update metadata for the service account on password reset
	 * @param params
	 * @param token
	 * @return
	 */
	public static Response updateMetadataOnSvcaccPwdReset(Map<String,String> params,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, UPDATEMETADATASTR).
				put(LogMessage.MESSAGE, "Trying to upate metadata on Service account password reset").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		String _type = params.get("type");
		String path = params.get("path");
		path = METADATASTR+path;

		ObjectMapper objMapper = new ObjectMapper();
		String pathjson =PATHSTR+path+"\"}";
		// Read info for the path
		Response metadataResponse = reqProcessor.process(READSTR,pathjson,token);
		Map<String,Object> _metadataMap = null;
		if(HttpStatus.OK.equals(metadataResponse.getHttpstatus())){
			try {
				_metadataMap = objMapper.readValue(metadataResponse.getResponse(), new TypeReference<Map<String,Object>>() {});
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATEMETADATASTR).
						put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for type [%s] and path [%s] message [%s]", _type, path, e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}

			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>) _metadataMap.get("data");

			@SuppressWarnings("unchecked")
			boolean initialPasswwordReset = (boolean) metadataMap.get(_type);
			if(StringUtils.isEmpty(initialPasswwordReset) || !initialPasswwordReset) {
				metadataMap.put(_type, true);
				String metadataJson = "";
				try {
					metadataJson = objMapper.writeValueAsString(metadataMap);
				} catch (JsonProcessingException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, UPDATEMETADATASTR).
							put(LogMessage.MESSAGE, String.format ("Error in creating metadataJson for type [%s] and path [%s] with message [%s]", _type, path, e.getMessage())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}

				String writeJson =  PATHSTR+path+DATASTR+ metadataJson +"}";
				metadataResponse = reqProcessor.process(WRITESTR,writeJson,token);
				return metadataResponse;
			}
            return metadataResponse;
		}
		return null;
	}
	
	/**
	 * Update metadata on service account password reset - modifiedBy and
	 * modifiedAt
	 * 
	 * @param params
	 * @param token
	 * @return
	 */
	public static Response updateMetadataOnSvcPwdReset(String path,
			ADServiceAccountResetDetails adServiceAccountResetDetails, String token) {
		String svcPath = METADATASTR + path;
		ObjectMapper objMapper = new ObjectMapper();
		String pathjson = PATHSTR + svcPath + "\"}";

		Response metadataResponse = reqProcessor.process(READSTR, pathjson, token);
		Map<String, Object> svcMetadataMap = null;
		if (HttpStatus.OK.equals(metadataResponse.getHttpstatus())) {
			try {
				svcMetadataMap = objMapper.readValue(metadataResponse.getResponse(),
						new TypeReference<Map<String, Object>>() {
						});
			} catch (IOException e) {
				log.error(e);
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, UPDATEMETADATASTR)
								.put(LogMessage.MESSAGE,
										String.format(
												"Error creating _metadataMap for type service account update, message [%s]",
												e.getMessage()))
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> metadataMap = (Map<String, Object>) svcMetadataMap.get("data");

			metadataMap.put("modifiedBy", adServiceAccountResetDetails.getModifiedBy());
			metadataMap.put("modifiedAt", adServiceAccountResetDetails.getModifiedAt());

			String metadataJson = "";
			try {
				metadataJson = objMapper.writeValueAsString(metadataMap);
			} catch (JsonProcessingException e) {
				log.error(e);
				log.error(
						JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, UPDATEMETADATASTR)
								.put(LogMessage.MESSAGE,
										String.format(
												"Error creating _metadataMap for type service account update, message [%s]",
												e.getMessage()))
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
			}

			String writeJson = PATHSTR + svcPath + DATASTR + metadataJson + "}";
			metadataResponse = reqProcessor.process(WRITESTR, writeJson, token);
			return metadataResponse;
		}
		return null;

	}
	
	/**
	 * 
	 * @param jsonString
	 * @return
	 */
	public static Map<String,Object> parseJson (String jsonString){
		Map<String, Object> response = new HashMap<>(); 
		try {
			if(jsonString !=null )
				response = new ObjectMapper().readValue(jsonString, new TypeReference<Map<String, Object>>(){});
		} catch (Exception e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "parseJson").
					put(LogMessage.MESSAGE, "parseJson failed ").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return response;
	}
	
	public static String convetToJson (Map<String,Object> jsonMap){
		String jsonStr = TVaultConstants.EMPTY_JSON;
		try {
			jsonStr = new ObjectMapper().writeValueAsString(jsonMap);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "convetToJson").
					put(LogMessage.MESSAGE, "convetToJson failed ").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
	
		return jsonStr;
	}
	/**
	 * Convenient method to get policies as comma separated String
	 * @param objMapper
	 * @param policyJson
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static String getPoliciesAsStringFromJson(ObjectMapper objMapper, String policyJson) throws JsonProcessingException, IOException{
		String currentpolicies = TVaultConstants.EMPTY;
		JsonNode policiesNode = objMapper.readTree(policyJson).get("data").get(POLICYSTR);
		if (policiesNode.isContainerNode()) {
			Iterator<JsonNode> elementsIterator = policiesNode.elements();
		       while (elementsIterator.hasNext()) {
		    	   JsonNode element = elementsIterator.next();
		           currentpolicies += element.asText()+",";
		       }
		}
		else {
			currentpolicies = policiesNode.asText();
		}
		if (currentpolicies.endsWith(",")) {
			currentpolicies = currentpolicies.substring(0, currentpolicies.length()-1);
		}
		return currentpolicies;
	}

	/**
	 * Convenient method to get policies as list
	 * @param objMapper
	 * @param policyJson
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static List<String> getPoliciesAsListFromJson(ObjectMapper objMapper, String policyJson) throws JsonProcessingException, IOException{
		List<String> currentpolicies = new ArrayList<>();
		JsonNode policiesNode = objMapper.readTree(policyJson).get("data").get(POLICYSTR);
		if (policiesNode.isContainerNode()) {
			Iterator<JsonNode> elementsIterator = policiesNode.elements();
			while (elementsIterator.hasNext()) {
				JsonNode element = elementsIterator.next();
				currentpolicies.add(element.asText());
			}
		}
		else {
			currentpolicies.add(policiesNode.asText());
		}
		return currentpolicies;
	}

	public static void updateUserPolicyAssociationOnSDBDelete(String sdb,Map<String,String> acessInfo,String token, UserDetails userDetails){
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, UPDATEPOLICYONDELETE).
				put(LogMessage.MESSAGE, "trying updateUserPolicyAssociationOnSDBDelete").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		log.debug ("updateUserPolicyAssociationOnSDBDelete...for auth method {} " , vaultAuthMethod);
		if(acessInfo!=null){
			String folders[] = sdb.split("[/]+");
			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			String s_policy = "s_";

			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
						s_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
						s_policy += folders[index] +"_";
					}
				}
			}	
			Set<String> users = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for(String userName : users){
				
				Response userResponse = new Response();
				if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
					userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token);
				}
				else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
					userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
				}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					// OIDC implementation changes
					ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, null, true);
					if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
						if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
							log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
									.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
									.put(LogMessage.ACTION, "Add User to SDB")
									.put(LogMessage.MESSAGE,
											"Trying to fetch OIDC user policies, failed")
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
									.build()));
							ResponseEntity.status(HttpStatus.FORBIDDEN)
									.body("{\"messages\":[\"User configuration failed. Please try again.\"]}");
						}
						ResponseEntity.status(HttpStatus.NOT_FOUND)
								.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
					}
					oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
					oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
					userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
					userResponse.setHttpstatus(responseEntity.getStatusCode());
				}	
				String responseJson="";
				String groups="";
				List<String> policies = new ArrayList<>();
				List<String> currentpolicies = new ArrayList<>();

				if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
					responseJson = userResponse.getResponse();	
					try {
						// OIDC implementation changes
						if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
							currentpolicies.addAll(oidcEntityResponse.getPolicies());
						} else {
							currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
							if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
								groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
							}
						}
					} catch (IOException e) {
						log.error(e);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, UPDATEPOLICYONDELETE).
								put(LogMessage.MESSAGE, String.format ("updateUserPolicyAssociationOnSDBDelete failed [%s]", e.getMessage())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
					}
					policies.addAll(currentpolicies);
					policies.remove(r_policy);
					policies.remove(w_policy);
					policies.remove(d_policy);
					policies.remove(s_policy);

					String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");

					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, UPDATEPOLICYONDELETE).
							put(LogMessage.MESSAGE, String.format ("Current policies [%s]", policies )).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
						log.debug ("Inside userpass");
						ControllerUtil.configureUserpassUser(userName,policiesString,token);
					}
					else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
						log.debug ("Inside non-userpass");
						ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
					}
					else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						//OIDC Implementation : Entity Update
						try {
							oidcUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName());
							oidcUtil.renewUserToken(userDetails.getClientToken());
						} catch (Exception e) {
							log.error(e);
							log.error(
									JSONUtil.getJSON(
											ImmutableMap.<String, String> builder()
													.put(LogMessage.USER,
															ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
													.put(LogMessage.ACTION, "Add User to SDB")
													.put(LogMessage.MESSAGE,
															"Exception while adding or updating the identity ")
													.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
													.put(LogMessage.APIURL,
															ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
													.build()));
						}
					}
				}
				
			}
		}
	}
	public static void updateGroupPolicyAssociationOnSDBDelete(String sdb,Map<String,String> acessInfo,String token, UserDetails userDetails){
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "updateGroupPolicyAssociationOnSDBDelete").
				put(LogMessage.MESSAGE, "trying updateGroupPolicyAssociationOnSDBDelete").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			log.debug ("Inside userpass of updateGroupPolicyAssociationOnSDBDelete...Just Returning...");
			return;
		}
		if(acessInfo!=null){
			String folders[] = sdb.split("[/]+");
			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
					}
					else {
						r_policy += folders[index] +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}	
			Set<String> groups = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for(String groupName : groups){
				Response response = new Response();
				if( TVaultConstants.LDAP.equals(vaultAuthMethod) ){
					response = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
				}else if ( TVaultConstants.OIDC.equals(vaultAuthMethod) ) {
					//call read api with groupname
					oidcGroup = oidcUtil.getIdentityGroupDetails(groupName, token);
					if (oidcGroup != null) {
						response.setHttpstatus(HttpStatus.OK);
						response.setResponse(oidcGroup.getPolicies().toString());
					} else {
						response.setHttpstatus(HttpStatus.BAD_REQUEST);
					}
				}
				 
				String responseJson="";
				List<String> policies = new ArrayList<>();
				List<String> currentpolicies = new ArrayList<>();
				if(HttpStatus.OK.equals(response.getHttpstatus())){
					responseJson = response.getResponse();	
					try {
						//OIDC Changes
						if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
							currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
						} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && oidcGroup != null) {
							currentpolicies.addAll(oidcGroup.getPolicies());
						}
					} catch (IOException e) {
						log.error(e);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, UPDATEPOLICYONDELETE).
								put(LogMessage.MESSAGE, String.format ("updateUserPolicyAssociationOnSDBDelete failed [%s]", e.getMessage())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
					}
					policies.addAll(currentpolicies);
					policies.remove(r_policy);
					policies.remove(w_policy);
					policies.remove(d_policy);
					String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, UPDATEPOLICYONDELETE).
							put(LogMessage.MESSAGE, String.format ("Current policies [%s]", policies )).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						ControllerUtil.configureLDAPGroup(groupName, policiesString, token);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
						oidcUtil.updateGroupPolicies(token, groupName, policies, currentpolicies, oidcGroup != null ? oidcGroup.getId() : null);
						oidcUtil.renewUserToken(userDetails.getClientToken());
					}
					
				}
			}
		}
	}
	
	// Not using this method and decided to delete the role instead with the concept that you cant have same role used by different safe.S
	public static void updateAwsRolePolicyAssociationOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "updateAwsRolePolicyAssociation OnSDBDelete").
				put(LogMessage.MESSAGE, "trying updateAwsRolePolicyAssociationOnSDBDelete").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if(acessInfo!=null){
			String folders[] = sdb.split("[/]+");
			String r_policy = "r_";
			String w_policy = "w_";
			String d_policy = "d_";
			if (folders.length > 0) {
				for (int index = 0; index < folders.length; index++) {
					if (index == folders.length -1 ) {
						r_policy += folders[index];
						w_policy += folders[index];
						d_policy += folders[index];
					}
					else {
						r_policy += folders[index]  +"_";
						w_policy += folders[index] +"_";
						d_policy += folders[index] +"_";
					}
				}
			}	

			updateAwsRolePolicyOnSDBDelete(r_policy,d_policy,w_policy, token, acessInfo);		
		}
	}
	
	private static void updateAwsRolePolicyOnSDBDelete(String r_policy,String d_policy,String w_policy,String token,Map<String,String> acessInfo) {
		Set<String> roles = acessInfo.keySet();
		ObjectMapper objMapper = new ObjectMapper();
		for(String role : roles){
			Response roleResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+role+"\"}",token);
			String responseJson="";
			String policies ="";
			String currentpolicies =TVaultConstants.EMPTY;
			
			if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
				responseJson = roleResponse.getResponse();	
				try {
					JsonNode policiesArry =objMapper.readTree(responseJson).get(POLICYSTR);
					for(JsonNode policyNode : policiesArry){
						currentpolicies =	(currentpolicies.equals("") ) ? currentpolicies+policyNode.asText():currentpolicies+","+policyNode.asText();
					}
				} catch (IOException e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "updateAwsRolePolicyAssociationOnSDBDelete").
							put(LogMessage.MESSAGE, String.format ("Generation of currentpolicies failed for [%s]", e.getMessage())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}
				policies = currentpolicies;
				policies = policies.replaceAll(r_policy, "");
				policies = policies.replaceAll(w_policy, "");
				policies = policies.replaceAll(d_policy, "");
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "updateAwsRolePolicyAssociationOnSDBDelete").
						put(LogMessage.MESSAGE, String.format ("currentpolicies [%s]",policies)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				ControllerUtil.configureAWSRole(role, policies, token);
			}
		}
	}
	
	public static void deleteAwsRoleOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "delete AwsRoleOnSDBDelete").
				put(LogMessage.MESSAGE, "Trying to deleteAwsRoleOnSDBDelete").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			log.debug ("Inside userpass of deleteAwsRoleOnSDBDelete...Just Returning...");
			return;
		}
		if(acessInfo!=null){
			Set<String> roles = acessInfo.keySet();
			for(String role : roles){
				Response response = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token);
				if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
					log.debug(role +" , AWS Role is deleted as part of sdb delete. SDB path "+ sdb );
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "deleteAwsRoleOnSDBDelete").
							put(LogMessage.MESSAGE, String.format ("%s, AWS Role is deleted as part of sdb delete. SDB path %s ", role, sdb)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}else{
					log.debug(role +" , AWS Role deletion as part of sdb delete failed . SDB path "+ sdb );
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "deleteAwsRoleOnSDBDelete").
							put(LogMessage.MESSAGE, String.format ("%s, AWS Role is deletion failed. SDB path %s ", role, sdb)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}
			}
		}
	}
	public static boolean isValidDataPath(String path){
		String paths[] =  path.split("/");
		if(paths.length==3){
			String safeType =  paths[0];
			if(!(TVaultConstants.APPS.equals(safeType)||TVaultConstants.SHARED.equals(safeType)||TVaultConstants.USERS.equals(safeType))){
				return false;
			}
		}else{
			return false;
		}
		return true;
	}
	
	public static boolean isPathValid(String path){
		String paths[] =  path.split("/");
		if(paths.length > 0){
			String safeType =  paths[0];
			if(!(TVaultConstants.APPS.equals(safeType)||TVaultConstants.SHARED.equals(safeType)||TVaultConstants.USERS.equals(safeType))){
				return false;
			}
		}else{
			return false;
		}
		return true;
	}
	
	public static boolean isValidSafePath(String path){
		String paths[] =  path.split("/");
		if(paths.length==2){
			String safeType =  paths[0];
			if(!(TVaultConstants.APPS.equals(safeType)||TVaultConstants.SHARED.equals(safeType)||TVaultConstants.USERS.equals(safeType))){
				return false;
			}
		}else{
			return false;
		}
		return true;
	}
	public static String getSafePath(String path){
		String paths[] =  path.split("/");
		return paths[0]+"/"+paths[1];
	}
	/**
	 * Gets the safe type for a given path
	 * @param path
	 * @return
	 */
	public static String getSafeType(String path){
		String safeType = TVaultConstants.UNKNOWN;
		if (!StringUtils.isEmpty(path)) {
			String paths[] =  path.split("/");
			if (paths != null && paths.length > 0) {
				safeType = paths[0];
			}
		}
		return safeType;
	}
	/**
	 * Gets the safe type for a given path
	 * @param path
	 * @return
	 */
	public static String getSafeName(String path){
		String safeName = TVaultConstants.EMPTY;
		if (!StringUtils.isEmpty(path)) {
			String paths[] =  path.split("/");
			if (paths != null && paths.length > 1) {
				safeName = paths[1];
			}
		}
		return safeName;
	}
	/**
	 * Decides whether a user can be added to a safe or not
	 * @param path
	 * @param token
	 * @return
	 */
	public static boolean canAddPermission(String path,String token) {
		String safeType = ControllerUtil.getSafeType(path);
		String safeName = ControllerUtil.getSafeName(path);
		
		List<String> existingSafeNames = getAllExistingSafeNames(safeType, token);
		List<String> duplicateSafeNames = new ArrayList<String>();
		int count=0;
		for (String existingSafeName: existingSafeNames) {
			if (existingSafeName.equalsIgnoreCase(safeName)) {
				count++;
				duplicateSafeNames.add(existingSafeName);
			}
		}
		if (count == 1) {
			// There is one valid safe, Hence permission can be added
			// Exact match
			return true;
		}
		else {
			// There are no safes or more than one and hence permission can't be added
			return false;
		}
	}

	/**
	 * Checks whether a safe exists in given path
	 * @param path
	 * @param token
	 * @return
	 */
	public static boolean isValidSafe(String path,String token){
		String safePath = getSafePath(path);
		String _path = METADATASTR+safePath;
		Response response = reqProcessor.process("/sdb",PATHSTR+_path+"\"}",token);
		if(HttpStatus.OK.equals(response.getHttpstatus())){
			return true;
		}
		return false;
	}
	/**
	 * Checks whether a given sdb name is vaild
	 * @param sdbName
	 * @return
	 */
	private static boolean isSdbNameValid(String sdbName) {
		boolean valid = Pattern.matches(sdbNameAllowedCharacters, sdbName);
		return valid;
	}
	
	/**
	 * Validates inputs values required for SDB creation
	 * @param requestParams
	 * @return
	 */
	public static boolean areSDBInputsValid(Map<String, Object> requestParams) {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) requestParams.get("data");
		if (MapUtils.isEmpty(map)) {
			return false;
		}
		String sdbName = (String) map.get("name");
		String sdbOwner = (String) map.get("owner");
		String sdbDescription = (String) map.get("description");
		String path = (String) requestParams.get("path");
		if (StringUtils.isEmpty(sdbName) 
				|| StringUtils.isEmpty(sdbOwner) 
				|| StringUtils.isEmpty(sdbDescription) 
				|| StringUtils.isEmpty(path) 
				) {
			return false;
		}
		if (!isSdbNameValid(sdbName) || sdbName.length() > 40 || !sdbName.equals(sdbName.toLowerCase())) {
			return false;
		}
		String safeName = getSafeName(path);
		if (!sdbName.equals(safeName)) {
			return false;
		}
		
		if (!EmailValidator.getInstance().isValid(sdbOwner)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validates inputs values required for SDB creation
	 * @param safe
	 * @return
	 */
	public static boolean areSDBInputsValid(Safe safe) {
		if (safe == null) {
			return false;
		}
		SafeBasicDetails safeBasicDetails = safe.getSafeBasicDetails();
		if (safeBasicDetails == null) {
			return false;
		}
		String sdbName = safeBasicDetails.getName();
		String sdbOwner = safeBasicDetails.getOwner();
		String sdbDescription = safeBasicDetails.getDescription();
		String appName=safeBasicDetails.getAppName();
		String path = safe.getPath();
		if (StringUtils.isEmpty(sdbName) 
				|| StringUtils.isEmpty(sdbOwner) 
				|| StringUtils.isEmpty(sdbDescription) 
				|| StringUtils.isEmpty(path) 
				|| StringUtils.isEmpty(appName) 
				) {
			return false;
		}
		if(appName != null ) {
			String applicationName=appName.trim();
			if(StringUtils.isEmpty(applicationName)) {
				return false;
			}
		}
		if (!isSdbNameValid(sdbName) || sdbName.length() > 40 || !sdbName.equals(sdbName.toLowerCase())) {
			return false;
		}
		String safeName = getSafeName(path);
		if (!sdbName.equals(safeName)) {
			return false;
		}
		
		if (!EmailValidator.getInstance().isValid(sdbOwner)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validates inputs values required for SDB creation
	 * @param requestParams
	 * @return
	 */
	public static boolean areSDBInputsValidForUpdate(Map<String, Object> requestParams) {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) requestParams.get("data");
		if (MapUtils.isEmpty(map)) {
			return false;
		}
		String sdbName = (String) map.get("name");
		String sdbOwner = (String) map.get("owner");
		String sdbDescription = (String) map.get("description");
		String sdbAppName = (String) map.get("appName");
		String path = (String) requestParams.get("path");
		if (StringUtils.isEmpty(sdbName) 
				|| StringUtils.isEmpty(sdbOwner) 
				|| StringUtils.isEmpty(sdbDescription) 
				|| StringUtils.isEmpty(path) 
				|| StringUtils.isEmpty(sdbAppName)
				) {
			return false;
		}
		if(sdbAppName != null ) {
			String applicationName=sdbAppName.trim();
			if(StringUtils.isEmpty(applicationName)) {
				return false;
			}
		}
		if (sdbName.length() > 40) {
			return false;
		}
		String safeName = getSafeName(path);
		if (!sdbName.equalsIgnoreCase(safeName)) {
			return false;
		}
		if (!EmailValidator.getInstance().isValid(sdbOwner)) {
			return false;
		}
		return true;
	}

	/**
	 * Validates Safe Group Inputs
	 * @param requestMap
	 * @return
	 */
	public static boolean areSafeGroupInputsValid(Map<String,String> requestMap) {
		if (MapUtils.isEmpty(requestMap)) {
			return false;
		}
		if (ObjectUtils.isEmpty(requestMap.get("groupname"))
				|| ObjectUtils.isEmpty(requestMap.get("path"))
				|| ObjectUtils.isEmpty(requestMap.get(ACCESSTR))
				) {
			return false;
		}
		String path = requestMap.get("path");
		if (!isPathValid(path)) {
			return false;
		}
		String access = (String) requestMap.get(ACCESSTR);
		if (!ArrayUtils.contains(permissions, access)) {
			return false;
		}
		return true;
	}
	/**
	 * Validates AWS Role User inputs
	 * @param requestMap
	 * @return
	 */
	public static boolean areAWSRoleInputsValid(Map<String, String> requestMap) {
		if (MapUtils.isEmpty(requestMap)) {
			return false;
		}
		if (ObjectUtils.isEmpty(requestMap.get("role"))
				|| ObjectUtils.isEmpty(requestMap.get("path"))
				|| ObjectUtils.isEmpty(requestMap.get(ACCESSTR))
				) {
			return false;
		}
		String path = (String) requestMap.get("path");
		if (!isPathValid(path)) {
			return false;
		}
		String access = (String) requestMap.get(ACCESSTR);
		if (!ArrayUtils.contains(permissions, access)) {
			return false;
		}
		return true;
	}
	/**
	 * Validates Safe User inputs
	 * @param safeUser
	 * @return
	 */
	public static boolean areSafeUserInputsValid(SafeUser safeUser) {
		if (ObjectUtils.isEmpty(safeUser)) {
			return false;
		}
		if (ObjectUtils.isEmpty(safeUser.getUsername())
				|| ObjectUtils.isEmpty(safeUser.getAccess())
				|| ObjectUtils.isEmpty(safeUser.getPath())
				) {
			return false;
		}
		String path = safeUser.getPath();
		if (!isPathValid(path)) {
			return false;
		}
		String access = safeUser.getAccess();
		if (!ArrayUtils.contains(permissions, access)) {
			return false;
		}
		return true;
	}
	/**
	 * Validates Safe User inputs
	 * @param requestMap
	 * @return
	 */
	public static boolean areSafeUserInputsValid(Map<String,Object> requestMap) {
		if (MapUtils.isEmpty(requestMap)) {
			return false;
		}
		if (ObjectUtils.isEmpty(requestMap.get(USERNAMESTR))
				|| ObjectUtils.isEmpty(requestMap.get("path"))
				|| ObjectUtils.isEmpty(requestMap.get(ACCESSTR))
				) {
			return false;
		}
		String path = (String) requestMap.get("path");
		if (!isPathValid(path)) {
			return false;
		}
		String access = (String) requestMap.get(ACCESSTR);
		if (!ArrayUtils.contains(permissions, access)) {
			return false;
		}
		return true;
	}
	/**
	 * Validates Safe Group inputs
	 * @param safeUser
	 * @return
	 */
	public static boolean areSafeGroupInputsValid(SafeGroup safeGroup) {
		if (ObjectUtils.isEmpty(safeGroup)) {
			return false;
		}
		if (ObjectUtils.isEmpty(safeGroup.getGroupname())
				|| ObjectUtils.isEmpty(safeGroup.getAccess())
				|| ObjectUtils.isEmpty(safeGroup.getPath())
				) {
			return false;
		}
		String path = safeGroup.getPath();
		if (!isPathValid(path)) {
			return false;
		}
		String access = safeGroup.getAccess();
		if (!ArrayUtils.contains(permissions, access)) {
			return false;
		}
		return true;
	}
	/**
	 * Validates Safe User inputs for AppRole association
	 * @param requestMap
	 * @return
	 */
	public static boolean areSafeAppRoleInputsValid(Map<String,Object> requestMap) {
		if (MapUtils.isEmpty(requestMap)) {
			return false;
		}
		if (ObjectUtils.isEmpty(requestMap.get("role_name"))
				|| ObjectUtils.isEmpty(requestMap.get("path"))
				|| ObjectUtils.isEmpty(requestMap.get(ACCESSTR))
				) {
			return false;
		}
		String path = (String) requestMap.get("path");
		if (!isPathValid(path)) {
			return false;
		}
		String access = (String) requestMap.get(ACCESSTR);
		if (!ArrayUtils.contains(permissions, access)) {
			return false;
		}
		return true;
	}
	/**
	 * Validates AWS Role Group inputs
	 * @param safeUser
	 * @return
	 */
	public static boolean areAWSRoleInputsValid(AWSRole awsRole) {
		if (ObjectUtils.isEmpty(awsRole)) {
			return false;
		}
		if (ObjectUtils.isEmpty(awsRole.getRole())
				|| ObjectUtils.isEmpty(awsRole.getAccess())
				|| ObjectUtils.isEmpty(awsRole.getPath())
				) {
			return false;
		}
		String path = awsRole.getPath();
		if (!isPathValid(path)) {
			return false;
		}
		String access = awsRole.getAccess();
		if (!ArrayUtils.contains(permissions, access)) {
			return false;
		}
		return true;
	}
	public static String converSDBInputsToLowerCase(String jsonStr) {
		try {
			Safe safe = (Safe)JSONUtil.getObj(jsonStr, Safe.class);
			safe.getSafeBasicDetails().setName(safe.getSafeBasicDetails().getName().toLowerCase());
			safe.setPath(safe.getPath().toLowerCase());
			jsonStr = JSONUtil.getJSON(safe);
			return jsonStr;
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "converSDBInputsToLowerCase").
				      put(LogMessage.MESSAGE, String.format (FAILMESGSTR, jsonStr)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return jsonStr;
		}
	}
	/**
	 * 
	 * @param safe
	 */
	public static void converSDBInputsToLowerCase(Safe safe) {
		try {
			safe.getSafeBasicDetails().setName(safe.getSafeBasicDetails().getName().toLowerCase());
			safe.setPath(safe.getPath().toLowerCase());
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "converSDBInputsToLowerCase").
				      put(LogMessage.MESSAGE, "Failed while converting safe details to lowercase.").
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
		}
	}
	/**
	 * Converts the appRole Inputs to lower case
	 * @param jsonstr
	 * @return
	 */
	public static String convertAppRoleInputsToLowerCase(String jsonstr) {
		try {
			AppRole appRole = (AppRole)JSONUtil.getObj(jsonstr, AppRole.class);
			appRole.setRole_name(appRole.getRole_name());
			jsonstr = JSONUtil.getJSON(appRole);
			return jsonstr;
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "convertAppRoleInputsToLowerCase").
				      put(LogMessage.MESSAGE, String.format (FAILMESGSTR, jsonstr)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return jsonstr;
		}
	}
	
	public static String convertSafeAppRoleAccessToLowerCase(String jsonstr) {
		try {
			SafeAppRoleAccess safeAppRoleAccess = (SafeAppRoleAccess)JSONUtil.getObj(jsonstr, SafeAppRoleAccess.class);
			if (!StringUtils.isEmpty(safeAppRoleAccess.getRole_name())) {
				safeAppRoleAccess.setRole_name(safeAppRoleAccess.getRole_name().toLowerCase());
			}
			if (!StringUtils.isEmpty(safeAppRoleAccess.getAccess())) {
				safeAppRoleAccess.setAccess(safeAppRoleAccess.getAccess().toLowerCase());
			}
			jsonstr = JSONUtil.getJSON(safeAppRoleAccess);
			return jsonstr;
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "convertSafeAppRoleAccessToLowerCase").
				      put(LogMessage.MESSAGE, String.format (FAILMESGSTR, jsonstr)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return jsonstr;
		}
	}
	
	public static String convertAppRoleSecretIdToLowerCase(String jsonstr) {
		try {
			AppRoleSecretData appRoleSecretData = (AppRoleSecretData)JSONUtil.getObj(jsonstr, AppRoleSecretData.class);
			if (!StringUtils.isEmpty(appRoleSecretData.getRole_name())) {
				appRoleSecretData.setRole_name(appRoleSecretData.getRole_name().toLowerCase());
			}
			jsonstr = JSONUtil.getJSON(appRoleSecretData);
			return jsonstr;
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "convertAppRoleSecretIdToLowerCase").
				      put(LogMessage.MESSAGE, String.format (FAILMESGSTR, jsonstr)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return jsonstr;
		}
	}
	
	/**
	 * Validates the SecretKey
	 * @return
	 */
	public static boolean isSecretKeyValid(String jsonString) {
		Pattern pattern = Pattern.compile(secretKeyAllowedCharacters, Pattern.CASE_INSENSITIVE);
		String secretKey = getSecretKey(jsonString);
		if (StringUtils.isEmpty(secretKey)) {
			return false;
		}
		Matcher matcher = pattern.matcher(secretKey);
		boolean valid = matcher.find();
		return !valid;
	}
	/**
	 * Checks whether the given approle is valid
	 * @param approleName
	 * @return
	 */
	private static boolean isAppRoleNameValid(String approleName) {
		boolean valid = Pattern.matches(approleAllowedCharacters, approleName);
		return valid;
	}
	/**
	 * Validates the approle inputs
	 * @param requestParams
	 * @return
	 */
	public static boolean areAppRoleInputsValid(String jsonstr) {
		AppRole approle = getAppRoleObjFromString(jsonstr);
		return (areAppRoleInputsValid(approle));
	}
	/**
	 * Validates the approle inputs
	 * @param approle
	 * @return
	 */
	public static boolean areAppRoleInputsValid(AppRole approle) {
		if (null!=approle) {
			String approleName = approle.getRole_name();
			if (StringUtils.isEmpty(approleName) || !isAppRoleNameValid(approleName)) {
				return false;
			}
			return true;
		}
		return false;
	}
	/**
	 * Generates AppRole object from JSON
	 * @param jsonstr
	 * @return
	 */
	public static AppRole getAppRoleObjFromString(String jsonstr) {
		try {
			return (AppRole)JSONUtil.getObj(jsonstr, AppRole.class);
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					  put(LogMessage.ACTION, "getAppRoleObjFromString").
				      put(LogMessage.MESSAGE, String.format ("Failed to convert [%s] to AppRole object.", jsonstr)).
				      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				      build()));
			return null;
		}
	}
	/**
	 * Validates one or more SecretKeys
	 * @return
	 */
	public static boolean areSecretKeysValid(String jsonString) {
		Map<String, Boolean> validationMap = new HashMap<String, Boolean>();
		ArrayList<String> secretKeys = getSecretKeys(jsonString);
		for (String secretKey : secretKeys) {
			if (StringUtils.isEmpty(secretKey)) {
				return false;
			}
			boolean valid = Pattern.matches(secretKeyAllowedCharacters, secretKey);
			// Collect validation result for all.
			validationMap.put(secretKey, valid);
		}
		if (validationMap.values().contains(false)) {
			return false;
		}
		return true;
	}
	
	private static String getSecretKey(String jsonString) {
		String secretKey = null ;
		try {
			Map<String, Object> requestParams = new ObjectMapper().readValue(jsonString, new TypeReference<Map<String, Object>>(){});
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) requestParams.get("data");
			for (Object key : map.keySet()) {
				secretKey = (String) key;
				if(secretKey != null) {
					break;
				}
			 }
			return secretKey;
		} catch (JsonParseException e) {
			return secretKey;
		} catch (JsonMappingException e) {
			return secretKey;
		} catch (IOException e) {
			return secretKey;
		}
	}
	/**
	 * 
	 * @param jsonString
	 * @return
	 */
	private static ArrayList<String> getSecretKeys(String jsonString) {
		ArrayList<String> secretKeys = new ArrayList<String>() ;
		try {
			Map<String, Object> requestParams = new ObjectMapper().readValue(jsonString, new TypeReference<Map<String, Object>>(){});
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) requestParams.get("data");
			for (Object key : map.keySet()) {
				secretKeys.add((String) key);
			  }
			return secretKeys;
		} catch (JsonParseException e) {
			return secretKeys;
		} catch (JsonMappingException e) {
			return secretKeys;
		} catch (IOException e) {
			return secretKeys;
		}
	}
	/**
	 * 
	 * @param jsonString
	 * @return
	 */
	public static String  addDefaultSecretKey(String jsonString) {
		try {
			Map<String, Object> requestParams = new ObjectMapper().readValue(jsonString, new TypeReference<Map<String, Object>>(){});
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) requestParams.get("data");
			if (map.isEmpty()) {
				map.put("default", "default");
			}
			return JSONUtil.getJSON(requestParams);
		} catch (JsonParseException e) {
			return jsonString;
		} catch (JsonMappingException e) {
			return jsonString;
		} catch (IOException e) {
			return jsonString;
		}
	}
	/**
	 * Validate the AWS Login inputs
	 * @param authType
	 * @return
	 */
	public static boolean areAwsLoginInputsValid(AWSAuthType authType, AWSAuthLogin awsAuthLogin) {
		if (awsAuthLogin == null) {
			return false;
		}
		if (StringUtils.isEmpty(awsAuthLogin.getRole())) {
			return false;
		}
				
		if (AWSAuthType.EC2.equals(authType)) {
			if (!StringUtils.isEmpty(awsAuthLogin.getPkcs7())) {
				return true;
			}
		}
		else if (AWSAuthType.IAM.equals(authType)) {
			if (!StringUtils.isEmpty(awsAuthLogin.getIam_http_request_method()) || !StringUtils.isEmpty(awsAuthLogin.getIam_request_body())
					|| !StringUtils.isEmpty(awsAuthLogin.getIam_request_headers()) || !StringUtils.isEmpty(awsAuthLogin.getIam_request_url())
					) {
				return true;
			}
		}
		return false;
	}
	/**
	 * validate EC2Role inputs
	 * @param awsLoginRole
	 * @return
	 */
	public static boolean areAWSEC2RoleInputsValid(AWSLoginRole awsLoginRole) throws TVaultValidationException {
		if (awsLoginRole == null) {
			return false;
		}
		if (StringUtils.isEmpty(awsLoginRole.getRole())) {
			throw new TVaultValidationException(ROLESTR);
		}
		else if (StringUtils.isEmpty(awsLoginRole.getAuth_type()) || !awsLoginRole.getAuth_type().equalsIgnoreCase("ec2")) {
			throw new TVaultValidationException("auth_type is required and it should be ec2.");
		}
		else if (!StringUtils.isEmpty(awsLoginRole.getBound_account_id()) 
				|| !StringUtils.isEmpty(awsLoginRole.getBound_ami_id()) 
				|| !StringUtils.isEmpty(awsLoginRole.getBound_iam_instance_profile_arn()) 
				|| !StringUtils.isEmpty(awsLoginRole.getBound_iam_role_arn()) 
				|| !StringUtils.isEmpty(awsLoginRole.getBound_region()) 
				|| !StringUtils.isEmpty(awsLoginRole.getBound_subnet_id()) 
				|| !StringUtils.isEmpty(awsLoginRole.getBound_vpc_id()) 
			) {
			return true;
		}
		throw new TVaultValidationException("At least one bound parameter should be specified.");
	}
	
	public static boolean areAWSEC2RoleInputsValid(String jsonStr) throws TVaultValidationException {
		
		Map<String,String> map = null;
		try {
			ObjectMapper objMapper = new ObjectMapper();
			map = objMapper.readValue(jsonStr, new TypeReference<Map<String,String>>() {});
		} catch (IOException e) {
			throw new TVaultValidationException("Invalid Inputs");
		}

		if (MapUtils.isEmpty(map)) {
			return false;
		}
		
		if (StringUtils.isEmpty(map.get("role"))) {
			throw new TVaultValidationException(ROLESTR);
		}
		else if (StringUtils.isEmpty(map.get("auth_type")) || !"ec2".equalsIgnoreCase(map.get("auth_type"))) {
			throw new TVaultValidationException("auth_type is required and it should be ec2.");
		}
		else if (!StringUtils.isEmpty(map.get("bound_account_id")) 
				|| !StringUtils.isEmpty(map.get("bound_ami_id")) 
				|| !StringUtils.isEmpty(map.get("bound_iam_instance_profile_arn")) 
				|| !StringUtils.isEmpty(map.get("bound_iam_role_arn")) 
				|| !StringUtils.isEmpty(map.get("bound_region")) 
				|| !StringUtils.isEmpty(map.get("bound_subnet_id")) 
				|| !StringUtils.isEmpty(map.get("bound_vpc_id")) 
			) {
			return true;
		}
		throw new TVaultValidationException("At least one bound parameter should be specified.");
	}
	/**
	 * Validate IAM role inputs
	 * @param awsiamRole
	 * @return
	 */
	public static boolean areAWSIAMRoleInputsValid(AWSIAMRole awsiamRole) throws TVaultValidationException{
		if (awsiamRole == null) {
			return false;
		}
		if (StringUtils.isEmpty(awsiamRole.getRole())) {
			throw new TVaultValidationException(ROLESTR);
		}
		else if (StringUtils.isEmpty(awsiamRole.getAuth_type()) || !awsiamRole.getAuth_type().equalsIgnoreCase("iam")) {
			throw new TVaultValidationException("auth_type is required and it should be iam.");
		}
		else if (ArrayUtils.isNotEmpty(awsiamRole.getBound_iam_principal_arn())
			) {
			boolean containsEmptyString = Stream.of(awsiamRole.getBound_iam_principal_arn())
		            .anyMatch(string -> string == null || string.isEmpty());
			if(containsEmptyString) {
				throw new TVaultValidationException("Invalid value specified for bound_iam_principal_arn.");
			}
			else {
				return true;
			}
		}
		throw new TVaultValidationException("Bound parameter should be specified.");
	}
	/**
	 * Get the map of all existing safe names.
	 * @return
	 */
	public static HashMap<String, List<String>> getAllExistingSafeNames(String token) {
		HashMap<String, List<String>> allExistingSafeNames = new HashMap<String, List<String>>();
		for (String mountPath : mountPaths) {
			List<String> safeNames = getAllExistingSafeNames(mountPath, token);
			allExistingSafeNames.put(mountPath, safeNames);
		}
		return allExistingSafeNames;
	}
	
	/**
	 * Get the map of all existing safe names for a given type.
	 * @return
	 */
	public static List<String> getAllExistingSafeNames(String type, String token) {
		List<String> safeNames = new ArrayList<String>();
		String path = METADATASTR + type;
		Response response = reqProcessor.process(SAFELIST,PATHSTR+path+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.OK)){
			try {
				Map<String, Object> requestParams = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
				safeNames = (ArrayList<String>) requestParams.get("keys");
			} catch (Exception e) {
				log.error("Unable to get list of safes.");
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "getAllExistingSafeNames").
						put(LogMessage.MESSAGE, String.format ("Unable to get list of safes due to [%s] ",e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
		}
		return safeNames;
	}
	/**
	 * Get the count of redundant safe names...
	 * @param safeName
	 * @return
	 */
	public static int getCountOfSafesForGivenSafeName(String safeName, String token) {
		HashMap<String, List<String>> allExistingSafeNames = getAllExistingSafeNames(token);
		int count = 0;
		for (Map.Entry<String, List<String>> entry : allExistingSafeNames.entrySet()) {
			List<String> existingSafeNames = entry.getValue();
			for (String existingSafeName: existingSafeNames) {
				// Note: SafeName is duplicate if it is found in any type (Shared/User/Apps). Hence no need to compare by prefixing with SafeType
				if (safeName.equalsIgnoreCase(existingSafeName)) {
					count++;
				}
			}
		}
		return count;
	}
	/**
	 * Get the count of redundant safe names...
	 * @param safeName
	 * @param safeType
	 * @param token
	 * @return
	 */
	public static int getCountOfSafesForGivenSafeName(String safeName, String safeType, String token) {
		List<String> existingSafeNames = getAllExistingSafeNames(safeType, token);
		int count = 0;
		for (String existingSafeName: existingSafeNames) {
			if (safeName.equalsIgnoreCase(existingSafeName)) {
				count++;
			}
		}
		return count;
	}
	
	public  static String generateSafePath(String safeName, String safeType) {
		String safePath = "";
		if (StringUtils.isEmpty(safeName) || StringUtils.isEmpty(safeType)) {
			return safePath;
		}
		switch (safeType) {
		case TVaultConstants.USERS: case "User Safe":
			safePath = "users/"+safeName;
			break;
		case TVaultConstants.SHARED: case "Shared Safe":
			safePath = "shared/"+safeName;
			break;
		case TVaultConstants.APPS	: case "Application Safe":
			safePath = "apps/"+safeName;
			break;
		default:
			
		}

		return safePath;
	}

	/**
	 * Populate aws metadata json
	 * @param appRoleName
	 * @param username
	 * @return
	 */
	public static String populateAWSMetaJson(String appRoleName, String username) {
		String _path = TVaultConstants.AWSROLE_METADATA_MOUNT_PATH + "/" + appRoleName;
		AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails(appRoleName);
		appRoleMetadataDetails.setCreatedBy(username);
		AppRoleMetadata appRoleMetadata =  new AppRoleMetadata(_path, appRoleMetadataDetails);
		String jsonStr = JSONUtil.getJSON(appRoleMetadata);
		Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		rqstParams.put("path",_path);
		return ControllerUtil.convetToJson(rqstParams);
	}
	/**
     * Populate aws metadata json with the user information
     * @param RoleName
     * @param username
     * @param auth_type
     * @return
     */
    public static  String populateUserMetaJson(String RoleName, String username,String type) {
        String _path = TVaultConstants.AWS_USERS_METADATA_MOUNT_PATH + "/" + username +"/" + RoleName;
        AWSMetadataDetails awsRoleMetadataDetails = new AWSMetadataDetails();
        awsRoleMetadataDetails.setCreatedBy(username);
        awsRoleMetadataDetails.setName(RoleName);
        awsRoleMetadataDetails.setType(type);
		AWSRoleMetadata awsRoleMetadata =  new AWSRoleMetadata(_path, awsRoleMetadataDetails);
		String jsonStr = JSONUtil.getJSON(awsRoleMetadata);
		Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		rqstParams.put("path",_path);
		return ControllerUtil.convetToJson(rqstParams);
    }
	/**
	 * Create metadata
	 * @param metadataJson
	 * @param token
	 * @return
	 */
	public static boolean createMetadata(String metadataJson, String token) {
		Response response = reqProcessor.process(WRITESTR,metadataJson,token);
		boolean isMetaDataUpdated = false;

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			isMetaDataUpdated = true;
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATEMETADATASTR).
					put(LogMessage.MESSAGE, "Metadata created successfully ").
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATEMETADATASTR).
					put(LogMessage.MESSAGE, "Failed to create metadata").
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return isMetaDataUpdated;
	}

	/**
	 * createSSLCertificateMetadata
	 * @param sslCertificateRequest
	 * @param userDetails
	 * @param token
	 * @return boolean
	 */



	/**
	 * Check whether the current user can delete a role
	 * @param approle
	 * @param token
	 * @param userDetails
	 * @return
	 */
	public static Response canDeleteRole(String roleName, String token, UserDetails userDetails, String metadataPath) {
		Response response = new Response();
		String _path = metadataPath + "/" + roleName;
		Response readResponse = reqProcessor.process(READSTR,PATHSTR+_path+"\"}",token);
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(readResponse.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(readResponse.getResponse());
			if(responseMap.isEmpty()) {
				response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				response.setResponse("Error reading role info");
				response.setSuccess(false);
				return response;
			}
			// Safeadmin can always delete any role
			if (userDetails.isAdmin()) {
				response.setHttpstatus(HttpStatus.OK);
				response.setResponse(TVaultConstants.EMPTY);
				response.setSuccess(true);
				return response;
			}
			// normal users
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			if (userDetails.getUsername().equalsIgnoreCase((String)metadataMap.get("createdBy"))) {
				response.setHttpstatus(HttpStatus.OK);
				response.setResponse(TVaultConstants.EMPTY);
				response.setSuccess(true);
				return response;
			}
		} else if (HttpStatus.NOT_FOUND.equals(readResponse.getHttpstatus()) && userDetails.isAdmin()) {
			response.setHttpstatus(HttpStatus.OK);
			response.setResponse(TVaultConstants.EMPTY);
			response.setSuccess(true);
			return response;
		}
		response.setHttpstatus(HttpStatus.UNAUTHORIZED);
		response.setResponse("Either role doesn't exist or you don't have enough permission to remove this role from Safe");
		response.setSuccess(false);
		return response;
	}

    /**
     * Populate approle metadata json
     * @param appRoleName
     * @param username
     * @return
     */
    public static  String populateAppRoleMetaJson(String appRoleName, String username) {
        String _path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + appRoleName;
        AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails(appRoleName);
        appRoleMetadataDetails.setCreatedBy(username);
        AppRoleMetadata appRoleMetadata =  new AppRoleMetadata(_path, appRoleMetadataDetails);
        String jsonStr = JSONUtil.getJSON(appRoleMetadata);
        Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
        rqstParams.put("path",_path);
        return ControllerUtil.convetToJson(rqstParams);
    }
    /**
     * Populate approle metadata json with the user information
     * @param appRoleName
     * @param username
     * @return
     */
    public static  String populateUserMetaJson(String appRoleName, String username) {
        String _path = TVaultConstants.APPROLE_USERS_METADATA_MOUNT_PATH + "/" + username +"/" + appRoleName;
        AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails(appRoleName);
        appRoleMetadataDetails.setCreatedBy(username);
        AppRoleMetadata appRoleMetadata =  new AppRoleMetadata(_path, appRoleMetadataDetails);
        String jsonStr = JSONUtil.getJSON(appRoleMetadata);
        Map<String,Object> rqstParams = ControllerUtil.parseJson(jsonStr);
        rqstParams.put("path",_path);
        return ControllerUtil.convetToJson(rqstParams);
    }
	/**
	 * Reads the SSCred from the location
	 * @param fileLocation
	 * @param isDelete
	 * @return sscred 
	 */
	public static SSCred readSSCredFile(String fileLocation, boolean isDelete)  {
		File ssFile = null;
		log.debug("Trying to read sscred file");
		try {
			ssFile = new File(fileLocation+"/sscred");
			if (ssFile.exists()) {
				sscred = new SSCred();
				setSscred(sscred);
				Scanner sc = new Scanner(ssFile);
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					if (line.startsWith(USERSTR)) {
						ssUsername = line.substring(USERSTR.length(), line.length());
						sscred.setUsername(line.substring(USERSTR.length(), line.length()));
						log.debug("Successfully read username: from sscred file");
					}
					else if (line.startsWith(PASSWORDSTR)) {
						ssPassword = line.substring(PASSWORDSTR.length(), line.length());
						sscred.setPassword(line.substring(PASSWORDSTR.length(), line.length()));
						log.debug("Successfully read password: from sscred file");
					}
					else if (line.startsWith(NCLMUSERNAMESTR)) {
						nclmUsername = line.substring(NCLMUSERNAMESTR.length(), line.length());
						sscred.setNclmusername(line.substring(NCLMUSERNAMESTR.length(), line.length()));
						log.debug("Successfully read nclm username: from sscred file");
					}
					else if (line.startsWith(NCLMPASSWORDSTR)) {
						nclmPassword = line.substring(NCLMPASSWORDSTR.length(), line.length());
						sscred.setNclmpassword(line.substring(NCLMPASSWORDSTR.length(), line.length()));
						log.debug("Successfully read nclmpassword: from sscred file");
					}
					else if (line.startsWith(CWMTOKENSTR)) {
						cwmToken = line.substring(CWMTOKENSTR.length(), line.length());
						sscred.setCwmToken(line.substring(CWMTOKENSTR.length(), line.length()));
						log.debug("Successfully read cwmToken: from sscred file");
					}

				}
				sc.close();
			}
		} catch (IOException e) {
			log.error(String.format("Unable to read sscred file: [%s]", e.getMessage()));
		}
		deletesscredFile(ssFile,isDelete);
		
		return sscred;
	}
	
	private static void deletesscredFile(File ssFile, boolean isDelete) {
		try {
			if (ssFile != null && ssFile.exists() && isDelete) {
				if (ssFile.delete()) {
					log.debug("Successfully deleted sscred file");
				}
				else {
					log.error("Unable to get delete sscred file");
				}
			}
		} catch (Exception e) {
			log.error(String.format("Unable to get delete sscred file: [%s]", e.getMessage()));
		}
	}

	public static String getNclmUsername() {
		return nclmUsername;
	}


	public static String getNclmPassword() {
		return nclmPassword;
	}

	public static String getCwmToken() {
		return cwmToken;
	}

	/**
	 * @return the ssUsername
	 */
		public static String getSsUsername() {
		return ssUsername;
	}


	public static void setSscred(SSCred sscred) {
		ControllerUtil.sscred = sscred;
	}

	/**
	 * @return the ssPassword
	 */
	public static String getSsPassword() {
		return ssPassword;
	}

	/**
	 * @return the sscred
	 */
	public static SSCred getSscred() {
		return sscred;
	}


	/**
	 * Reads the OIDCCred from the location.
	 * @param fileLocation
	 * @param isDelete
	 * @return
	 */
	public static OIDCCred readOIDCCredFile(String fileLocation, boolean isDelete) {
		File oidcFile = null;
		log.debug("Trying to read oidccred file");
		try {
			oidcFile = new File(fileLocation+"/oidccred");
			if (oidcFile != null && oidcFile.exists()) {
				oidcCred = new OIDCCred();
				Scanner sc = new Scanner(oidcFile);
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					if (line.startsWith(OIDCCLIENTNAMESTR)) {
						oidcClientName = line.substring(OIDCCLIENTNAMESTR.length(), line.length());
						oidcCred.setClientName(line.substring(OIDCCLIENTNAMESTR.length(), line.length()));
						log.debug("Successfully read OIDC_CLIENT_NAME from oidcCred file");
					}
					else if (line.startsWith(OIDCCLIENTIDSTR)) {
						oidcClientId = line.substring(OIDCCLIENTIDSTR.length(), line.length());
						oidcCred.setClientId(line.substring(OIDCCLIENTIDSTR.length(), line.length()));
						log.debug("Successfully read OIDC_CLIENT_ID from oidcCred file");
					}
					else if (line.startsWith(OIDCCLIENTSECRETSTR)) {
						oidcClientSecret = line.substring(OIDCCLIENTSECRETSTR.length(), line.length());
						oidcCred.setClientSecret(line.substring(OIDCCLIENTSECRETSTR.length(), line.length()));
						log.debug("Successfully read OIDC_CLIENT_SECRET from oidcCred file");
					}
					else if (line.startsWith(BOUNDAUDIENCE)) {
						oidcBoundAudiences = line.substring(BOUNDAUDIENCE.length(), line.length());
						oidcCred.setBoundAudiences(line.substring(BOUNDAUDIENCE.length(), line.length()));
						log.debug("Successfully read BOUND_AUDIENCES from oidcCred file");
					}
					else if (line.startsWith(OIDCURL)) {
						oidcDiscoveryUrl = line.substring(OIDCURL.length(), line.length());
						oidcCred.setDiscoveryUrl(line.substring(OIDCURL.length(), line.length()));
						log.debug("Successfully read OIDC_DISCOVERY_URL from oidcCred file");
					}
					else if (line.startsWith(ADLOGINURL)) {
						oidcADLoginUrl = line.substring(ADLOGINURL.length(), line.length());
						oidcCred.setAdLoginUrl(line.substring(ADLOGINURL.length(), line.length()));
						log.debug("Successfully read AD_LOGIN_URL from oidcCred file");
					}
				}
				sc.close();
			}
		} catch (IOException e) {
			log.error(String.format("Unable to read oidcCred file: [%s]", e.getMessage()));
		}
		try {
			if (oidcFile != null && oidcFile.exists() && isDelete) {
				if (oidcFile.delete()) {
					log.debug("Successfully deleted oidcCred file");
				}
				else {
					log.error("Unable to get delete oidcCred file");
				}
			}
		} catch (Exception e) {
			log.error(String.format("Unable to get delete oidcCred file: [%s]", e.getMessage()));
		}
		return oidcCred;
	}

	public static String getIamUsername() {
		return iamUsername;
	}

	public static void setIamUsername(String iamUsername) {
		ControllerUtil.iamUsername = iamUsername;
	}

	public static String getIamPassword() {
		return iamPassword;
	}

	public static void setIamPassword(String iamPassword) {
		ControllerUtil.iamPassword = iamPassword;
	}

	public static IAMPortalCred getIamPortalCred() {
		return iamPortalCred;
	}

	public static void setIamPortalCred(IAMPortalCred iamPortalCred) {
		ControllerUtil.iamPortalCred = iamPortalCred;
	}

	/**
	 * To read IAM portal cred file
	 * @param fileLocation
	 * @param isDelete
	 * @return
	 */
	public static IAMPortalCred readIAMPortalCredFile(String fileLocation, boolean isDelete) {
		File iamCredFile = null;
		log.debug("Trying to read IAM cred file");
		try {
			iamCredFile = new File(fileLocation+"/iamportalcred");
			if ( iamCredFile.exists()) {
				iamPortalCred = new IAMPortalCred();
				Scanner sc = new Scanner(iamCredFile);
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					if (line.startsWith(USERSTR)) {
						iamUsername = line.substring(USERSTR.length(), line.length());
						iamPortalCred.setRoleId(line.substring(USERSTR.length(), line.length()));
						log.debug("Successfully read username: from iamportalcred file");
					}
					else if (line.startsWith(PASSWORDSTR)) {
						iamPassword = line.substring(PASSWORDSTR.length(), line.length());
						iamPortalCred.setSecretId(line.substring(PASSWORDSTR.length(), line.length()));
						log.debug("Successfully read password: from iamportalcred file");
					}
				}
				sc.close();
			}
		} catch (IOException e) {
			log.error(String.format("Unable to read IAM cred file: [%s]", e.getMessage()));
		}
		try {
			if (iamCredFile.exists() && isDelete) {
				if (iamCredFile.delete()) {
					log.debug("Successfully deleted IAM cred file");
				}
				else {
					log.error("Unable to get delete IAM cred file");
				}
			}
		} catch (Exception e) {
			log.error(String.format("Unable to get delete IAM cred file: [%s]", e.getMessage()));
		}
		return iamPortalCred;
	}

	/**
	 *
	 * @return the oidcClientName.
	 */
	public static String getOidcClientName() {
		return oidcClientName;
	}

	/**
	 *
	 * @return the oidcClientId.
	 */
	public static String getOidcClientId() {
		return oidcClientId;
	}

	/**
	 *
	 * @return the oidcClientSecret.
	 */
	public static String getOidcClientSecret() {
		return oidcClientSecret;
	}

	/**
	 *
	 * @return the oidcBoundAudiences.
	 */
	public static String getOidcBoundAudiences() {
		return oidcBoundAudiences;
	}

	/**
	 *
	 * @return the oidcDiscoveryUrl.
	 */
	public static String getOidcDiscoveryUrl() {
		return oidcDiscoveryUrl;
	}

	/**
	 *
	 * @return the oidcADLoginUrl.
	 */
	public static String getOidcADLoginUrl() {
		return oidcADLoginUrl;
	}

	/**
	 *
	 * @return the oidcCred.
	 */
	public static OIDCCred getOidcCred() {
		return oidcCred;
	}

    /**
     * To hide the master approle from responses to UI
     * @param response
     * @return
     */
    public static Response hideMasterAppRoleFromResponse(Response response) {
        ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = response.getResponse();
        Map<String,String[]> requestMap = null;
        try {
            requestMap = objMapper.readValue(jsonStr, new TypeReference<Map<String,String[]>>() {});
        } catch (IOException e) {
            log.error(e);
        }
        if (null != requestMap.get("keys")) {
			List<String> policyList = new ArrayList<>(Arrays.asList((String[]) requestMap.get("keys")));
			policyList.removeAll(Arrays.asList(TVaultConstants.MASTER_APPROLES));
			String policies = policyList.stream().collect(Collectors.joining("\", \""));
			if (StringUtils.isEmpty(policies)) {
				response.setResponse("{\"keys\": []}");
			}
			else {
				response.setResponse("{\"keys\": [\"" + policies + "\"]}");
			}
		}
        return response;
    }

	public static boolean validateInputs(String certificateName,String sslCertType) {
		boolean isValid = true;
		if (certificateName.contains(" ")
				|| (!certificateName.endsWith(EMAILSTR))
				|| (certificateName.contains(".-"))
				|| (certificateName.contains("-."))
				|| (!sslCertType.matches(INTERNALEXTERNAL))
		) {
			return false;
		}

		return isValid;
	}

    /**
     * Update MetaData
     * @param params
     * @param token
     * @return
     * @throws JsonProcessingException
     */
    public static boolean updateMetaDataOnPath(String path, Map<String,String> params,String token) throws JsonProcessingException {

    	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, UPDATEMETADATASTR).
				put(LogMessage.MESSAGE, "Trying to upate metadata with params").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));

    	ObjectMapper objMapper = new ObjectMapper();
    	String metadataJson = objMapper.writeValueAsString(params);
    	String writeJson =  PATHSTR+path+DATASTR+ metadataJson +"}";
		Response response = reqProcessor.process(WRITESTR, writeJson, token);
		boolean isMetaDataUpdated = false;

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			isMetaDataUpdated = true;
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATEMETADATASTR).
					put(LogMessage.MESSAGE, "Metadata created successfully ").
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, CREATEMETADATASTR).
					put(LogMessage.MESSAGE, "Failed to create metadata").
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return isMetaDataUpdated;
    }

    public static boolean arecertificateGroupInputsValid(CertificateGroup certificateGroup) {

		if (ObjectUtils.isEmpty(certificateGroup)) {
			return false;
		}
		if (ObjectUtils.isEmpty(certificateGroup.getGroupname())
				|| ObjectUtils.isEmpty(certificateGroup.getAccess())
				|| ObjectUtils.isEmpty(certificateGroup.getCertificateName())
				|| certificateGroup.getCertificateName().contains(" ")
                || (!certificateGroup.getCertificateName().endsWith(EMAILSTR))
                || (certificateGroup.getCertificateName().contains(".-"))
	            || (certificateGroup.getCertificateName().contains("-."))
                || (!certificateGroup.getCertType().matches(INTERNALEXTERNAL))
				) {
			return false;
		}
			boolean isValid = true;
			String access = certificateGroup.getAccess();
			if (!ArrayUtils.contains(permissions, access)) {
				isValid = false;
			}
			return isValid;
		}
    
    public static boolean arecertificateDownloadInputsValid(CertificateDownloadRequest certificateDownloadRequest) {
    	boolean isValid = true;
		if (ObjectUtils.isEmpty(certificateDownloadRequest)) {
			return false;
		}
		if (ObjectUtils.isEmpty(certificateDownloadRequest.getCertificateName())
				|| ObjectUtils.isEmpty(certificateDownloadRequest.getCertType())
				|| ObjectUtils.isEmpty(certificateDownloadRequest.getCertificateCred())
				|| certificateDownloadRequest.getCertificateName().contains(" ")
                || (!certificateDownloadRequest.getCertificateName().endsWith(EMAILSTR))
                || (certificateDownloadRequest.getCertificateName().contains(".-"))
	            || (certificateDownloadRequest.getCertificateName().contains("-."))
                || (!certificateDownloadRequest.getCertType().matches(INTERNALEXTERNAL))
				) {
			return false;
		}
			
			return isValid;
		}
    public static boolean areDownloadInputsValid(String certificateName,String sslCertType) {
    	boolean isValid = true;
		if (certificateName.contains(" ")
                || (!certificateName.endsWith(EMAILSTR))
                || (certificateName.contains(".-"))
	            || (certificateName.contains("-."))
                || (!sslCertType.matches(INTERNALEXTERNAL))
				) {
			return false;
		}
			
			return isValid;
		}

	/**
	 * Decides whether a user can be added to a certificate or not
	 * @param path
	 * @param token
	 * @return
	 */
	//metadata/(certtype)sslcerts/(certname)cert1//
	public static boolean canAddCertPermission(String path,String certificateName,String token) {
		String certName =certificateName;

		List<String> existingCertNames = getAllExistingCertNames(path, token);
		List<String> duplicateCertNames = new ArrayList<>();
		int count=0;
		for (String existingCertName: existingCertNames) {

			if (existingCertName.equalsIgnoreCase(certName)) {
				count++;
				duplicateCertNames.add(existingCertName);
			}
		}

		if (count !=0 ) {
			// There is one valid certificate, Hence permission can be added
			// Exact match
			return true;
		}
		else {
			// There are no certificates or more than one and hence permission can't be added
			return false;
		}
	}

	/**
	 * Get the map of all existing certificate names for a given type.
	 * @return
	 */
	public static List<String> getAllExistingCertNames(String type, String token) {
		List<String> certNames = new ArrayList<String>();
		String path =  type;
		Response response = reqProcessor.process("/certificates/list",PATHSTR+path+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.OK)){
			try {
				Map<String, Object> requestParams = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
				certNames = (ArrayList<String>) requestParams.get("keys");
			} catch (Exception e) {
				log.error("Unable to get list of safes.");
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "getAllExistingCertificateNames").
						put(LogMessage.MESSAGE, String.format ("Unable to get list of certificate due to [%s] ",e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
		}
		return certNames;
	}

	public static Response updateSslCertificateMetadata(Map<String,String> params,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, UPDATEMETADATASTR).
				put(LogMessage.MESSAGE, "Trying to update metadata with params").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		String _type = params.get("type");
		String name = params.get("name");
		String access = params.get(ACCESSTR);
		String path = METADATASTR+ params.get("path");

		ObjectMapper objMapper = new ObjectMapper();
		String pathjson =PATHSTR+path+"\"}";
		// Read info for the path
		Response metadataResponse = reqProcessor.process(READSTR,pathjson,token);
		Map<String,Object> _metadataMap = null;
		if(HttpStatus.OK.equals(metadataResponse.getHttpstatus())){
			try {
				_metadataMap = objMapper.readValue(metadataResponse.getResponse(), new TypeReference<Map<String,Object>>() {});
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATEMETADATASTR).
						put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for type [%s], name [%s], access [%s] and path [%s] message [%s]", _type, name, access, path, e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}

			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>) _metadataMap.get("data");

			@SuppressWarnings("unchecked")
			Map<String,String> dataMap = (Map<String,String>) metadataMap.get(_type);
			if(dataMap == null) { dataMap = new HashMap<String,String>(); metadataMap.put(_type, dataMap);}

			dataMap.remove(name);
			if(!DELETESTR.equals(access))
				dataMap.put(name, access);

			String metadataJson = "";
			try {
				metadataJson = objMapper.writeValueAsString(metadataMap);
			} catch (JsonProcessingException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, UPDATEMETADATASTR).
						put(LogMessage.MESSAGE, String.format ("Error in creating metadataJson for type [%s], name [%s], access [%s] and path [%s] with message [%s]", _type, name, access, path, e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}

			String writeJson =  PATHSTR+path+DATASTR+ metadataJson +"}";
			metadataResponse = reqProcessor.process(WRITESTR,writeJson,token);
			return metadataResponse;
		}
		return null;
	}

	public static Response updateSslMetaDataOnConfigChanges(String name, String type,String currentPolicies, String latestPolicies, String token){

		List<String> _currentPolicies = Arrays.asList(currentPolicies.split(","));
		List<String> _latestpolicies = Arrays.asList(latestPolicies.split(","));
		List<String> _new = new ArrayList<String>();
		List<String> _del = new ArrayList<String>();
		for(String currPolicy : _currentPolicies){
			if(!_latestpolicies.contains(currPolicy)){
				_del.add(currPolicy);
			}
		}

		for(String latest : _latestpolicies){
			if(!_currentPolicies.contains(latest)){
				_new.add(latest);
			}
		}

		Map<String,String> sslAccessMap = new HashMap<String,String>();

		for(String policy : _new){
			String policyInfo[] = policy.split("_");
			if(policyInfo.length==3){
				String access ="" ;
				switch(policyInfo[0]) {
					case "r" : 	access = TVaultConstants.READ_POLICY; break;
					case "w" : 	access = TVaultConstants.WRITE_POLICY; break;
					default:	access= TVaultConstants.DENY_POLICY ;break;
				}
				String path = policyInfo[1]+"/"+policyInfo[2];
				sslAccessMap.put(path, access);
			}
		}
		for(String policy : _del){
			String policyInfo[] = policy.split("_");
			if(policyInfo.length==3){
				String path = policyInfo[1]+"/"+policyInfo[2];
				if(!sslAccessMap.containsKey(path)){
					sslAccessMap.put(path, DELETESTR);
				}
			}
		}

		Iterator<Entry<String,String>> itr = sslAccessMap.entrySet().iterator();
		List<String> failed = new ArrayList<String>();
		while(itr.hasNext()){
			Entry<String,String> entry = itr.next();
			Map<String,String> params = new HashMap<String,String>();
			params.put("type", type);
			params.put("name", name);
			params.put("path", entry.getKey());
			params.put(ACCESSTR, entry.getValue());
			Response rsp = updateSslCertificateMetadata(params, token);
			if(rsp == null || !HttpStatus.NO_CONTENT.equals(rsp.getHttpstatus())){
				failed.add(entry.getKey());
			}
		}
		Response response = new Response();
		if(failed.size()==0){
			response.setHttpstatus(HttpStatus.OK);
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "updateMetaDataOnConfigChanges").
					put(LogMessage.MESSAGE, "updateMetaDataOnConfigChanges failed ").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			response.setHttpstatus(HttpStatus.MULTI_STATUS);
			response.setResponse("Meta data update failed for "+failed.toString() );
		}
		return response;
	}
	/**
	 * To filter the duplicate safe permissions
	 * @param access
	 * @return
	 */
	public static Map<String,Object> filterDuplicateSafePermissions(Map<String,Object> access) {
		if (!MapUtils.isEmpty(access)) {
			String[] safeTypes = {TVaultConstants.USERS, TVaultConstants.SHARED, TVaultConstants.APPS};

			for (String type: safeTypes) {
				List<Map<String,String>> safePermissions = (List<Map<String,String>>)access.get(type);
				if (safePermissions != null) {
					//map to check duplicate permission
					Map<String,String> filteredPermissions = Collections.synchronizedMap(new HashMap());
					List<Map<String,String>> updatedPermissionList = new ArrayList<>();
					for (Map<String,String> permissionMap: safePermissions) {
						Set<String> keys = permissionMap.keySet();
						String key = keys.stream().findFirst().orElse("");

						if (!key.equals("") && !filteredPermissions.containsKey(key)) {
							filteredPermissions.put(key, permissionMap.get(key));
							Map<String,String> permission = Collections.synchronizedMap(new HashMap());
							permission.put(key, permissionMap.get(key));
							updatedPermissionList.add(permission);
						}
					}
					access.put(type, updatedPermissionList);
				}
			}
		}
		return access;
	}

	/**
	 * To filter the duplicate Service account permissions
	 * @param access
	 * @return
	 */
	public static  Map<String,Object> filterDuplicateSvcaccPermissions(Map<String,Object> access) {
		if (!MapUtils.isEmpty(access)) {
			List<Map<String,String>> svcaccPermissions = (List<Map<String,String>>)access.get(TVaultConstants.SVC_ACC_PATH_PREFIX);
			if (svcaccPermissions != null) {
				//map to check duplicate permission
				Map<String,String> filteredPermissions = Collections.synchronizedMap(new HashMap());
				List<Map<String,String>> updatedPermissionList = new ArrayList<>();
				for (Map<String,String> permissionMap: svcaccPermissions) {
					Set<String> keys = permissionMap.keySet();
					String key = keys.stream().findFirst().orElse("");

					if (!key.equals("") && !filteredPermissions.containsKey(key)) {
						filteredPermissions.put(key, permissionMap.get(key));
						Map<String,String> permission = Collections.synchronizedMap(new HashMap());
						permission.put(key, permissionMap.get(key));
						updatedPermissionList.add(permission);
					}
				}
				access.put(TVaultConstants.SVC_ACC_PATH_PREFIX, updatedPermissionList);
			}
		}
		return access;
	}
}
