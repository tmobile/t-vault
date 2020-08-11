package com.tmobile.cso.vault.api.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tmobile.cso.vault.api.model.OIDCGroup;
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
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
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

        String groupResponseString = "{\"value\": [ {\"id\": \"abcdefg\"}]}";
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
}
