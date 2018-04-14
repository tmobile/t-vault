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

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
@Component
public final class ControllerUtil {
	
	private static RequestProcessor reqProcessor;
	private static Logger log = LogManager.getLogger(ControllerUtil.class);

	@Value("${vault.auth.method}")
    private String tvaultAuthMethod;

	private static String vaultAuthMethod;
	
	@Value("${vault.secret.key.whitelistedchars:[^a-z0-9_]}")
    private String secretKeyWhitelistedCharacters;
	
	private static String secretKeyAllowedCharacters;

	@PostConstruct     
	private void initStatic () {
		vaultAuthMethod = this.tvaultAuthMethod;
		secretKeyAllowedCharacters = this.secretKeyWhitelistedCharacters;
	}

	@Autowired(required = true)
	public void setreqProcessor(RequestProcessor reqProcessor) {
		ControllerUtil.reqProcessor = reqProcessor;
	}
	
	public static void recursivedeletesdb(String jsonstr,String token,  Response responseVO){
		
		ObjectMapper objMapper =  new ObjectMapper();
		String path = "";
		try {
			path = objMapper.readTree(jsonstr).at("/path").asText();
		} catch (IOException e) {
			log.error(e);
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
				responseVO.setSuccess(false);
				responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				responseVO.setResponse("{\"errors\":[\"Unexpected error :"+e.getMessage() +"\"]}");
			}
			recursivedeletesdb ("{\"path\":\""+path+"\"}" ,token,responseVO);
		}
	}
	
	/** TODO
	 *  Need to test this..Currently put on hold since UI doesnt support nested folders
	 * @param jsonstr
	 * @param token
	 * @param responseVO
	 * @param secretMap
	 */
	public static void recursiveRead(String jsonstr,String token,  Response responseVO,Map<String,String> secretMap){
		
		ObjectMapper objMapper =  new ObjectMapper();
		String path = "";
		try {
			path = objMapper.readTree(jsonstr).at("/path").asText();
		} catch (IOException e) {
			log.error(e);
			responseVO.setSuccess(false);
			responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
			responseVO.setResponse("{\"errors\":[\"Unexpected error :"+e.getMessage() +"\"]}");
		}
		
		Response lisresp = reqProcessor.process("/sdb/list",jsonstr,token);
		if(HttpStatus.NOT_FOUND.equals(lisresp.getHttpstatus())){
			Response resp = reqProcessor.process("/read",jsonstr,token);
			responseVO.setResponse(resp.getResponse());
			responseVO.setHttpstatus(resp.getHttpstatus());
			secretMap.put(path, resp.getResponse());
		}else if ( HttpStatus.FORBIDDEN.equals(lisresp.getHttpstatus())){
			responseVO.setResponse(lisresp.getResponse());
			responseVO.setHttpstatus(lisresp.getHttpstatus());
			return;
		}else{
			try {
				 JsonNode folders = objMapper.readTree(lisresp.getResponse()).get("keys");
				 for(JsonNode node : folders){
					 recursiveRead ("{\"path\":\""+path+"/"+node.asText()+"\"}" ,token,responseVO,secretMap);
				 }
			} catch (IOException e) {
				log.error(e);
				responseVO.setSuccess(false);
				responseVO.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
				responseVO.setResponse("{\"errors\":[\"Unexpected error :"+e.getMessage() +"\"]}");
			}
			recursiveRead("{\"path\":\""+path+"\"}" ,token,responseVO,secretMap);
		}
	}

	
	public static Response configureLDAPUser(String userName,String policies,String groups,String token ){
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
		}
		return reqProcessor.process("/auth/aws/roles/update",awsConfigJson,token);
	}

	
	
	public static Response updateMetadata(Map<String,String> params,String token){
		
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
			response.setHttpstatus(HttpStatus.MULTI_STATUS);
			response.setResponse("Meta data update failed for "+failed.toString() );
		}
		return response;
	}
	
	public static Map<String,Object> parseJson (String jsonString){
		Map<String, Object> response = new HashMap<>(); 
		try {
			if(jsonString !=null )
				response = new ObjectMapper().readValue(jsonString, new TypeReference<Map<String, Object>>(){});
		} catch (Exception e) {
			log.error(e);
		}
		return response;
	}
	
	public static String convetToJson (Map<String,Object> jsonMap){
		String jsonStr = "{}";
		try {
			jsonStr = new ObjectMapper().writeValueAsString(jsonMap);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	
		return jsonStr;
	}
	
	public static void updateUserPolicyAssociationOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
		log.debug ("updateUserPolicyAssociationOnSDBDelete...for auth method " + vaultAuthMethod);
		if(acessInfo!=null){
			String folders[] = sdb.split("[/]+");
			String r_policy = "r_"+folders[0]+"_"+folders[1];
			String w_policy = "w_"+folders[0]+"_"+folders[1];
			String d_policy = "d_"+folders[0]+"_"+folders[1];
	
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
						currentpolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
						if (!("userpass".equals(vaultAuthMethod))) {
							groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
						}
					} catch (IOException e) {
						log.error(e);
					}
					policies = currentpolicies;
					policies = policies.replaceAll(r_policy, "");
					policies = policies.replaceAll(w_policy, "");
					policies = policies.replaceAll(d_policy, "");
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
		if ("userpass".equals(vaultAuthMethod)) {
			log.debug ("Inside userpass of updateGroupPolicyAssociationOnSDBDelete...Just Returning...");
			return;
		}
		if(acessInfo!=null){
			String folders[] = sdb.split("[/]+");
			String r_policy = "r_"+folders[0]+"_"+folders[1];
			String w_policy = "w_"+folders[0]+"_"+folders[1];
			String d_policy = "d_"+folders[0]+"_"+folders[1];
	
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
						currentpolicies =objMapper.readTree(responseJson).get("data").get("policies").asText();
					} catch (IOException e) {
						log.error(e);
					}
					policies = currentpolicies;
					policies = policies.replaceAll(r_policy, "");
					policies = policies.replaceAll(w_policy, "");
					policies = policies.replaceAll(d_policy, "");
					ControllerUtil.configureLDAPGroup(groupName,policies,token);
				}
				
			}
		}
	}
	
	// Not using this method and decided to delete the role instead with the concept that you cant have same role used by different safe.S
	public static void updateAwsRolePolicyAssociationOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
		if(acessInfo!=null){
			String folders[] = sdb.split("[/]+");
			String r_policy = "r_"+folders[0]+"_"+folders[1];
			String w_policy = "w_"+folders[0]+"_"+folders[1];
			String d_policy = "d_"+folders[0]+"_"+folders[1];
	
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
					}
					policies = currentpolicies;
					policies = policies.replaceAll(r_policy, "");
					policies = policies.replaceAll(w_policy, "");
					policies = policies.replaceAll(d_policy, "");
					ControllerUtil.configureAWSRole(role, policies, token);
				}
			}
		}
	}
	
	public static void deleteAwsRoleOnSDBDelete(String sdb,Map<String,String> acessInfo,String token){
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
				}else{
					log.debug(role +" , AWS Role deletion as part of sdb delete failed . SDB path "+ sdb );
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
	 * Validates inputs values required for SDB creation
	 * @param requestParams
	 * @return
	 */
	public static boolean areSDBInputsValid(Map<String, Object> requestParams) {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) requestParams.get("data");
		String sdbName = (String) map.get("name");
		String sdbOwner = (String) map.get("owner");
		String sdbDescription = (String) map.get("description");
		if (StringUtils.isEmpty(sdbName) 
				|| StringUtils.isEmpty(sdbOwner) 
				|| StringUtils.isEmpty(sdbDescription) 
				) {
			return false;
		}
		return true;
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
}
