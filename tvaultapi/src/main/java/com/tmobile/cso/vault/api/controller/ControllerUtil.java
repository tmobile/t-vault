// =========================================================================
// Copyright 2018 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License");
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.AWSAuthLogin;
import com.tmobile.cso.vault.api.model.AWSAuthType;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.AWSRole;
import com.tmobile.cso.vault.api.model.AppRole;
import com.tmobile.cso.vault.api.model.AppRoleSecretData;
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeAppRoleAccess;
import com.tmobile.cso.vault.api.model.SafeBasicDetails;
import com.tmobile.cso.vault.api.model.SafeGroup;
import com.tmobile.cso.vault.api.model.SafeNode;
import com.tmobile.cso.vault.api.model.SafeUser;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
@Component
public final class ControllerUtil {
	
	public static RequestProcessor reqProcessor;
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
	
	@PostConstruct     
	private void initStatic () {
		vaultAuthMethod = this.tvaultAuthMethod;
		secretKeyAllowedCharacters = this.secretKeyWhitelistedCharacters;
		approleAllowedCharacters = this.approleWhitelistedCharacters;
		sdbNameAllowedCharacters = this.sdbNameWhitelistedCharacters;
	}

	@Autowired(required = true)
	public void setreqProcessor(RequestProcessor reqProcessor) {
		ControllerUtil.reqProcessor = reqProcessor;
	}
	
