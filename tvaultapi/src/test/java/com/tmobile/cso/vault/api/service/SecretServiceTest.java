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
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.model.SafeNode;
import com.tmobile.cso.vault.api.model.Secret;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.CommonUtils;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SecretServiceTest {

    @InjectMocks
    SecretService secretService;

    @Mock
    RequestProcessor reqProcessor;

    @Mock
    CommonUtils commonUtils;

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

    UserDetails getMockUser(boolean isAdmin) {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(isAdmin);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        return userDetails;
    }

    @Test
    public void test_write_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);

        String path ="shared/mysafe01/myfolder";
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        String policies[] = {"w_shared_mysafe01"};
        UserDetails userDetails = getMockUser(false);
        userDetails.setPolicies(policies);

        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write_successWithDeleteFlag() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);

        String path ="shared/mysafe01/myfolder";
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        String policies[] = {"w_shared_mysafe01"};
        UserDetails userDetails = getMockUser(false);
        userDetails.setPolicies(policies);

        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, userDetails,"true");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write_successWithDeleteFlag_Test2() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);

        String path ="shared/mysafe01/myfolder";
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        String policies[] = {"w_shared_mysafe01"};
        UserDetails userDetails = getMockUser(false);
        userDetails.setPolicies(policies);

        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, userDetails,"true");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void test_write_Failure_Test2() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);

        String path ="shared/mysafe01/myfolder";
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        String policies[] = {"w_shared_mysafe01"};
        UserDetails userDetails = getMockUser(false);
        userDetails.setPolicies(policies);

        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        //assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write_failure_no_explicit_permission_Delete() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"No permisison to write secret in this safe\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);

        String path ="shared/mysafe01/myfolder";
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        String policies[] = {"s_shared_mysafe01"};
        UserDetails userDetails = getMockUser(false);
        userDetails.setPolicies(policies);

        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, userDetails,"true");
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write_failure_invalidPath_Delete() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(false);
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, getMockUser(false),"true");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_write_failure_no_explicit_permission() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"No permisison to write secret in this safe\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);

        String path ="shared/mysafe01/myfolder";
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        String policies[] = {"s_shared_mysafe01"};
        UserDetails userDetails = getMockUser(false);
        userDetails.setPolicies(policies);

        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, userDetails);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write_failure_invalidPath() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(false);
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, getMockUser(false));
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_readFromVault_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responsejson = "{  \"data\": {    \"secret1\": \"value1\",    \"secret2\": \"value2\"  }}";

        Response response = getMockResponse(HttpStatus.OK, true, responsejson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responsejson);

        when(reqProcessor.process("/read","{\"path\":\"shared/mysafe01/myfolder\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = secretService.readFromVault(token, "shared/mysafe01/myfolder");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteFromVault_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01/myfolder";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secrets deleted\"]}");

        when(ControllerUtil.isValidDataPath(path)).thenReturn(true);
        when(reqProcessor.process("/delete","{\"path\":\""+path+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = secretService.deleteFromVault(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteFromVault_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01/myfolder";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");

        when(ControllerUtil.isValidDataPath(path)).thenReturn(false);

        ResponseEntity<String> responseEntity = secretService.deleteFromVault(token, path);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    
    @Test
    public void test_write_success() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);

        String path ="shared/mysafe01/myfolder";
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        String policies[] = {"w_shared_mysafe01"};
        UserDetails userDetails = getMockUser(false);
        userDetails.setPolicies(policies);
        String deleteFlag = "true";
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, userDetails, deleteFlag);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    
    @Test
    public void test_write1_failure_no_explicit_permission() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"No permisison to write secret in this safe\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);

        String path ="shared/mysafe01/myfolder";
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        String policies[] = {"s_shared_mysafe01"};
        UserDetails userDetails = getMockUser(false);
        userDetails.setPolicies(policies);
        String deleteFlag = "true";
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, userDetails, deleteFlag);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write1_failure_invalidPath() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
        String deleteFlag = "true";
        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(false);
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret, getMockUser(false), deleteFlag);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSecretCount_successfully() {
        ReflectionTestUtils.setField(secretService, "safeListLimit", 50);


        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responsejson = "{\"id\":\"5PDrOhsy4ig8L3EpsJZSLAMg\",\"policies\":[\"root\"]}";

        Response response = getMockResponse(HttpStatus.OK, true, responsejson);

        when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(response);
        String[] policies = {"root"};
        try {
            when(commonUtils.getPoliciesAsArray(Mockito.any(), Mockito.any())).thenReturn(policies);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SafeNode userSafeNode = new SafeNode();
        userSafeNode.setId(TVaultConstants.USERS);
        userSafeNode.setType(TVaultConstants.SAFE);

        SafeNode userSafe = new SafeNode();
        userSafe.setId(TVaultConstants.USERS + "/safeu1");
        userSafe.setValue(TVaultConstants.USERS + "/safeu1");
        userSafe.setType(TVaultConstants.FOLDER);
        userSafe.setParentId(TVaultConstants.USERS);

        SafeNode folder1 = new SafeNode();
        folder1.setId(TVaultConstants.USERS + "/safeu1/folder1");
        folder1.setValue(TVaultConstants.USERS + "/safeu1/folder1");
        folder1.setType(TVaultConstants.FOLDER);
        folder1.setParentId(TVaultConstants.USERS + "/safeu1");

        SafeNode secret1 = new SafeNode();
        secret1.setId(TVaultConstants.USERS + "/safeu1/folder1/secret1");
        secret1.setValue("{\"data\":{\"qwe\":\"qwe\"}}");
        secret1.setType(TVaultConstants.SECRET);
        secret1.setParentId(TVaultConstants.USERS + "/safeu1/folder1");

        List<SafeNode> secrets = new ArrayList<>();
        secrets.add(secret1);
        folder1.setChildren(secrets);

        List<SafeNode> folders = new ArrayList<>();
        folders.add(folder1);
        userSafeNode.setChildren(folders);

        when(ControllerUtil.recursiveReadForCount(Mockito.any(),eq(token),Mockito.any(), Mockito.any(), eq(TVaultConstants.SAFE))).thenReturn(userSafeNode);

        SecretCount secretCount = new SecretCount();
        Map<String, Integer> userSecretCount = new HashMap<>();
        userSecretCount.put("safe1", 3);
        userSecretCount.put("safe2", 3);
        secretCount.setSafeSecretCount(userSecretCount);

        secretCount.setTotalSafes(6);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(secretCount));

        try {
            Secret data = new Secret();
            HashMap<String, String> secretDetailsMap = new HashMap<>();
            secretDetailsMap.put("abc", "abc");
            data.setPath("testpath");
            data.setDetails(secretDetailsMap);
            when(JSONUtil.getObj(Mockito.any(), Mockito.any())).thenReturn(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(reqProcessor.process(eq("/sdb/list"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"users/safe1\",\"users/safe2\"]}"));
        ResponseEntity<String> responseEntity = secretService.getSecretCount(token, TVaultConstants.USERS, 0);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSecretCount_faile_400() {
        ReflectionTestUtils.setField(secretService, "safeListLimit", 50);


        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responsejson = "{\"id\":\"5PDrOhsy4ig8L3EpsJZSLAMg\",\"policies\":[\"root\"]}";

        Response response = getMockResponse(HttpStatus.OK, true, responsejson);

        when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(response);
        String[] policies = {"root"};
        try {
            when(commonUtils.getPoliciesAsArray(Mockito.any(), Mockito.any())).thenReturn(policies);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path or offset\"]}");

        ResponseEntity<String> responseEntity = secretService.getSecretCount(token, TVaultConstants.USERS, -1);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSecretCount_failed_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responsejson = "{\"id\":\"5PDrOhsy4ig8L3EpsJZSLAMg\",\"policies\":[\"root\"]}";

        Response response = getMockResponse(HttpStatus.OK, true, responsejson);

        when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(response);
        String[] policies = {"default"};
        try {
            when(commonUtils.getPoliciesAsArray(Mockito.any(), Mockito.any())).thenReturn(policies);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access Denied: No enough permission to access this API\"]}");

        ResponseEntity<String> responseEntity = secretService.getSecretCount(token, TVaultConstants.USERS, 0);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

}
