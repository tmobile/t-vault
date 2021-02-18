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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.*;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.validator.TokenValidator;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.SafeUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;


@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class, OIDCUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SafesServiceTest {

    @InjectMocks
    SafesService safesService;

    @Mock
    AppRoleService appRoleService;

    @Mock
    TokenValidator tokenValidator;

    @Mock
    AWSIAMAuthService awsiamAuthService;

    @Mock
    AWSAuthService awsAuthService;

    @Mock
    RequestProcessor reqProcessor;
    
    @Mock
    DirectoryService directoryService;
    
    @Mock
    OIDCAuthService oidcAuthService;
    
    @Mock
    SafeUtils safeUtils;
    
    @Mock
    TokenUtils tokenUtils;
    
    @Mock
    OIDCUtil OIDCUtil;
    
    @Mock
    private WorkloadDetailsService workloadDetailsService;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(OIDCUtil.class);
        

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(OIDCUtil.class, "log", LogManager.getLogger(OIDCUtil.class));

        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
        String workloadApiResponse = "{\"kind\":\"Application\",\"spec\":{\"akmid\":\"103001\",\"brtContactEmail\":\"" +
                " testspec@mail.com\",\"businessUnit\":\"\",\"classification\":\"\",\"directorEmail\":\"abc.joe@mail.com\"," +
                "\"directorName\":\"abc amith\",\"executiveSponsor\":\"Dar Web\",\"opsContactEmail\":\"rick.nick@test.com\"," +
                "\"organizationalUnits\":[\"tvt\"],\"projectLeadEmail\":\"rick.nick@test.com\",\"scope\":\"Production\",\"summary\":" +
                "\"T-Vault\",\"tag\":\"T-Vault\",\"tier\":\"Tier II\",\"workflowStatus\":\"Open_CCP\",\"workload\":\"Adaptive Security\"}}";
        when(workloadDetailsService.getWorkloadDetailsByAppName(anyString())).
                thenReturn(ResponseEntity.status(HttpStatus.OK).body(workloadApiResponse));

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
    public void test_getFolders_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{  \"keys\": [    \"mysafe01\"  ]}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/sdb/list","{\"path\":\""+path+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.getFolders(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getInfo_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"data\": { \"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\", \"type\": \"\" }}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(reqProcessor.process("/sdb","{\"path\":\"metadata/"+path+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.getInfo(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createfolder_successfully() {

        String responseJson = "{  \"messages\": [    \"Folder created \"  ]}";
        String path = "shared/mysafe01";
        String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(ControllerUtil.isPathValid(any())).thenReturn(true);
        when(reqProcessor.process("/sdb/createfolder",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.createfolder(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createfolder_successfully_noContent() {

        String responseJson = "{\"messages\":[\"Folder created \"]}";
        String path = "shared/mysafe01";
        String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(ControllerUtil.isPathValid(any())).thenReturn(true);
        when(reqProcessor.process("/sdb/createfolder",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.createfolder(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createfolder_failure_400() {

        String responseJson = "{\"errors\":[\"Invalid path\"]}";
        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);

        when(ControllerUtil.isPathValid(any())).thenReturn(false);
        ResponseEntity<String> responseEntity = safesService.createfolder(token, path);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testcreateSafesuccessfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        String jsonStr = "{ \"data\": {\"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\"}, \"path\": \"shared/mysafe01\"}";
        String metadatajson = "{\"path\":\"metadata/shared/mysafe03\",\"data\":{\"name\":\"mysafe03\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe))).thenReturn(jsonStr);
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(reqparams);

        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);
        when(JSONUtil.getJSON(safe)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(any())).thenReturn(true);
        when(reqProcessor.process("/sdb/create",jsonStr,token)).thenReturn(responseNoContent);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write",metadatajson,token)).thenReturn(responseNoContent);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseNoContent);
        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testcreateSafefailureinvalidsafename() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01_", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01_",safeBasicDetails);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid Safe name: unexpected character _ in the end\"]}");
        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);

        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testcreateSafefailuresafenametooshort() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("sa", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/sa",safeBasicDetails);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid Safe name: Please enter minimum 3 characters\"]}");
        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);

        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testcreateSafefailuredescriptiontooshort() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("safe1", "youremail@yourcompany.com", null, "My safe","T-Vault","tvt");
        Safe safe = new Safe("shared/safe1",safeBasicDetails);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid Description: Please enter minimum 10 characters\"]}");
        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);

        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testcreateSafefailuredescriptiontoolong() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe My first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values: Description too long\"]}");
        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);

        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testcreateSafefailurepoliciescreation() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        String jsonStr = "{ \"data\": {\"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\"}, \"path\": \"shared/mysafe01\"}";
        String metadatajson = "{\"path\":\"metadata/shared/mysafe03\",\"data\":{\"name\":\"mysafe03\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseBadRequest = getMockResponse(HttpStatus.BAD_REQUEST, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Safe created however one ore more policy (read/write/deny) creation failed \"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe))).thenReturn(jsonStr);
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(reqparams);

        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);
        when(JSONUtil.getJSON(safe)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(any())).thenReturn(true);
        when(reqProcessor.process("/sdb/create",jsonStr,token)).thenReturn(responseNoContent);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write",metadatajson,token)).thenReturn(responseNoContent);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseBadRequest);
        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testcreateSafefailure400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        String jsonStr = "{ \"data\": {\"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\"}, \"path\": \"shared/mysafe01\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe))).thenReturn(jsonStr);
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(reqparams);

        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(any())).thenReturn(false);
        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSafe_successfully() {

        String path = "shared/mysafe01";
        String _path = "metadata/"+path;
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(reqProcessor.process("/sdb/list","{\"path\":\""+path+"\"}",token)).thenReturn(response);
        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.getSafe(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testdeleteSafefailed400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe(path,safeBasicDetails);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        when(ControllerUtil.isValidSafePath(path)).thenReturn(false);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(false);
        when(ControllerUtil.isValidDataPath(path)).thenReturn(false);
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1"); 
        ResponseEntity<String> responseEntity = safesService.deleteSafe(token, safe, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testupdateSafesuccessfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        String metadatajson = "{\"path\":\"metadata/shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\",\"aws-roles\":null,\"groups\":null,\"users\":null}}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response readResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValidForUpdate(reqparams)).thenReturn(true);
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getCountOfSafesForGivenSafeName(safe.getSafeBasicDetails().getName(), token)).thenReturn(1);
        when(ControllerUtil.generateSafePath("mysafe01", "shared")).thenReturn("shared/mysafe01");
        when(ControllerUtil.isValidSafePath(any())).thenReturn(true);

        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/sdb/update",metadatajson,token)).thenReturn(responseNoContent);
        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void testupdateSafefailuredescriptiontoolong() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe My first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe" +
                "My first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safeMy first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values: Description too long\"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValidForUpdate(reqparams)).thenReturn(true);

        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testupdateSafefailure400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValidForUpdate(reqparams)).thenReturn(true);
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getCountOfSafesForGivenSafeName(safe.getSafeBasicDetails().getName(), token)).thenReturn(1);
        when(ControllerUtil.generateSafePath("mysafe01", "shared")).thenReturn("shared/mysafe01");
        when(ControllerUtil.isValidSafePath(any())).thenReturn(false);
        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testupdateSafefailure404() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        Response readResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified \"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValidForUpdate(reqparams)).thenReturn(true);
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getCountOfSafesForGivenSafeName(safe.getSafeBasicDetails().getName(), token)).thenReturn(1);
        when(ControllerUtil.generateSafePath("mysafe01", "shared")).thenReturn("shared/mysafe01");
        when(ControllerUtil.isValidSafePath(any())).thenReturn(true);

        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testupdateSafefailureinvalidInput() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe","T-Vault","tvt");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValid(reqparams)).thenReturn(false);
        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_addUserToSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");
        
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(false);
        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
        
        ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser, userDetails, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_OIDC_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
        try {
            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testuser1"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

      //oidc test cases
        String mountAccessor = "auth_oidc";
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUser");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testUser");
        
        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);

        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setId(null);
			oidcLookupEntityRequest.setAlias_id(null);
			oidcLookupEntityRequest.setName(null);
			oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
					.body(oidcEntityResponse);

			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
	
			Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
			when(OIDCUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName()))
					.thenReturn(responseEntity3);
        when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    
    @Test
    public void test_removeUserFromSafe_ldap_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
        try {
            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testuser1"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

      //oidc test cases
        String mountAccessor = "auth_oidc";
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUser");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testUser");
        
        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "ldap");

        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed. Please try again\"]}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testuser1"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNotFound);
        //oidc test cases
        String mountAccessor = "auth_oidc";
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
        
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setId(null);
			oidcLookupEntityRequest.setAlias_id(null);
			oidcLookupEntityRequest.setName(null);
			oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<DirectoryObjects> responseEntity1 = ResponseEntity.status(HttpStatus.OK).body(users);
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);
			when(directoryService.searchByCorpId(userDetails.getUsername())).thenReturn(responseEntity1);

			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
					.body(oidcEntityResponse);

			when(oidcAuthService.entityLookUp(eq(token), Mockito.any(OIDCLookupEntityRequest.class))).thenReturn(responseEntity2);
			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
	
			ResponseEntity<String> responseEntity3 = ResponseEntity.status(HttpStatus.OK)
					.body("success");
			when(oidcAuthService.updateEntityByName(eq(token), Mockito.any(OIDCEntityRequest.class)))
					.thenReturn(responseEntity3);
        
        
        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_failure_orphan_entries_oidc() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
        Response responseNoContent = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"messages\":[\"User configuration failed. Invalid user\"]}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(responseNotFound);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(null);
        
      //oidc test cases
        String mountAccessor = "auth_oidc";
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
        
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setId(null);
			oidcLookupEntityRequest.setAlias_id(null);
			oidcLookupEntityRequest.setName(null);
			oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);


			ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(oidcEntityResponse);

