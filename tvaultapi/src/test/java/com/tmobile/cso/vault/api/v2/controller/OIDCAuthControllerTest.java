package com.tmobile.cso.vault.api.v2.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tmobile.cso.vault.api.model.GroupAliasRequest;
import com.tmobile.cso.vault.api.model.OIDCEntityRequest;
import com.tmobile.cso.vault.api.model.OIDCIdentityGroupRequest;
import com.tmobile.cso.vault.api.model.OIDCLookupEntityRequest;
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
		when(oidcAuthService.updateEntityByName(token, oidcEntityRequest, name)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
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
		when(oidcAuthService.updateIdentityGroupByName(token, oidcIdentityGroupRequest, name))
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


}
