// =========================================================================
// Copyright 2019 T-Mobile, US
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
package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class VaultAuthServiceTest {

    @InjectMocks
    VaultAuthService vaultAuthService;

    @Mock
    RequestProcessor reqProcessor;

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
    public void test_login_successfully() {

        String jsonStr = "{  \"username\": \"safeadmin\",  \"password\": \"safeadmin\"}";
        UserLogin userLogin = new UserLogin("safeadmin", "safeadmin");
        String responseJson = "{  \"client_token\": \"8766fdhjSAtH2a4MdvMyzWid\",\"admin\": \"yes\",\"access\": {\"users\":[{\"safe1\":\"read\"}]},\"policies\": [\"default\",\"safeadmin\"],\"lease_duration\": 1800000}";

        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        Map<String, Object> responseMap = null;
        try {
            responseMap = new ObjectMapper().readValue(response.getResponse(), new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(JSONUtil.getJSON(Mockito.any(UserLogin.class))).thenReturn(jsonStr);
        when(JSONUtil.getJSON(responseMap)).thenReturn(responseJson);
        when(reqProcessor.process("/auth/userpass/login",jsonStr,"")).thenReturn(response);

        ResponseEntity<String> responseEntity = vaultAuthService.login(userLogin);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_login_failure_400() {

        String jsonStr = "{  \"username\": \"safeadmin\",  \"password\": \"safeadmin\"}";
        UserLogin userLogin = new UserLogin("safeadmin", "safeadmin");
        String responseJson = "{\"errors\": [\"User Authentication failed\", \"Invalid username or password. Please retry again after correcting username or password.\"]}";

        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);

        when(JSONUtil.getJSON(Mockito.any(UserLogin.class))).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/login",jsonStr,"")).thenReturn(response);
        ResponseEntity<String> responseEntity = vaultAuthService.login(userLogin);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_login_failure_500() {

        String jsonStr = "{  \"username\": \"safeadmin\",  \"password\": \"safeadmin\"}";
        UserLogin userLogin = new UserLogin("safeadmin", "safeadmin");
        String responseJson = "{\"errors\": [\"User Authentication failed\", \"This may be due to vault services are down or vault services are not reachable\"]}";

        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);

        when(JSONUtil.getJSON(Mockito.any(UserLogin.class))).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/login",jsonStr,"")).thenReturn(response);
        ResponseEntity<String> responseEntity = vaultAuthService.login(userLogin);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_renew_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        String responseJson = "{\"client_token\": \"18oVRlB3ft88S6U9raoEDnKn\",\"policies\": [\"safeadmin\"],\"lease_duration\": 1800000}";

        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/auth/tvault/renew","{}", token)).thenReturn(response);
        ResponseEntity<String> responseEntity = vaultAuthService.renew(token);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_renew_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        String responseJson ="{\"errors\":[\"Self renewal of token Failed.\"]}";

        Response response = getMockResponse(HttpStatus.FORBIDDEN, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseJson);

        when(reqProcessor.process("/auth/tvault/renew","{}", token)).thenReturn(response);
        ResponseEntity<String> responseEntity = vaultAuthService.renew(token);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_lookup_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        String responseJson ="{\"id\": \"18oVRlB3ft88S6U9raoEDnKn\",\"last_renewal_time\": 1542013233,\"renewable\": false,\"policies\": [\"default\",\"safeadmin\"],\"creation_ttl\": 1800000}";

        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(response);
        ResponseEntity<String> responseEntity = vaultAuthService.lookup(token);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_lookup_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        String responseJson ="{\"errors\":[\"Token Lookup Failed.\"]}";

        Response response = getMockResponse(HttpStatus.FORBIDDEN, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseJson);

        when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(response);
        ResponseEntity<String> responseEntity = vaultAuthService.lookup(token);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_revoke_sucessfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        String responseJson ="{\"messages\":[\"Revoked Successfully\"]}";

        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/auth/tvault/revoke","{}", token)).thenReturn(response);
        ResponseEntity<String> responseEntity = vaultAuthService.revoke(token);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_revoke_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        String responseJson ="{\"errors\":[\"Token revoke Failed.\"]}";

        Response response = getMockResponse(HttpStatus.FORBIDDEN, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseJson);

        when(reqProcessor.process("/auth/tvault/revoke","{}", token)).thenReturn(response);
        ResponseEntity<String> responseEntity = vaultAuthService.revoke(token);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
}