//			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
//	
//			ResponseEntity<String> responseEntity3 = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body("failure");
//			when(oidcAuthService.updateEntityByName(eq(token), Mockito.any(OIDCEntityRequest.class)))
//					.thenReturn(responseEntity3);
//        

        when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    
    @Test
    public void test_removeUserFromSafe_failure_orphan_entries_ldap() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed. Please try again\"]}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(responseNotFound);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(null);
		ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "ldap");
        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(false);

        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, getMockUser(true));
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");

        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "ldap");
        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(userResponse);
        try {
            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"groups\": {\"mygroup01\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Try again \"]}");


        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(userResponse);
        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNotFound);

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"groups\": {\"mygroup01\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response groupResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");

        when(ControllerUtil.areSafeGroupInputsValid(safeGroup)).thenReturn(true);
        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(groupResponse);

        try {
            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
        List<String> policies = new ArrayList<>();
        policies.add("default");
        policies.add("w_shared_mysafe02");
        policies.add("r_shared_mysafe01");
        List<String> currentpolicies = new ArrayList<>();
        currentpolicies.add("default");
        currentpolicies.add("w_shared_mysafe01");
        currentpolicies.add("w_shared_mysafe02");
        OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
        when(OIDCUtil.getIdentityGroupDetails("mygroup01", token)).thenReturn(oidcGroup);

        Response response = new Response();
        response.setHttpstatus(HttpStatus.NO_CONTENT);
        when(OIDCUtil.updateGroupPolicies(token, "mygroup01", policies, currentpolicies, oidcGroup.getId())).thenReturn(response);


        ResponseEntity<String> responseEntity = safesService.addGroupToSafe(token, safeGroup, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_successfully_all_safes() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response groupResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");

        when(ControllerUtil.areSafeGroupInputsValid(safeGroup)).thenReturn(true);
        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(groupResponse);

        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),eq(token))).thenReturn(idapConfigureResponse);

        when(ControllerUtil.updateMetadata(any(),eq(token))).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return responseNoContent;

                return response_404;
            }
        });
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));

        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
        List<String> policies = new ArrayList<>();
        policies.add("default");
        policies.add("w_shared_mysafe02");
        policies.add("r_shared_mysafe01");
        List<String> currentpolicies = new ArrayList<>();
        currentpolicies.add("default");
        currentpolicies.add("w_shared_mysafe01");
        currentpolicies.add("w_shared_mysafe02");
        OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
        when(OIDCUtil.getIdentityGroupDetails("mygroup01", token)).thenReturn(oidcGroup);

        Response response = new Response();
        response.setHttpstatus(HttpStatus.NO_CONTENT);
        when(OIDCUtil.updateGroupPolicies(token, "mygroup01", policies, currentpolicies, oidcGroup.getId())).thenReturn(response);

        ResponseEntity<String> responseEntity = safesService.addGroupToSafe(token, safeGroup, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response groupResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Try Again\"]}");

        when(ControllerUtil.areSafeGroupInputsValid(safeGroup)).thenReturn(true);
        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(groupResponse);

        try {
            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),eq(token))).thenReturn(idapConfigureResponse);

        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(response_404);
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));

        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
        List<String> policies = new ArrayList<>();
        policies.add("default");
        policies.add("w_shared_mysafe02");
        policies.add("r_shared_mysafe01");
        List<String> currentpolicies = new ArrayList<>();
        currentpolicies.add("default");
        currentpolicies.add("w_shared_mysafe01");
        currentpolicies.add("w_shared_mysafe02");
        OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
        when(OIDCUtil.getIdentityGroupDetails("mygroup01", token)).thenReturn(oidcGroup);

        Response response = new Response();
        response.setHttpstatus(HttpStatus.BAD_REQUEST);
        when(OIDCUtil.updateGroupPolicies(token, "mygroup01", policies, currentpolicies, oidcGroup.getId())).thenReturn(response);

        ResponseEntity<String> responseEntity = safesService.addGroupToSafe(token, safeGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_successfully_orphan_entries() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"Message\":\"Group not available or deleted from AD, removed the group assignment and permissions \"}");
        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(responseNotFound);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"groups\": {\"mygroup01\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.areAWSRoleInputsValid(any(Map.class))).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"iam\"}",token)).thenReturn(readResponse);
        when(awsiamAuthService.configureAWSIAMRole(eq("iam"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Try Again\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.areAWSRoleInputsValid(any(Map.class))).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"iam\"}",token)).thenReturn(readResponse);
        when(awsiamAuthService.configureAWSIAMRole(eq("iam"),any(),eq(token))).thenReturn(responseNotFound);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.areAWSRoleInputsValid(any(Map.class))).thenReturn(false);
        //when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_failure_404() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.areAWSRoleInputsValid(any(Map.class))).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"iam\"}",token)).thenReturn(readResponse);
        when(awsiamAuthService.configureAWSIAMRole(eq("iam"),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNotFound);
        when(awsAuthService.configureAWSRole(any(),any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from Safe\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);

        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"iam\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);

        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        UserDetails userDetails = getMockUser(false);
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(awsRole.getRole(), token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH)).thenReturn(responseOk);
        Response deleteResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(deleteResponse);

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"aws-roles\": {\"iam\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSEC2RoleFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"ec2role","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"ec2role\"}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from Safe\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);

        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"ec2role\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);

        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        UserDetails userDetails = getMockUser(false);
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(awsRole.getRole(), token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH)).thenReturn(responseOk);
        Response deleteResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(deleteResponse);

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"aws-roles\": {\"ec2role\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"ec2role","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"ec2role\"}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");


        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"ec2role\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNotFound);
        UserDetails userDetails = getMockUser(false);
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(awsRole.getRole(), token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH)).thenReturn(responseOk);
        Response deleteResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/delete"),Mockito.any(),eq(token))).thenReturn(deleteResponse);

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"aws-roles\": {\"ec2role\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failure_404() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"ec2role","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"ec2role\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Contact Admin \"]}");
        Response responseBadRequest = getMockResponse(HttpStatus.BAD_REQUEST, true, "{  \"errors\": [   \"Invalid 'path' specified\"  ]}");


        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"ec2role\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        UserDetails userDetails = getMockUser(false);
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(awsRole.getRole(), token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH)).thenReturn(responseOk);

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"aws-roles\": {\"ec2role\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveAWSEc2RoleFromSafeFailure404() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"ec2role","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"ec2role\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
        Response responseBadRequest = getMockResponse(HttpStatus.BAD_REQUEST, true, "{  \"errors\": [   \"Invalid 'path' specified\"  ]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"ec2role\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        UserDetails userDetails = getMockUser(false);
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(awsRole.getRole(), token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH)).thenReturn(responseOk);

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"aws-roles\": {\"ec2role\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failure_invalidPath() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(false);
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failed_404() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove AWS role from safe. AWS role association to safe not found\"]}");

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        UserDetails userDetails = getMockUser(false);

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failed_metadata_invalid() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified\"]}");

        String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        UserDetails userDetails = getMockUser(false);

        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(new HashMap<>());

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failed_metadata_notFound() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified\"]}");

        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(getMockResponse(HttpStatus.NOT_FOUND, true, ""));

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        UserDetails userDetails = getMockUser(false);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_AssociateAppRole_succssfully() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(safeAppRoleAccess)).thenReturn(jsonStr);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  safesService.associateApproletoSDB(token, safeAppRoleAccess);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure_400() throws Exception {


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any safe\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("selfservicesupportrole", "shared/mysafe01", "write");
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.any())).thenReturn(true);
        ResponseEntity<String> responseEntityActual =  safesService.associateApproletoSDB(token, safeAppRoleAccess);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_succssfully_new_meta() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response updateMetadataResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
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
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(safeAppRoleAccess)).thenReturn(jsonStr);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  safesService.associateApproletoSDB(token, safeAppRoleAccess);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failed_configuration() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response updateMetadataResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
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
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(safeAppRoleAccess)).thenReturn(jsonStr);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  safesService.associateApproletoSDB(token, safeAppRoleAccess);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_AssociateAppRole_failed() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Approle :approle1 failed to be associated with SDB\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"Internal server error\"]}");
        Response updateMetadataResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(safeAppRoleAccess)).thenReturn(jsonStr);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  safesService.associateApproletoSDB(token, safeAppRoleAccess);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_removeApproleFromSafe_successfully() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String inputJson = "{\"role_name\":\"approle1\",\"path\":\"users/safe1\",\"access\":\"read\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle is successfully removed(if existed) from Safe\"]}");
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        when(appRoleService.configureApprole(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(response);
        when(ControllerUtil.isValidSafePath("users/safe1")).thenReturn(true);
        when(ControllerUtil.isValidSafe("users/safe1", token)).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(), eq(token))).thenReturn(response);

        ResponseEntity<String> responseEntityActual = safesService.removeApproleFromSafe(token, inputJson);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_removeApproleFromSafe_successfully_all_safes() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        String path = "users/safe1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        //when(ControllerUtil.updateMetadata(Mockito.anyMap(),eq(token))).thenReturn(response);
        String inputJson = "{\"role_name\":\"approle1\",\"path\":\"users/safe1\",\"access\":\"read\"}";
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        when(appRoleService.configureApprole(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(response);

        when(ControllerUtil.updateMetadata(Mockito.anyMap(), eq(token))).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return response;

                return response_404;
            }
        });
        when(ControllerUtil.getSafeType(path)).thenReturn("users");
        when(ControllerUtil.getSafeName(path)).thenReturn("safe1");
        List<String> safeNames = new ArrayList<>();
        safeNames.add("safe1");
        when(ControllerUtil.getAllExistingSafeNames("users", token)).thenReturn(safeNames);

        ResponseEntity<String> responseEntityActual = safesService.removeApproleFromSafe(token, inputJson);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_removeApproleFromSafe_failure() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String inputJson = "{\"role_name\":\"approle1\",\"path\":\"users/safe1\",\"access\":\"read\"}";

        String path = "users/safe1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
        Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        //when(ControllerUtil.updateMetadata(Mockito.anyMap(),eq(token))).thenReturn(response);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
        when(appRoleService.configureApprole(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(response);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(), eq(token))).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return response_404;

                return response_404;
            }
        });
        when(ControllerUtil.getSafeType(path)).thenReturn("users");
        when(ControllerUtil.getSafeName(path)).thenReturn("safe1");
        List<String> safeNames = new ArrayList<>();
        safeNames.add("safe1");
        when(ControllerUtil.getAllExistingSafeNames("users", token)).thenReturn(safeNames);

        ResponseEntity<String> responseEntityActual = safesService.removeApproleFromSafe(token, inputJson);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_removeApproleFromSafe_failure_400() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String inputJson = "{\"role\":\"approle1\",\"path\":\"users/safe1\"}";
        String path = "users/safe1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");
        Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(ControllerUtil.isValidSafePath(path)).thenReturn(false);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);

        ResponseEntity<String> responseEntityActual = safesService.removeApproleFromSafe(token, inputJson);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_createNestedfolder_successfully() {

        String responseJson = "{\"messages\":[\"Folder created \"]}";
        String path = "shared/mysafe01";
        String jsonStr = "{\"path\":\"" + path + "\",\"data\":{\"default\":\"default\"}}";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(ControllerUtil.isPathValid(path)).thenReturn(true);
        when(reqProcessor.process("/sdb/createfolder",jsonStr,token)).thenReturn(response);
        UserDetails userDetails = getMockUser(false);
        VaultTokenLookupDetails  vaultTokenLookupDetails = new VaultTokenLookupDetails();
        vaultTokenLookupDetails.setPolicies(new String[] {"w_shared_mysafe01"});
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        try {
            when(tokenValidator.getVaultTokenLookupDetails(token)).thenReturn(vaultTokenLookupDetails);
        } catch (TVaultValidationException e) {}
        ResponseEntity<String> responseEntity = safesService.createNestedfolder(token, path, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void test_createNestedfolder_failure_400() {

        String responseJson = "{\"errors\":[\"Invalid path\"]}";
        String path = "shared/mysafe01";
        String jsonStr = "{\"path\":\"" + path + "\",\"data\":{\"default\":\"default\"}}";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(ControllerUtil.isPathValid(path)).thenReturn(false);
        UserDetails userDetails = getMockUser(false);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        ResponseEntity<String> responseEntity = safesService.createNestedfolder(token, path, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void test_getFoldersRecursively_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{  \"keys\": [    \"mysafe01\"  ]}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/sdb/list","{\"path\":\""+path+"\"}",token)).thenReturn(response);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn("{  \"keys\": [    \"mysafe01\"  ]}");
        ResponseEntity<String> responseEntity = safesService.getFoldersRecursively(token, path, 25, 0);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    
    @Test
    public void test_addUserToSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("testuser1");
        
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");

        when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);

        try {
            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //oidc test cases
        String mountAccessor = "auth_oidc";
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUser");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testUser");
        
        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);

        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setId(null);
			oidcLookupEntityRequest.setAlias_id(null);
			oidcLookupEntityRequest.setName(null);
			oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<DirectoryObjects> responseEntity1 = ResponseEntity.status(HttpStatus.OK).body(users);
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
					.body(oidcEntityResponse);

			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

			Response responseEntity3 = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
			when(OIDCUtil.updateOIDCEntity(any(), any()))
					.thenReturn(responseEntity3);

			when(ControllerUtil
					.configureLDAPUser(eq("testuser1"), any(), any(), eq(token)))
					.thenReturn(responseNotFound);

            when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
			when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
			ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser, userDetails, false);
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
    }
	   
	      @Test
 public void test_addUserToSafe_oidc_successfully() {
     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
     String path = "shared/mysafe01";
     SafeUser safeUser = new SafeUser(path, "testuser1","write");
     UserDetails userDetails = new UserDetails();
     userDetails.setUsername("testuser1");
     
     Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
     Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
     Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

     ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

     when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(true);
     when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
     when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
     when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
     when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
     when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);

     try {
         //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
         List<String> resList = new ArrayList<>();
         resList.add("default");
         resList.add("w_shared_mysafe01");
         resList.add("w_shared_mysafe02");
         when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
     } catch (IOException e) {
         e.printStackTrace();
     }

     when(ControllerUtil.configureLDAPUser(eq("testuser1"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
     when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
     when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
   //oidc test cases
     ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
     String mountAccessor = "auth_oidc";
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
     
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setId(null);
			oidcLookupEntityRequest.setAlias_id(null);
			oidcLookupEntityRequest.setName(null);
			oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<DirectoryObjects> responseEntity1 = ResponseEntity.status(HttpStatus.OK).body(users);
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
					.body(oidcEntityResponse);

			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
			String entityName = "entity";
			
			Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
			when(OIDCUtil.updateOIDCEntity(any(), any()))
					.thenReturn(responseEntity3);
              when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
     ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser, userDetails, false);
     assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
     assertEquals(responseEntityExpected, responseEntity);
 }
	      
	      @Test
	      public void test_addUserToSafe_ldap_successfully() {
	          String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	          String path = "shared/mysafe01";
	          SafeUser safeUser = new SafeUser(path, "testuser1","write");
	          UserDetails userDetails = new UserDetails();
	          userDetails.setUsername("testuser1");
	          userDetails.setAccess("write");
	          userDetails.setEmail("test@t-mobile.com");
	          
	          Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
	          Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
	          Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	          ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");
	          ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "ldap");
	          when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(true);
	          when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
	          when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
	          when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
	          when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
	          when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);

	          try {
	              //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
	              List<String> resList = new ArrayList<>();
	              resList.add("default");
	              resList.add("w_shared_mysafe01");
	              resList.add("w_shared_mysafe02");
	              when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
	          } catch (IOException e) {
	              e.printStackTrace();
	          }

	          when(ControllerUtil.configureLDAPUser(eq("testuser1"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
	          when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
	          when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);

	          ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser, userDetails, false);
	          assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	          assertEquals(responseEntityExpected, responseEntity);
	      }

 @Test
 public void test_addUserToSafe_successfully_all_safes() {
     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
     String path = "shared/mysafe01";
     SafeUser safeUser = new SafeUser(path, "testuser1","write");
     UserDetails userDetails = new UserDetails();
     userDetails.setUsername("testuser1");

     Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
     Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
     Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
     Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

     ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

     when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(true);
     when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
     when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
     when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
     when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
     when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);

     try {
         when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
     } catch (IOException e) {
         e.printStackTrace();
     }

     when(ControllerUtil.configureLDAPUser(eq("testuser1"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
     //when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);
     when(ControllerUtil.updateMetadata(any(),eq(token))).thenAnswer(new Answer() {
         private int count = 0;

         public Object answer(InvocationOnMock invocation) {
             if (count++ == 1)
                 return responseNoContent;

             return response_404;
         }
     });
     when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
     when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
     when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));

     when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
     
     //OIDC changes
     
   //oidc test cases
     
     ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
     String mountAccessor = "auth_oidc";
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
     
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setId(null);
			oidcLookupEntityRequest.setAlias_id(null);
			oidcLookupEntityRequest.setName(null);
			oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<DirectoryObjects> responseEntity1 = ResponseEntity.status(HttpStatus.OK).body(users);
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
					.body(oidcEntityResponse);

			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

			Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
			when(OIDCUtil.updateOIDCEntity(any(), any()))
					.thenReturn(responseEntity3);
			when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
     
     ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser, userDetails, false);
     assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
     assertEquals(responseEntityExpected, responseEntity);
 }

 @Test
 public void test_addUserToSafe_failure_all_safes() {
     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
     String path = "shared/mysafe01";
     SafeUser safeUser = new SafeUser(path, "testuser1","write");
     UserDetails userDetails = new UserDetails();
     userDetails.setUsername("testuser1");

     Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
     Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
     Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
     Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

     ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");

     when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(true);
     when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
     when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
     when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
     when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
     when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);

     try {
         when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
     } catch (IOException e) {
         e.printStackTrace();
     }

     when(ControllerUtil.configureLDAPUser(eq("testuser1"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
     //when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);
     when(ControllerUtil.updateMetadata(any(),eq(token))).thenAnswer(new Answer() {
         private int count = 0;

         public Object answer(InvocationOnMock invocation) {
             if (count++ == 1)
                 return response_404;

             return response_404;
         }
     });
     //ReflectionTestUtils.setField(safeUtils, "vaultAuthMethod", "userpass");
     when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
     when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
     when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));

     when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
   //oidc test cases
     
     ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
     String mountAccessor = "auth_oidc";
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
     
			OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
			oidcLookupEntityRequest.setId(null);
			oidcLookupEntityRequest.setAlias_id(null);
			oidcLookupEntityRequest.setName(null);
			oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
			oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<DirectoryObjects> responseEntity1 = ResponseEntity.status(HttpStatus.OK).body(users);
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
					.body(oidcEntityResponse);

			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

			Response responseEntity3 = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
		when(OIDCUtil.updateOIDCEntity(any(), any())).thenReturn(responseEntity3);
     when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
     ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser, userDetails, false);
     assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
 }
 
 @Test
 public void test_removeGroupFromSafe_successfully1() {
     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
     String path = "shared/mysafe01";
     SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
     String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

     Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
     Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
     ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");


     when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
     when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
     when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
 	ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
	List<String> policie = new ArrayList<>();
	policie.add("default");
	policie.add("w_shared_mysafe02");
	policie.add("r_shared_mysafe01");
	List<String> currentpolicies = new ArrayList<>();
	currentpolicies.add("default");
	currentpolicies.add("w_shared_mysafe01");
	currentpolicies.add("w_shared_mysafe02");
	OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
	when(OIDCUtil.getIdentityGroupDetails(any(), any())).thenReturn(oidcGroup);

	Response response1 = new Response();
	response1.setHttpstatus(HttpStatus.NO_CONTENT);
	when(OIDCUtil.updateGroupPolicies(any(), any(), any(), any(), any())).thenReturn(response1);     try {
         //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
         List<String> resList = new ArrayList<>();
         resList.add("default");
         resList.add("w_shared_mysafe01");
         resList.add("w_shared_mysafe02");
         when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
     } catch (IOException e) {
         e.printStackTrace();
     }
     when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
     when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

     String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"groups\": {\"mygroup01\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
     Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
     when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
     Map<String,Object> reqparams = null;
     try {
         reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
     } catch (IOException e) {
         e.printStackTrace();
     }
     when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

     ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
     assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
     assertEquals(responseEntityExpected, responseEntity);
 }
 
 @Test
 public void test_removeGroupFromSafe_failure1() {
     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
     String path = "shared/mysafe01";
     SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
     String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

     when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
     when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
     when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
     String metdataJsonString = "{}";
     Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
     when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
     Map<String,Object> reqparams = null;
     try {
         reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
     } catch (IOException e) {
         e.printStackTrace();
     }
     when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

     ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
 	assertNotNull(responseEntity);
 }
 
 @Test
 public void test_removeGroupFromSafe_failure2() {
     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
     String path = "shared/mysafe01";
     SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
     String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

     Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
     Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
     ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");


     when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
     when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
     when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
     String metdataJsonString = "{}";
     Response readResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, metdataJsonString);
     when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
     Map<String,Object> reqparams = null;
     try {
         reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
     } catch (IOException e) {
         e.printStackTrace();
     }
     when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

     ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
 	assertNotNull(responseEntity);
 }
 
 @Test
 public void test_removeGroupFromSafe_failure3() {
     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
     String path = "shared/mysafe01";
     SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
     String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";
  
     ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "userpass");
     String metdataJsonString = "{}";
     Response readResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, metdataJsonString);
     when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
     Map<String,Object> reqparams = null;
     try {
         reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
     } catch (IOException e) {
         e.printStackTrace();
     }
     when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

     ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
 	assertNotNull(responseEntity);
 }
 
 @Test
 public void test_removeGroupFromSafe_successfully2() {
     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
     String path = "shared/mysafe01";
     SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
     String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

     Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
     Response responseNoContent = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
     ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messaged\":[\"Group association is removed \"]}");

     ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "ldap");
     when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
     when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
     when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
     when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(userResponse);
     try {
         //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
         List<String> resList = new ArrayList<>();
         resList.add("default");
         resList.add("w_shared_mysafe01");
         resList.add("w_shared_mysafe02");
         when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
     } catch (IOException e) {
         e.printStackTrace();
     }
     when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
     when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

     String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"groups\": {\"mygroup01\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
     Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
     when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
     Map<String,Object> reqparams = null;
     try {
         reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
     } catch (IOException e) {
         e.printStackTrace();
     }
     when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

     ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
     assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
 }
 
	@Test
	public void test_deletefolder_failure() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String path = "test/mysafe01";
		ResponseEntity<String> responseEntity = safesService.deletefolder(token, path, getMockUser(true));
		assertNotNull(responseEntity);
	}
	
	@Test
	public void test_getSafe_failure() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String path = "test/mysafe01";
		ResponseEntity<String> responseEntity = safesService.getSafe(token, path);
		assertNotNull(responseEntity);
	}
	
	@Test
	public void test_getSafe() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String path = "shared";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setResponse("success");
		when(reqProcessor.process(eq("/sdb/list"),Mockito.anyString(), Mockito.anyString())).thenReturn(response);
		ResponseEntity<String> responseEntity = safesService.getSafe(token, path);
		assertNotNull(responseEntity);
	}
	
	@Test
	public void test_getSafe_failure1() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String path = "test/mysafe01";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.BAD_REQUEST);
		response.setResponse("success");
		when(ControllerUtil.isValidSafePath(any())).thenReturn(true);
		when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
		when(reqProcessor.process(eq("/sdb"),Mockito.anyString(), Mockito.anyString())).thenReturn(response);
		ResponseEntity<String> responseEntity = safesService.getSafe(token, path);
		assertNotNull(responseEntity);
	}
	
	@Test
	public void test_getSafe_failure_safeName_null() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String path = "test/mysafe01";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.BAD_REQUEST);
		response.setResponse("success");
		when(ControllerUtil.isValidSafePath(any())).thenReturn(true);
		when(ControllerUtil.getSafeName(path)).thenReturn(null);
		when(reqProcessor.process(eq("/sdb"),Mockito.anyString(), Mockito.anyString())).thenReturn(response);
		ResponseEntity<String> responseEntity = safesService.getSafe(token, path);
		assertNotNull(responseEntity);
	}
	
	@Test
	public void test_getSafe_failure2() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String path = "/test/mysafe01/";
		ResponseEntity<String> responseEntity = safesService.getSafe(token, path);
		assertNotNull(responseEntity);
	}
	
	@Test
	public void test_associateApproletoSDB_failure() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		
		String jsonStr = "{ \"data\": {\"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\"}, \"path\": \"shared/mysafe01\"}";

		Map<String, Object> reqparams = null;
		try {
			reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		when(ControllerUtil.parseJson(jsonStr)).thenReturn(reqparams);
		when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(false);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
		ResponseEntity<String> responseEntity = safesService.associateApproletoSDB(token, safeAppRoleAccess);
		assertNotNull(responseEntity);
	}
	
	 @Test
	    public void test_AssociateAppRole_succssfully_read_access() throws Exception {

	        Response response = getMockResponse(HttpStatus.OK, true, "");
	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"read\"}";
	        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
	        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
	        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
	        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
	        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
	        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
	        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
	        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
	        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
	        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
	        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "read");
	        when(JSONUtil.getJSON(safeAppRoleAccess)).thenReturn(jsonStr);
	        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_shared_mysafe01\"}}");
	        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
	        ResponseEntity<String> responseEntityActual =  safesService.associateApproletoSDB(token, safeAppRoleAccess);

	        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
	        assertEquals(responseEntityExpected, responseEntityActual);

	    }
	
	 @Test
	    public void test_AssociateAppRole_succssfully_deny_access() throws Exception {

	        Response response = getMockResponse(HttpStatus.OK, true, "");
	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"deny\"}";
	        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
	        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
	        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
	        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
	        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
	        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
	        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
	        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
	        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
	        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
	        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "deny");
	        when(JSONUtil.getJSON(safeAppRoleAccess)).thenReturn(jsonStr);
	        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"d_shared_mysafe01\"}}");
	        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}",token)).thenReturn(appRoleResponse);
	        ResponseEntity<String> responseEntityActual =  safesService.associateApproletoSDB(token, safeAppRoleAccess);

	        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
	        assertEquals(responseEntityExpected, responseEntityActual);

	    }
	 
	 @Test
	 public void test_removeGroupFromSafe_failure4() {
	     String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	     String path = "shared/mysafe01";
	     SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
	     String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

	     Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
	     Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
	     ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");


	     when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
	     when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
	     when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
	 	ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");
		List<String> policie = new ArrayList<>();
		policie.add("default");
		policie.add("w_shared_mysafe02");
		policie.add("r_shared_mysafe01");
		List<String> currentpolicies = new ArrayList<>();
		currentpolicies.add("default");
		currentpolicies.add("w_shared_mysafe01");
		currentpolicies.add("w_shared_mysafe02");
		OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
		when(OIDCUtil.getIdentityGroupDetails(any(), any())).thenReturn(oidcGroup);

		Response response1 = new Response();
		response1.setHttpstatus(HttpStatus.NO_CONTENT);
		when(OIDCUtil.updateGroupPolicies(any(), any(), any(), any(), any())).thenReturn(response1);     try {
	         //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
	         List<String> resList = new ArrayList<>();
	         resList.add("default");
	         resList.add("w_shared_mysafe01");
	         resList.add("w_shared_mysafe02");
	         when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
	     } catch (IOException e) {
	         e.printStackTrace();
	     }
	     when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
	     Response responseBadContent = getMockResponse(HttpStatus.BAD_REQUEST, false, "");
	     when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseBadContent);
	     String metdataJsonString = "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"groups\": {\"mygroup01\": \"write\"},\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}";
	     Response readResponse = getMockResponse(HttpStatus.OK, true, metdataJsonString);
	     when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
	     Map<String,Object> reqparams = null;
	     try {
	         reqparams = new ObjectMapper().readValue(metdataJsonString, new TypeReference<Map<String, Object>>(){});
	     } catch (IOException e) {
	         e.printStackTrace();
	     }
	     when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

	     ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup, getMockUser(true));
	     assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
	 }
	 
	 @Test
	    public void test_addAwsRoleToSafe_write_failure() {
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        String path = "shared/mysafe01";
	        AWSRole awsRole = new AWSRole(path,"iam","read");
	        String jsonStr = "{  \"access\": \"write\",  \"path\": \"shared/mysafe01\",  \"role\": \"ec2\"}";

	        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
	                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
	                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
	                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
	        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);
	        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");

	        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
	        when(ControllerUtil.areAWSRoleInputsValid(any(Map.class))).thenReturn(true);
	        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
	        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
	        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
	        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"ec2\"}",token)).thenReturn(readResponse);
	        when(awsAuthService.configureAWSRole(eq("ec2"),any(),eq(token))).thenReturn(idapConfigureResponse);
	        Response responseBadContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		     when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseBadContent);
	        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
	        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	    }
	 
	 @Test
	    public void test_removeUserFromSafe_userpass_successfully() {
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        String path = "shared/mysafe01";
	        SafeUser safeUser = new SafeUser(path, "testuser1","write");
	        UserDetails userDetails = new UserDetails();
	        userDetails.setUsername("testuser1");
	        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
	        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

	        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
	        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
	        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
	        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

	        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
	        try {
	            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
	            List<String> resList = new ArrayList<>();
	            resList.add("default");
	            resList.add("w_shared_mysafe01");
	            resList.add("w_shared_mysafe02");
	            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        when(ControllerUtil.configureUserpassUser(eq("testuser1"),any(),eq(token))).thenReturn(responseNoContent);
	        Response responseBadContent = getMockResponse(HttpStatus.BAD_REQUEST, false, "");

	        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseBadContent);
	        
	        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "userpass");
	        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
	        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
	        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(null);
	        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
            assertNotNull(responseEntity);
	    }
	 
	 @Test
	    public void test_removeUserFromSafe_oidc_successfully() {
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        String path = "shared/mysafe01";
	        SafeUser safeUser = new SafeUser(path, "testuser1","write");
	        UserDetails userDetails = new UserDetails();
	        userDetails.setUsername("testuser1");
	        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
	        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

	        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
	        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
	        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
	        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

	        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
	        try {
	            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
	            List<String> resList = new ArrayList<>();
	            resList.add("default");
	            resList.add("w_shared_mysafe01");
	            resList.add("w_shared_mysafe02");
	            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        when(ControllerUtil.configureUserpassUser(eq("testuser1"),any(),eq(token))).thenReturn(responseNoContent);
	        Response responseBadContent = getMockResponse(HttpStatus.BAD_REQUEST, false, "");

	        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseBadContent);
	        
	        //oidc test cases
	        String mountAccessor = "auth_oidc";
	        DirectoryUser directoryUser = new DirectoryUser();
	        directoryUser.setDisplayName("testUser");
	        directoryUser.setGivenName("testUser");
	        directoryUser.setUserEmail("testUser@t-mobile.com");
	        directoryUser.setUserId("testuser01");
	        directoryUser.setUserName("testUser");
	        
	        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");

	        List<DirectoryUser> persons = new ArrayList<>();
	        persons.add(directoryUser);

	        DirectoryObjects users = new DirectoryObjects();
	        DirectoryObjectsList usersList = new DirectoryObjectsList();
	        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
	        users.setData(usersList);
	        
				OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
				oidcLookupEntityRequest.setId(null);
				oidcLookupEntityRequest.setAlias_id(null);
				oidcLookupEntityRequest.setName(null);
				oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
				oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
				OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
				oidcEntityResponse.setEntityName("entity");
				List<String> policies = new ArrayList<>();
				policies.add("safeadmin");
				oidcEntityResponse.setPolicies(policies);
				when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

				ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
						.body(oidcEntityResponse);

				when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
		
				Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
				when(OIDCUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName()))
						.thenReturn(responseEntity3);
	        when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
	        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
	        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
	        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));
	        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
         assertNotNull(responseEntity);
	    }
	 
	 @Test
	    public void test_removeUserFromSafe_oidc_failure() {
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        String path = "shared/mysafe01";
	        SafeUser safeUser = new SafeUser(path, "testuser1","write");
	        UserDetails userDetails = new UserDetails();
	        userDetails.setUsername("testuser1");
	        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
	        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

	        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
	        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
	        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
	        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

	        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
	        try {
	            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
	            List<String> resList = new ArrayList<>();
	            resList.add("default");
	            resList.add("w_shared_mysafe01");
	            resList.add("w_shared_mysafe02");
	            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        when(ControllerUtil.configureUserpassUser(eq("testuser1"),any(),eq(token))).thenReturn(responseNoContent);
	        Response responseBadContent = getMockResponse(HttpStatus.BAD_REQUEST, false, "");

	        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseBadContent);
	        
	        //oidc test cases
	        String mountAccessor = "auth_oidc";
	        DirectoryUser directoryUser = new DirectoryUser();
	        directoryUser.setDisplayName("testUser");
	        directoryUser.setGivenName("testUser");
	        directoryUser.setUserEmail("testUser@t-mobile.com");
	        directoryUser.setUserId("testuser01");
	        directoryUser.setUserName("testUser");
	        
	        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");

	        List<DirectoryUser> persons = new ArrayList<>();
	        persons.add(directoryUser);

	        DirectoryObjects users = new DirectoryObjects();
	        DirectoryObjectsList usersList = new DirectoryObjectsList();
	        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
	        users.setData(usersList);
	        
				OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
				oidcLookupEntityRequest.setId(null);
				oidcLookupEntityRequest.setAlias_id(null);
				oidcLookupEntityRequest.setName(null);
				oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
				oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
				OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
				oidcEntityResponse.setEntityName("entity");
				List<String> policies = new ArrayList<>();
				policies.add("safeadmin");
				oidcEntityResponse.setPolicies(policies);
				when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

				ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(oidcEntityResponse);

				when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
		
				Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
				when(OIDCUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName()))
						.thenReturn(responseEntity3);
	        when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
	        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
	        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
	        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));
	        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
      assertNotNull(responseEntity);
	    }
	 
	 @Test
	    public void test_removeUserFromSafe_oidc_failure1() {
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        String path = "shared/mysafe01";
	        SafeUser safeUser = new SafeUser(path, "testuser1","write");
	        UserDetails userDetails = new UserDetails();
	        userDetails.setUsername("testuser1");
	        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
	        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

	        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
	        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
	        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
	        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

	        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
	        try {
	            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
	            List<String> resList = new ArrayList<>();
	            resList.add("default");
	            resList.add("w_shared_mysafe01");
	            resList.add("w_shared_mysafe02");
	            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        when(ControllerUtil.configureUserpassUser(eq("testuser1"),any(),eq(token))).thenReturn(responseNoContent);
	        Response responseBadContent = getMockResponse(HttpStatus.NO_CONTENT, false, "");

	        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseBadContent);
	        
	        //oidc test cases
	        String mountAccessor = "auth_oidc";
	        DirectoryUser directoryUser = new DirectoryUser();
	        directoryUser.setDisplayName("testUser");
	        directoryUser.setGivenName("testUser");
	        directoryUser.setUserEmail("testUser@t-mobile.com");
	        directoryUser.setUserId("testuser01");
	        directoryUser.setUserName("testUser");
	        
	        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "oidc");

	        List<DirectoryUser> persons = new ArrayList<>();
	        persons.add(directoryUser);

	        DirectoryObjects users = new DirectoryObjects();
	        DirectoryObjectsList usersList = new DirectoryObjectsList();
	        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
	        users.setData(usersList);
	        
				OIDCLookupEntityRequest oidcLookupEntityRequest = new OIDCLookupEntityRequest();
				oidcLookupEntityRequest.setId(null);
				oidcLookupEntityRequest.setAlias_id(null);
				oidcLookupEntityRequest.setName(null);
				oidcLookupEntityRequest.setAlias_name(directoryUser.getUserEmail());
				oidcLookupEntityRequest.setAlias_mount_accessor(mountAccessor);
				OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
				oidcEntityResponse.setEntityName("entity");
				List<String> policies = new ArrayList<>();
				policies.add("safeadmin");
				oidcEntityResponse.setPolicies(policies);
				when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

				ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(oidcEntityResponse);

				when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
		
				Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
				when(OIDCUtil.updateOIDCEntity(policies, oidcEntityResponse.getEntityName()))
						.thenReturn(responseEntity3);
	        when(OIDCUtil.oidcFetchEntityDetails(token, "testuser1", userDetails, true)).thenReturn(responseEntity2);
	        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
	        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
	        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));
	        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
            assertNotNull(responseEntity);
	    }
	 
	 @Test
	    public void test_addGroupToSafe_failure1() {
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
	        UserDetails userDetails = new UserDetails();
	        userDetails.setUsername("testuser1");
	        when(ControllerUtil.areSafeGroupInputsValid(safeGroup)).thenReturn(false);
	        ResponseEntity<String> responseEntity = safesService.addGroupToSafe(token, safeGroup, userDetails);
	        assertNotNull(responseEntity);
	    }
	 
	 @Test
	    public void test_addGroupToSafe_failure2() {
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
	        UserDetails userDetails = new UserDetails();
	        userDetails.setUsername("testuser1");
	        when(ControllerUtil.areSafeGroupInputsValid(safeGroup)).thenReturn(true);
	   	    ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "userpass");

	        ResponseEntity<String> responseEntity = safesService.addGroupToSafe(token, safeGroup, userDetails);
	        assertNotNull(responseEntity);
	    }
	 
	 @Test
	    public void test_removeUserFromSafe_ldap_failure() {
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        String path = "shared/mysafe01";
	        SafeUser safeUser = new SafeUser(path, "testuser1","write");
	        UserDetails userDetails = new UserDetails();
	        userDetails.setUsername("testuser1");
	        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
	        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

	        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
	        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
	        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
	        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

	        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
	        try {
	            //when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
	            List<String> resList = new ArrayList<>();
	            resList.add("default");
	            resList.add("w_shared_mysafe01");
	            resList.add("w_shared_mysafe02");
	            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        Response responseBadContent = getMockResponse(HttpStatus.NO_CONTENT, false, "");

	        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseBadContent);
	        try{
	        List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
	        ReflectionTestUtils.setField(safesService, "vaultAuthMethod", "ldap");
	        when(ControllerUtil.configureLDAPUser(any(),any(),any(), any())).thenReturn(responseNoContent);
	        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
	        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
	        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(Arrays.asList("mysafe02"));
	        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser, userDetails);
         assertNotNull(responseEntity);
	    }
	 
}