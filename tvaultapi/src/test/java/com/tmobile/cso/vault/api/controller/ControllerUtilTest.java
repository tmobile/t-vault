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

package com.tmobile.cso.vault.api.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.AWSAuthLogin;
import com.tmobile.cso.vault.api.model.AWSAuthType;
import com.tmobile.cso.vault.api.model.AWSIAMLogin;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.AWSRole;
import com.tmobile.cso.vault.api.model.AppRole;
import com.tmobile.cso.vault.api.model.AppRoleSecretData;
import com.tmobile.cso.vault.api.model.SSCred;
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeAppRoleAccess;
import com.tmobile.cso.vault.api.model.SafeBasicDetails;
import com.tmobile.cso.vault.api.model.SafeGroup;
import com.tmobile.cso.vault.api.model.SafeUser;
import com.tmobile.cso.vault.api.model.SecretData;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class ControllerUtilTest {

    @Mock
    RequestProcessor reqProcessor;
    
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
    public void test_configureLDAPUser_successfully() {
        String userName = "normaluser";
        String policies = "{\"default\"}";
        String groups = "group1";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/ldap/users/configure"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureLDAPUser(userName, policies, groups, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_configureUserpassUser_successfully() {
        String userName = "normaluser";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/userpass/updatepolicy"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureUserpassUser(userName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }


    @Test
    public void test_configureLDAPGroup_successfully() {
        String groupName = "group1";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/ldap/groups/configure"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureLDAPGroup(groupName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_configureAWSRole_successfully() {
        String roleName = "role1";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/aws/roles/update"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureAWSRole(roleName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_configureAWSIAMRole_successfully() {
        String roleName = "role1";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/aws/iam/roles/update"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureAWSIAMRole(roleName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_updateMetadata_successfully() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String path = "users/safe01";
        Map<String,String> params = new HashMap<>();
        params.put("type", "users");
        params.put("name", "safe01");
        params.put("access", "write");
        params.put("path", path);
        String pathjson ="{\"path\":\"metadata/"+path+"\"}";

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"description\":\"My first safe\",\"name\":\"safe01\",\"owner\":\"youremail@yourcompany.com\",\"ownerid\":\"normaluser\",\"type\":\"\"}}");
        when(reqProcessor.process("/read",pathjson,token)).thenReturn(metaResponse);
        Response response = getMockResponse(HttpStatus.CREATED, true, "");
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);

        Response actualResponse = ControllerUtil.updateMetadata(params, token);
        assertEquals(HttpStatus.CREATED, actualResponse.getHttpstatus());
    }

    @Test
    public void test_updateMetaDataOnConfigChanges_successfully() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response actualResponse = ControllerUtil.updateMetaDataOnConfigChanges("role1", "roles", "", "\"[prod, dev\"]", token);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
    }

    @Test
    public void test_updateMetaDataOnConfigChanges_successfully_() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"description\":\"My first safe\",\"name\":\"safe01\",\"owner\":\"youremail@yourcompany.com\",\"ownerid\":\"normaluser\",\"type\":\"\"}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);

        Response actualResponse = ControllerUtil.updateMetaDataOnConfigChanges("role1", "roles", "", "\"[prod, w_users_safe01\"]", token);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
    }

    @Test
    public void test_updateMetaDataOnConfigChanges_successfully_existing_policies() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"description\":\"My first safe\",\"name\":\"safe01\",\"owner\":\"youremail@yourcompany.com\",\"ownerid\":\"normaluser\",\"type\":\"\"}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);

        Response actualResponse = ControllerUtil.updateMetaDataOnConfigChanges("role1", "roles", "\"[w_users_safe01\"]", "\"[prod, dev\"]", token);
        assertEquals(HttpStatus.MULTI_STATUS, actualResponse.getHttpstatus());
        assertEquals("Meta data update failed for [users/safe01\"]]", actualResponse.getResponse());
    }

    @Test
    public void test_updateMetadataOnSvcaccPwdReset_successfully_update_initialResetStatus() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Map<String,String> params = new HashMap<String,String>();
        params.put("type", "initialPasswordReset");
        params.put("path",new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append("testacc02").toString());
        params.put("value","true");
        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{ \"initialPasswordReset\": false,\"managedBy\": \"snagara14\",\"name\": \"svc_vault_test2\",\"users\": {\"snagara14\": \"sudo\"}}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);
        Response response = getMockResponse(HttpStatus.OK, true, "");
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);

        Response actualResponse = ControllerUtil.updateMetadataOnSvcaccPwdReset(params, token);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
        assertEquals(response, actualResponse);
    }

    @Test
    public void test_updateMetadataOnSvcaccPwdReset_successfully() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Map<String,String> params = new HashMap<String,String>();
        params.put("type", "initialPasswordReset");
        params.put("path",new StringBuffer(TVaultConstants.SVC_ACC_ROLES_PATH).append("testacc02").toString());
        params.put("value","true");
        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{ \"initialPasswordReset\": true,\"managedBy\": \"snagara14\",\"name\": \"svc_vault_test2\",\"users\": {\"snagara14\": \"sudo\"}}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);

        Response actualResponse = ControllerUtil.updateMetadataOnSvcaccPwdReset(params, token);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
        assertEquals(metaResponse, actualResponse);
    }

    @Test
    public void test_parseJson_successfully() {
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\"}";

        Map<String,Object> actualResponse = ControllerUtil.parseJson(jsonStr);
        assertEquals("testuser", actualResponse.get("username"));
        assertEquals("testuser", actualResponse.get("password"));
    }

    @Test
    public void test_parseJson_error() {
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\",}";

        Map<String,Object> actualResponse = ControllerUtil.parseJson(jsonStr);
        assertTrue(actualResponse.isEmpty());
    }

    @Test
    public void test_convetToJson_successfully() {
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\"}";

        Map<String,Object> jsonmap = new LinkedHashMap<>();
        jsonmap.put("username", "testuser");
        jsonmap.put("password", "testuser");
        String actualResponse = ControllerUtil.convetToJson(jsonmap);
        assertEquals(jsonStr, actualResponse);
    }

    @Test
    public void test_getPoliciesAsStringFromJson_successfully() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String policyjson = "{\"data\":{\"policies\":[\"w_users_safe02\",\"w_users_safe01\"]}}";
        String actualResponse = ControllerUtil.getPoliciesAsStringFromJson(mapper, policyjson);
        assertEquals("w_users_safe02,w_users_safe01", actualResponse);
    }

    @Test
    public void test_updateUserPolicyAssociationOnSDBDelete_successfully() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String userName = "testuser1";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"approle_normal_user\",\"w_users_safe01\"],\"ttl\":0}}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token)).thenReturn(userResponse);

        ReflectionTestUtils.setField(ControllerUtil.class,"vaultAuthMethod", "userpass");
        Map<String,String> acessInfo = new HashMap<>();
        acessInfo.put("testuser1", "write");

        ControllerUtil.updateUserPolicyAssociationOnSDBDelete("users/safe01", acessInfo,  token);
        assertTrue(true);
    }

    @Test
    public void test_updateGroupPolicyAssociationOnSDBDelete_successfully() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String groupName = "group1";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"approle_normal_user\",\"w_users_safe01\"],\"ttl\":0}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token)).thenReturn(userResponse);

        ReflectionTestUtils.setField(ControllerUtil.class,"vaultAuthMethod", "ldap");
        Map<String,String> acessInfo = new HashMap<>();
        acessInfo.put("group1", "write");

        ControllerUtil.updateGroupPolicyAssociationOnSDBDelete("users/safe01", acessInfo,  token);
        assertTrue(true);
    }

    @Test
    public void test_updateAwsRolePolicyAssociationOnSDBDelete_successfully() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String role = "role1";
        Response roleResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"approle_normal_user\",\"w_users_safe01\"]}");
        when( reqProcessor.process("/auth/aws/roles","{\"role\":\""+role+"\"}",token)).thenReturn(roleResponse);

        ReflectionTestUtils.setField(ControllerUtil.class,"vaultAuthMethod", "ldap");
        Map<String,String> acessInfo = new HashMap<>();
        acessInfo.put("role1", "write");

        ControllerUtil.updateAwsRolePolicyAssociationOnSDBDelete("users/safe01", acessInfo,  token);
        assertTrue(true);
    }

    @Test
    public void test_deleteAwsRoleOnSDBDelete_successfully() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String role = "role1";
        Response roleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when( reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token)).thenReturn(roleResponse);

        ReflectionTestUtils.setField(ControllerUtil.class,"vaultAuthMethod", "ldap");
        Map<String,String> acessInfo = new HashMap<>();
        acessInfo.put("role1", "write");

        ControllerUtil.deleteAwsRoleOnSDBDelete("users/safe01", acessInfo,  token);
        assertTrue(true);
    }

    @Test
    public void test_isValidDataPath_successfully() {

        boolean valid = ControllerUtil.isValidDataPath("users/safe01/s1");
        assertEquals(true, valid);
    }

    @Test
    public void test_isValidDataPath_failure() {

        boolean valid = ControllerUtil.isValidDataPath("test/safe01/s1");
        assertEquals(false, valid);
    }

    @Test
    public void test_isValidDataPath_failure_path() {

        boolean valid = ControllerUtil.isValidDataPath("users/safe01");
        assertEquals(false, valid);
    }

    @Test
    public void test_isPathValid_successfully()  {

        boolean valid = ControllerUtil.isPathValid("users/safe01");
        assertEquals(true, valid);
    }

    @Test
    public void test_isPathValid_failure()  {

        boolean valid = ControllerUtil.isPathValid("test/safe01");
        assertEquals(false, valid);
    }

    @Test
    public void test_isPathValid_failure_invalid_path()  {

        boolean valid = ControllerUtil.isPathValid("safe01");
        assertEquals(false, valid);
    }

    @Test
    public void test_isValidSafePath_successfully()  {

        boolean valid = ControllerUtil.isValidSafePath("users/safe01");
        assertEquals(true, valid);
    }

    @Test
    public void test_isValidSafePath_failure()  {

        boolean valid = ControllerUtil.isValidSafePath("test/safe01");
        assertEquals(false, valid);
    }

    @Test
    public void test_isValidSafePath_failure_invalid_path()  {

        boolean valid = ControllerUtil.isValidSafePath("users");
        assertEquals(false, valid);
    }

    @Test
    public void test_getSafePath_successfully()  {

        String path = ControllerUtil.getSafePath("users/safes/safe01");
        assertEquals("users/safes", path);
    }

    @Test
    public void test_getSafeType_successfully()  {

        String safeType = ControllerUtil.getSafeType("users/safes/safe01");
        assertEquals("users", safeType);
    }

    @Test
    public void test_getSafeName_successfully()  {

        String safeName = ControllerUtil.getSafeName("users/safes/safe01");
        assertEquals("safes", safeName);
    }

    @Test
    public void test_canAddPermission_successfully()  {

        String path = "users/safe01";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"safe01\", \"safe02\"]}");
        when(reqProcessor.process("/sdb/list","{\"path\":\"metadata/users\"}",token)).thenReturn(response);

        boolean valid = ControllerUtil.canAddPermission("users/safe01", token);
        assertTrue(valid);
    }

    @Test
    public void test_canAddPermission_failure()  {

        String path = "users/safe01";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"safe01\", \"safe02\"]}");
        when(reqProcessor.process("/sdb/list","{\"path\":\"metadata/users\"}",token)).thenReturn(response);

        boolean valid = ControllerUtil.canAddPermission("users/safe03", token);
        assertFalse(valid);
    }

    @Test
    public void test_isValidSafe_successfully()  {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String path = "users/safe01";
        String _path = "metadata/"+path;
        Response response = getMockResponse(HttpStatus.OK, true, "");
        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        boolean valid = ControllerUtil.isValidSafe(path, token);
        assertTrue(valid);
    }

    @Test
    public void test_isValidSafe_failure()  {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String path = "users/safe01";
        String _path = "metadata/"+path;
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        boolean valid = ControllerUtil.isValidSafe(path, token);
        assertFalse(valid);
    }

    @Test
    public void test_areSDBInputsValid_successfully()  {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        Map<String, Object> dataParam = new LinkedHashMap<>();
        dataParam.put("name", "safe01");
        dataParam.put("owner", "normaluser@g.com");
        dataParam.put("description", "Safe 01");
        requestParams.put("data", dataParam);
        requestParams.put("path", "users/safe01");
        ReflectionTestUtils.setField(ControllerUtil.class,"sdbNameAllowedCharacters", "[a-z0-9_-]+");
        boolean valid = ControllerUtil.areSDBInputsValid(requestParams);
        assertTrue(valid);
    }

    @Test
    public void test_areSDBInputsValid_safe()  {
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("safe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/safe01",safeBasicDetails);

        boolean valid = ControllerUtil.areSDBInputsValid(safe);
        assertTrue(valid);
    }

    @Test
    public void test_areSDBInputsValidForUpdate()  {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        Map<String, Object> dataParam = new LinkedHashMap<>();
        dataParam.put("name", "safe01");
        dataParam.put("owner", "normaluser@g.com");
        dataParam.put("description", "Safe 01");
        requestParams.put("data", dataParam);
        requestParams.put("path", "users/safe01");

        boolean valid = ControllerUtil.areSDBInputsValidForUpdate(requestParams);
        assertTrue(valid);
    }

    @Test
    public void test_areSafeGroupInputsValid()  {
        Map<String, String> dataParam = new LinkedHashMap<>();
        dataParam.put("groupname", "group1");
        dataParam.put("path", "users/safe01");
        dataParam.put("access", "write");

        boolean valid = ControllerUtil.areSafeGroupInputsValid(dataParam);
        assertTrue(valid);
    }

    @Test
    public void test_areAWSRoleInputsValid()  {
        Map<String, String> dataParam = new LinkedHashMap<>();
        dataParam.put("role", "role1");
        dataParam.put("path", "users/safe01");
        dataParam.put("access", "write");

        boolean valid = ControllerUtil.areAWSRoleInputsValid(dataParam);
        assertTrue(valid);
    }

    @Test
    public void test_areSafeUserInputsValid_safeuser()  {
        SafeUser safeUser = new SafeUser("users/safe01", "normaluser", "write");

        boolean valid = ControllerUtil.areSafeUserInputsValid(safeUser);
        assertTrue(valid);
    }

    @Test
    public void test_areSafeUserInputsValid()  {
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "users/safe01");
        requestMap.put("username", "normaluser");
        requestMap.put("access", "write");
        boolean valid = ControllerUtil.areSafeUserInputsValid(requestMap);
        assertTrue(valid);
    }

    @Test
    public void test_areSafeGroupInputsValid_safegroup()  {
        SafeGroup safeGroup = new SafeGroup("users/safe01", "group1", "write");

        boolean valid = ControllerUtil.areSafeGroupInputsValid(safeGroup);
        assertTrue(valid);
    }

    @Test
    public void test_areSafeAppRoleInputsValid()  {
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "users/safe01");
        requestMap.put("role_name", "role1");
        requestMap.put("access", "write");
        boolean valid = ControllerUtil.areSafeAppRoleInputsValid(requestMap);
        assertTrue(valid);
    }

    @Test
    public void test_areAWSRoleInputsValid_awsrole()  {
        AWSRole awsRole = new AWSRole("users/safe01", "role1", "write");

        boolean valid = ControllerUtil.areAWSRoleInputsValid(awsRole);
        assertTrue(valid);
    }

    @Test
    public void test_converSDBInputsToLowerCase() throws IOException {
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("Safe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("Shared/safe01",safeBasicDetails);
        String jsonStr = "{\"path\":\"Shared/Safe01\",\"safeBasicDetails\":{\"name\":\"Safe01\",\"ownwe\":\"youremail@yourcompany.com\", \"description\":\"My first safe\"}}";
        String jsonStrlowercase = "{\"path\":\"shared/safe01\",\"safeBasicDetails\":{\"name\":\"safe01\",\"ownwe\":\"youremail@yourcompany.com\", \"description\":\"My first safe\"}}";

        when(JSONUtil.getObj(jsonStr, Safe.class)).thenReturn(safe);
        when(JSONUtil.getJSON(safe)).thenReturn(jsonStrlowercase);

        String jsonLowerCase = ControllerUtil.converSDBInputsToLowerCase(jsonStr);
        assertEquals(jsonStrlowercase, jsonLowerCase);
    }

    @Test
    public void test_converSDBInputsToLowerCase_safe()  {
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("Safe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("Shared/safe01",safeBasicDetails);
        ControllerUtil.converSDBInputsToLowerCase(safe);
        assertEquals("shared/safe01", safe.getPath());
        assertEquals("safe01", safe.getSafeBasicDetails().getName());
    }

    @Test
    public void test_convertAppRoleInputsToLowerCase() throws IOException {
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, 1, 100, 0);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        when(JSONUtil.getObj(jsonStr, AppRole.class)).thenReturn(appRole);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        String json = ControllerUtil.convertAppRoleInputsToLowerCase(jsonStr);
        assertEquals(jsonStr, json);
    }

    @Test
    public void test_convertSafeAppRoleAccessToLowerCase() throws IOException {
        String [] policies = {"default"};
        String jsonStr = "{\"role_name\":\"Role1\", \"path\":\"users/safe01\", \"write\"}";
        String jsonStrLowerCase = "{\"role_name\":\"role1\", \"path\":\"users/safe01\", \"write\"}";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("Role1", "users/safe01", "write");
        when(JSONUtil.getObj(jsonStr, SafeAppRoleAccess.class)).thenReturn(safeAppRoleAccess);
        when(JSONUtil.getJSON(safeAppRoleAccess)).thenReturn(jsonStrLowerCase);
        String json = ControllerUtil.convertSafeAppRoleAccessToLowerCase(jsonStr);
        assertEquals(jsonStrLowerCase, json);
    }

    @Test
    public void test_convertAppRoleSecretIdToLowerCase() throws IOException {
        String [] policies = {"default"};
        String jsonStr = "{\"role_name\":\"Approle1\",\"data\":{\"env\":\"dev\",\"appname\":\"appl\"}}";
        String jsonStrLowerCase =  "{\"role_name\":\"approle1\",\"data\":{\"env\":\"dev\",\"appname\":\"appl\"}}";
        AppRoleSecretData appRoleSecretData = new AppRoleSecretData("Approle1", new SecretData("dev", "appl"));

        when(JSONUtil.getObj(jsonStr, AppRoleSecretData.class)).thenReturn(appRoleSecretData);
        when(JSONUtil.getJSON(appRoleSecretData)).thenReturn(jsonStrLowerCase);
        String json = ControllerUtil.convertAppRoleSecretIdToLowerCase(jsonStr);
        assertEquals(jsonStrLowerCase, json);
    }

    @Test
    public void test_areAppRoleInputsValid()  {
        String [] policies = {"default"};
        ReflectionTestUtils.setField(ControllerUtil.class, "approleAllowedCharacters", "[a-z0-9_-]+");
        AppRole appRole = new AppRole("role1", policies, true, 1, 100, 0);
        boolean valid = ControllerUtil.areAppRoleInputsValid(appRole);
        assertTrue(valid);
    }

    @Test
    public void test_areAppRoleInputsValid_invalid()  {
        String [] policies = {"default"};
        ReflectionTestUtils.setField(ControllerUtil.class, "approleAllowedCharacters", "[a-z0-9_-]+");
        AppRole appRole = new AppRole("", policies, true, 1, 100, 0);
        boolean valid = ControllerUtil.areAppRoleInputsValid(appRole);
        assertFalse(valid);
    }

    @Test
    public void test_getAppRoleObjFromString() throws IOException {
        String [] policies = {"default"};
        String jsonStr = "{\"role_name\":\"role1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        AppRole appRole = new AppRole("role1", policies, true, 1, 100, 0);
        when(JSONUtil.getObj(jsonStr, AppRole.class)).thenReturn(appRole);
        AppRole appRoleActual = ControllerUtil.getAppRoleObjFromString(jsonStr);
        assertEquals(appRole, appRoleActual);
    }

    @Test
    public void test_getAppRoleObjFromString_failure() throws IOException {
        String [] policies = {"default"};
        String jsonStr = "{\"role_names\":\"role1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        when(JSONUtil.getObj(jsonStr, AppRole.class)).thenThrow(Exception.class);
        AppRole appRoleActual = ControllerUtil.getAppRoleObjFromString(jsonStr);
        assertEquals(null, appRoleActual);
    }

    @Test
    public void test_areSecretKeysValid()  {


        String jsonStr = "{\"data\": {\"key1\": \"value1\"}}";
        ReflectionTestUtils.setField(ControllerUtil.class, "secretKeyAllowedCharacters", "[a-z0-9_-]+");

        boolean valid = ControllerUtil.areSecretKeysValid(jsonStr);
        assertTrue(valid);
    }

    @Test
    public void test_addDefaultSecretKey()  {

        Map<String, Object> requestParams = new LinkedHashMap<>();
        Map<String, Object> dataParam = new LinkedHashMap<>();
        requestParams.put("data", dataParam);

        String jsonStr = "{\"data\": {}}";
        String jsonStrDefault = "{\"data\": {\"default\":\"default\"}}";
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStrDefault);
        String actualStr = ControllerUtil.addDefaultSecretKey(jsonStr);
        assertEquals(jsonStrDefault, actualStr);
    }

    @Test
    public void test_areAwsLoginInputsValid()  {

        AWSAuthLogin awsAuthLogin = new AWSIAMLogin();
        awsAuthLogin.setIam_http_request_method("POST");
        awsAuthLogin.setIam_request_body("{}");
        awsAuthLogin.setIam_request_headers("{\"token\":\"4qJC0tWjMDIKjRDDmtcUAZBt\"}");
        awsAuthLogin.setIam_request_url("http://testurl.com");
        awsAuthLogin.setRole("testawsrole");
        awsAuthLogin.setPkcs7("MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====");
        boolean valid = ControllerUtil.areAwsLoginInputsValid(AWSAuthType.IAM, awsAuthLogin);
        assertTrue(valid);
    }

    @Test
    public void test_areAwsLoginInputsValid_EC2()  {

        AWSAuthLogin awsAuthLogin = new AWSIAMLogin();
        awsAuthLogin.setIam_http_request_method("POST");
        awsAuthLogin.setIam_request_body("{}");
        awsAuthLogin.setIam_request_headers("{\"token\":\"4qJC0tWjMDIKjRDDmtcUAZBt\"}");
        awsAuthLogin.setIam_request_url("http://testurl.com");
        awsAuthLogin.setRole("testawsrole");
        awsAuthLogin.setPkcs7("MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====");
        boolean valid = ControllerUtil.areAwsLoginInputsValid(AWSAuthType.EC2, awsAuthLogin);
        assertTrue(valid);
    }

    @Test
    public void test_areAwsLoginInputsValid_invalid()  {

        boolean valid = ControllerUtil.areAwsLoginInputsValid(AWSAuthType.EC2, null);
        assertFalse(valid);
    }

    @Test
    public void test_areAWSEC2RoleInputsValid() throws TVaultValidationException {

        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "",
                "", "", "vpc-2f09a348", "",
                "", "",
                "\"[prod, dev\"]");
        boolean valid = ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole);
        assertTrue(valid);
    }

    @Test
    public void test_areAWSEC2RoleInputsValid_invalid()  {

        try {
            AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "", "",
                    "", "", "vpc-2f09a348", "",
                    "", "",
                    "\"[prod, dev\"]");
            boolean valid = ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole);
        } catch (TVaultValidationException e) {
            assertTrue(true);
        }
    }

    @Test
    public void test_areAWSEC2RoleInputsValid_invalid_authType()  {

        try {
            AWSLoginRole awsLoginRole = new AWSLoginRole("iam", "mytestawsrole", "",
                    "", "", "vpc-2f09a348", "",
                    "", "",
                    "\"[prod, dev\"]");
            boolean valid = ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole);
        } catch (TVaultValidationException e) {
            assertTrue(true);
        }
    }

    @Test
    public void test_areAWSIAMRoleInputsValid() throws TVaultValidationException {

        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");
        boolean valid = ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole);
        assertTrue(valid);
    }

    @Test
    public void test_areAWSIAMRoleInputsValid_invalid_authType()  {

        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("ec2");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");
        try {
            boolean valid = ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole);
        } catch (TVaultValidationException e) {
            assertTrue(true);
        }
    }

    @Test
    public void test_areAWSIAMRoleInputsValid_invalid() throws TVaultValidationException {

        boolean valid = ControllerUtil.areAWSIAMRoleInputsValid(null);
        assertFalse(valid);
    }

    @Test
    public void test_getCountOfSafesForGivenSafeName()  {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"safe01\", \"safe02\"]}");
        Response responseEmpty = getMockResponse(HttpStatus.OK, true, "{\"keys\":[]}");
        when(reqProcessor.process("/sdb/list","{\"path\":\"metadata/apps\"}",token)).thenReturn(response);
        when(reqProcessor.process("/sdb/list","{\"path\":\"metadata/users\"}",token)).thenReturn(responseEmpty);
        when(reqProcessor.process("/sdb/list","{\"path\":\"metadata/shared\"}",token)).thenReturn(responseEmpty);
        int count = ControllerUtil.getCountOfSafesForGivenSafeName("safe01", token);
        assertEquals(1, count);
    }

    @Test
    public void test_generateSafePath_users()  {
        String safePath = ControllerUtil.generateSafePath("safe01", "users");
        assertEquals("users/safe01", safePath);
    }

    @Test
    public void test_generateSafePath_apps()  {
        String safePath = ControllerUtil.generateSafePath("safe01", "apps");
        assertEquals("apps/safe01", safePath);
    }

    @Test
    public void test_generateSafePath_shared()  {
        String safePath = ControllerUtil.generateSafePath("safe01", "shared");
        assertEquals("shared/safe01", safePath);
    }

    @Test
    public void test_populateAWSMetaJson()  {
        String metaJson = ControllerUtil.populateAWSMetaJson("role1", "normaluser");
        assertEquals("{\"path\":\"metadata/awsrole/role1\"}", metaJson);
    }

    @Test
    public void test_createMetadata_successfully()  {
        String metadataJson = "{\"path\":\"metadata/awsrole/role1\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process("/write",metadataJson,token)).thenReturn(response);
        boolean status = ControllerUtil.createMetadata(metadataJson, token);
        assertEquals(true, status);
    }

    @Test
    public void test_createMetadata_failure()  {
        String metadataJson = "{\"path\":\"metadata/awsrole/role1\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "");
        when(reqProcessor.process("/write",metadataJson,token)).thenReturn(response);
        boolean status = ControllerUtil.createMetadata(metadataJson, token);
        assertEquals(false, status);
    }

    @Test
    public void test_canDeleteRole_successfully()  {
        String metadataJson = "{\"path\":\"metadata/approle/role1\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String _path = "metadata/approle/role1";
        UserDetails userDetails = getMockUser(false);
        Response expectedResponse = getMockResponse(HttpStatus.OK, true, "");
        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"createdBy\":\"normaluser\",\"name\":\"z1\"}}");
        when(reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        Response actualResponse = ControllerUtil.canDeleteRole("role1", token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
    }

    @Test
    public void test_canDeleteRole_successfully_admin()  {
        String metadataJson = "{\"path\":\"metadata/approle/role1\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String _path = "metadata/approle/role1";
        UserDetails userDetails = getMockUser(true);
        Response expectedResponse = getMockResponse(HttpStatus.OK, true, "");
        Response response = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"createdBy\":\"normaluser\",\"name\":\"z1\"}}");
        when(reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        Response actualResponse = ControllerUtil.canDeleteRole("role1", token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
    }

    @Test
    public void test_canDeleteRole_successfully_admin_()  {
        String metadataJson = "{\"path\":\"metadata/approle/role1\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String _path = "metadata/approle/role1";
        UserDetails userDetails = getMockUser(true);
        Response expectedResponse = getMockResponse(HttpStatus.OK, true, "");
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        Response actualResponse = ControllerUtil.canDeleteRole("role1", token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
    }

    @Test
    public void test_canDeleteRole_failure()  {
        String metadataJson = "{\"path\":\"metadata/approle/role1\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String _path = "metadata/approle/role1";
        UserDetails userDetails = getMockUser(false);
        Response expectedResponse = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "Error reading role info");
        Response response = getMockResponse(HttpStatus.OK, true, "");
        when(reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        Response actualResponse = ControllerUtil.canDeleteRole("role1", token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, actualResponse.getHttpstatus());
    }

    @Test
    public void test_canDeleteRole_failure_403()  {
        String metadataJson = "{\"path\":\"metadata/approle/role1\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String _path = "metadata/approle/role1";
        UserDetails userDetails = getMockUser(false);
        Response expectedResponse = getMockResponse(HttpStatus.OK, true, "");
        Response response = getMockResponse(HttpStatus.UNAUTHORIZED, true, "");
        when(reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        Response actualResponse = ControllerUtil.canDeleteRole("role1", token, userDetails, TVaultConstants.APPROLE_METADATA_MOUNT_PATH);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getHttpstatus());
    }

    @Test
    public void test_populateAppRoleMetaJson()  {
        String json = ControllerUtil.populateAppRoleMetaJson("role1", "normalsuer");
        assertEquals("{\"path\":\"metadata/approle/role1\"}", json);
    }

    @Test
    public void test_populateUserMetaJson()  {
        String json = ControllerUtil.populateUserMetaJson("role1", "normalsuer");
        assertEquals("{\"path\":\"metadata/approle_users/normalsuer/role1\"}", json);
    }
    
    @Test
    public void test_getPoliciesAsListFromJson() {
        List<String> policyList = new ArrayList<>();;
        List<String> expectedList = new ArrayList<>();
        expectedList.add("s_shared_mysafe01");
        try {
            policyList = ControllerUtil.getPoliciesAsListFromJson(new ObjectMapper(), "{\"data\":{\"policies\":\"s_shared_mysafe01\"}}");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(expectedList, policyList);
    }
    
    @Test
    public void test_readSSCredFile() throws IOException{
    	File sscredFile = getSSCredFile();
    	boolean isDelete = true;
    	SSCred expected = new SSCred();
    	expected.setUsername("c2FmZWFkbWlu");
    	expected.setPassword("c2FmZWFkbWlu");
    	SSCred actual = ControllerUtil.readSSCredFile(sscredFile.getParent(), isDelete);
    	assertNotNull(actual);
    	assertEquals(expected.getUsername(), actual.getUsername());
    	assertEquals(expected.getPassword(), actual.getPassword());
    }
    
    @Test
    public void test_readSSCredFile_Failure() throws IOException{
    	File sscredFile = getSSCredFile();
    	boolean isDelete = true;
    	SSCred expected = new SSCred();
    	expected.setUsername("c2FmZWFkbWlu");
    	expected.setPassword("c2FmZWFkbWlu");
    	ReflectionTestUtils.setField(ControllerUtil.class,"sscred", null);
    	SSCred actual = ControllerUtil.readSSCredFile(sscredFile.getAbsolutePath(), isDelete);
    	assertNull(actual);
    }    
    private File getSSCredFile() throws IOException {
    	TemporaryFolder folder= new TemporaryFolder();
    	folder.create();
    	File sscredFile = folder.newFile("sscred");
    	PrintWriter pw =  new PrintWriter(sscredFile);
    	pw.write("username:c2FmZWFkbWlu"+ System.getProperty("line.separator") + "password:c2FmZWFkbWlu");
    	pw.close();
    	return sscredFile;
    }

}
