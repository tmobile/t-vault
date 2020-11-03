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
package com.tmobile.cso.vault.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.model.SSCred;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class TokenUtilsTest {

    @InjectMocks
    TokenUtils tokenUtils;

    @Mock
    RequestProcessor reqProcessor;
    
    @Mock
    Response response;
    
    @Before
    public void setUp() {
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(ControllerUtil.class, "reqProcessor", reqProcessor);
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
    
	private String getJSON(Object obj)  {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return "{}";
		}
	}

	@Test
    public void test_getSelfServiceToken_success() throws Exception {

        String jsonStr = "{\"username\":\"testadmin\",\"password\":\"testadmin\"}";

        ReflectionTestUtils.setField(tokenUtils, "selfserviceUsername", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "selfservicePassword", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "vaultAuthMethod", "userpass");

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"7QPMPIGiyDFlJkrK3jFykUqa\",\"admin\":\"yes\",\"access\":{},\"policies\":[\"default\",\"testadmin\"],\"lease_duration\":1800000}");
        when(reqProcessor.process("/auth/userpass/login",jsonStr,"")).thenReturn(response);
        String token = tokenUtils.getSelfServiceToken();
        assertEquals("7QPMPIGiyDFlJkrK3jFykUqa", token);
    }

    @Test
    public void test_getSelfServiceToken_success_ldap() throws Exception {

        String jsonStr = "{\"username\":\"testadmin\",\"password\":\"testadmin\"}";

        ReflectionTestUtils.setField(tokenUtils, "selfserviceUsername", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "selfservicePassword", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "vaultAuthMethod", "ldap");

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"7QPMPIGiyDFlJkrK3jFykUqa\",\"admin\":\"yes\",\"access\":{},\"policies\":[\"default\",\"testadmin\"],\"lease_duration\":1800000}");
        when(reqProcessor.process("/auth/ldap/login",jsonStr,"")).thenReturn(response);
        String token = tokenUtils.getSelfServiceToken();
        assertEquals("7QPMPIGiyDFlJkrK3jFykUqa", token);
    }

    @Test
    public void test_getSelfServiceToken_failure() throws Exception {

        String jsonStr = "{\"username\":\"testadmin\",\"password\":\"testadmin\"}";

        ReflectionTestUtils.setField(tokenUtils, "selfserviceUsername", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "selfservicePassword", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "vaultAuthMethod", "userpass");

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process("/auth/userpass/login",jsonStr,"")).thenReturn(response);
        String token = tokenUtils.getSelfServiceToken();
        assertNull(token);
    }

    @Test
    public void test_revokePowerToken_success() {
    	String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	Response res = getMockResponse(HttpStatus.NO_CONTENT, true, "");
    	when(reqProcessor.process("/auth/tvault/revoke","{}", token)).thenReturn(res);
    	tokenUtils.revokePowerToken(token);
    }

    @Test
    public void test_revokePowerToken_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response res = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process("/auth/tvault/revoke","{}", token)).thenReturn(res);
        tokenUtils.revokePowerToken(token);
    }

    @Test
    public void test_getSelfServiceToken_success_approle() throws Exception {

        String jsonStr = "{\"roleId\":\"a736d57a-ac97-a08d-53ab-eb5ab16d03b5\",\"secretId\":\"10884921-79d6-fde1-3f28-56af75d83616\"}";

        ReflectionTestUtils.setField(tokenUtils, "selfserviceUsername", "YTczNmQ1N2EtYWM5Ny1hMDhkLTUzYWItZWI1YWIxNmQwM2I1");
        ReflectionTestUtils.setField(tokenUtils, "selfservicePassword", "MTA4ODQ5MjEtNzlkNi1mZGUxLTNmMjgtNTZhZjc1ZDgzNjE2");
        ReflectionTestUtils.setField(tokenUtils, "vaultAuthMethod", "userpass");
        ReflectionTestUtils.setField(tokenUtils, "selfServiceTokenGenerator", "approle");

        when(JSONUtil.getJSON(Mockito.any(AppRoleIdSecretId.class))).thenReturn(jsonStr);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"auth\":{\"client_token\":\"6WcC0r5Nw9z0RILHEJnn0OCB\",\"accessor\":\"2xmlSRCSxGb0AlopA1JfQyuY\",\"policies\":[\"default\",\"selfservicesupport\"],\"token_policies\":[\"default\",\"selfservicesupport\"],\"metadata\":{\"role_name\":\"selfservicesupportrole\"},\"lease_duration\":2764800,\"renewable\":true,\"entity_id\":\"eb74b6b1-b9a0-fd59-ff33-5813be3fdbbf\"},\"data\":null,\"lease_duration\":0,\"lease_id\":\"\"}");
        when(reqProcessor.process(eq("/auth/approle/login"),Mockito.any(),eq(""))).thenReturn(response);
        String token = tokenUtils.getSelfServiceToken();
        assertEquals("6WcC0r5Nw9z0RILHEJnn0OCB", token);
    }

    @Test
    public void test_getSelfServiceToken_failure_approle() throws Exception {

        String jsonStr = "{\"roleId\":\"a736d57a-ac97-a08d-53ab-eb5ab16d03b5\",\"secretId\":\"10884921-79d6-fde1-3f28-56af75d83616\"}";

        ReflectionTestUtils.setField(tokenUtils, "selfserviceUsername", "YTczNmQ1N2EtYWM5Ny1hMDhkLTUzYWItZWI1YWIxNmQwM2I1");
        ReflectionTestUtils.setField(tokenUtils, "selfservicePassword", "MTA4ODQ5MjEtNzlkNi1mZGUxLTNmMjgtNTZhZjc1ZDgzNjE2");
        ReflectionTestUtils.setField(tokenUtils, "vaultAuthMethod", "userpass");
        ReflectionTestUtils.setField(tokenUtils, "selfServiceTokenGenerator", "approle");

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process("/auth/approle/login",jsonStr,"")).thenReturn(response);
        String token = tokenUtils.getSelfServiceToken();
        assertNull(token);
    }
    
	@Test
    public void test_getSelfServiceTokenWithAppRole_success() throws Exception {

        String jsonStr = "{\"role_id\":\"testroleid\",\"secret_id\":\"testsecretid\"}";

        ReflectionTestUtils.setField(tokenUtils, "selfserviceUsername", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "selfservicePassword", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "vaultAuthMethod", "userpass");
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        Response response = getMockResponse(HttpStatus.OK, true, "{\n" + 
        		"  \"auth\": {\n" + 
        		"    \"client_token\": \"s.6tdd9QSp50YwJwjXViRXGnMQT4\",\n" + 
        		"    \"accessor\": \"7M3bp7PTaOHeB4u2J9zVfE9g\",\n" + 
        		"    \"policies\": [\n" + 
        		"      \"default\",\n" + 
        		"      \"string\"\n" + 
        		"    ],\n" + 
        		"    \"token_policies\": [\n" + 
        		"      \"default\",\n" + 
        		"      \"string\"\n" + 
        		"    ],\n" + 
        		"    \"metadata\": {\n" + 
        		"      \"role_name\": \"approle\"\n" + 
        		"    },\n" + 
        		"    \"lease_duration\": 2764800,\n" + 
        		"    \"renewable\": true,\n" + 
        		"    \"entity_id\": \"5e191de5-7b96-b9cc-71d5-d254b636308b\",\n" + 
        		"    \"token_type\": \"service\"\n" + 
        		"  },\n" + 
        		"  \"data\": null,\n" + 
        		"  \"lease_duration\": 0,\n" + 
        		"  \"lease_id\": \"\"\n" + 
        		"}");
        when(reqProcessor.process("/auth/approle/login",jsonStr,"")).thenReturn(response);
        String token = tokenUtils.getSelfServiceTokenWithAppRole();
        assertEquals("s.6tdd9QSp50YwJwjXViRXGnMQT4", token);
    }
	
	@Test
    public void test_getSelfServiceTokenWithAppRole_sscred_success() throws Exception {

		String jsonStr = "{\"role_id\":\"testroleid\",\"secret_id\":\"testsecretid\"}";

        ReflectionTestUtils.setField(tokenUtils, "selfserviceUsername", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "selfservicePassword", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "vaultAuthMethod", "userpass");
        
        SSCred sscred = new SSCred();
        sscred.setUsername("dGVzdGFkbWlu");
        sscred.setPassword("dGVzdGFkbWlu");
        
        ReflectionTestUtils.setField(ControllerUtil.class,"sscred", sscred);
        ReflectionTestUtils.setField(ControllerUtil.class,"ssUsername", sscred.getUsername());
        ReflectionTestUtils.setField(ControllerUtil.class,"ssPassword", sscred.getPassword());
        
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        Response response = getMockResponse(HttpStatus.OK, true, "{\n" + 
        		"  \"auth\": {\n" + 
        		"    \"client_token\": \"s.6tdd9QSp50YwJwjXViRXGnMQT4\",\n" + 
        		"    \"accessor\": \"7M3bp7PTaOHeB4u2J9zVfE9g\",\n" + 
        		"    \"policies\": [\n" + 
        		"      \"default\",\n" + 
        		"      \"string\"\n" + 
        		"    ],\n" + 
        		"    \"token_policies\": [\n" + 
        		"      \"default\",\n" + 
        		"      \"string\"\n" + 
        		"    ],\n" + 
        		"    \"metadata\": {\n" + 
        		"      \"role_name\": \"approle\"\n" + 
        		"    },\n" + 
        		"    \"lease_duration\": 2764800,\n" + 
        		"    \"renewable\": true,\n" + 
        		"    \"entity_id\": \"5e191de5-7b96-b9cc-71d5-d254b636308b\",\n" + 
        		"    \"token_type\": \"service\"\n" + 
        		"  },\n" + 
        		"  \"data\": null,\n" + 
        		"  \"lease_duration\": 0,\n" + 
        		"  \"lease_id\": \"\"\n" + 
        		"}");
        when(reqProcessor.process("/auth/approle/login",jsonStr,"")).thenReturn(response);
        String token = tokenUtils.getSelfServiceTokenWithAppRole();
        assertEquals("s.6tdd9QSp50YwJwjXViRXGnMQT4", token);
    }
	
	@Test
    public void test_getSelfServiceTokenWithAppRole_sscred_failure() throws Exception {

		String jsonStr = "{\"role_id\":\"testroleid\",\"secret_id\":\"testsecretid\"}";

        ReflectionTestUtils.setField(tokenUtils, "selfserviceUsername", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "selfservicePassword", "dGVzdGFkbWlu");
        ReflectionTestUtils.setField(tokenUtils, "vaultAuthMethod", "userpass");
        
        SSCred sscred = new SSCred();
        sscred.setUsername("dGVzdGFkbWlu");
        sscred.setPassword("dGVzdGFkbWlu");
        ReflectionTestUtils.setField(ControllerUtil.class,"sscred", sscred);
        ReflectionTestUtils.setField(ControllerUtil.class,"ssUsername", sscred.getUsername());
        ReflectionTestUtils.setField(ControllerUtil.class,"ssPassword", sscred.getPassword());
        
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\n" + 
        		"  \"error\": \"no response from server\"\n" + 
        		"}");
        when(reqProcessor.process("/auth/approle/login",jsonStr,"")).thenReturn(response);
        String token = tokenUtils.getSelfServiceTokenWithAppRole();
        assertNull(token);
    }
}
