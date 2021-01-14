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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeBasicDetails;
import com.tmobile.cso.vault.api.model.SafeUser;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SafeUtilsTest {
    @InjectMocks
    SafeUtils safeUtils;

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
        response.setResponse("");
        if (expectedBody != "") {
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
    public void test_getPoliciesForManagedSafes_successfully() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":[\"approle_normal_user\",\"default\",\"s_users_ert\"],\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        List<String> expectedAdminPolicies = new ArrayList<>();
        expectedAdminPolicies.add("s_users_ert");
        List<String> adminPolicies = safeUtils.getPoliciesForManagedSafes(policiesJsonNode);
        assertEquals(expectedAdminPolicies, adminPolicies);
    }

    @Test
    public void testGetPoliciesForManagedSafesEmptyNodes() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":[\"approle_normal_user\",\"default\",\"s_users_ert\"],\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        List<String> expectedAdminPolicies = new ArrayList<>();
        expectedAdminPolicies.add("s_users_ert");
        List<String> adminPolicies = safeUtils.getPoliciesForManagedSafes(null);
    }

    @Test
    public void testGetPoliciesForManagedSafesEmptyContainerNode() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":\"\",\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        List<String> expectedAdminPolicies = new ArrayList<>();
        expectedAdminPolicies.add("s_users_ert");
        List<String> adminPolicies = safeUtils.getPoliciesForManagedSafes(policiesJsonNode);
    }

    @Test
    public void testGetPoliciesForManagedSafesInvalidPolicy() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":[\"approle_normal_user\",\"default\",\"s_users_ert\"],\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        List<String> expectedAdminPolicies = new ArrayList<>();
        expectedAdminPolicies.add("d_users_ert");
        List<String> adminPolicies = safeUtils.getPoliciesForManagedSafes(policiesJsonNode);
    }

    @Test
    public void testGetPoliciesForManagedSafesEmptyPolicy() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":[\"approle_normal_user\",\"default\"],\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        List<String> expectedAdminPolicies = new ArrayList<>();
        expectedAdminPolicies.add("d_users_ert");
        List<String> adminPolicies = safeUtils.getPoliciesForManagedSafes(policiesJsonNode);
    }

    @Test
    public void test_getManagedSafesFromPolicies_successfully() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":[\"approle_normal_user\",\"default\",\"s_users_ert\"],\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        String[] expectedList = {"ert"};
        String[] policies = {"s_users_ert"};
        String[] policiesRes = safeUtils.getManagedSafes(policies, "users");
        assertEquals(expectedList, policiesRes);
    }

    @Test
    public void testGetManagedSafesFromPoliciesEmpty() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":[\"approle_normal_user\",\"default\",\"s_users_ert\"],\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        String[] expectedList = {"ert"};
        String[] policies = {"s_users_ert"};
        String[] policiesRes = safeUtils.getManagedSafes(null, "users");
    }

    @Test
    public void testGetManagedSafesFromPoliciesNotValid() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":[\"approle_normal_user\",\"default\",\"d_users_ert\"],\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        String[] expectedList = {"ert"};
        String[] policies = {"d_users_ert"};
        String[] policiesRes = safeUtils.getManagedSafes(policies, "users");
    }

    @Test
    public void testGetManagedSafesFromPoliciesInvalidSafetype() throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        Response response = getMockResponse(HttpStatus.OK, true, "{\"client_token\":\"8zyIbj3i9hXJFuIPC5AzeUK3\",\"admin\":\"no\",\"access\":{},\"policies\":[\"approle_normal_user\",\"default\",\"s_group_ert\"],\"lease_duration\":1800000}");
        JsonNode policiesJsonNode = objMapper.readTree(response.getResponse().toString()).get("policies");
        String[] expectedList = {"ert"};
        String[] policies = {"s_groups_ert"};
        String[] policiesRes = safeUtils.getManagedSafes(policies, "users");
    }

    @Test
    public void test_canAddOrRemoveUser_successfully() {
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser", "write");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":" +
                "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"}," +
                "\"description\":\"asd\",\"name\":\"ert\",\"owner\":\"sd@g.com\",\"ownerid\":\"normaluser\"," +
                "\"appName\":\"tvt\","+
                "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertTrue(canAdd);
    }

	@Test
	public void testGetSafeMetaDataSuccess() throws JsonProcessingException, IOException {
		String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";
		Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":"
				+ "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"},"
				+ "\"description\":\"asd\",\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"ownerid\":\"normaluser\","
				+ "\"appName\":\"tvt\","
				+ "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");
		SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null,
				"My first safe", "normaluser","tvt");
		Safe safe = new Safe("users/mysafe01", safeBasicDetails);

		when(ControllerUtil.getSafeType("users/mysafe01")).thenReturn("users");
		when(ControllerUtil.getSafeName("users/mysafe01")).thenReturn("mysafe01");
		when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/mysafe01\"}",
				"5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
		ObjectMapper objMapper = new ObjectMapper();
		JsonNode dataNode = objMapper.readTree(response.getResponse().toString()).get("data");
		Safe safeInfo=safeUtils.getSafeInfo(dataNode);
		Safe safeRes = safeUtils.getSafeMetaData("5PDrOhsy4ig8L3EpsJZSLAMg", "users", "mysafe01");
		assertEquals(safe.getSafeBasicDetails().getName(), safeInfo.getSafeBasicDetails().getName());
	}

	@Test
	public void testGetSafeMetaDataEmpty() {
		String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";
		Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\":{\"aws-roles\":"
				+ "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"},"
				+ "\"description\":\"asd\",\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"ownerid\":\"normaluser\","
				+ "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");
		SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null,
				"My first safe", "normaluser");
		Safe safe = new Safe("users/mysafe01", safeBasicDetails);

		when(ControllerUtil.getSafeType("users/mysafe01")).thenReturn("users");
		when(ControllerUtil.getSafeName("users/mysafe01")).thenReturn("mysafe01");
		when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/mysafe01\"}",
				"5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
		Safe safeRes = safeUtils.getSafeMetaData("5PDrOhsy4ig8L3EpsJZSLAMg", "users", "mysafe01");
	}

    @Test
    public void test_getSafeMetaData_failure() {
        Response response = getMockResponse(HttpStatus.OK, true, "");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        Safe safe = safeUtils.getSafeMetaData("5PDrOhsy4ig8L3EpsJZSLAMg", "users", "ert");
        assertNull(safe);
    }

    @Test
    public void test_canAddOrRemoveUser_successfully_admin() {
        UserDetails userDetails = getMockUser(true);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser", "write");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":" +
                "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"}," +
                "\"description\":\"asd\",\"name\":\"ert\",\"owner\":\"sd@g.com\",\"ownerid\":\"normaluser\"," +
                "\"appName\":\"tvt\","+
                "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertTrue(canAdd);
    }

    @Test
    public void test_canAddOrRemoveUser_failure_owner_deny() {
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser", "deny");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":" +
                "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"}," +
                "\"description\":\"asd\",\"name\":\"ert\",\"owner\":\"sd@g.com\",\"ownerid\":\"normaluser\"," +
                "\"appName\":\"tvt\","+
                "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertFalse(canAdd);
    }

    @Test
    public void test_canAddOrRemoveUser_failure_owner_deny_admin() {
        UserDetails userDetails = getMockUser(true);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser", "deny");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":" +
                "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"}," +
                "\"description\":\"asd\",\"name\":\"ert\",\"owner\":\"sd@g.com\",\"ownerid\":\"normaluser\"," +
                "\"appName\":\"tvt\","+
                "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertFalse(canAdd);
    }

    @Test
    public void test_canAddOrRemoveUser_failure_admin() {
        UserDetails userDetails = getMockUser(true);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser", "write");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":" +
                "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"}," +
                "\"description\":\"asd\",\"name\":\"ert\",\"owner\":\"sd@g.com\"," +
                "\"appName\":\"tvt\","+
                "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertTrue(canAdd);
    }

    @Test
    public void test_canAddOrRemoveUser_successfully_for_normaluser() {
        UserDetails userDetails = getMockUser(true);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser", "write");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":" +
                "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"}," +
                "\"description\":\"asd\",\"name\":\"ert\",\"owner\":\"sd@g.com\",\"ownerid\":\"normaluser1\"," +
                "\"appName\":\"tvt\","+
                "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertTrue(canAdd);
    }

    @Test
    public void test_canAddOrRemoveUser_failure_normaluser() {
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser", "write");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":" +
                "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"}," +
                "\"description\":\"asd\",\"name\":\"ert\",\"owner\":\"sd@g.com\",\"ownerid\":\"normaluser1\"," +
                "\"appName\":\"tvt\","+
                "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertFalse(canAdd);
    }

    @Test
    public void test_canAddOrRemoveUser_successfully_normaluser() {
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser1", "write");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"aws-roles\":" +
                "{\"erole\":\"read\",\"role1\":\"read\",\"role22\":\"read\",\"testrole3\":\"read\"}," +
                "\"description\":\"asd\",\"name\":\"ert\",\"owner\":\"sd@g.com\",\"ownerid\":\"normaluser\"," +
                "\"appName\":\"tvt\","+
                "\"type\":\"\",\"users\":{\"normaluser\":\"sudo\",\"normaluser2\":\"read\"}}}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertTrue(canAdd);
    }

    @Test
    public void test_canAddOrRemoveUser_failure_empty_safetype() {
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("ert", "normaluser", "write");

        when(ControllerUtil.getSafeType("ert")).thenReturn("");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertFalse(canAdd);
    }

    @Test
    public void testCanAddOrRemoveUserFailureEmptySafeName() {
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("ert", "normaluser", "write");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("");
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertFalse(canAdd);
    }

    @Test
    public void testCanAddOrRemoveUserFailedSafeMetadataEmpty() {
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("users/ert", "normaluser1", "write");

        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "{}");

        when(ControllerUtil.getSafeType("users/ert")).thenReturn("users");
        when(ControllerUtil.getSafeName("users/ert")).thenReturn("ert");
        when(ControllerUtil.getReqProcessor().process("/sdb", "{\"path\":\"metadata/users/ert\"}", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(response);
        boolean canAdd = safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser");
        assertFalse(canAdd);
    }

}
