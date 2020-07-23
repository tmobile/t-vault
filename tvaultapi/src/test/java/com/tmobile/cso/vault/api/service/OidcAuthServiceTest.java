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

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.OidcRequest;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class OidcAuthServiceTest {

    @InjectMocks
    OidcAuthService oidcAuthService;

    @Mock
    RequestProcessor reqProcessor;

    @Mock
    TokenUtils tokenUtils;

    @Mock
    VaultAuthService vaultAuthService;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
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
    public void test_getAuthUrl_successful() {

        OidcRequest oidcRequest = new OidcRequest("default", "http://localhost:3000");
        String jsonStr = "{  \"role\": \"default\",  \"redirect_uri\": \"http://localhost:3000\"}";
        String token = "8766fdhjSAtH2a4MdvMyzWid";
        String responseJson = "{\n" +
                "  \"request_id\": \"test8b8-6ac68f0ab58d\",\n" +
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
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(JSONUtil.getJSON(Mockito.any(OidcRequest.class))).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/oidc/oidc/auth_url",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        ResponseEntity<String> responseEntity = oidcAuthService.getAuthUrl(oidcRequest);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getAuthUrl_failed() {

        OidcRequest oidcRequest = new OidcRequest("default", "http://localhost:3000");
        String jsonStr = "{  \"role\": \"default\",  \"redirect_uri\": \"http://localhost:3000\"}";
        String token = "8766fdhjSAtH2a4MdvMyzWid";
        String responseJson = "{\"errors\":[\"Failed to get OIDC auth url\"]}";
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(JSONUtil.getJSON(Mockito.any(OidcRequest.class))).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/oidc/oidc/auth_url",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);

        ResponseEntity<String> responseEntity = oidcAuthService.getAuthUrl(oidcRequest);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_processCallback_successful() {

        String state = "teststatecode";
        String code = "testauthcode";
        String pathStr = "?code="+code+"&state="+state;
        String token = "8766fdhjSAtH2a4MdvMyzWid";
        String responseJson = "{\n" +
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

        Response response = getMockResponse(HttpStatus.OK, true, responseJson);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process("/auth/oidc/oidc/callback","{\"path\":\""+pathStr+"\"}",token)).thenReturn(response);
        Map<String, Object> access = new HashMap<>();
        when(vaultAuthService.filterDuplicateSafePermissions(Mockito.any())).thenReturn(access);
        when(vaultAuthService.filterDuplicateSvcaccPermissions(Mockito.any())).thenReturn(access);
        when(JSONUtil.getJSON(Mockito.any(Map.class))).thenReturn(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        ResponseEntity<String> responseEntity = oidcAuthService.processCallback(state, code);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_processCallback_failed() {

        String state = "teststatecode";
        String code = "testauthcode";
        String pathStr = "?code="+code+"&state="+state;
        String token = "8766fdhjSAtH2a4MdvMyzWid";
        String responseJson = "{\"errors\":[\"Failed to get process callback\"]}";;

        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process("/auth/oidc/oidc/callback","{\"path\":\""+pathStr+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);

        ResponseEntity<String> responseEntity = oidcAuthService.processCallback(state, code);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

}
