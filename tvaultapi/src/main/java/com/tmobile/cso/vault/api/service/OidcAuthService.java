package com.tmobile.cso.vault.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.GroupAliasRequest;
import com.tmobile.cso.vault.api.model.OIDCEntityRequest;
import com.tmobile.cso.vault.api.model.OIDCIdentityGroupRequest;
import com.tmobile.cso.vault.api.model.OIDCLookupEntityRequest;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component
public class OIDCAuthService {
	
	@Autowired
    private RequestProcessor reqProcessor;
	
	private static Logger log = LogManager.getLogger(OIDCAuthService.class);
	
	/**
	 * Get Authentication Mounts
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getAuthenticationMounts(String token) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
				.put(LogMessage.ACTION, "List Auth Methods").put(LogMessage.MESSAGE, "Trying to get all auth Methods")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).build()));
		Response response = reqProcessor.process("/sys/list", "{}", token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
    /**
     * Entity Lookup from identity engine
     * @param token
     * @param oidcLookupEntityRequest
     * @return
     */
	public ResponseEntity<String> entityLookUp(String token, OIDCLookupEntityRequest oidcLookupEntityRequest) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
				.put(LogMessage.ACTION, "Entity Lookup from identity engine").put(LogMessage.MESSAGE, "Trying to Lookup entity from identity engine")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).build()));
		
		String jsonStr = JSONUtil.getJSON(oidcLookupEntityRequest);
		Response response = reqProcessor.process("/identity/lookup/entity", jsonStr, token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * Group Entity Lookup from identity engine
	 * @param token
	 * @param oidcLookupEntityRequest
	 * @return
	 */
	public ResponseEntity<String> groupEntityLookUp(String token, OIDCLookupEntityRequest oidcLookupEntityRequest) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
				.put(LogMessage.ACTION, "Group Entity Lookup from identity engine").put(LogMessage.MESSAGE, "Trying to Lookup group entity from identity engine")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).build()));
		
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
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Read Entity Alias By ID")
								.put(LogMessage.MESSAGE, "Trying to read Entity Alias").put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
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
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Read Entity By Name")
								.put(LogMessage.MESSAGE, "Trying to read Entity").put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));
		Response response = reqProcessor.process("/identity/entity/name", "{\"name\":\"" + entityName + "\"}", token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	/**
	 * Update Entity By Name
	 * @param token
	 * @param oidcEntityRequest
	 * @param entityName
	 * @return
	 */
	public ResponseEntity<String> updateEntityByName(String token, OIDCEntityRequest oidcEntityRequest,
			String entityName) {
		log.debug(
				JSONUtil.getJSON(
						ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Update Entity By Name")
								.put(LogMessage.MESSAGE, "Trying to update entity by name").put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));

		String jsonStr = JSONUtil.getJSON(oidcEntityRequest);
		Response response = reqProcessor.process("/identity/entity/name/update", jsonStr, token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	/**
	 * Update Identity Group By Name
	 * @param token
	 * @param oidcIdentityGroupRequest
	 * @param entityName
	 * @return
	 */
	public ResponseEntity<String> updateIdentityGroupByName(String token,
			OIDCIdentityGroupRequest oidcIdentityGroupRequest, String entityName) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
				.put(LogMessage.ACTION, "Update Identity Group By Name")
				.put(LogMessage.MESSAGE, "Trying to update identity group entity by name")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).build()));

		String jsonStr = JSONUtil.getJSON(oidcIdentityGroupRequest);
		Response response = reqProcessor.process("/identity/group/name/update", jsonStr, token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	/**
	 * Read Group Alias By Id
	 * @param token
	 * @param id
	 * @return
	 */
	public ResponseEntity<String> readGroupAliasById(String token, String id) {
		log.debug(
				JSONUtil.getJSON(
						ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Read Group Alias By Id")
								.put(LogMessage.MESSAGE, "Trying to read Group Alias By Id").put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
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
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Read Group Alias By Id")
								.put(LogMessage.MESSAGE, "Trying to read Group Alias By Id").put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));
		Response response = reqProcessor.process("/identity/group/name", "{\"name\":\"" + name + "\"}", token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	
	/**
	 * Read Group Alias By Id
	 * @param token
	 * @param id
	 * @return
	 */
	public ResponseEntity<String> deleteGroupAliasByID(String token, String id) {
		log.debug(
				JSONUtil.getJSON(
						ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Read Group Alias By Id")
								.put(LogMessage.MESSAGE, "Trying to read Group Alias By Id").put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));
		Response response = reqProcessor.process("/identity/group-alias/id", "{\"id\":\"" + id + "\"}", token);
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
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Create Group Alias")
								.put(LogMessage.MESSAGE, "Trying to create Group Alias").put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));
		String jsonStr = JSONUtil.getJSON(groupAliasRequest);
		Response response = reqProcessor.process("/identity/group-alias", jsonStr, token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

}
