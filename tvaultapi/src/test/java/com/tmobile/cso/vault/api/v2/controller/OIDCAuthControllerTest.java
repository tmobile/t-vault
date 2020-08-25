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

package com.tmobile.cso.vault.api.v2.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tmobile.cso.vault.api.service.OIDCAuthService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class OIDCAuthControllerTest {

	
	@Mock
	public OIDCAuthService oidcAuthService;

	private MockMvc mockMvc;

	@InjectMocks
	public OIDCAuthController oidcAuthController;
	
	 @Mock
	    HttpServletRequest httpServletRequest;
	    String token;

	    @Before
	    public void setUp() {
	        MockitoAnnotations.initMocks(this);
	        this.mockMvc = MockMvcBuilders.standaloneSetup(oidcAuthController).build();
	        token = "5PDrOhsy4ig8L3EpsJZSLAMg";  
	    }
	    
	@Test
	public void test_getAuthenticationMounts_successful() throws Exception {
		when(oidcAuthService.getAuthenticationMounts(token)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.getAuthenticationMounts(httpServletRequest, token).getStatusCode());
	}
	
	@Test
	public void test_entityLookUp_successful() throws Exception {
		OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
		oidcLookupEntityRequest.setId("1223");
		oidcLookupEntityRequest.setAlias_id("123");
		oidcLookupEntityRequest.setAlias_mount_accessor("mount");
		oidcLookupEntityRequest.setAlias_name("alias_name");
		oidcLookupEntityRequest.setName("name");
		when(oidcAuthService.entityLookUp(token, oidcLookupEntityRequest)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.entityLookUp(httpServletRequest, token,oidcLookupEntityRequest).getStatusCode());
	}
	
	@Test
	public void test_groupEntityLookUp_successful() throws Exception {
		OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
		oidcLookupEntityRequest.setId("1223");
		oidcLookupEntityRequest.setAlias_id("123");
		oidcLookupEntityRequest.setAlias_mount_accessor("mount");
		oidcLookupEntityRequest.setAlias_name("alias_name");
		oidcLookupEntityRequest.setName("name");
		when(oidcAuthService.groupEntityLookUp(token, oidcLookupEntityRequest)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.groupEntityLookUp(httpServletRequest, token,oidcLookupEntityRequest).getStatusCode());
	}
	
	@Test
	public void test_readEntityAliasById_successful() throws Exception {
		String id ="124";
		when(oidcAuthService.readEntityAliasById(token, id)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.readEntityAliasById(httpServletRequest, token,id).getStatusCode());
	}
	
	@Test
	public void test_readEntityByName_successful() throws Exception {
		String name ="name";
		when(oidcAuthService.readEntityByName(token, name)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.readEntityByName(httpServletRequest, token,name).getStatusCode());
	}
	
	@Test
	public void test_updateEntityByName_successful() throws Exception {
		String name ="name"; 
		OIDCEntityRequest oidcEntityRequest = new OIDCEntityRequest();
		oidcEntityRequest.setDisabled(false);
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("organization", "t-vault");
		oidcEntityRequest.setMetadata(metadata);
		oidcEntityRequest.setPolicies(null);
		oidcEntityRequest.setName(name);
		when(oidcAuthService.updateEntityByName(token, oidcEntityRequest)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.updateEntityByName(httpServletRequest, token,name,oidcEntityRequest).getStatusCode());
	}
	
	@Test
	public void test_readGroupAliasById_successful() throws Exception {
		String id ="124"; 
		when(oidcAuthService.readGroupAliasById(token, id)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.readGroupAliasById(httpServletRequest, token,id).getStatusCode());
	}
	
	@Test
	public void test_updateIdentityGroupByName_successful() throws Exception {
		String name = "name";
		OIDCIdentityGroupRequest oidcIdentityGroupRequest = new OIDCIdentityGroupRequest();
		oidcIdentityGroupRequest.setMember_group_ids(null);
		oidcIdentityGroupRequest.setMember_entity_ids(null);
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("organization", "t-vault");
		oidcIdentityGroupRequest.setMetadata(metadata);
		oidcIdentityGroupRequest.setPolicies(null);
		oidcIdentityGroupRequest.setName(name);
		when(oidcAuthService.updateIdentityGroupByName(token, oidcIdentityGroupRequest))
				.thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK, oidcAuthController
				.updateIdentityGroupByName(httpServletRequest, token, name, oidcIdentityGroupRequest).getStatusCode());
	}
	
	@Test
	public void test_deleteGroupByName_successful() throws Exception {
		String name ="name"; 
		when(oidcAuthService.deleteGroupByName(token, name)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.deleteGroupByName(httpServletRequest, token,name).getStatusCode());
	}
	
	@Test
	public void test_deleteGroupAliasByID_successful() throws Exception {
		String id ="124"; 
		when(oidcAuthService.deleteGroupAliasByID(token, id)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.deleteGroupAliasByID(httpServletRequest, token,id).getStatusCode());
	}
	
	@Test
	public void test_createGroupAlias_successful() throws Exception {
		String id ="124"; 
		GroupAliasRequest groupAliasRequest = new GroupAliasRequest();
		groupAliasRequest.setCanonical_id("1212-122");
		groupAliasRequest.setId(id);
		groupAliasRequest.setMount_accessor("mount_accessor");
		groupAliasRequest.setName("name");
		when(oidcAuthService.createGroupAlias(token, groupAliasRequest)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.createGroupAlias(httpServletRequest, token,groupAliasRequest).getStatusCode());
	}

	@Test
	public void test_getAuthUrl() throws Exception {

		OidcRequest oidcRequest = new OidcRequest("default", "http://localhost:3000");
		String inputJson =new ObjectMapper().writeValueAsString(oidcRequest);
		String responseMessage = "{\n" +
				"  \"request_id\": \"test-b184-88b8-6ac68f0ab58d\",\n" +
				"  \"lease_id\": \"\",\n" +
				"  \"renewable\": false,\n" +
				"  \"lease_duration\": 0,\n" +
				"  \"data\": {\n" +
				"    \"auth_url\": \"https://login.authdomain.com/test123123/oauth2/v2.0/authorize?client_id=test123123&nonce=123123&redirect_uri=http%3A%2F%2Flocalhost%3A3000&response_type=code&scope=openid+https%3A%2F%2Fgraph.authdomain.com%2F.default+profile&state=test4343545\"\n" +
				"  },\n" +
				"  \"wrap_info\": null,\n" +
				"  \"warnings\": null,\n" +
				"  \"auth\": null\n" +
				"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

		when(oidcAuthService.getAuthUrl(Mockito.any())).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/oidc/auth_url")
				.header("vault-token", "testy4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8")
				.content(inputJson))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(responseMessage)));
	}


	@Test
	public void test_proessOIDCCallback() throws Exception {

		String responseMessage = "{\n" +
				"\"client_token\": \"testmioJHmaUB1k7PB2wUDh\",\n" +
				"\"admin\": \"yes\",\n" +
				"\"access\": {\n" +
				"\"cert\": [{\n" +
				"\"test.company.com\": \"read\"\n" +
				"}],\n" +
				"\"svcacct\": [{\n" +
				"\"svc_test04\": \"write\"\n" +
				"}],\n" +
				"\"users\": [{\n" +
				"\"testsafe1\": \"read\"\n" +
				"}]\n" +
				"},\n" +
				"\"policies\": [\"default\"],\n" +
				"\"lease_duration\": 1800\n" +
				"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

		when(oidcAuthService.processOIDCCallback(Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/auth/oidc/callback")
				.header("vault-token", "test4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(responseMessage)));
	}

	@Test
	public void test_getGroupObjectIdFromAzure() throws Exception {

		String responseMessage = "{\n" +
				"  \"data\": {\n" +
				"    \"objectId\": \"123123-1232-123-123-123123\"\n" +
				"  }\n" +
				"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

		when(oidcAuthService.getGroupObjectIdFromAzure("group1")).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/azure/group/group1/objectid")
				.header("vault-token", "test4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(responseMessage)));
	}

	@Test
	public void test_getIdentityGroupDetails() throws Exception {

		String responseMessage = "{\n" +
				"    \"id\": \"123-123-123-123\",\n" +
				"    \"policies\": [\n" +
				"        \"w_users_testsafe\"\n" +
				"    ]\n" +
				"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

		when(oidcAuthService.getIdentityGroupDetails("testgroup", "test4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/identity/group/testgroup")
				.header("vault-token", "test4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(responseMessage)));
	}
	
	@Test
	public void test_getUserName_successful() throws Exception {
		when(oidcAuthService.getUserName(Mockito.any(UserDetails.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				oidcAuthController.getUserName(httpServletRequest, token).getStatusCode());
	}
}
