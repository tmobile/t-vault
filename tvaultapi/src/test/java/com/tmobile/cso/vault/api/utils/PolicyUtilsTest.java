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

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
@Ignore
public class PolicyUtilsTest {
    @InjectMocks
    PolicyUtils policyUtils;

    @Mock
    RequestProcessor reqProcessor;

    @Mock
    Response response;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(ControllerUtil.class);
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(ControllerUtil.class, "reqProcessor", reqProcessor);
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");
        when(ControllerUtil.getReqProcessor()).thenReturn(reqProcessor);
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
    public void test_getCurrentPolicies_successfully() throws IOException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ReflectionTestUtils.setField(policyUtils, "vaultAuthMethod", "userpass");
        String[] expectedPolicies = {"s_shared_mysafe01", "s_shared_mysafe02"};

        Response response =  getMockResponse(HttpStatus.OK, true, "{\"data\":{\"policies\":\"s_shared_mysafe01,s_shared_mysafe02\"}}");
        when(ControllerUtil.getReqProcessor().process("/auth/userpass/read","{\"username\":\"normaluser\"}",token)).thenReturn(response);
        when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), eq("{\"data\":{\"policies\":\"s_shared_mysafe01,s_shared_mysafe02\"}}"))).thenReturn("s_shared_mysafe01,s_shared_mysafe02");
        String[] policies = policyUtils.getCurrentPolicies(token, "normaluser");
        assertEquals(expectedPolicies, policies);
    }

    @Test
    public void test_getCurrentPolicies_successfully_ldap() throws IOException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ReflectionTestUtils.setField(policyUtils, "vaultAuthMethod", "ldap");
        String[] expectedPolicies = {"s_shared_mysafe01", "s_shared_mysafe02"};

        Response response =  getMockResponse(HttpStatus.OK, true, "{\"data\":{\"policies\":\"s_shared_mysafe01,s_shared_mysafe02\"}}");
        when(ControllerUtil.getReqProcessor().process("/auth/ldap/users","{\"username\":\"normaluser\"}",token)).thenReturn(response);
        when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), eq("{\"data\":{\"policies\":\"s_shared_mysafe01,s_shared_mysafe02\"}}"))).thenReturn("s_shared_mysafe01,s_shared_mysafe02");
        String[] policies = policyUtils.getCurrentPolicies(token, "normaluser");
        assertEquals(expectedPolicies, policies);
    }

    @Test
    public void test_getCurrentPolicies_failure() throws IOException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ReflectionTestUtils.setField(policyUtils, "vaultAuthMethod", "userpass");
        Response response =  getMockResponse(HttpStatus.OK, true, "{\"data\":{\"policies\":\"s_shared_mysafe01,s_shared_mysafe02\"}}");
        when(ControllerUtil.getReqProcessor().process("/auth/userpass/read","{\"username\":\"normaluser\"}",token)).thenReturn(response);
        when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), eq("{\"data\":{\"policies\":\"s_shared_mysafe01,s_shared_mysafe02\"}}"))).thenThrow(IOException.class);
        String[] policies = policyUtils.getCurrentPolicies(token, "normaluser");
        String[] expectedPolicies = {};
        assertEquals(expectedPolicies, policies);
    }

    @Test
    public void test_getPoliciesTobeCheked() {
        List<String> expectedPolicies = new ArrayList<>();
        expectedPolicies.add("safeadmin");
        expectedPolicies.add("s_users_mysafe01");
        List<String> policies = policyUtils.getPoliciesTobeCheked("users", "mysafe01");
        assertEquals(expectedPolicies, policies);
    }
}
