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

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AccessPolicy;
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
import org.mockito.cglib.core.CollectionUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({CollectionUtils.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class AccessServiceTest {

    @InjectMocks
    AccessService accessService;

    @Mock
    RequestProcessor reqProcessor;

    @Before
    public void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException{
        PowerMockito.mockStatic(CollectionUtils.class);
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
    public void test_createAccessPolicy_successfully() {

        Response response =getMockResponse(HttpStatus.OK, true, "");

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        HashMap<String, String> access = new HashMap<>();
        access.put("users/*", "read");
        access.put("apps/*", "read");
        access.put("shared/*", "read");
        AccessPolicy accessPolicy = new AccessPolicy("my-test-policy", access);

        String jsonStr = "{\"access\": { \"users/*\": \"read\", \"apps/*\": \"read\",\"shared/*\": \"read\" }, \"accessid\": \"my-test-policy\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access Policy created \"]}");
        when(reqProcessor.process("/access/create",jsonStr,token)).thenReturn(response);

        when(JSONUtil.getJSON(accessPolicy)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = accessService.createPolicy(token, accessPolicy);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createAccessPolicy_failure_400() {
        String responseBody = "{\"errors\":[\"Requried Parameter Missing : accessid\"]}";
        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, responseBody);

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        HashMap<String, String> access = new HashMap<>();
        access.put("users/*", "read");
        access.put("apps/*", "read");
        access.put("shared/*", "read");
        AccessPolicy accessPolicy = new AccessPolicy();
        accessPolicy.setAccess(access);

        String jsonStr = "{\"access\": { \"users/*\": \"read\", \"apps/*\": \"read\",\"shared/*\": \"read\" }}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        when(reqProcessor.process("/access/create",jsonStr,token)).thenReturn(response);

        when(JSONUtil.getJSON(accessPolicy)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = accessService.createPolicy(token, accessPolicy);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateAccessPolicy_successfully() {
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        HashMap<String, String> access = new HashMap<>();
        access.put("users/*", "read");
        access.put("apps/*", "read");
        access.put("shared/*", "read");
        AccessPolicy accessPolicy = new AccessPolicy("my-test-policy", access);

        String jsonStr = "{\"access\": {\"users/*\": \"read\", \"apps/*\": \"read\",\"shared/*\": \"read\"},\"accessid\": \"my-test-policy\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Policy updated \"]}");
        when(reqProcessor.process("/access/update",jsonStr,token)).thenReturn(response);
        when(JSONUtil.getJSON(accessPolicy)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = accessService.updatePolicy(token, accessPolicy);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_updateAccessPolicy_failure_400() {
        String responseBody = "{\"errors\":[\"Requried Parameter Missing : accessid\"]}";
        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, responseBody);

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        HashMap<String, String> access = new HashMap<>();
        access.put("users/*", "read");
        access.put("apps/*", "read");
        access.put("shared/*", "read");
        AccessPolicy accessPolicy = new AccessPolicy();
        accessPolicy.setAccess(access);
        String jsonStr = "{\"access\": {\"users/*\": \"read\", \"apps/*\": \"read\",\"shared/*\": \"read\"}\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        when(reqProcessor.process("/access/update",jsonStr,token)).thenReturn(response);
        when(JSONUtil.getJSON(accessPolicy)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = accessService.updatePolicy(token, accessPolicy);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_listAllAccessPolicy_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"policies\":[\"test-access-policy\"]}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/access/list","{}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = accessService.listAllPolices(token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getAccessPolicyInfo_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String accessId = "test-access-policy";
        String responseJson = "{\"policies\": [\"default\",\"my-test-policy\",\"root\"]}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/access","{\"accessid\":\""+accessId+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = accessService.getPolicyInfo(token, accessId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getAccessPolicyInfo_failure_400() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String accessId = "";
        String responseJson = "{\"errors\":[\"Missing accessid \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);

        ResponseEntity<String> responseEntity = accessService.getPolicyInfo(token, accessId);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteAccessPolicy_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String accessId = "test-access-policy";
        String responseJson = "{\"messages\":[\"Access is deleted\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");


        when(reqProcessor.process("/access/delete","{\"accessid\":\""+accessId+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = accessService.deletePolicyInfo(token, accessId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteAccessPolicy_failure_500() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String accessId = "test-access-policy";
        String responseJson = "";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);

        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, "");


        when(reqProcessor.process("/access/delete","{\"accessid\":\""+accessId+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = accessService.deletePolicyInfo(token, accessId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    public void test_deleteAccessPolicy_failure_400() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String accessId = "";
        String responseJson = "{\"errors\":[\"Missing accessid \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);

        ResponseEntity<String> responseEntity = accessService.deletePolicyInfo(token, accessId);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }
}