	public static void recursivedeletesdb(String jsonstr,String token,  Response responseVO){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "recursivedeletesdb").
				put(LogMessage.MESSAGE, String.format ("Trying recursive delete...")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		ObjectMapper objMapper =  new ObjectMapper();
		String path = "";
		try {
			path = objMapper.readTree(jsonstr).at("/path").asText();
		} catch (IOException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "recursivedeletesdb").
					put(LogMessage.MESSAGE, String.format ("recursivedeletesdb failed for [%s]", e.getMessage())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			responseVO.setSuccess(false);
			responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
			responseVO.setResponse("{\"errors\":[\"Unexpected error :"+e.getMessage() +"\"]}");
		}
		
		Response lisresp = reqProcessor.process("/sdb/list",jsonstr,token);
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
					recursivedeletesdb ("{\"path\":\""+path+"/"+node.asText()+"\"}" ,token,responseVO);
				 }
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "recursivedeletesdb").
						put(LogMessage.MESSAGE, String.format ("recursivedeletesdb failed for [%s]", e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				responseVO.setSuccess(false);
				responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				responseVO.setResponse("{\"errors\":[\"Unexpected error :"+e.getMessage() +"\"]}");
			}
			recursivedeletesdb ("{\"path\":\""+path+"\"}" ,token,responseVO);
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

		String path = "";
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
			responseVO.setResponse("{\"errors\":[\"Unexpected error :"+e.getMessage() +"\"]}");
		}
		return path;
	}
	

	/**
	 * Recursively reads the folders/secrets for a given path
	 * @param jsonstr
	 * @param token
	 * @param responseVO
	 * @param secretMap
	 */
	public static void recursiveRead(String jsonstr,String token,  Response responseVO, SafeNode safeNode){
		ObjectMapper objMapper =  new ObjectMapper();
		String path = getPath(objMapper, jsonstr, responseVO);
		/* Read the secrets for the given path */
		Response secresp = reqProcessor.process("/read",jsonstr,token);
		if (HttpStatus.OK.equals(secresp.getHttpstatus())) {
			responseVO.setResponse(secresp.getResponse());
			responseVO.setHttpstatus(secresp.getHttpstatus());
			SafeNode sn = new SafeNode();
			sn.setId(path);
			sn.setValue(secresp.getResponse());
			if (!"safe".equals(safeNode.getType())) {
				sn.setType("secret");
				sn.setParentId(safeNode.getId());
				safeNode.addChild(sn);
			}
			else {
				safeNode.setValue(secresp.getResponse());
			}
		}
		/* Read the folders for the given path */
		Response lisresp = reqProcessor.process("/sdb/list",jsonstr,token);
		if(HttpStatus.NOT_FOUND.equals(lisresp.getHttpstatus())){
			Response resp = reqProcessor.process("/read",jsonstr,token);
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
						jsonstr = "{\"path\":\""+path+"/"+node.asText()+"\"}";
						SafeNode sn = new SafeNode();
						sn.setId(path+"/"+node.asText());
						sn.setValue(path+"/"+node.asText());
						sn.setType("folder");
						sn.setParentId(safeNode.getId());
						safeNode.addChild(sn);
						/* Recursively read the folders for the given folder/sub folders */
						recursiveRead ( jsonstr,token,responseVO, sn);
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
					responseVO.setResponse("{\"errors\":[\"Unexpected error :"+e.getMessage() +"\"]}");
				}
			}
			else {
				log.error("Unable to recursively read the given path " + jsonstr);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "recursiveRead").
						put(LogMessage.MESSAGE, String.format ("Unable to recursively read the given path")).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				responseVO.setSuccess(false);
				responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				responseVO.setResponse("{\"errors\":[\"Unable to recursively read the given path :"+jsonstr +"\"]}");
			}
		}
	}

	/**
	 * Gets the folders and secrets for a given path
	 * @param jsonstr
	 * @param token
	 * @param responseVO
	 * @param secretMap
	 */
	public static void getFoldersAndSecrets(String jsonstr,String token,  Response responseVO, SafeNode safeNode){
		ObjectMapper objMapper =  new ObjectMapper();
		String path = getPath(objMapper, jsonstr, responseVO);
		/* Read the secrets for the given path */
		Response secresp = reqProcessor.process("/read",jsonstr,token);
		responseVO.setResponse(secresp.getResponse());
		responseVO.setHttpstatus(secresp.getHttpstatus());
		boolean secretsExist = false;
		if (HttpStatus.OK.equals(secresp.getHttpstatus())) {
			SafeNode sn = new SafeNode();
			sn.setId(path);
			sn.setValue(secresp.getResponse());
			if (!"safe".equals(safeNode.getType())) {
				secretsExist = true;
				sn.setType("secret");
				sn.setParentId(safeNode.getId());
				safeNode.addChild(sn);
			}
			else {
				safeNode.setValue(secresp.getResponse());
			}
		}

		/* Read the folders for the given path */
		Response lisresp = reqProcessor.process("/sdb/list",jsonstr,token);
		if(HttpStatus.NOT_FOUND.equals(lisresp.getHttpstatus())){
			if (!secretsExist) {
				// No secrets and no folders
				if ("safe".equals(safeNode.getType())) {
					responseVO.setResponse("{}");
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
						jsonstr = "{\"path\":\""+path+"/"+node.asText()+"\"}";
						SafeNode sn = new SafeNode();
						sn.setId(path+"/"+node.asText());
						sn.setValue(path+"/"+node.asText());
						sn.setType("folder");
						sn.setParentId(safeNode.getId());
						safeNode.addChild(sn);
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
					responseVO.setResponse("{\"errors\":[\"Unexpected error :"+e.getMessage() +"\"]}");
				}
			}
			else {
				log.error("Unable to read the given path " + jsonstr);
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
		configureUserMap.put("username", userName);
		configureUserMap.put("policies", policies);
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
	
	
	
	public static Response configureApprole(String rolename,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureUserMap = new HashMap<String,String>();
		configureUserMap.put("role_name", rolename);
		configureUserMap.put("policies", policies);
		String approleConfigJson ="";
		
		try {
			approleConfigJson = objMapper.writeValueAsString(configureUserMap);
		} catch (JsonProcessingException e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "configureApprole").
					put(LogMessage.MESSAGE, String.format ("Unable to create approleConfigJson  [%s] with rolename [%s] policies [%s] ", e.getMessage(), rolename, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return reqProcessor.process("/auth/approle/role/create",approleConfigJson,token);
	}
	


	public static Response configureUserpassUser(String userName,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureUserMap = new HashMap<String,String>();
		configureUserMap.put("username", userName);
		configureUserMap.put("policies", policies);
		String userpassUserConfigJson ="";
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
		configureGrouMap.put("policies", policies);
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
		configureRoleMap.put("policies", policies);
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
		configureRoleMap.put("policies", policies);
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
				put(LogMessage.ACTION, "updateMetadata").
				put(LogMessage.MESSAGE, String.format ("Trying to upate metadata with params")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		String _type = params.get("type");
		String name = params.get("name");
		String access = params.get("access");
		String path = params.get("path");
		path = "metadata/"+path;
		
		ObjectMapper objMapper = new ObjectMapper();
		String pathjson ="{\"path\":\""+path+"\"}";
		// Read info for the path
		Response metadataResponse = reqProcessor.process("/read",pathjson,token);
		Map<String,Object> _metadataMap = null;
		if(HttpStatus.OK.equals(metadataResponse.getHttpstatus())){
			try {
				_metadataMap = objMapper.readValue(metadataResponse.getResponse(), new TypeReference<Map<String,Object>>() {});
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "updateMetadata").
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
			if(!"delete".equals(access))
				dataMap.put(name, access);
			
			String metadataJson = "";
			try {
				metadataJson = objMapper.writeValueAsString(metadataMap);
			} catch (JsonProcessingException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "updateMetadata").
						put(LogMessage.MESSAGE, String.format ("Error in creating metadataJson for type [%s], name [%s], access [%s] and path [%s] with message [%s]", _type, name, access, path, e.getMessage())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			
			String writeJson =  "{\"path\":\""+path+"\",\"data\":"+ metadataJson +"}";
			metadataResponse = reqProcessor.process("/write",writeJson,token);
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
					case "r" : 	access = "read"; break;
					case "w" : 	access = "write"; break;
					default:	access= "deny" ;break;
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
					sdbAccessMap.put(path, "delete");
				}
			}
		}
		
		Iterator<Entry<String,String>> itr = sdbAccessMap.entrySet().iterator();
		List<String> failed = new ArrayList<String>();
		while(itr.hasNext()){
			Entry<String,String> entry = itr.next();
			Map<String,String> params = new HashMap<String,String>();
			params.put("type", type);
			params.put("name", name);
			params.put("path", entry.getKey());
			params.put("access", entry.getValue());
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
					put(LogMessage.MESSAGE, String.format ("updateMetaDataOnConfigChanges failed ")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			response.setHttpstatus(HttpStatus.MULTI_STATUS);
			response.setResponse("Meta data update failed for "+failed.toString() );
		}
		return response;
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
					put(LogMessage.MESSAGE, String.format ("parseJson failed ")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return response;
	}
	
	public static String convetToJson (Map<String,Object> jsonMap){
		String jsonStr = "{}";
		try {
			jsonStr = new ObjectMapper().writeValueAsString(jsonMap);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "convetToJson").
					put(LogMessage.MESSAGE, String.format ("convetToJson failed ")).
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
		String currentpolicies = "";
		JsonNode policiesNode = objMapper.readTree(policyJson).get("data").get("policies");
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
	
	public static void updateUserPolicyAssociationOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "updateUserPolicyAssociationOnSDBDelete").
				put(LogMessage.MESSAGE, String.format ("trying updateUserPolicyAssociationOnSDBDelete")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		log.debug ("updateUserPolicyAssociationOnSDBDelete...for auth method " + vaultAuthMethod);
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
				
				Response userResponse;
				if ("userpass".equals(vaultAuthMethod)) {
					log.debug ("Inside userpass");
					userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token);
				}
				else {
					log.debug ("Inside non - userpass");
					userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
				}	
				String responseJson="";	
				String policies ="";
				String groups="";
				String currentpolicies ="";
				if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
					responseJson = userResponse.getResponse();	
					try {
						currentpolicies = getPoliciesAsStringFromJson(objMapper, responseJson);
						if (!("userpass".equals(vaultAuthMethod))) {
							groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
						}
					} catch (IOException e) {
						log.error(e);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "updateUserPolicyAssociationOnSDBDelete").
								put(LogMessage.MESSAGE, String.format ("updateUserPolicyAssociationOnSDBDelete failed [%s]", e.getMessage())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
					}
					policies = currentpolicies;
					policies = policies.replaceAll(r_policy, "");
					policies = policies.replaceAll(w_policy, "");
					policies = policies.replaceAll(d_policy, "");
					policies = policies.replaceAll(s_policy, "");
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "updateUserPolicyAssociationOnSDBDelete").
							put(LogMessage.MESSAGE, String.format ("Current policies [%s]", policies )).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					if ("userpass".equals(vaultAuthMethod)) {
						log.debug ("Inside userpass");
						ControllerUtil.configureUserpassUser(userName,policies,token);
					}
					else {
						log.debug ("Inside non-userpass");
						ControllerUtil.configureLDAPUser(userName,policies,groups,token);
					}
				}
				
			}
		}
	}
	public static void updateGroupPolicyAssociationOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "updateGroupPolicyAssociationOnSDBDelete").
				put(LogMessage.MESSAGE, String.format ("trying updateGroupPolicyAssociationOnSDBDelete")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if ("userpass".equals(vaultAuthMethod)) {
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
				Response response = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
				String responseJson="";	
				String policies ="";
				String currentpolicies ="";
				if(HttpStatus.OK.equals(response.getHttpstatus())){
					responseJson = response.getResponse();	
					try {
						currentpolicies = getPoliciesAsStringFromJson(objMapper, responseJson);
					} catch (IOException e) {
						log.error(e);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "updateUserPolicyAssociationOnSDBDelete").
								put(LogMessage.MESSAGE, String.format ("updateUserPolicyAssociationOnSDBDelete failed [%s]", e.getMessage())).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
					}
					policies = currentpolicies;
					policies = policies.replaceAll(r_policy, "");
					policies = policies.replaceAll(w_policy, "");
					policies = policies.replaceAll(d_policy, "");
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "updateUserPolicyAssociationOnSDBDelete").
							put(LogMessage.MESSAGE, String.format ("Current policies [%s]", policies )).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					ControllerUtil.configureLDAPGroup(groupName,policies,token);
				}
			}
		}
	}
	
	// Not using this method and decided to delete the role instead with the concept that you cant have same role used by different safe.S
	public static void updateAwsRolePolicyAssociationOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "updateAwsRolePolicyAssociationOnSDBDelete").
				put(LogMessage.MESSAGE, String.format ("trying updateAwsRolePolicyAssociationOnSDBDelete")).
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

			Set<String> roles = acessInfo.keySet();
			ObjectMapper objMapper = new ObjectMapper();
			for(String role : roles){
				Response roleResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+role+"\"}",token);
				String responseJson="";
				String policies ="";
				String currentpolicies ="";
				
				if(HttpStatus.OK.equals(roleResponse.getHttpstatus())){
					responseJson = roleResponse.getResponse();	
					try {
						JsonNode policiesArry =objMapper.readTree(responseJson).get("policies");
						for(JsonNode policyNode : policiesArry){
							currentpolicies =	(currentpolicies == "" ) ? currentpolicies+policyNode.asText():currentpolicies+","+policyNode.asText();
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
	}
	
	public static void deleteAwsRoleOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "deleteAwsRoleOnSDBDelete").
				put(LogMessage.MESSAGE, String.format ("Trying to deleteAwsRoleOnSDBDelete")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if ("userpass".equals(vaultAuthMethod)) {
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
			if(!("apps".equals(safeType)||"shared".equals(safeType)||"users".equals(safeType))){
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
			if(!("apps".equals(safeType)||"shared".equals(safeType)||"users".equals(safeType))){
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
			if(!("apps".equals(safeType)||"shared".equals(safeType)||"users".equals(safeType))){
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
		String safeType = "unknown";
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
		String safeName = "";
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
		String _path = "metadata/"+safePath;
		Response response = reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token);
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
		String path = safe.getPath();
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
		String path = (String) requestParams.get("path");
		if (StringUtils.isEmpty(sdbName) 
				|| StringUtils.isEmpty(sdbOwner) 
				|| StringUtils.isEmpty(sdbDescription) 
				|| StringUtils.isEmpty(path) 
				) {
			return false;
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
				|| ObjectUtils.isEmpty(requestMap.get("access"))
				) {
			return false;
		}
		String path = requestMap.get("path");
		if (!isPathValid(path)) {
			return false;
		}
		String access = (String) requestMap.get("access");
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
				|| ObjectUtils.isEmpty(requestMap.get("access"))
				) {
			return false;
		}
		String path = (String) requestMap.get("path");
		if (!isPathValid(path)) {
			return false;
		}
		String access = (String) requestMap.get("access");
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
		if (ObjectUtils.isEmpty(requestMap.get("username"))
				|| ObjectUtils.isEmpty(requestMap.get("path"))
				|| ObjectUtils.isEmpty(requestMap.get("access"))
				) {
			return false;
		}
		String path = (String) requestMap.get("path");
		if (!isPathValid(path)) {
			return false;
		}
		String access = (String) requestMap.get("access");
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
				|| ObjectUtils.isEmpty(requestMap.get("access"))
				) {
			return false;
		}
		String path = (String) requestMap.get("path");
		if (!isPathValid(path)) {
			return false;
		}
		String access = (String) requestMap.get("access");
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
				      put(LogMessage.MESSAGE, String.format ("Failed to convert [%s] to lowercase.", jsonStr)).
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
				      put(LogMessage.MESSAGE, String.format ("Failed while converting safe details to lowercase.")).
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
				      put(LogMessage.MESSAGE, String.format ("Failed to convert [%s] to lowercase.", jsonstr)).
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
				      put(LogMessage.MESSAGE, String.format ("Failed to convert [%s] to lowercase.", jsonstr)).
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
				      put(LogMessage.MESSAGE, String.format ("Failed to convert [%s] to lowercase.", jsonstr)).
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
		String secretValue = null;
		try {
			Map<String, Object> requestParams = new ObjectMapper().readValue(jsonString, new TypeReference<Map<String, Object>>(){});
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) requestParams.get("data");
			for (Object key : map.keySet()) {
				secretKey = (String) key;
				secretValue = (String) map.get(key);
			    break;
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
			throw new TVaultValidationException("Role is required.");
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
			throw new TVaultValidationException("Role is required.");
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
			throw new TVaultValidationException("Role is required.");
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
		String path = "metadata/" + type;
		Response response = reqProcessor.process("/sdb/list","{\"path\":\""+path+"\"}",token);
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
		case "users": case "User Safe":
			safePath = "users/"+safeName;
			break;
		case "shared": case "Shared Safe":
			safePath = "shared/"+safeName;
			break;
		case "apps"	: case "Application Safe":
			safePath = "apps/"+safeName;
			break;
		default:
			
		}

		return safePath;
	}
}
