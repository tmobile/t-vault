package com.tmobile.cso.vault.api.controller;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.service.DirectoryService;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class OIDCUtilTest {
	
	@Mock
    RequestProcessor reqProcessor;

    @Mock
    StatusLine statusLine;

    @Mock
    HttpEntity mockHttpEntity;

    @Mock
    CloseableHttpClient httpClient;

    @Mock
    CloseableHttpResponse httpResponse;

    @Mock
    HttpUtils httpUtils;

    @InjectMocks
    OIDCUtil oidcUtil;
    
    @Mock
    TokenUtils tokenUtils;
    
    @Mock
    DirectoryService directoryService;
    
    @Before
    public void setUp() {
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(ControllerUtil.class);

        Whitebox.setInternalState(OIDCUtil.class, "log", LogManager.getLogger(OIDCUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }

    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        Response response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        if (expectedBody!="") {
            response.setResponse(expectedBody);
        }
        return response;
    }
    
    @Test
    public void test_fetchMountAccessorForOidc() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String dataOutput = "{\"data\":{\"oidc/\":{\"accessor\":\"auth_oidc_8b51f292\",\"config\":{\"default_lease_ttl\":0,\"force_no_cache\":false,\"max_lease_ttl\":0,\"token_type\":\"default-service\"},\"description\":\"\",\"external_entropy_access\":false,\"local\":false,\"options\":null,\"seal_wrap\":false,\"type\":\"oidc\",\"uuid\":\"fbd45cc4-d6b6-8b49-6d1a-d4d931345df9\"}}}";
        Response responsemock = getMockResponse(HttpStatus.OK, true, dataOutput);
        when(reqProcessor.process(eq("/sys/list"),Mockito.any(),eq(token))).thenReturn(responsemock);
        String mountAccessor = oidcUtil.fetchMountAccessorForOidc(token);
        assertEquals("auth_oidc_8b51f292", mountAccessor);
    }
    
    @Test
    public void test_getEntityLookUpResponse() {
        String authMountResponse = "{\"data\":{\"name\":\"entity_63f119d2\",\"policies\":[\"safeadmin\"]}}";
        OIDCEntityResponse expectedResposne = new OIDCEntityResponse();
        expectedResposne.setEntityName("entity_63f119d2");
        List<String> policies = new ArrayList<>();
        policies.add("safeadmin");
        expectedResposne.setPolicies(policies);
        OIDCEntityResponse oidcEntityResponse = oidcUtil.getEntityLookUpResponse(authMountResponse);
        assertEquals(expectedResposne, oidcEntityResponse);
    }

    @Test
    public void test_getIdentityGroupDetails_success() {
        String group = "group1";
        String token = "test4ig8L3EpsJZSLAMg";
        String dataOutput = "{\"id\": \"123-123-123-123\", \"policies\": [\"r_users_safe1\", \"r_users_safe2\"]}";
        Response responsemock = getMockResponse(HttpStatus.OK, true, dataOutput);

        List<String> policies = new ArrayList<>();
        policies.add("r_users_safe1");
        policies.add("r_users_safe2");
        OIDCGroup expectedOidcGroup = new OIDCGroup("123-123-123-123", policies);

        when(reqProcessor.process("/identity/group/name", "{\"group\":\""+group+"\"}", token)).thenReturn(responsemock);
        OIDCGroup oidcGroup = oidcUtil.getIdentityGroupDetails(group, token);
        assertEquals(expectedOidcGroup.toString(), oidcGroup.toString());
    }

    @Test
    public void test_getIdentityGroupDetails_failed() {
        String group = "group1";
        String token = "test4ig8L3EpsJZSLAMg";
        Response responsemock = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(reqProcessor.process("/identity/group/name", "{\"group\":\""+group+"\"}", token)).thenReturn(responsemock);
        OIDCGroup oidcGroup = oidcUtil.getIdentityGroupDetails(group, token);
        assertEquals(null, oidcGroup);
    }

    @Test
    public void test_getSSOToken_success() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String responseString = "{\"access_token\": \"abcd\"}";
        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(responseString.getBytes()));
        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");
        when(ControllerUtil.getOidcADLoginUrl()).thenReturn("testurl");

        String responseJson = "{\"data\":{\"objectId\": \"abcdefg\"}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        String actualToken = oidcUtil.getSSOToken();
        assertNotNull(actualToken);
    }

    @Test
    public void test_getSSOToken_400() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String responseString = "";
        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(responseString.getBytes()));
        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");
        when(ControllerUtil.getOidcADLoginUrl()).thenReturn("testurl");
        String responseJson = "{\"errors\":[\"Failed to get SSO token for Azure AD access\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);
        String actualToken = oidcUtil.getSSOToken();
        assertEquals(null, actualToken);

    }

    @Test
    public void test_getGroupObjectResponse_success() throws Exception {
        String group = "group1";
        String token = "test4ig8L3EpsJZSLAMg";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String groupResponseString = "{\"value\": [ {\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":null}]}";
        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(groupResponseString.getBytes()));
        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");

        String actualResponse = oidcUtil.getGroupObjectResponse(token, group);
        assertEquals("abcdefg", actualResponse);
    }

    @Test
    public void test_getGroupObjectResponse_success_no_cloud_entity() throws Exception {
        String group = "group1";
        String token = "test4ig8L3EpsJZSLAMg";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String groupResponseString = "{\"value\": [ {\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":\"true\"}]}";
        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(groupResponseString.getBytes()));
        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");

        String actualResponse = oidcUtil.getGroupObjectResponse(token, group);
        assertEquals("abcdefg", actualResponse);
    }

    @Test
    public void test_getGroupObjectResponse_success_no_cloud_entity_no_onprem_entity() throws Exception {
        String group = "group1";
        String token = "test4ig8L3EpsJZSLAMg";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String groupResponseString = "{\"value\": [ {\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":\"false\"}]}";
        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(groupResponseString.getBytes()));
        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");

        String actualResponse = oidcUtil.getGroupObjectResponse(token, group);
        assertEquals("abcdefg", actualResponse);
    }

    @Test
    public void test_getGroupObjectResponse_404() throws Exception {

        String group = "group1";
        String token = "test4ig8L3EpsJZSLAMg";

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String groupResponseString = "{\"value\": [ ]}";

        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(groupResponseString.getBytes()));

        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");
        String actualResponse = oidcUtil.getGroupObjectResponse(token, group);
        assertEquals(null, actualResponse);

    }
    
    @Test
    public void deleteGroupAliasByIDSuccess(){
		String token = "testqhdjddk";
		String id = "12wdsadsad";
		Response responsemock = getMockResponse(HttpStatus.OK, true, "");

		when(reqProcessor.process("/identity/group-alias/id/delete", "{\"id\":\"" + id + "\"}", token))
				.thenReturn(responsemock);
		Response response = oidcUtil.deleteGroupAliasByID(token, id);
		assertEquals(responsemock.getHttpstatus(), response.getHttpstatus());
    }
    
    @Test
    public void oidcFetchEntityDetailsSuccess(){
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Success\"]}");
    	String token = "qwqwdsfsf";
    	String accessor = "testUser";
    	String username = "testUser";
    	   DirectoryUser directoryUser = new DirectoryUser();
           directoryUser.setDisplayName("testUser");
           directoryUser.setGivenName("testUser");
           directoryUser.setUserEmail("testUser@t-mobile.com");
           directoryUser.setUserId("testuser01");
           directoryUser.setUserName("testUser");

           List<DirectoryUser> persons = new ArrayList<>();
           persons.add(directoryUser);

           DirectoryObjects users = new DirectoryObjects();
           DirectoryObjectsList usersList = new DirectoryObjectsList();
           usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
           users.setData(usersList);
           ResponseEntity<DirectoryObjects> responseEntity1 = ResponseEntity.status(HttpStatus.OK).body(users);
           String dataOutput = "{\"data\":{\"oidc/\":{\"accessor\":\"auth_oidc_8b51f292\",\"config\":{\"default_lease_ttl\":0,\"force_no_cache\":false,\"max_lease_ttl\":0,\"token_type\":\"default-service\"},\"description\":\"\",\"external_entropy_access\":false,\"local\":false,\"options\":null,\"seal_wrap\":false,\"type\":\"oidc\",\"uuid\":\"fbd45cc4-d6b6-8b49-6d1a-d4d931345df9\"}}}";
           Response responsemock = getMockResponse(HttpStatus.OK, true, dataOutput);
           when(reqProcessor.process(eq("/sys/list"),Mockito.any(),eq(token))).thenReturn(responsemock);
           OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
           oidcLookupEntityRequest.setAlias_name("alias_name");
           oidcLookupEntityRequest.setAlias_mount_accessor("alias_mount_accessor");
           String jsonStr = JSONUtil.getJSON(oidcLookupEntityRequest);
           String authMountResponse = "{\"data\":{\"name\":\"entity_63f119d2\",\"policies\":[\"default\"]}}";
           Response response = getMockResponse(HttpStatus.OK, true, authMountResponse);
           when(reqProcessor.process("/identity/lookup/entity", jsonStr, token)).thenReturn(response);
           when(reqProcessor.process("/auth/tvault/lookup", "{}", token)).thenReturn(response);
           when(directoryService.getUserDetailsByCorpId(username)).thenReturn(directoryUser);
    	ResponseEntity<OIDCEntityResponse> oiEntity = oidcUtil.oidcFetchEntityDetails(token, username, null);
        assertEquals(oiEntity.getStatusCode(), responseEntityExpected.getStatusCode());
    }

    @Test
    public void oidcFetchEntityDetailsNewUserSuccess(){
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Success\"]}");
        String token = "qwqwdsfsf";
        String accessor = "testUser";
        String username = "testUser";
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUser");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testUser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);

        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        ResponseEntity<DirectoryObjects> responseEntity1 = ResponseEntity.status(HttpStatus.OK).body(users);
        String dataOutput = "{\"data\":{\"oidc/\":{\"accessor\":\"auth_oidc_8b51f292\",\"config\":{\"default_lease_ttl\":0,\"force_no_cache\":false,\"max_lease_ttl\":0,\"token_type\":\"default-service\"},\"description\":\"\",\"external_entropy_access\":false,\"local\":false,\"options\":null,\"seal_wrap\":false,\"type\":\"oidc\",\"uuid\":\"fbd45cc4-d6b6-8b49-6d1a-d4d931345df9\"}}}";
        Response responsemock = getMockResponse(HttpStatus.OK, true, dataOutput);
        when(reqProcessor.process(eq("/sys/list"),Mockito.any(),eq(token))).thenReturn(responsemock);
        OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
        oidcLookupEntityRequest.setAlias_name("alias_name");
        oidcLookupEntityRequest.setAlias_mount_accessor("alias_mount_accessor");
        String jsonStr = JSONUtil.getJSON(oidcLookupEntityRequest);
        String authMountResponse = "{\"data\":{\"name\":\"entity_63f119d2\",\"policies\":[\"default\"]}}";
        Response response = getMockResponse(HttpStatus.OK, true, authMountResponse);
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        //when(reqProcessor.process("/identity/lookup/entity", jsonStr, token)).thenReturn(response);

        when(reqProcessor.process("/identity/lookup/entity", jsonStr, token)).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return response;

                return response404;
            }
        });
        when(reqProcessor.process("/identity/entity-alias", jsonStr, token)).thenReturn(getMockResponse(HttpStatus.OK, true, ""));
        when(reqProcessor.process("/auth/tvault/lookup", "{}", token)).thenReturn(response);
        when(directoryService.getUserDetailsByCorpId(username)).thenReturn(directoryUser);
        ResponseEntity<OIDCEntityResponse> oiEntity = oidcUtil.oidcFetchEntityDetails(token, username, null);
        assertEquals(oiEntity.getStatusCode(), responseEntityExpected.getStatusCode());
    }

    @Test
    public void createGroupAlias(){
    	String token = "sdsadsadasdasd";
    	Response responsemock = getMockResponse(HttpStatus.OK, true, "");
    	GroupAliasRequest groupAliasRequest = new GroupAliasRequest();
    	groupAliasRequest.setCanonical_id("canonical_id");
    	groupAliasRequest.setId("id");
    	groupAliasRequest.setMount_accessor("mount_accessor");
    	groupAliasRequest.setName("name");
    	String jsonStr = JSONUtil.getJSON(groupAliasRequest);
    	 when(reqProcessor.process("/identity/group-alias", jsonStr, token)).thenReturn(responsemock);
         Response response = oidcUtil.createGroupAlias(token, groupAliasRequest);
    	 assertEquals(responsemock.getHttpstatus(), response.getHttpstatus());
    }
    
    @Test
    public void updateIdentityGroupByNameSuccess(){
    	String token = "Adadsadasdasd";
    	String responseJson = "{\"data\":{\"id\": \"canonicalID\"}}";
    	Response responsemock = getMockResponse(HttpStatus.OK, true, "canonicalID");
    	OIDCIdentityGroupRequest oidcIdentityGroupRequest = new OIDCIdentityGroupRequest();
    	oidcIdentityGroupRequest.setName("name");
    	List<String> policies = new ArrayList<>();
    	policies.add("safeadmin");
    	oidcIdentityGroupRequest.setPolicies(policies);
    	Response rsResponse = getMockResponse(HttpStatus.OK, true, responseJson);
    	String jsonStr = JSONUtil.getJSON(oidcIdentityGroupRequest);
		when(reqProcessor.process("/identity/group/name/update", jsonStr, token)).thenReturn(rsResponse);
		String canonicalId = oidcUtil.updateIdentityGroupByName(token, oidcIdentityGroupRequest);
		assertEquals(responsemock.getResponse(), canonicalId);
    }
    
    @Test
    public void deleteGroupByNameSuccess(){
    	String name = "r_vault_demo";
    	String token = "Sdasdadasdasd";		
    	Response responsemock = getMockResponse(HttpStatus.OK, true, "");
    	when(reqProcessor.process("/identity/group/name/delete", "{\"name\":\"" + name + "\"}", token)).thenReturn(responsemock);
    	Response response = oidcUtil.deleteGroupByName(token, name);
    	assertEquals(responsemock.getHttpstatus(), response.getHttpstatus());
    }
    
    @Test
    public void updateOIDCEntity() throws Exception {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String name = "name";
        List<String> policies = new ArrayList<>();
        policies.add("safeadmin");
        String entityName = "entityName";
        OIDCEntityRequest oidcEntityRequest = new OIDCEntityRequest();
        oidcEntityRequest.setDisabled(false);
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("organization", "t-vault");
        oidcEntityRequest.setMetadata(metadata);
        oidcEntityRequest.setPolicies(null);
        oidcEntityRequest.setName(name);
        String jsonStr = JSONUtil.getJSON(oidcEntityRequest);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
//        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
//                .body("{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
        when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
        when(reqProcessor.process("/identity/entity/name/update", jsonStr, token)).thenReturn(response);
//       
        Response responseEntity = oidcUtil.updateOIDCEntity(policies, entityName);
        assertEquals(HttpStatus.OK, responseEntity.getHttpstatus());
    }

    @Test
    public void test_renewUserToken_success() throws Exception {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String responseJson = "{\"client_token\": \"18oVRlB3ft88S6U9raoEDnKn\",\"policies\": [\"safeadmin\"],\"lease_duration\": 1800000}";

        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        when(reqProcessor.process("/auth/tvault/renew", "{}", token)).thenReturn(response);
        oidcUtil.renewUserToken(token);
        assertTrue(true);
    }

    @Test
    public void test_renewUserToken_failed() throws Exception {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String responseJson = "{\"client_token\": \"18oVRlB3ft88S6U9raoEDnKn\",\"policies\": [\"safeadmin\"]," +
                "\"lease_duration\": 1800000}";

        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        when(reqProcessor.process("/auth/tvault/renew", "{}", token)).thenReturn(response);
        oidcUtil.renewUserToken(token);
        assertTrue(true);
    }

    @Test
    public void test_updateGroupPolicies_success() throws Exception {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String id = "12wdsadsad";
        Response deleteByIdResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(reqProcessor.process("/identity/group-alias/id/delete", "{\"id\":\"" + id + "\"}", token)).thenReturn(deleteByIdResponse);

        String name = "r_vault_demo";
        Response deleteResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process("/identity/group/name/delete", "{\"name\":\"" + name + "\"}",  token)).thenReturn(deleteResponse);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        when(ControllerUtil.getOidcADLoginUrl()).thenReturn("testurl");

        String groupResponseString = "{\"value\": [ {\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":null}], \"access_token\": \"abcd\"}";
        //when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(groupResponseString.getBytes()));
        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");
        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(groupResponseString.getBytes())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return new ByteArrayInputStream(groupResponseString.getBytes());

                return new ByteArrayInputStream(groupResponseString.getBytes());
            }
        });
        String dataOutput = "{\"data\":{\"oidc/\":{\"accessor\":\"auth_oidc_8b51f292\",\"config\":{\"default_lease_ttl\":0,\"force_no_cache\":false,\"max_lease_ttl\":0,\"token_type\":\"default-service\"},\"description\":\"\",\"external_entropy_access\":false,\"local\":false,\"options\":null,\"seal_wrap\":false,\"type\":\"oidc\",\"uuid\":\"fbd45cc4-d6b6-8b49-6d1a-d4d931345df9\"}}}";
        Response responsemock = getMockResponse(HttpStatus.OK, true, dataOutput);
        when(reqProcessor.process(eq("/sys/list"),Mockito.any(),eq(token))).thenReturn(responsemock);

        List<String> currentPolicies = new ArrayList<>();
        currentPolicies.add("testpolicy2");

        GroupAliasRequest groupAliasRequest = new GroupAliasRequest();
        groupAliasRequest.setCanonical_id("canonical_id");
        groupAliasRequest.setId("id");
        groupAliasRequest.setMount_accessor("mount_accessor");
        groupAliasRequest.setName("name");
        String jsonStr = JSONUtil.getJSON(groupAliasRequest);
        when(reqProcessor.process("/identity/group-alias", jsonStr, token)).thenReturn(getMockResponse(HttpStatus.OK, true, ""));

        String responseJson = "{\"data\":{\"id\": \"canonicalID\"}}";
        OIDCIdentityGroupRequest oidcIdentityGroupRequest = new OIDCIdentityGroupRequest();
        oidcIdentityGroupRequest.setName("name");
        List<String> policies = new ArrayList<>();
        policies.add("testpolicy1");
        policies.add("safeadmin");
        oidcIdentityGroupRequest.setPolicies(policies);
        Response rsResponse = getMockResponse(HttpStatus.OK, true, responseJson);
        when(reqProcessor.process("/identity/group/name/update", JSONUtil.getJSON(oidcIdentityGroupRequest), token)).thenReturn(rsResponse);

        Response expectedResponse = getMockResponse(HttpStatus.OK, true, "");

        Response updateResponse = oidcUtil.updateGroupPolicies(token, name, policies, currentPolicies,  id);

        assertEquals(expectedResponse.getHttpstatus(), updateResponse.getHttpstatus());
    }

    @Test
    public void test_updateGroupPolicies_failed() throws Exception {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String id = "12wdsadsad";
        Response deleteByIdResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");

        when(reqProcessor.process("/identity/group-alias/id/delete", "{\"id\":\"" + id + "\"}", token)).thenReturn(deleteByIdResponse);

        String name = "r_vault_demo";
        List<String> policies = new ArrayList<>();
        policies.add("testpolicy1");
        policies.add("safeadmin");


        List<String> currentPolicies = new ArrayList<>();
        currentPolicies.add("testpolicy2");

        String responseJson = "{\"data\":{\"id\": \"canonicalID\"}}";
        OIDCIdentityGroupRequest oidcIdentityGroupRequest = new OIDCIdentityGroupRequest();
        oidcIdentityGroupRequest.setName("name");
        oidcIdentityGroupRequest.setPolicies(policies);
        Response rsResponse = getMockResponse(HttpStatus.OK, true, responseJson);
        when(reqProcessor.process("/identity/group/name/update", JSONUtil.getJSON(oidcIdentityGroupRequest), token)).thenReturn(rsResponse);

        Response expectedResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");

        Response updateResponse = oidcUtil.updateGroupPolicies(token, name, policies, currentPolicies,  id);

        assertEquals(expectedResponse.getHttpstatus(), updateResponse.getHttpstatus());
    }
    
    
    @Test
    public void getUserName_Success(){
		String username = "testuser";
		String email = "testUser@t-mobile.com";
		Response responsemock = getMockResponse(HttpStatus.OK, true, username);
		
		 DirectoryUser directoryUser = new DirectoryUser();
         directoryUser.setDisplayName("testUser");
         directoryUser.setGivenName("testUser");
         directoryUser.setUserEmail("testUser@t-mobile.com");
         directoryUser.setUserId("testuser01");
         directoryUser.setUserName("testuser");

         List<DirectoryUser> persons = new ArrayList<>();
         persons.add(directoryUser);

         DirectoryObjects users = new DirectoryObjects();
         DirectoryObjectsList usersList = new DirectoryObjectsList();
         usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
         users.setData(usersList);
         ResponseEntity<DirectoryObjects> responseEntity1 = ResponseEntity.status(HttpStatus.OK).body(users);

         when(directoryService.searchByUPN(directoryUser.getUserEmail())).thenReturn(responseEntity1);

		String originalUserName = oidcUtil.getUserName(email);
		assertEquals(originalUserName, responsemock.getResponse().toString());
    }

    @Test
    public void test_getGroupsFromAAD_success() throws Exception {
        String group = "testgroup";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String groupResponseString = "{\"value\": [ {\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":null, \"displayName\":\"testgroup1\"}]}";
        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(groupResponseString.getBytes()));
        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");

        List<DirectoryGroup> groups = oidcUtil.getGroupsFromAAD("testssotoken", group);
        assertEquals(1, groups.size());
        assertEquals("testgroup1", groups.get(0).getDisplayName());

    }

    @Test
    public void test_getGroupsFromAAD_failed() throws Exception {
        String group = "testgroup";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        ReflectionTestUtils.setField(oidcUtil, "ssoGroupsEndpoint", "testgroupurl");

        List<DirectoryGroup> groups = oidcUtil.getGroupsFromAAD("testssotoken", group);
        assertEquals(0, groups.size());

    }

	@Test
	public void test_getIdOfTheUser_success() throws Exception {
		String userEmail = "testuser@t-mobile.com";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setSuccess(true);
		response.setResponse(null);

		when(httpUtils.getHttpClient()).thenReturn(httpClient);
		when(httpClient.execute(any())).thenReturn(httpResponse);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

		String userIdResponseString = "{\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":null, \"displayName\":\"testgroup1\"}";
		when(mockHttpEntity.getContent()).thenReturn(new ByteArrayInputStream(userIdResponseString.getBytes()));
		ReflectionTestUtils.setField(oidcUtil, "ssoGetUserEndpoint", "testgroupurl");
		ReflectionTestUtils.setField(oidcUtil, "sprintMailTailText", "sprint.com");

		String userId = oidcUtil.getIdOfTheUser("testssotoken", userEmail);
		assertEquals("abcdefg", userId);
	}

	@Test
	public void test_getIdOfTheSprintUser_success() throws Exception {
		String userEmail = "testuser@sprint.com";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setSuccess(true);
		response.setResponse(null);

		when(httpUtils.getHttpClient()).thenReturn(httpClient);
		when(httpClient.execute(any())).thenReturn(httpResponse);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

		String userIdResponseString = "{\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":null, \"displayName\":\"testgroup1\"}";
		when(mockHttpEntity.getContent()).thenReturn(new ByteArrayInputStream(userIdResponseString.getBytes()));
		ReflectionTestUtils.setField(oidcUtil, "ssoGetUserEndpoint", "testgroupurl");
		ReflectionTestUtils.setField(oidcUtil, "sprintMailTailText", "sprint.com");

		String userId = oidcUtil.getIdOfTheUser("testssotoken", userEmail);
		assertEquals("abcdefg", userId);
	}

	@Test
	public void test_getIdOfTheUser_failed() throws Exception {
		String userEmail = "testuser@t-mobile.com";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setSuccess(true);
		response.setResponse(null);

		when(httpUtils.getHttpClient()).thenReturn(httpClient);
		when(httpClient.execute(any())).thenReturn(httpResponse);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(400);

		ReflectionTestUtils.setField(oidcUtil, "ssoGetUserEndpoint", "testgroupurl");
		ReflectionTestUtils.setField(oidcUtil, "sprintMailTailText", "sprint.com");

		String userId = oidcUtil.getIdOfTheUser("testssotoken", userEmail);
		assertEquals(null, userId);
	}

	@Test
	public void test_getSelfServiceGroupsFromAADById_success() throws Exception {
		String userAADId = "abcdefg";
		String userName = "testuser";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setSuccess(true);
		response.setResponse(null);

		when(httpUtils.getHttpClient()).thenReturn(httpClient);
		when(httpClient.execute(any())).thenReturn(httpResponse);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

		String groupResponseString = "{\"value\": [ {\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":null, \"displayName\":\"r_selfservice_tvt_admin\"}]}";
		when(mockHttpEntity.getContent()).thenReturn(new ByteArrayInputStream(groupResponseString.getBytes()));
		ReflectionTestUtils.setField(oidcUtil, "ssoGetUserEndpoint", "testgroupurl");
		ReflectionTestUtils.setField(oidcUtil, "ssoGetUserGroups", "groupurlvalue");
		ReflectionTestUtils.setField(oidcUtil, "ssoGroupPattern", "r_selfservice_[a-z]{3}_admin");

		List<String> groups = oidcUtil.getSelfServiceGroupsFromAADById("testssotoken", userAADId, userName);
		assertEquals(1, groups.size());
		assertEquals("tvt", groups.get(0));

	}

	@Test
	public void test_getSelfServiceGroupsFromAADById_failed() throws Exception {
		String userAADId = "abcdefg";
		String userName = "testuser";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setSuccess(true);
		response.setResponse(null);

		when(httpUtils.getHttpClient()).thenReturn(httpClient);
		when(httpClient.execute(any())).thenReturn(httpResponse);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(400);

		ReflectionTestUtils.setField(oidcUtil, "ssoGetUserEndpoint", "testgroupurl");
		ReflectionTestUtils.setField(oidcUtil, "ssoGetUserGroups", "groupurlvalue");
		ReflectionTestUtils.setField(oidcUtil, "ssoGroupPattern", "r_selfservice_[a-z]{3}_admin");

		List<String> groups = oidcUtil.getSelfServiceGroupsFromAADById("testssotoken", userAADId, userName);
		assertEquals(0, groups.size());
	}

    @Test
    public void test_getAzureUserObject_success() throws Exception {
        String userEmail = "testuser@t-mobile.com";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        when(ControllerUtil.getOidcADLoginUrl()).thenReturn("testurl");
        when(mockHttpEntity.getContent()).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1) {
                    String userIdResponseString = "{\"id\": \"abcdefg\", \"onPremisesSyncEnabled\":null, \"displayName\":\"testgroup1\", \"mail\": \"TestUser@T-Mobile.com\"}";
                    return new ByteArrayInputStream(userIdResponseString.getBytes());
                }
                String responseString = "{\"access_token\": \"abcd\"}";
                return new ByteArrayInputStream(responseString.getBytes());
            }
        });


        ReflectionTestUtils.setField(oidcUtil, "ssoGetUserEndpoint", "testgroupurl");
        ReflectionTestUtils.setField(oidcUtil, "sprintMailTailText", "sprint.com");

        AADUserObject aadUserObject = oidcUtil.getAzureUserObject(userEmail);
        assertEquals("abcdefg", aadUserObject.getUserId());
        assertEquals("TestUser@T-Mobile.com", aadUserObject.getEmail());
    }

    @Test
    public void test_getAzureUserObject_failed() throws Exception {
        String userEmail = "testuser@t-mobile.com";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(403);
        when(ControllerUtil.getOidcADLoginUrl()).thenReturn("testurl");
        ReflectionTestUtils.setField(oidcUtil, "ssoGetUserEndpoint", "testgroupurl");
        ReflectionTestUtils.setField(oidcUtil, "sprintMailTailText", "sprint.com");

        AADUserObject aadUserObject = oidcUtil.getAzureUserObject(userEmail);
        assertEquals(null, aadUserObject.getUserId());
        assertEquals(null, aadUserObject.getEmail());
    }
}
