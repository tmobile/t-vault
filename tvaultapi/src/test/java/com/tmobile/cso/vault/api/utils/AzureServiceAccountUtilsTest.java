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
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AzureServiceAccountSecret;
import com.tmobile.cso.vault.api.model.AzureServicePrincipalRotateRequest;
import com.tmobile.cso.vault.api.model.SSCred;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.service.DirectoryService;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
public class AzureServiceAccountUtilsTest {
	@Mock
    RequestProcessor reqProcessor;

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

    @InjectMocks
    AzureServiceAccountUtils azureServiceAccountUtils;
    
    @Mock
    TokenUtils tokenUtils;
    
    @Mock
    IAMServiceAccountUtils iamUtils;
    
    @Mock
    DirectoryService directoryService;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(ControllerUtil.class);
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(ControllerUtil.class, "reqProcessor", reqProcessor);
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");
        ReflectionTestUtils.setField(azureServiceAccountUtils, "iamPortalAuthEndpoint", "testendpoint");
        ReflectionTestUtils.setField(azureServiceAccountUtils, "iamPortalDomain", "testdomain");
        ReflectionTestUtils.setField(azureServiceAccountUtils, "azurePortalrotateSecretEndpoint", "testendpoint");
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
    public void test_getTokenPoliciesAsListFromTokenLookupJson() throws IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	List<String> currentpolicies = new ArrayList<>();
    	List<String> resultpolicies = new ArrayList<>();
        String policyjson = "{\"policies\":[\"svc_cce_usertest1\",\"svc_cce_usertest2\"]}";
        currentpolicies = azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(mapper, policyjson);
        resultpolicies.add("svc_cce_usertest1");
        resultpolicies.add("svc_cce_usertest2");
        assertEquals(resultpolicies, currentpolicies);
    }

    @Test
    public void testGetTokenPoliciesAsListFromTokenLookupJsonSuccess() throws IOException {
        List<String> expectedPolicies = new ArrayList<>();
        expectedPolicies.add("default");
        String policyJson = "{ \"policies\": [\"default\"]}";

        List<String> currentpolicies = azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(new ObjectMapper(), policyJson);
        assertEquals(expectedPolicies, currentpolicies);
    }

    @Test
    public void testGetTokenPoliciesAsListFromTokenLookupJsonSuccessSinglePolicy() throws IOException {
        List<String> expectedPolicies = new ArrayList<>();
        expectedPolicies.add("default");
        String policyJson = "{ \"policies\": \"default\"}";
        List<String> currentpolicies = azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(new ObjectMapper(), policyJson);
        assertEquals(expectedPolicies, currentpolicies);
    }

    @Test
    public void test_rotateAzureServicePrincipalSecretMOCK() {
        AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret();
        AzureServicePrincipalRotateRequest azureServicePrincipalRotateRequest = new AzureServicePrincipalRotateRequest();
        azureServicePrincipalRotateRequest.setServicePrincipalId("100");
        azureServicePrincipalRotateRequest.setTenantId("101");
        azureServicePrincipalRotateRequest.setSecretKeyId("secret1");
        
        azureServiceAccountSecret.setServicePrincipalId("100");
        azureServiceAccountSecret.setTenantId("101");
        azureServiceAccountSecret.setSecretKeyId("secret1");
        azureServiceAccountSecret.setSecretText("mocksecrettext_"+ new Date().getTime());
        azureServiceAccountSecret.setExpiryDateEpoch(604800000L);
        azureServiceAccountSecret.setExpiryDate(new Date(604800000L).toString());
        AzureServiceAccountSecret result = azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(azureServicePrincipalRotateRequest);
        assertEquals(result.getSecretKeyId(), azureServiceAccountSecret.getSecretKeyId());
    }

    @Test
    public void test_writeAzureSvcAccSecret_success() {

        String iamServiceAccountName = "svc_vault_test5";
        String token = "123123123123";
        String azureAccountId = "1234567890";
        String path = "metadata/iamsvcacc/1234567890_svc_vault_test5";
        String azureSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";
        Long expiryDateEpoch = 604800000L;
        String expiryDate = new Date(604800000L).toString();
        String servicePrincipalId = "100";
        String tenantId = "101";
        AzureServiceAccountSecret iamServiceAccountSecret = new AzureServiceAccountSecret(azureSecret, azureSecret, expiryDateEpoch, expiryDate, servicePrincipalId, tenantId);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response);
        boolean actualStatus = azureServiceAccountUtils.writeAzureSPSecret(token, path, iamServiceAccountName, iamServiceAccountSecret);
        assertTrue(actualStatus);
    }

    @Test
    public void test_updateActivatedStatusInMetadata_success() {

        String servicePrincipalName = "svc_vault_test5";
        String token = "123123123123";
        String awsAccountId = "1234567890";
        String path = "metadata/azuresvcacc/1234567890_svc_vault_test5";
        String iamSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";

        Response response = getMockResponse(HttpStatus.OK, true, "{ \"data\": { \"isActivated\": false}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(response);
        Response response204 = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response204);
        Response actualResponse = azureServiceAccountUtils.updateActivatedStatusInMetadata(token, servicePrincipalName);
        assertEquals(HttpStatus.NO_CONTENT, actualResponse.getHttpstatus());
    }

    @Test
    public void updateAzureSPSecretKeyInfoInMetadata_success() {

        String iamServiceAccountName = "svc_vault_test5";
        String token = "123123123123";
        String awsAccountId = "1234567890";
        String path = "metadata/iamsvcacc/1234567890_svc_vault_test5";
        String iamSecret = "abcdefgh";
        String accessKeyId = "testaccesskey";
        String azureSecret = "abcdefgh";
        String secretKeyId = "testaccesskey";
        Long expiryDateEpoch = 604800000L;
        String expiryDate = new Date(604800000L).toString();
        String servicePrincipalId = "100";
        String tenantId = "101";

        Response response = getMockResponse(HttpStatus.OK, true, "{ \"data\": {\"secret\": [{\"secretKeyId\": \"testaccesskey\", \"expiryDuration\": 1609668443000}]}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(response);
        Response response204 = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response204);

        AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(azureSecret, azureSecret, expiryDateEpoch, expiryDate, servicePrincipalId, tenantId);

        Response actualResponse = azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(token, iamServiceAccountName, secretKeyId, azureServiceAccountSecret);
        assertEquals(HttpStatus.NO_CONTENT, actualResponse.getHttpstatus());
    }

    @Test
    public void testWriteAzureSPSecretSuccess() {
        String token = "123123123123";
        String path = "metadata/azuresvcacc/svc_vault_test5";
        String azureSvcAccName = "svc_vault_test5";
        String secretKeyId = "testaccesskey";
        String servicePrincipalId = "testservicePrincipalId";
        String tenantId = "testtenantId";
        String azureSecret = "abcdefgh";

        AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret,  1609754282000L, "20201215", servicePrincipalId, tenantId);
        Response responseData = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(responseData);
        boolean actualStatus = azureServiceAccountUtils.writeAzureSPSecret(token, path, azureSvcAccName, azureServiceAccountSecret);
        assertTrue(actualStatus);
    }

    @Test
    public void testWriteAzureSPSecretFailed() {
		String token = "123123123123";
		String path = "metadata/azuresvcacc/svc_vault_test5";
		String azureSvcAccName = "svc_vault_test5";
        String secretKeyId = "testaccesskey";
        String servicePrincipalId = "testservicePrincipalId";
        String tenantId = "testtenantId";
        String azureSecret = "abcdefgh";
        AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret,  1609754282000L, "20201215", servicePrincipalId, tenantId);
        Response responseData = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(responseData);
        boolean actualStatus = azureServiceAccountUtils.writeAzureSPSecret(token, path, azureSvcAccName, azureServiceAccountSecret);
        assertFalse(actualStatus);
    }

    @Test
    public void testUpdateAzureSPSecretKeyInfoInMetadataSuccess() {
        String token = "123123123123";
        String azureSvcAccName = "svc_vault_test5";
        String secretKeyId = "testaccesskey";
        String servicePrincipalId = "testservicePrincipalId";
        String tenantId = "testtenantId";
        String azureSecret = "abcdefgh";
        Response responseData = getMockResponse(HttpStatus.OK, true, "{ \"data\": {\"secret\": [{\"secretKeyId\": \"testaccesskey\", \"expiryDuration\": 1609668443000}]}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseData);
        Response response204 = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response204);
        AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret,  1609754282000L, "20201215", servicePrincipalId, tenantId);

        Response actualResponse = azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(token, azureSvcAccName, secretKeyId, azureServiceAccountSecret);
        assertEquals(HttpStatus.NO_CONTENT, actualResponse.getHttpstatus());
    }

    @Test
    public void testUpdateAzureSPSecretKeyInfoInMetadataFailureNoMetadata() {
		String token = "123123123123";
		String azureSvcAccName = "svc_vault_test5";
		String secretKeyId = "testaccesskey";
		String servicePrincipalId = "testservicePrincipalId";
		String tenantId = "testtenantId";
		String azureSecret = "abcdefgh";
        Response responseData = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseData);
        AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret,  1609754282000L, "20201215", servicePrincipalId, tenantId);
        Response actualResponse = azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(token, azureSvcAccName, secretKeyId, azureServiceAccountSecret);
        assertNull(actualResponse);
    }

    @Test
    public void testUpdateActivatedStatusInMetadataSuccess() {
		String token = "123123123123";
		String azureSvcAccName = "svc_vault_test5";
        Response responseData = getMockResponse(HttpStatus.OK, true, "{ \"data\": { \"isActivated\": false}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseData);
        Response response204 = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/write"), Mockito.any(), eq(token))).thenReturn(response204);
        Response actualResponse = azureServiceAccountUtils.updateActivatedStatusInMetadata(token, azureSvcAccName);
        assertEquals(HttpStatus.NO_CONTENT, actualResponse.getHttpstatus());
    }

    @Test
    public void testUpdateActivatedStatusInMetadataFailed() {
		String token = "123123123123";
		String azureSvcAccName = "svc_vault_test5";
        Response responseData = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(responseData);
        Response actualResponse = azureServiceAccountUtils.updateActivatedStatusInMetadata(token, azureSvcAccName);
        assertNull(actualResponse);
    }
}