/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*  
*     http://www.apache.org/licenses/LICENSE-2.0
*  
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */
package com.tmobile.cso.vault.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class IAMServiceAccountUtilsTest {

    @InjectMocks
    IAMServiceAccountUtils iamServiceAccountUtils;

    @Mock
    RequestProcessor reqProcessor;

    @Mock
    Response response;

    @Mock
    StatusLine statusLine;

    @Mock
    HttpEntity mockHttpEntity;

    @Mock
    CloseableHttpClient httpClient;

    @Mock
    CloseableHttpResponse httpResponse;

    @Mock
    HttpUtils httpUtils;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(ControllerUtil.class);
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(ControllerUtil.class, "reqProcessor", reqProcessor);
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");
        ReflectionTestUtils.setField(iamServiceAccountUtils, "iamPortalAuthEndpoint", "testendpoint");
        ReflectionTestUtils.setField(iamServiceAccountUtils, "iamPortalDomain", "testdomain");
        ReflectionTestUtils.setField(iamServiceAccountUtils, "iamPortalrotateSecretEndpoint", "testendpoint");
        when(ControllerUtil.getReqProcessor()).thenReturn(reqProcessor);
        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }


    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        response.setResponse("");
        if (!StringUtils.isEmpty(expectedBody)) {
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
    public void test_getIAMApproleToken_success() throws IOException {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        when(ControllerUtil.getSscred()).thenReturn(new SSCred());
        when(ControllerUtil.getIamUsername()).thenReturn("M2UyNTA0MGYtODIwNS02ZWM2LTI4Y2ItOGYwZTQ1NDI1YjQ4");
        when(ControllerUtil.getIamPassword()).thenReturn("MWFjOGM1ZTgtZjE5Ny0yMTVlLTNmODUtZWIwMDc3ZmY3NmQw");

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String responseString = "{\"auth\": {\"client_token\": \""+token+"\"}}";
        when(mockHttpEntity.getContent()).thenReturn( new ByteArrayInputStream(responseString.getBytes()));

        String actualToken = iamServiceAccountUtils.getIAMApproleToken();
        assertEquals(token, actualToken);
    }

    @Test
    public void test_getIAMApproleToken_failed_invalid_sscred() throws IOException {

        when(ControllerUtil.getSscred()).thenReturn(null);
        String actualToken = iamServiceAccountUtils.getIAMApproleToken();
        assertNull(actualToken);
    }

    @Test
    public void test_getIAMApproleToken_failed_httpClient_error() throws IOException {

        when(ControllerUtil.getSscred()).thenReturn(new SSCred());
        when(ControllerUtil.getIamUsername()).thenReturn("M2UyNTA0MGYtODIwNS02ZWM2LTI4Y2ItOGYwZTQ1NDI1YjQ4");
        when(ControllerUtil.getIamPassword()).thenReturn("MWFjOGM1ZTgtZjE5Ny0yMTVlLTNmODUtZWIwMDc3ZmY3NmQw");

        when(httpUtils.getHttpClient()).thenReturn(null);

        String actualToken = iamServiceAccountUtils.getIAMApproleToken();
        assertNull(actualToken);
    }

    @Test
    public void test_rotateIAMSecret_success() throws IOException {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String iamServiceAccountName = "svc_vault_test5";
        String awsAccountId = "1234567890";
        String accessKeyId = "testaccesskey";
        String iamSecret = "abcdefgh";
        when(ControllerUtil.getSscred()).thenReturn(new SSCred());
        when(ControllerUtil.getIamUsername()).thenReturn("M2UyNTA0MGYtODIwNS02ZWM2LTI4Y2ItOGYwZTQ1NDI1YjQ4");
        when(ControllerUtil.getIamPassword()).thenReturn("MWFjOGM1ZTgtZjE5Ny0yMTVlLTNmODUtZWIwMDc3ZmY3NmQw");

        when(httpUtils.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);

        String responseString = "{\"accessKeyId\": \"testaccesskey\", \"userName\": \"svc_vault_test5\", \"accessKeySecret\": \"abcdefgh\", \"expiryDateEpoch\": \"1609754282000\"}";
        String responseStringToken = "{\"auth\": {\"client_token\": \""+token+"\"}}";
        when(mockHttpEntity.getContent()).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return new ByteArrayInputStream(responseString.getBytes());

                return new ByteArrayInputStream(responseStringToken.getBytes());
            }
        });

        IAMServiceAccountSecret expectedIamServiceAccountSecret = new IAMServiceAccountSecret(iamServiceAccountName, accessKeyId, iamSecret, 1609754282000L, awsAccountId);
        IAMServiceAccountRotateRequest iamServiceAccountRotateRequest = new IAMServiceAccountRotateRequest(accessKeyId, iamServiceAccountName, awsAccountId);
        IAMServiceAccountSecret iamServiceAccountSecret = iamServiceAccountUtils.rotateIAMSecret(iamServiceAccountRotateRequest);
        assertEquals(expectedIamServiceAccountSecret.getAccessKeySecret(), iamServiceAccountSecret.getAccessKeySecret());
    }

    @Test
    public void test_writeIAMSvcAccSecret_success() {

        String iamServiceAccountName = "svc_vault_test5";
        String token = "123123123123";
        String awsAccountId = "1234567890";
        String path = "metadata/iamsvcacc/1234567890_svc_vault_test5";
        String iamSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";
        IAMServiceAccountSecret iamServiceAccountSecret = new IAMServiceAccountSecret(iamServiceAccountName, accessKeyId, iamSecret, 1609754282000L, awsAccountId);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response);
        boolean actualStatus = iamServiceAccountUtils.writeIAMSvcAccSecret(token, path, iamServiceAccountName, iamServiceAccountSecret);
        assertTrue(actualStatus);
    }

    @Test
    public void test_writeIAMSvcAccSecret_failed() {

        String iamServiceAccountName = "svc_vault_test5";
        String token = "123123123123";
        String awsAccountId = "1234567890";
        String path = "metadata/iamsvcacc/1234567890_svc_vault_test5";
        String iamSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";
        IAMServiceAccountSecret iamServiceAccountSecret = new IAMServiceAccountSecret(iamServiceAccountName, accessKeyId, iamSecret, 1609754282000L, awsAccountId);
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response);
        boolean actualStatus = iamServiceAccountUtils.writeIAMSvcAccSecret(token, path, iamServiceAccountName, iamServiceAccountSecret);
        assertFalse(actualStatus);
    }

    @Test
    public void test_updateActivatedStatusInMetadata_success() {

        String iamServiceAccountName = "svc_vault_test5";
        String token = "123123123123";
        String awsAccountId = "1234567890";
        String path = "metadata/iamsvcacc/1234567890_svc_vault_test5";
        String iamSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";

        Response response = getMockResponse(HttpStatus.OK, true, "{ \"data\": { \"isActivated\": false}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(response);
        Response response204 = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response204);
        Response actualResponse = iamServiceAccountUtils.updateActivatedStatusInMetadata(token, iamServiceAccountName, awsAccountId);
        assertEquals(HttpStatus.NO_CONTENT, actualResponse.getHttpstatus());
    }

    @Test
    public void test_updateActivatedStatusInMetadata_failed() {

        String iamServiceAccountName = "svc_vault_test5";
        String token = "123123123123";
        String awsAccountId = "1234567890";
        String path = "metadata/iamsvcacc/1234567890_svc_vault_test5";
        String iamSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";

        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(response);

        Response actualResponse = iamServiceAccountUtils.updateActivatedStatusInMetadata(token, iamServiceAccountName, awsAccountId);
        assertNull(actualResponse);
    }

    @Test
    public void test_updateIAMSvcAccNewAccessKeyIdInMetadata_success() {

        String iamServiceAccountName = "svc_vault_test5";
        String token = "123123123123";
        String awsAccountId = "1234567890";
        String path = "metadata/iamsvcacc/1234567890_svc_vault_test5";
        String iamSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";

        Response response = getMockResponse(HttpStatus.OK, true, "{ \"data\": {\"secret\": [{\"accessKeyId\": \"testaccesskey\", \"expiryDuration\": 1609668443000}]}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(response);
        Response response204 = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response204);

        IAMServiceAccountSecret iamServiceAccountSecret = new IAMServiceAccountSecret(iamServiceAccountName, accessKeyId, iamSecret, 1609754282000L, awsAccountId);

        Response actualResponse = iamServiceAccountUtils.updateIAMSvcAccNewAccessKeyIdInMetadata(token, awsAccountId, iamServiceAccountName, accessKeyId, iamServiceAccountSecret);
        assertEquals(HttpStatus.NO_CONTENT, actualResponse.getHttpstatus());
    }

    @Test
    public void test_updateIAMSvcAccNewAccessKeyIdInMetadata_failure_no_metadata() {

        String iamServiceAccountName = "svc_vault_test5";
        String token = "123123123123";
        String awsAccountId = "1234567890";
        String path = "metadata/iamsvcacc/1234567890_svc_vault_test5";
        String iamSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";

        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(response);
        IAMServiceAccountSecret iamServiceAccountSecret = new IAMServiceAccountSecret(iamServiceAccountName, accessKeyId, iamSecret, 1609754282000L, awsAccountId);

        Response actualResponse = iamServiceAccountUtils.updateIAMSvcAccNewAccessKeyIdInMetadata(token, awsAccountId, iamServiceAccountName, accessKeyId, iamServiceAccountSecret);
        assertNull(actualResponse);
    }

    @Test
    public void test_getTokenPoliciesAsListFromTokenLookupJson_success() throws IOException {

        List<String> expectedPolicies = new ArrayList<>();
        expectedPolicies.add("default");
        String policyJson = "{ \"policies\": [\"default\"]}";

        List<String> currentpolicies = iamServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(new ObjectMapper(), policyJson);
        assertEquals(expectedPolicies,currentpolicies);
    }

    @Test
    public void test_getTokenPoliciesAsListFromTokenLookupJson_success_single_policy() throws IOException {

        List<String> expectedPolicies = new ArrayList<>();
        expectedPolicies.add("default");
        String policyJson = "{ \"policies\": \"default\"}";

        List<String> currentpolicies = iamServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(new ObjectMapper(), policyJson);
        assertEquals(expectedPolicies,currentpolicies);
    }

    @Test
    public void test_getIdentityPoliciesAsListFromTokenLookupJson_success() throws IOException {

        List<String> expectedPolicies = new ArrayList<>();
        expectedPolicies.add("default");
        String policyJson = "{ \"identity_policies\": [\"default\"]}";

        List<String> currentpolicies = iamServiceAccountUtils.getIdentityPoliciesAsListFromTokenLookupJson(new ObjectMapper(), policyJson);
        assertEquals(expectedPolicies,currentpolicies);
    }

    @Test
    public void test_getIdentityPoliciesAsListFromTokenLookupJson_success_single_policy() throws IOException {

        List<String> expectedPolicies = new ArrayList<>();
        expectedPolicies.add("default");
        String policyJson = "{ \"identity_policies\": \"default\"}";

        List<String> currentpolicies = iamServiceAccountUtils.getIdentityPoliciesAsListFromTokenLookupJson(new ObjectMapper(), policyJson);
        assertEquals(expectedPolicies,currentpolicies);
    }
}