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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
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
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AppRole;
import com.tmobile.cso.vault.api.model.AppRoleDetails;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.model.AppRoleMetadata;
import com.tmobile.cso.vault.api.model.AppRoleMetadataDetails;
import com.tmobile.cso.vault.api.model.AppRoleNameSecretId;
import com.tmobile.cso.vault.api.model.AppRoleSecretData;
import com.tmobile.cso.vault.api.model.SafeAppRoleAccess;
import com.tmobile.cso.vault.api.model.SecretData;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore( {"javax.management.*"})
public class AppRoleServiceTest {

    @InjectMocks
    AppRoleService appRoleService;

    @Mock
    RequestProcessor reqProcessor;
    
    ObjectMapper objMapper = new ObjectMapper();

    @Before
    public void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException{
    	PowerMockito.mockStatic(ControllerUtil.class);
    	PowerMockito.mockStatic(JSONUtil.class);
    	
    	Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");
 	
        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);

    }

    private Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
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
    
    UserDetails getMockUser(String username, boolean isAdmin) {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String selfServToken = "s5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername(username);
        userDetails.setAdmin(isAdmin);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(selfServToken);
        return userDetails;
    }

    @Test
    public void test_createAppRole_successfully() {

        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseList = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, 1, 100, 0);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created successfully\"]}");

        Map<String,Object> appRolesList = new HashMap<>();
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("role1");
        appRolesList.put("keys", arrayList);
        when(ControllerUtil.parseJson("{\"keys\": [ \"role1\" ]}")).thenReturn(appRolesList);

        Response responseAfterHide = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(responseAfterHide);

        when(reqProcessor.process("/auth/approle/role/create", jsonStr,token)).thenReturn(response);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(true);
        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_createAppRole_failure_400() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("selfservicesupportrole", policies, true, 1, 100, 0);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to create an approle named "+TVaultConstants.SELF_SERVICE_APPROLE_NAME+"\"]}");
        UserDetails userDetails = getMockUser(true);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_createAppRole_successfully_metadata_failure_reverted() {

        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response_403 =getMockResponse(HttpStatus.UNAUTHORIZED, true, "");
        Response responseList = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        Response responseAfterHide = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(responseAfterHide);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, 1, 100, 0);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AppRole creation failed.\"]}");

        Map<String,Object> appRolesList = new HashMap<>();
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("role1");
        appRolesList.put("keys", arrayList);
        when(ControllerUtil.parseJson("{\"keys\": [ \"role1\" ]}")).thenReturn(appRolesList);

        when(reqProcessor.process("/auth/approle/role/create", jsonStr,token)).thenReturn(response);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response_403);
        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_createAppRole_successfully_metadata_failure() {

        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response500 =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response_403 =getMockResponse(HttpStatus.UNAUTHORIZED, true, "");
        Response responseList = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        Response responseAfterHide = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(responseAfterHide);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, 1, 100, 0);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AppRole creation failed.\"]}");

        Map<String,Object> appRolesList = new HashMap<>();
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("role1");
        appRolesList.put("keys", arrayList);
        when(ControllerUtil.parseJson("{\"keys\": [ \"role1\" ]}")).thenReturn(appRolesList);

        when(reqProcessor.process("/auth/approle/role/create", jsonStr,token)).thenReturn(response);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response500);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response_403);
        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_createAppRole_failure_duplicate() {

        Response responseList = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"approle1\" ]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, 1, 100, 0);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"AppRole already exists and can't be created\"]}");

        Map<String,Object> appRolesList = new HashMap<>();
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("approle1");
        appRolesList.put("keys", arrayList);
        when(ControllerUtil.parseJson("{\"keys\": [ \"approle1\" ]}")).thenReturn(appRolesList);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);

        Response responseAfterHide = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"approle1\" ]}");
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(responseAfterHide);

        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_createAppRole_InvalidAppRoleInputs() {

        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("", policies, true, 1, 100, 0);
        String jsonStr = "{\"role_name\":\"\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values for AppRole creation\"]}");
        
        when(reqProcessor.process("/auth/approle/role/create", jsonStr,token)).thenReturn(response);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(false);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_createAppRole_Failure_404() {

        String responseBody = "{\"errors\":[\"Invalid input values for AppRole creation\"]}";
        Response response =getMockResponse(HttpStatus.NOT_FOUND, true, responseBody);
        Response responseList = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        Response responseAfterHide = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(responseAfterHide);

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("", policies, true, 1, 100, 0);
        String jsonStr = "{\"role_name\":\"\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
        
        when(reqProcessor.process("/auth/approle/role/create", jsonStr,token)).thenReturn(response);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole, userDetails);
        
        assertEquals(HttpStatus.NOT_FOUND, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_readAppRole_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"data\":{ \"bind_secret_id\": true, \"policies\": [\"test-access-policy\"]}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\":{ \"bind_secret_id\": true, \"policies\": [\"test-access-policy\"]}}");
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        String appRole = "approle1";
        
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+appRole+"\"}",token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRole(token, appRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_readAppRole_failure_400() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to read this AppRole\"]}");
        String appRole = "selfservicesupportrole";
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRole(token, appRole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_readAppRoleId_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"data\":{ \"role_id\": \"f1f72163-287e-b3a4-1fdc-fd21a35c7d57\"}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\":{ \"role_id\": \"f1f72163-287e-b3a4-1fdc-fd21a35c7d57\"}}");
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        String appRoleName = "approle1";
        
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+appRoleName+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleRoleId(token, appRoleName);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
        
    }

    @Test
    public void test_createSecretId_successfully() {
        String responseJson = "{\"data\":{ \"secret_id\": \"5973a6de-38c1-0402-46a3-6d76e38b773c\", \"secret_id_accessor\": \"cda12712-9845-c271-aeb1-833681fac295\"}}";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret ID created for AppRole\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AppRoleSecretData appRoleSecretData = new AppRoleSecretData("approle1", new SecretData("dev", "appl"));

        String jsonStr = "{\"role_name\":\"approle1\",\"data\":{\"env\":\"dev\",\"appname\":\"appl\"}}";
        
        when(reqProcessor.process("/auth/approle/secretid/create", jsonStr,token)).thenReturn(response);
        when(ControllerUtil.convertAppRoleSecretIdToLowerCase(Mockito.any())).thenReturn(jsonStr);
        when(JSONUtil.getJSON(appRoleSecretData)).thenReturn(jsonStr);
        
        ResponseEntity<String> responseEntityActual = appRoleService.createsecretId(token, appRoleSecretData);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_createSecretId_failure() {
        String responseJson = "{\"error\":[\"Internal Server Error\"]}";
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AppRoleSecretData appRoleSecretData = new AppRoleSecretData("approle1", new SecretData("dev", "appl"));

        String jsonStr = "{\"role_name\":\"approle1\",\"data\":{\"env\":\"dev\",\"appname\":\"appl\"}}";

        when(reqProcessor.process("/auth/approle/secretid/create", jsonStr,token)).thenReturn(response);
        when(ControllerUtil.convertAppRoleSecretIdToLowerCase(Mockito.any())).thenReturn(jsonStr);
        when(JSONUtil.getJSON(appRoleSecretData)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntityActual = appRoleService.createsecretId(token, appRoleSecretData);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_readSecretId_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"data\":{ \"secret_id\": \"5973a6de-38c1-0402-46a3-6d76e38b773c\", \"secret_id_accessor\": \"cda12712-9845-c271-aeb1-833681fac295\"}}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        String appRoleName = "approle1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        
        when(reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+appRoleName+"\"}",token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleSecretId(token, appRoleName);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteAppRole_successfully() throws Exception{

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseOK =getMockResponse(HttpStatus.OK, true, "{\"createdBy\":\"safeadmin\"}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted\"]}");
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";

        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseOK);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("createdBy", "safeadmin");
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(responseMap);
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(response);
        // START - isAllowed
        String approleusername="safeadmin";
        String role_name=appRole.getRole_name();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
    	String appRoleResponseJspn = "{\"path\":\"metadata/approle/approle1\",\"data\":{\"name\":\"approle1\",\"createdBy\":\"safeadmin\"}}";
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> appRoleMetadataMap = new HashMap<>();
        appRoleMetadataMap.put("createdBy",approleusername);
        appRoleResponseMap.put ("data", appRoleMetadataMap);
        when(ControllerUtil.parseJson(appRoleResponseJspn)).thenReturn(appRoleResponseMap);
        // END - isAllowed
        Response permissionResponse =getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(appRole.getRole_name(), token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH)).thenReturn(permissionResponse);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteAppRole_successfully_meta_failure() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response404 =getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response responseOK =getMockResponse(HttpStatus.OK, true, "{\"createdBy\":\"safeadmin\"}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted, metadata delete failed\"]}");
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";

        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseOK);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("createdBy", "safeadmin");
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(responseMap);
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(response404);
        // START - isAllowed
        String approleusername="safeadmin";
        String role_name=appRole.getRole_name();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
    	String appRoleResponseJspn = "{\"path\":\"metadata/approle/approle1\",\"data\":{\"name\":\"approle1\",\"createdBy\":\"safeadmin\"}}";
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> appRoleMetadataMap = new HashMap<>();
        appRoleMetadataMap.put("createdBy",approleusername);
        appRoleResponseMap.put ("data", appRoleMetadataMap);
        when(ControllerUtil.parseJson(appRoleResponseJspn)).thenReturn(appRoleResponseMap);
        // END - isAllowed
        Response permissionResponse =getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(appRole.getRole_name(), token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH)).thenReturn(permissionResponse);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteAppRole_successfully_normaluser() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseOK =getMockResponse(HttpStatus.OK, true, "{\"data\":{\"createdBy\":\"normaluser\"}}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted\"]}");
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";

        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(false);
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseOK);
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("createdBy", "normaluser");
        responseMap.put("data", data);
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(responseMap);
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(response);
        Response permissionResponse =getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(appRole.getRole_name(), token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH)).thenReturn(permissionResponse);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteAppRole_successfully_metadata404() throws Exception{

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response404 =getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted\"]}");
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";

        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(response404);
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("createdBy", "normaluser");
        responseMap.put("data", data);
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(responseMap);
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(response);
        // START - isAllowed
        String approleusername="safeadmin";
        String role_name=appRole.getRole_name();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
        // END - isAllowed
        Response permissionResponse =getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(appRole.getRole_name(), token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH)).thenReturn(permissionResponse);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteAppRole_failure_metadata_403() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response403 =getMockResponse(HttpStatus.UNAUTHORIZED, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to remove the role\"]}");
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";

        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(response403);
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("createdBy", "normaluser");
        responseMap.put("data", data);
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(responseMap);
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(response);
        Response permissionResponse =getMockResponse(HttpStatus.UNAUTHORIZED, true, "Access denied: no permission to remove the role");
        when(ControllerUtil.canDeleteRole(appRole.getRole_name(), token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH)).thenReturn(permissionResponse);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteAppRole_failure() throws Exception{

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        String responseJson = "{\"error\":[\"Internal Server Error\"]}";
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        Response responseOK =getMockResponse(HttpStatus.OK, true, "{\"createdBy\":\"safeadmin\"}");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";

        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseOK);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("createdBy", "safeadmin");
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(responseMap);
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(response);
        // START - isAllowed
        String approleusername="safeadmin";
        String role_name=appRole.getRole_name();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
    	String appRoleResponseJspn = "{\"path\":\"metadata/approle/approle1\",\"data\":{\"name\":\"approle1\",\"createdBy\":\"safeadmin\"}}";
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> appRoleMetadataMap = new HashMap<>();
        appRoleMetadataMap.put("createdBy",approleusername);
        appRoleResponseMap.put ("data", appRoleMetadataMap);
        when(ControllerUtil.parseJson(appRoleResponseJspn)).thenReturn(appRoleResponseMap);
        // END - isAllowed
        Response permissionResponse =getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(appRole.getRole_name(), token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH)).thenReturn(permissionResponse);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole, userDetails);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteAppRole_failure_500() throws Exception{

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        String responseJson = "{\"error\":[\"Error reading role info\"]}";
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        Response responseOK =getMockResponse(HttpStatus.OK, true, "{\"createdBy\":\"safeadmin\"}");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";

        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseOK);
        Map<String, Object> responseMap = new HashMap<>();
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(responseMap);
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(response);
        // START - isAllowed
        String approleusername="safeadmin";
        String role_name=appRole.getRole_name();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
    	String appRoleResponseJspn = "{\"path\":\"metadata/approle/approle1\",\"data\":{\"name\":\"approle1\",\"createdBy\":\"safeadmin\"}}";
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> appRoleMetadataMap = new HashMap<>();
        appRoleMetadataMap.put("createdBy",approleusername);
        appRoleResponseMap.put ("data", appRoleMetadataMap);
        when(ControllerUtil.parseJson(appRoleResponseJspn)).thenReturn(appRoleResponseMap);
        // END - isAllowed
        Response permissionResponse =getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(appRole.getRole_name(), token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH)).thenReturn(permissionResponse);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole, userDetails);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_deleteAppRole_failure_403() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "selfservicesupportrole";
        String responseJson = "{\"errors\":[\"Access denied: no permission to remove this AppRole\"]}";
        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteSecretId_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        AppRoleNameSecretId appRoleNameSecretId = new AppRoleNameSecretId(appRoleId, "5973a6de-38c1-0402-46a3-6d76e38b773c");
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String jsonStr = "{\"role_name\":\"approle1\",\"secret_id\":\"5973a6de-38c1-0402-46a3-6d76e38b773c\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SecretId for AppRole deleted\"]}");
        
        when(JSONUtil.getJSON(appRoleNameSecretId)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/secret/delete",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteSecretId(token, appRoleNameSecretId);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
        
    }

    @Test
    public void test_deleteSecretId_failure_400() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "selfservicesupportrole";
        AppRoleNameSecretId appRoleNameSecretId = new AppRoleNameSecretId(appRoleId, "5973a6de-38c1-0402-46a3-6d76e38b773c");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to delete secretId for this approle\"]}");

        ResponseEntity<String> responseEntityActual = appRoleService.deleteSecretId(token, appRoleNameSecretId);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteSecretId_failure() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        AppRoleNameSecretId appRoleNameSecretId = new AppRoleNameSecretId(appRoleId, "5973a6de-38c1-0402-46a3-6d76e38b773c");
        String jsonStr = "{\"role_name\":\"approle1\",\"secret_id\":\"5973a6de-38c1-0402-46a3-6d76e38b773c\"}";

        String responseJson = "{\"error\":[\"Internal Server Error\"]}";
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);

        when(JSONUtil.getJSON(appRoleNameSecretId)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/secret/delete",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteSecretId(token, appRoleNameSecretId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_succssfully() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(reqProcessor.process(eq("/auth/approle/role/create"),Mockito.any(),eq(token))).thenReturn(configureAppRoleResponse);
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  appRoleService.associateApprole(token, safeAppRoleAccess);
        
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure_approle_not_exists() throws Exception {

        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Non existing role name. Please configure approle as first step\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(reqProcessor.process(eq("/auth/approle/role/create"),Mockito.any(),eq(token))).thenReturn(configureAppRoleResponse);
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  appRoleService.associateApprole(token, safeAppRoleAccess);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_succssfully_new_meta() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response updateMetadataResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(reqProcessor.process(eq("/auth/approle/role/create"),Mockito.any(),eq(token))).thenReturn(configureAppRoleResponse);
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

        Map<String,String> params = new HashMap<String,String>();
        params.put("type", "app-roles");
        params.put("name","approle1");
        params.put("path","shared/mysafe01");
        params.put("access","write");

        //when(ControllerUtil.updateMetadata(Mockito.anyMap(),eq(token))).thenReturn(updateMetadataResponse_404);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),eq(token))).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return updateMetadataResponse;

                return updateMetadataResponse_404;
            }
        });
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  appRoleService.associateApprole(token, safeAppRoleAccess);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failed_configuration() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Contact Admin \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response updateMetadataResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(reqProcessor.process(eq("/auth/approle/role/create"),Mockito.any(),eq(token))).thenReturn(configureAppRoleResponse);
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

        Map<String,String> params = new HashMap<String,String>();
        params.put("type", "app-roles");
        params.put("name","approle1");
        params.put("path","shared/mysafe01");
        params.put("access","write");

        //when(ControllerUtil.updateMetadata(Mockito.anyMap(),eq(token))).thenReturn(updateMetadataResponse_404);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),eq(token))).thenReturn(updateMetadataResponse_404);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));
        params.put("path","shared/mysafe02");
        when(ControllerUtil.updateMetadata(params,token)).thenReturn(updateMetadataResponse);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  appRoleService.associateApprole(token, safeAppRoleAccess);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
    }

    @Test
    public void test_AssociateAppRole_failed() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle :approle1 failed to be associated with SDB\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"Internal server error\"]}");
        Response updateMetadataResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(reqProcessor.process(eq("/auth/approle/role/create"),Mockito.any(),eq(token))).thenReturn(configureAppRoleResponse);
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  appRoleService.associateApprole(token, safeAppRoleAccess);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
    }

    @Test
    public void test_AssociateAppRole_failed_400() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);

        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(false);
        ResponseEntity<String> responseEntityActual =  appRoleService.associateApprole(token, safeAppRoleAccess);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertTrue(responseEntityActual.getBody().contains("Invalid input values"));

    }

    @Test
    public void test_AssociateAppRole_failed_403() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle : approle1 failed to be associated with SDB.. Invalid Path specified\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);

        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(false);
        ResponseEntity<String> responseEntityActual =  appRoleService.associateApprole(token, safeAppRoleAccess);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertTrue(responseEntityActual.getBody().contains("Approle :approle1 failed to be associated with SDB.. Invalid Path specified"));

    }

    @Test
    public void test_loginWithApprole_successfully() {
        String expectedLoginResponse = "{  \"auth\": {   \"renewable\": true,    \"lease_duration\": 2764800,    \"metadata\": {},    \"policies\": [      \"default\"    ],    \"accessor\": \"5d7fb475-07cb-4060-c2de-1ca3fcbf0c56\",    \"client_token\": \"98a4c7ab-b1fe-361b-ba0b-e307aacfd587\"  }}";
        Response response =getMockResponse(HttpStatus.OK, true, expectedLoginResponse);
        AppRoleIdSecretId appRoleIdSecretId = new AppRoleIdSecretId("approle1", "5973a6de-38c1-0402-46a3-6d76e38b773c");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedLoginResponse);
        String jsonStr = "{\"role_id\":\"approle1\",\"secret_id\":\"5973a6de-38c1-0402-46a3-6d76e38b773c\"}";
        
        when(JSONUtil.getJSON(appRoleIdSecretId)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/login",jsonStr,"")).thenReturn(response);

        ResponseEntity<String> responseEntityActual = appRoleService.login(appRoleIdSecretId);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
        
    }

    @Test
    public void test_listAppRoles_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\r\n" + 
        		"  \"keys\": [\r\n" + 
        		"    \"testapprole01\"\r\n" + 
        		"  ]\r\n" + 
        		"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser("testuser1", false);
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        Response responseAfterHide = response;
        String _path = "metadata/approle_users/" + userDetails.getUsername();
        String jsonStr = "{\"path\":\""+_path+"\"}";
        when(reqProcessor.process("/auth/approles/rolesbyuser/list", jsonStr,userDetails.getSelfSupportToken())).thenReturn(response);
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(responseAfterHide);
        ResponseEntity<String> responseEntityActual = appRoleService.listAppRoles(token, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
    
    @Test
    public void test_listAppRoles_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\r\n" + 
        		"  \"error\": \r\n" + 
        		"    \"\"\r\n" + 
        		"  \r\n" + 
        		"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);
        UserDetails userDetails = getMockUser("testuser1", false);
        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        String _path = "metadata/approle_users/" + userDetails.getUsername();
        String jsonStr = "{\"path\":\""+_path+"\"}";
        when(reqProcessor.process("/auth/approles/rolesbyuser/list", jsonStr,userDetails.getSelfSupportToken())).thenReturn(response);
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(response);
        ResponseEntity<String> responseEntityActual = appRoleService.listAppRoles(token, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
    
    @Test
    public void test_readRoleId_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testapprole01";
        String roleId = "generated-role-id";
        String responseJson = "{\"data\":{ \"role_id\": \"generated-role-id\"}}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", roleId);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"data\":{ \"role_id\": \"generated-role-id\"}}")).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/readRoleID", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        String actualRoleId = appRoleService.readRoleId(token, role_name);
        assertEquals(roleId, actualRoleId);
    }
    
    @Test
    public void test_readRoleId_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testapprole01";
        String roleId = "generated-role-id";
        String responseJson = "{\"data\":{ \"role_id\": \"generated-role-id\"}}";
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, responseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", roleId);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"data\":{ \"role_id\": \"generated-role-id\"}}")).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/readRoleID", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        String actualRoleId = appRoleService.readRoleId(token, role_name);
        assertEquals(null, actualRoleId);
    }
    
    @Test
    public void test_readAccessorIds_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testapprole01";
        String responseJson = "{\r\n" + 
        		"  \"keys\": [\r\n" + 
        		"    \"generated-accessor-id1\"\r\n" + 
        		"  ]\r\n" + 
        		"}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        ArrayList<String> accessorIds = new ArrayList<String>();
        accessorIds.add("generated-accessor-id1");
        responseMap.put("keys", accessorIds);
        when(ControllerUtil.parseJson(responseJson)).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/accessors/list", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        List<String> actualAccessorIds = appRoleService.readAccessorIds(token, role_name);
        assertNotNull(actualAccessorIds);
        assertEquals("generated-accessor-id1", (String)actualAccessorIds.get(0));
    }
    
    @Test
    public void test_readAccessorIds_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testapprole01";
        String responseJson = "{\r\n" + 
        		"  \"error\": [\r\n" + 
        		"    \"\"\r\n" + 
        		"  ]\r\n" + 
        		"}";
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, responseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        ArrayList<String> accessorIds = new ArrayList<String>();
        accessorIds.add("generated-accessor-id1");
        responseMap.put("keys", accessorIds);
        when(ControllerUtil.parseJson(responseJson)).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/accessors/list", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        List<String> actualAccessorIds = appRoleService.readAccessorIds(token, role_name);
        assertEquals(null, actualAccessorIds);
    }
    
    @Test
    public void test_readAppRoleMetadata_successfully() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testapprole01";
        String username = "testuser1";
        String _path = "metadata/approle/" + role_name;
        AppRoleMetadata approleMetadataExpected = new AppRoleMetadata();
        approleMetadataExpected.setPath(_path);
        AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails();
        appRoleMetadataDetails.setCreatedBy(username);
        appRoleMetadataDetails.setName(role_name);
        approleMetadataExpected.setAppRoleMetadataDetails(appRoleMetadataDetails);
        
        String responseJson = new ObjectMapper().writeValueAsString(approleMetadataExpected);
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> appRoleMetadataMap = new HashMap<>();
        appRoleMetadataMap.put("createdBy",username);
        responseMap.put("data", appRoleMetadataMap);
        when(ControllerUtil.parseJson(responseJson)).thenReturn(responseMap);
        
        when(reqProcessor.process("/read", "{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        AppRoleMetadata appRoleMetadataExpected = appRoleService.readAppRoleMetadata(token, role_name);
        assertNotNull(appRoleMetadataExpected);
        assertNotNull(appRoleMetadataExpected.getAppRoleMetadataDetails());
        assertEquals(username, appRoleMetadataExpected.getAppRoleMetadataDetails().getCreatedBy());
    }
    
    @Test
    public void test_readAppRoleMetadata_failure() throws Exception{
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testapprole01";
        String username = "testuser1";
        AppRoleMetadata approleMetadataExpected = new AppRoleMetadata();
        String _path = "metadata/approle/" + role_name;
        approleMetadataExpected.setPath(_path);
        AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails();
        appRoleMetadataDetails.setCreatedBy(username);
        appRoleMetadataDetails.setName(role_name);
        approleMetadataExpected.setAppRoleMetadataDetails(appRoleMetadataDetails);
        
        String responseJson = new ObjectMapper().writeValueAsString(approleMetadataExpected);
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, responseJson);
        

        when(ControllerUtil.parseJson(responseJson)).thenReturn(null);
        
        when(reqProcessor.process("/read", "{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        AppRoleMetadata appRoleMetadataExpected = appRoleService.readAppRoleMetadata(token, role_name);
        assertEquals(null, appRoleMetadataExpected);
    }
    
    @Test
    public void test_readAppRoleBasicDetails_successfully() throws Exception{
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testapprole01";
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        
        AppRole appRoleExpected = new AppRole(role_name, policies, true, 0, 0, 0);
        
        String responseJson = new ObjectMapper().writeValueAsString(appRoleExpected);
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        responseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        
        when(ControllerUtil.parseJson(responseJson)).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        AppRole approleActual = appRoleService.readAppRoleBasicDetails(token, role_name);
        assertNotNull(approleActual);
        assertNotNull(approleActual.getRole_name());
        assertEquals(role_name, approleActual.getRole_name());
    }
    
    @Test
    public void test_readAppRoleBasicDetails_failure() throws Exception{
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testapprole01";
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        
        AppRole appRoleExpected = new AppRole(role_name, policies, true, 0, 0, 0);
        
        String responseJson = new ObjectMapper().writeValueAsString(appRoleExpected);
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, responseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        responseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        
        when(ControllerUtil.parseJson(responseJson)).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        AppRole approleActual = appRoleService.readAppRoleBasicDetails(token, role_name);
        assertEquals(null, approleActual);
    }
    
    @Test
    public void test_readAppRoleRoleId_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\r\n" + 
        		"  \"data\": {\r\n" + 
        		"    \"role_id\": \"generated-role-id\"\r\n" + 
        		"  }\r\n" + 
        		"}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        String appRoleName = "approle1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+appRoleName+"\"}",token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleRoleId(token, appRoleName);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    @Test
    public void test_readAppRoleRoleId_failure() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"error\":[\"Internal Server Error\"]}";
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        String appRoleName = "approle1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);
        
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+appRoleName+"\"}",token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleRoleId(token, appRoleName);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_readAppRoleSecretId_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\r\n" + 
        		"  \"data\": {\r\n" + 
        		"    \"secret_id\": \"generated-secret-id\",\r\n" + 
        		"    \"secret_id_accessor\": \"generated-accessor-id\"\r\n" + 
        		"  }\r\n" + 
        		"}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        String appRoleName = "approle1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        
        when(reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+appRoleName+"\"}",token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleSecretId(token, appRoleName);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_readAppRoleSecretId_failure() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"error\":[\"Internal Server Error\"]}";
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        String appRoleName = "approle1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);
        
        when(reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+appRoleName+"\"}",token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleSecretId(token, appRoleName);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    private String getAppRoleMetadataJSON(String path, String username, String role_name ) throws Exception {
        return objMapper.writeValueAsString(getAppRoleMetadata(path, username, role_name));
    }
    
    private AppRoleMetadata getAppRoleMetadata(String path, String username, String role_name ) throws Exception {
        AppRoleMetadata approleMetadata = new AppRoleMetadata();
        approleMetadata.setPath(path);
        AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails();
        appRoleMetadataDetails.setCreatedBy(username);
        appRoleMetadataDetails.setName(role_name);
        approleMetadata.setAppRoleMetadataDetails(appRoleMetadataDetails);
        return approleMetadata;
    }
    @Test
    public void test_readAppRoleRoleId_WithUserDetails_successfully() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        
        // START - AppRole exists
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        // END - AppRole exists
        // START - isAllowed
        String approleusername=username;
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
        // END - isAllowed
        
        String responseJson = "{\r\n" + 
        		"  \"data\": {\r\n" + 
        		"    \"role_id\": \"generated-role-id\"\r\n" + 
        		"  }\r\n" + 
        		"}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(response.getResponse());

        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", username);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"path\":\""+path+"\",\"data\":{\"name\":\""+role_name+"\",\"createdBy\":\""+username+"\"}}")).thenReturn(responseMap);
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleRoleId(token, role_name, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    @Test
    public void test_readAppRoleRoleId_WithUserDetails_failure_BAD_REQUEST() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        
        // START - AppRole exists
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        // END - AppRole exists
        // START - isAllowed
        String approleusername="nonexisting";
        Response approleMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
        // END - isAllowed
        
        String responseJson = "{\"errors\":[\"Access denied: You don't have enough permission to read the role_id associated with the AppRole\"]}";
        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.getResponse());

        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", username);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"path\":\""+path+"\",\"data\":{\"name\":\""+role_name+"\",\"createdBy\":\""+username+"\"}}")).thenReturn(responseMap);
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleRoleId(token, role_name, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
    @Test
    public void test_readAppRoleRoleId_WithUserDetails_failure() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "selfservicesupportrole";
        String responseJson = "{\"errors\":[\"Access denied: You don't have enough permission to read the role_id associated with the AppRole\"]}";
        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.getResponse());

        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleRoleId(token, role_name, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    @Test
    public void test_readAppRoleRoleId_WithUserDetails_failure_NonExistingRole() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "approle1x";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        
        // START - AppRole exists

        AppRole appRole = null;
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.NOT_FOUND, true, appRoleResponseJson);
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",null);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        // END - AppRole exists
        // START - isAllowed
        String approleusername=username;
        Response approleMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
        // END - isAllowed
        
        String responseJson = "{\"errors\":[\"AppRole doesn't exist\"]}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(response.getResponse());

        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", username);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"path\":\""+path+"\",\"data\":{\"name\":\""+role_name+"\",\"createdBy\":\""+username+"\"}}")).thenReturn(responseMap);
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleRoleId(token, role_name, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
    @Test
    public void test_readAppRoleSecretId_WithUserDetails_failure() throws Exception {
    	
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "selfservicesupportrole";
        String responseJson = "{\"errors\":[\"Access denied: You don't have enough permission to read the secret_id associated with the AppRole\"]}";
        
        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.getResponse());
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleSecretId(token, role_name, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_readAppRoleSecretId_WithUserDetails_successfully() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        
        // START - AppRole exists
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        // END - AppRole exists
        // START - isAllowed
        String approleusername=username;
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
        // END - isAllowed
        
        String responseJson = "{\r\n" + 
        		"  \"data\": {\r\n" + 
        		"    \"secret_id\": \"generated-secret-id\",\r\n" + 
        		"    \"secret_id_accessor\": \"generated-accessor-id\"\r\n" + 
        		"  }\r\n" + 
        		"}";
        
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        when(reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(response.getResponse());
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", username);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"path\":\""+path+"\",\"data\":{\"name\":\""+role_name+"\",\"createdBy\":\""+username+"\"}}")).thenReturn(responseMap);

        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleSecretId(token, role_name, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
    
    @Test
    public void test_readAppRoleSecretId_WithUserDetails_failure_BAD_REQUEST() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        
        // START - AppRole exists
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        // END - AppRole exists
        // START - isAllowed
        String approleusername="nonexisting";
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, approleusername, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
        // END - isAllowed
        
        String responseJson = "{\"errors\":[\"Access denied: You don't have enough permission to read the secret_id associated with the AppRole\"]}";
        
        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        when(reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.getResponse());
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", username);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"path\":\""+path+"\",\"data\":{\"name\":\""+role_name+"\",\"createdBy\":\""+username+"\"}}")).thenReturn(responseMap);

        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleSecretId(token, role_name, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_readSecretIdAccessors_WithUserDetails_successfully() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\r\n" + 
        		"  \"keys\": [\r\n" + 
        		"    \"generated-accessor-id1\",\r\n" + 
        		"    \"generated-accessor-id2\"\r\n" + 
        		"  ]\r\n" + 
        		"}";
    	String role_id_response = "{\n" + 
    			"  \"data\": {\n" + 
    			"    \"role_id\": \"generated-role-id\"\n" + 
    			"  }\n" + 
    			"}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", username);
        responseMap.put("data", roleIdDataMap);
        
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        
        when(reqProcessor.process("/auth/approle/role/accessors/list","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(response);
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn
        (getMockResponse(HttpStatus.OK, true, role_id_response));
        when(ControllerUtil.parseJson(role_id_response)).thenReturn(responseMap);
        
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, username, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);

        when(ControllerUtil.parseJson("{\"path\":\""+path+"\",\"data\":{\"name\":\""+role_name+"\",\"createdBy\":\""+username+"\"}}")).thenReturn(responseMap);
        ResponseEntity<String> responseEntityActual = appRoleService.readSecretIdAccessors(token, role_name, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_readSecretIdAccessors_WithUserDetails_failure() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"errors\":[\"Unable to read AppRole. AppRole does not exist.\"]}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        
        when(reqProcessor.process("/auth/approle/role/accessors/list","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(response);
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn
        (getMockResponse(HttpStatus.NOT_FOUND, true, responseJson));
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, username, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", username);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"path\":\""+path+"\",\"data\":{\"name\":\""+role_name+"\",\"createdBy\":\""+username+"\"}}")).thenReturn(responseMap);
        ResponseEntity<String> responseEntityActual = appRoleService.readSecretIdAccessors(token, role_name, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    @Test
    public void test_readSecretIdAccessors_WithUserDetails_BAD_REQUEST() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser("testuser1", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;

    	String role_id_response = "{\n" + 
    			"  \"data\": {\n" + 
    			"    \"role_id\": \"generated-role-id\"\n" + 
    			"  }\n" + 
    			"}";
        Map<String, Object> roleIdResponseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", username);
        roleIdResponseMap.put("data", roleIdDataMap);
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(getMockResponse(HttpStatus.OK, true, role_id_response));
        when(ControllerUtil.parseJson(role_id_response)).thenReturn(roleIdResponseMap);
        
        Response approleMetadataResponse = getMockResponse(HttpStatus.OK, true, getAppRoleMetadataJSON(path, username, role_name));
        when(reqProcessor.process("/read","{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(approleMetadataResponse);
        Map<String, Object> responseMap = new HashMap<>();
        roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", "generated-id");
        roleIdDataMap.put("createdBy", "testuser2");
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"path\":\""+path+"\",\"data\":{\"name\":\""+role_name+"\",\"createdBy\":\""+username+"\"}}")).thenReturn(responseMap);

        String responseJson = "{\"errors\":[\"Access denied: You don't have enough permission to read the accessors of SecretIds associated with the AppRole\"]}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);
        ResponseEntity<String> responseEntityActual = appRoleService.readSecretIdAccessors(token, role_name, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    @Test
    public void test_readAppRoleDetails_WithUserDetails_successfully() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        UserDetails userDetails = getMockUser("testuser1", true);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        String roleId="generated-role-id";
        
        
        
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(appRoleResponse);
        
        
        AppRoleMetadata approleMetadata = new AppRoleMetadata();
        approleMetadata.setPath(path);
        AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails();
        appRoleMetadataDetails.setCreatedBy(username);
        appRoleMetadataDetails.setName(role_name);
        approleMetadata.setAppRoleMetadataDetails(appRoleMetadataDetails);
        
        String appRoleMetadataResponseJson = new ObjectMapper().writeValueAsString(approleMetadata);
        Response appRoleMetadataResponse = getMockResponse(HttpStatus.OK, true, appRoleMetadataResponseJson);
        
        Map<String, Object> appRoleMetadatResponseMap = new HashMap<>();
        Map<String, Object> appRoleMetadataMap = new HashMap<>();
        appRoleMetadataMap.put("createdBy",username);
        appRoleMetadatResponseMap.put("data", appRoleMetadataMap);
        when(ControllerUtil.parseJson(appRoleMetadataResponseJson)).thenReturn(appRoleMetadatResponseMap);
        when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}",token)).thenReturn(appRoleMetadataResponse);
        
        String roleIdResponseJson = "{\"data\":{ \"role_id\": \""+roleId+"\"}}";
        Response roleIdResponse = getMockResponse(HttpStatus.OK, true, roleIdResponseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", roleId);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"data\":{ \"role_id\": \""+roleId+"\"}}")).thenReturn(responseMap);
        when(reqProcessor.process("/auth/approle/role/readRoleID", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(roleIdResponse);
        
        String accessorIdResponseJson = "{\r\n" + 
        		"  \"keys\": [\r\n" + 
        		"    \"generated-accessor-id1\"\r\n" + 
        		"  ]\r\n" + 
        		"}";
        Response accessorIdResponse = getMockResponse(HttpStatus.OK, true, accessorIdResponseJson);
        
        Map<String, Object> accessorIdResponseMap = new HashMap<>();
        ArrayList<String> accessorIds = new ArrayList<String>();
        accessorIds.add("generated-accessor-id1");
        accessorIdResponseMap.put("keys", accessorIds);
        when(ControllerUtil.parseJson(accessorIdResponseJson)).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/accessors/list", "{\"role_name\":\""+role_name+"\"}",token)).thenReturn(accessorIdResponse);
        
		AppRoleDetails appRoleDetails = new AppRoleDetails();
		appRoleDetails.setAppRole(appRole);
		appRoleDetails.setRole_id(roleId);
		appRoleDetails.setAppRoleMetadata(approleMetadata);
		if (!CollectionUtils.isEmpty(accessorIds)) {
			appRoleDetails.setAccessorIds(accessorIds.toArray(new String[accessorIds.size()]));
		}
		String appRoleDetailsJson = objMapper.writeValueAsString(appRoleDetails);
        String appRoleDetailsResponseJson = objMapper.writeValueAsString(appRoleDetails);
        Response appRoleDetailsResponse =getMockResponse(HttpStatus.OK, true, appRoleDetailsResponseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(appRoleDetailsResponse.getHttpstatus()).body(appRoleDetailsResponse.getResponse());
        when(JSONUtil.getJSON(Mockito.any(AppRoleDetails.class))).thenReturn(appRoleDetailsJson);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleDetails(token, role_name, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    @Test
    public void test_readAppRoleDetails_WithUserDetails_admin_success() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        UserDetails userDetails = getMockUser("testuser2", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        String roleId="generated-role-id";
        
        
        
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        
        
        AppRoleMetadata approleMetadata = new AppRoleMetadata();
        approleMetadata.setPath(path);
        AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails();
        appRoleMetadataDetails.setCreatedBy(username);
        appRoleMetadataDetails.setName(role_name);
        approleMetadata.setAppRoleMetadataDetails(appRoleMetadataDetails);
        
        String appRoleMetadataResponseJson = new ObjectMapper().writeValueAsString(approleMetadata);
        Response appRoleMetadataResponse = getMockResponse(HttpStatus.OK, true, appRoleMetadataResponseJson);
        
        Map<String, Object> appRoleMetadatResponseMap = new HashMap<>();
        Map<String, Object> appRoleMetadataMap = new HashMap<>();
        appRoleMetadataMap.put("createdBy",username);
        appRoleMetadatResponseMap.put("data", appRoleMetadataMap);
        when(ControllerUtil.parseJson(appRoleMetadataResponseJson)).thenReturn(appRoleMetadatResponseMap);
        when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleMetadataResponse);
        
        String roleIdResponseJson = "{\"data\":{ \"role_id\": \""+roleId+"\"}}";
        Response roleIdResponse = getMockResponse(HttpStatus.OK, true, roleIdResponseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", roleId);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"data\":{ \"role_id\": \""+roleId+"\"}}")).thenReturn(responseMap);
        when(reqProcessor.process("/auth/approle/role/readRoleID", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(roleIdResponse);
        
        String accessorIdResponseJson = "{\r\n" + 
        		"  \"keys\": [\r\n" + 
        		"    \"generated-accessor-id1\"\r\n" + 
        		"  ]\r\n" + 
        		"}";
        Response accessorIdResponse = getMockResponse(HttpStatus.OK, true, accessorIdResponseJson);
        
        Map<String, Object> accessorIdResponseMap = new HashMap<>();
        ArrayList<String> accessorIds = new ArrayList<String>();
        accessorIds.add("generated-accessor-id1");
        accessorIdResponseMap.put("keys", accessorIds);
        when(ControllerUtil.parseJson(accessorIdResponseJson)).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/accessors/list", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(accessorIdResponse);
        
		AppRoleDetails appRoleDetails = new AppRoleDetails();
		appRoleDetails.setAppRole(appRole);
		appRoleDetails.setRole_id(roleId);
		appRoleDetails.setAppRoleMetadata(approleMetadata);
		if (!CollectionUtils.isEmpty(accessorIds)) {
			appRoleDetails.setAccessorIds(accessorIds.toArray(new String[accessorIds.size()]));
		}
		String appRoleDetailsJson = objMapper.writeValueAsString(appRoleDetails);
        String appRoleDetailsResponseJson = objMapper.writeValueAsString(appRoleDetails);
        Response appRoleDetailsResponse =getMockResponse(HttpStatus.OK, true, appRoleDetailsResponseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(appRoleDetailsResponse.getHttpstatus()).body(appRoleDetailsResponse.getResponse());
        when(JSONUtil.getJSON(Mockito.any(AppRoleDetails.class))).thenReturn(appRoleDetailsJson);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleDetails(token, role_name, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    @Test
    public void test_readAppRoleDetails_WithUserDetails_failure() throws Exception {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        UserDetails userDetails = getMockUser("testuser2", false);
        String role_name = "approle1";
        String username = userDetails.getUsername();
        String path = TVaultConstants.APPROLE_METADATA_MOUNT_PATH + "/" + role_name;
        String roleId="generated-role-id";
        
        
        
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        
        
        AppRoleMetadata approleMetadata = new AppRoleMetadata();
        approleMetadata.setPath(path);
        AppRoleMetadataDetails appRoleMetadataDetails = new AppRoleMetadataDetails();
        appRoleMetadataDetails.setCreatedBy(username);
        appRoleMetadataDetails.setName(role_name);
        approleMetadata.setAppRoleMetadataDetails(appRoleMetadataDetails);
        
        String appRoleMetadataResponseJson = new ObjectMapper().writeValueAsString(approleMetadata);
        Response appRoleMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, appRoleMetadataResponseJson);
        
        Map<String, Object> appRoleMetadatResponseMap = new HashMap<>();
        Map<String, Object> appRoleMetadataMap = new HashMap<>();
        appRoleMetadataMap.put("createdBy",username);
        appRoleMetadatResponseMap.put("data", appRoleMetadataMap);
        when(ControllerUtil.parseJson(appRoleMetadataResponseJson)).thenReturn(appRoleMetadatResponseMap);
        when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleMetadataResponse);
        
        String roleIdResponseJson = "{\"data\":{ \"role_id\": \""+roleId+"\"}}";
        Response roleIdResponse = getMockResponse(HttpStatus.OK, true, roleIdResponseJson);
        
        Map<String, Object> responseMap = new HashMap<>();
        Map<String,Object> roleIdDataMap = new HashMap<>();
        roleIdDataMap.put("role_id", roleId);
        responseMap.put("data", roleIdDataMap);
        when(ControllerUtil.parseJson("{\"data\":{ \"role_id\": \""+roleId+"\"}}")).thenReturn(responseMap);
        when(reqProcessor.process("/auth/approle/role/readRoleID", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(roleIdResponse);
        
        String accessorIdResponseJson = "{\r\n" + 
        		"  \"keys\": [\r\n" + 
        		"    \"generated-accessor-id1\"\r\n" + 
        		"  ]\r\n" + 
        		"}";
        Response accessorIdResponse = getMockResponse(HttpStatus.OK, true, accessorIdResponseJson);
        
        Map<String, Object> accessorIdResponseMap = new HashMap<>();
        ArrayList<String> accessorIds = new ArrayList<String>();
        accessorIds.add("generated-accessor-id1");
        accessorIdResponseMap.put("keys", accessorIds);
        when(ControllerUtil.parseJson(accessorIdResponseJson)).thenReturn(responseMap);
        
        when(reqProcessor.process("/auth/approle/role/accessors/list", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(accessorIdResponse);
        
		AppRoleDetails appRoleDetails = new AppRoleDetails();
		appRoleDetails.setAppRole(appRole);
		appRoleDetails.setRole_id(roleId);
		appRoleDetails.setAppRoleMetadata(approleMetadata);
		if (!CollectionUtils.isEmpty(accessorIds)) {
			appRoleDetails.setAccessorIds(accessorIds.toArray(new String[accessorIds.size()]));
		}
        String appRoleDetailsResponseJson = "{\"errors\":[\"Access denied: You don't have enough permission to read the secret_id associated with the AppRole\"]}";
        Response appRoleDetailsResponse =getMockResponse(HttpStatus.BAD_REQUEST, true, appRoleDetailsResponseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(appRoleDetailsResponse.getHttpstatus()).body(appRoleDetailsResponse.getResponse());
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleDetails(token, role_name, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_updateAppRole_successfully() throws Exception{

        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseList = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name="approle1";
        UserDetails userDetails = getMockUser("testuser1", false);

        // START - AppRole exists
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        // END - AppRole exists
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole updated successfully.\"]}");

        Map<String,Object> appRolesList = new HashMap<>();
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("role1");
        appRolesList.put("keys", arrayList);
        when(ControllerUtil.parseJson("{\"keys\": [ \"role1\" ]}")).thenReturn(appRolesList);

        Response responseAfterHide = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(responseAfterHide);

        when(reqProcessor.process("/auth/approle/role/create", jsonStr,userDetails.getSelfSupportToken())).thenReturn(response);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);

        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(true);
        ResponseEntity<String> responseEntityActual = appRoleService.updateAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    @Test
    public void test_updateAppRole_BAD_REQUEST() throws Exception{

        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        Response responseList = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name="approle1";
        UserDetails userDetails = getMockUser("testuser1", false);

        // START - AppRole exists
        ArrayList<String> policiesList = new ArrayList<String>();
        policiesList.add("r_shared_safe01");
        String[] policies = policiesList.toArray(new String[policiesList.size()]);
        AppRole appRole = new AppRole(role_name, policies, true, 0, 0, 0);
        String appRoleResponseJson = new ObjectMapper().writeValueAsString(appRole);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, appRoleResponseJson);
        Map<String, Object> appRoleResponseMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        appRoleResponseMap.put("data", dataMap);
        dataMap.put("policies",policiesList);
        dataMap.put("bind_secret_id",new Boolean(true));
        dataMap.put("secret_id_num_uses", new Integer(0));
        dataMap.put("secret_id_ttl", new Integer(0));
        dataMap.put("token_num_uses", new Integer(0));
        dataMap.put("token_ttl", new Integer(0));
        dataMap.put("token_max_ttl", new Integer(0));
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\""+role_name+"\"}",userDetails.getSelfSupportToken())).thenReturn(appRoleResponse);
        when(ControllerUtil.parseJson(appRoleResponseJson)).thenReturn(appRoleResponseMap);
        // END - AppRole exists
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{}");

        Map<String,Object> appRolesList = new HashMap<>();
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("role1");
        appRolesList.put("keys", arrayList);
        when(ControllerUtil.parseJson("{\"keys\": [ \"role1\" ]}")).thenReturn(appRolesList);

        Response responseAfterHide = getMockResponse(HttpStatus.OK, true, "{\"keys\": [ \"role1\" ]}");
        when(ControllerUtil.hideMasterAppRoleFromResponse(Mockito.any())).thenReturn(responseAfterHide);

        when(reqProcessor.process("/auth/approle/role/create", jsonStr,userDetails.getSelfSupportToken())).thenReturn(response);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);

        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(true);
        ResponseEntity<String> responseEntityActual = appRoleService.updateAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());

    }
}
