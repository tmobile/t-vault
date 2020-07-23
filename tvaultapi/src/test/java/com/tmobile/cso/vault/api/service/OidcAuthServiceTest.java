package com.tmobile.cso.vault.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.GroupAliasRequest;
import com.tmobile.cso.vault.api.model.OIDCEntityRequest;
import com.tmobile.cso.vault.api.model.OIDCIdentityGroupRequest;
import com.tmobile.cso.vault.api.model.OIDCLookupEntityRequest;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = { "com.tmobile.cso.vault.api" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ ControllerUtil.class, JSONUtil.class, PolicyUtils.class })
@PowerMockIgnore({ "javax.management.*" })
public class OIDCAuthServiceTest {

	@InjectMocks
	OIDCAuthService oidcAuthService;

	@Mock
	private RequestProcessor reqProcessor;

	@Before
	public void setUp()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
		PowerMockito.mockStatic(ControllerUtil.class);
		PowerMockito.mockStatic(JSONUtil.class);

		Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
		when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

		Map<String, String> currentMap = new HashMap<>();
		currentMap.put("apiurl", "http://localhost:8080/vault/v2/identity");
		currentMap.put("user", "");
		ThreadLocalContext.setCurrentMap(currentMap);
	}

	Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
		Response response = new Response();
		response.setHttpstatus(status);
		response.setSuccess(success);
		if (expectedBody != "") {
			response.setResponse(expectedBody);
		}
		return response;
	}

	@Test
	public void getAuthenticationMounts() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String data = "{\n    \"data\": {\n        \"canonical_id\": \"7862bbe1-16ce-442d-756b-585ed77b7385\",\n        \"creation_time\": \"2020-07-15T14:07:14.7237705Z\",\n        \"id\": \"dea21830-f565-77d6-3005-8aab0c2596bb\",\n        \"last_update_time\": \"2020-07-15T14:07:14.7237705Z\",\n        \"merged_from_canonical_ids\": null,\n        \"metadata\": null,\n        \"mount_accessor\": \"auth_oidc_8b51f292\",\n        \"mount_path\": \"auth/oidc/\",\n        \"mount_type\": \"oidc\",\n        \"name\": \"Nithin.Nazeer1@T-Mobile.com\",\n        \"namespace_id\": \"root\"\n    }\n}";
		Response response = getMockResponse(HttpStatus.OK, true, data);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(data);
		when(reqProcessor.process("/sys/list", "{}", token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.getAuthenticationMounts(token);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void entityLookUp() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
		oidcLookupEntityRequest.setId("1223");
		oidcLookupEntityRequest.setAlias_id("123");
		oidcLookupEntityRequest.setAlias_mount_accessor("mount");
		oidcLookupEntityRequest.setAlias_name("alias_name");
		oidcLookupEntityRequest.setName("name");
		String jsonStr = JSONUtil.getJSON(oidcLookupEntityRequest);
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/lookup/entity", jsonStr, token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.entityLookUp(token, oidcLookupEntityRequest);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void groupEntityLookUp() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
		oidcLookupEntityRequest.setId("1223");
		oidcLookupEntityRequest.setAlias_id("123");
		oidcLookupEntityRequest.setAlias_mount_accessor("mount");
		oidcLookupEntityRequest.setAlias_name("alias_name");
		oidcLookupEntityRequest.setName("name");
		String jsonStr = JSONUtil.getJSON(oidcLookupEntityRequest);
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/lookup/group", jsonStr, token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.groupEntityLookUp(token, oidcLookupEntityRequest);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void readEntityAliasById() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String id = "1234-45";
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/entity-alias/id", "{\"id\":\"" + id + "\"}", token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.readEntityAliasById(token, id);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void readEntityByName() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String entityName = "1234ae-45fg";
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/entity/name", "{\"name\":\"" + entityName + "\"}", token))
				.thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.readEntityByName(token, entityName);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void updateEntityByName() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String name = "name";
		OIDCEntityRequest oidcEntityRequest = new OIDCEntityRequest();
		oidcEntityRequest.setDisabled(false);
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("organization", "t-vault");
		oidcEntityRequest.setMetadata(metadata);
		oidcEntityRequest.setPolicies(null);
		oidcEntityRequest.setName(name);
		String jsonStr = JSONUtil.getJSON(oidcEntityRequest);
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/entity/name/update", jsonStr, token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.updateEntityByName(token, oidcEntityRequest, name);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void updateIdentityGroupByName() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String name = "name";
		OIDCIdentityGroupRequest oidcIdentityGroupRequest = new OIDCIdentityGroupRequest();
		oidcIdentityGroupRequest.setMember_group_ids(null);
		oidcIdentityGroupRequest.setMember_entity_ids(null);
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("organization", "t-vault");
		oidcIdentityGroupRequest.setMetadata(metadata);
		oidcIdentityGroupRequest.setPolicies(null);
		oidcIdentityGroupRequest.setName(name);
		String jsonStr = JSONUtil.getJSON(oidcIdentityGroupRequest);
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/group/name/update", jsonStr, token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.updateIdentityGroupByName(token,
				oidcIdentityGroupRequest, name);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void readGroupAliasById() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String id = "1234ae-45fg";
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/group-alias/id", "{\"id\":\"" + id + "\"}", token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.readGroupAliasById(token, id);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void deleteGroupByName() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String entityName = "1234ae-45fg";
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/group/name", "{\"name\":\"" + entityName + "\"}", token))
				.thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.deleteGroupByName(token, entityName);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void deleteGroupAliasByID() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String id = "1234ae-45fg";
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/group-alias/id", "{\"id\":\"" + id + "\"}", token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.deleteGroupAliasByID(token, id);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void createGroupAlias() throws Exception {
		String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
		String id = "1234ae-45fg";
		GroupAliasRequest groupAliasRequest = new GroupAliasRequest();
		groupAliasRequest.setCanonical_id("1212-122");
		groupAliasRequest.setId(id);
		groupAliasRequest.setMount_accessor("mount_accessor");
		groupAliasRequest.setName("name");
		String jsonStr = JSONUtil.getJSON(groupAliasRequest);
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(reqProcessor.process("/identity/group-alias", jsonStr, token)).thenReturn(response);
		ResponseEntity<String> responseEntity = oidcAuthService.createGroupAlias(token, groupAliasRequest);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

}
