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
package com.tmobile.cso.vault.api.v2.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tmobile.cso.vault.api.model.*;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.service.ServiceAccountsService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@WebAppConfiguration
public class ServiceAccountsControllerV2Test {

    private MockMvc mockMvc;

    @Mock
    private ServiceAccountsService serviceAccountsService;

    @InjectMocks
    private ServiceAccountsControllerV2 serviceAccountsControllerV2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(serviceAccountsControllerV2).build();
    }
    
    private UserDetails getMockUser(boolean isAdmin) {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(isAdmin);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        return userDetails;
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
    private ADServiceAccount generateADServiceAccount(String  userid) {
        ADServiceAccount adServiceAccount = new ADServiceAccount();
        adServiceAccount.setDisplayName("testacc");
        adServiceAccount.setGivenName("testacc");
        adServiceAccount.setUserEmail("testacc@t-mobile.com");
        adServiceAccount.setUserId(userid);
        adServiceAccount.setUserName("testaccr");
        adServiceAccount.setPurpose("This is a test user account");
        adServiceAccount.setAccountExpires("292239827-01-08 11:35:09");
        adServiceAccount.setMaxPwdAge(90);
        adServiceAccount.setAccountStatus("active");
        adServiceAccount.setLockStatus("active");
        return adServiceAccount;
    }
    private List<ADServiceAccount> generateADSerivceAccounts() {
    	List<ADServiceAccount> allServiceAccounts = new ArrayList<ADServiceAccount>();
    	allServiceAccounts.add(generateADServiceAccount("testacc01"));
    	return allServiceAccounts;
    }
    
    private ADServiceAccountObjects generateADServiceAccountObjects(List<ADServiceAccount> allServiceAccounts) {
		ADServiceAccountObjects adServiceAccountObjects = new ADServiceAccountObjects();
		ADServiceAccountObjectsList adServiceAccountObjectsList = new ADServiceAccountObjectsList();
		if (!CollectionUtils.isEmpty(allServiceAccounts)) {
			adServiceAccountObjectsList.setValues(allServiceAccounts.toArray(new ADServiceAccount[allServiceAccounts.size()]));
		}
		adServiceAccountObjects.setData(adServiceAccountObjectsList);
		return adServiceAccountObjects;
    }
    
	private String getJSON(Object obj)  {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return TVaultConstants.EMPTY_JSON;
		}
	}
	
    @Test
    public void test_getADServiceAccounts_success() throws Exception {
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc01";
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        ADServiceAccountObjects adServiceAccountObjects = generateADServiceAccountObjects(allServiceAccounts);
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(adServiceAccountObjects);

        when(serviceAccountsService.getADServiceAccounts(token, userDetails, svcAccName, true)).thenReturn(responseEntityExpected);
        
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/ad/serviceaccounts")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails)
                .param("serviceAccountName", svcAccName))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        String expected = getJSON(adServiceAccountObjects);
        assertEquals(expected, actual);
    }

    @Test
    public void test_getOnboardedServiceAccounts_success() throws Exception {
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        
        Map<String, List<String>> onboardedServiceAccounts = new HashMap<String, List<String>>();
        List<String> accounts = new ArrayList<String>();
        accounts.add("testacc02");
        accounts.add("testacc03");
        accounts.add("testacc04");
        onboardedServiceAccounts.put("keys", accounts);
        
        String expected = getJSON(onboardedServiceAccounts);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);

        when(serviceAccountsService.getOnboardedServiceAccounts(token, userDetails)).thenReturn(responseEntityExpected);
        
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/serviceaccounts")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
    }
    
    @Test
    public void test_getServiceAccounts_Details_success() throws Exception{
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc02";
        
    	OnboardedServiceAccountDetails onboardedServiceAccountDetails = new OnboardedServiceAccountDetails();
    	onboardedServiceAccountDetails.setLastVaultRotation("2018-05-24T17:14:38.677370855Z");
    	onboardedServiceAccountDetails.setName(svcAccName+"@aaa.bbb.ccc.com");
    	onboardedServiceAccountDetails.setPasswordLastSet("2018-05-24T17:14:38.6038495Z");
    	onboardedServiceAccountDetails.setTtl(100L);
    	
        String expected = getJSON(onboardedServiceAccountDetails);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        
        when(serviceAccountsService.getOnboarderdServiceAccount(token, svcAccName, userDetails)).thenReturn(responseEntityExpected);
        
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/serviceaccounts/"+svcAccName)
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails))
        		.andExpect(status().isOk()).andReturn();
        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
    }
    private ServiceAccount generateServiceAccount(String svcAccName, String owner) {
    	ServiceAccount serviceAccount = new ServiceAccount();
    	serviceAccount.setName(svcAccName);
    	serviceAccount.setAutoRotate(true);
    	serviceAccount.setTtl(1234L);
    	serviceAccount.setMax_ttl(12345L);
    	serviceAccount.setOwner(owner);
    	return serviceAccount;
    }
    @Test
    public void test_onboardServiceAccount_success() throws Exception{
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = generateServiceAccount("testacc02", "testacc01");
   	
        String expected = "{\"messages\":[\"Successfully completed onboarding of AD service account into TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(serviceAccountsService.onboardServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        String inputJson = getJSON(serviceAccount);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v2/serviceaccounts/onboard")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails)
                .content(inputJson))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
        
    }
    
    @Test
    public void test_addUserToSvcAcc_success() throws Exception {
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
   	
        String expected = "{\"errors\":[\"Successfully added user to the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(serviceAccountsService.addUserToServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        String inputJson = getJSON(serviceAccountUser);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v2/serviceaccounts/user")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails)
                .content(inputJson))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
    }
    
    @Test
    public void test_removeUserServiceAccount_success() throws Exception {
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
   	
        String expected = "{\"message\":[\"Successfully removed user from the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(serviceAccountsService.removeUserFromServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        String inputJson = getJSON(serviceAccountUser);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/v2/serviceaccounts/user")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails)
                .content(inputJson))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
    }
    
    @Test
    public void test_resetSvcAccPwd_success() throws Exception {
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc02";
   	
    	String expected = "{\n" + 
    			"  \"current_password\": \"?@09AZGdnkinuq9OKXkeXW6D4oVGc\",\n" + 
    			"  \"last_password\": null,\n" + 
    			"  \"username\": \"testacc02\"\n" + 
    			"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails)).thenReturn(responseEntityExpected);
        
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/serviceaccounts/password/reset")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails)
                .param("serviceAccountName", svcAccName))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
    }
    
    @Test
    public void test_offboardServiceAccount_success() throws Exception{
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = generateServiceAccount("testacc02", "testacc01");
   	
        String expected = "{\"messages\":[\"Successfully completed offboarding of AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(serviceAccountsService.offboardServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        String inputJson = getJSON(serviceAccount);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v2/serviceaccounts/offboard")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails)
                .content(inputJson))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
        
    }

    @Test
    public void test_addGroupToSafe() throws Exception {
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("testacc02", "group1","write");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountGroup);
        String responseJson = "{\"messages\":[\"Group is successfully associated with Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(serviceAccountsService.addGroupToServiceAccount(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(ServiceAccountGroup.class), eq(userDetails))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/serviceaccounts/group").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_removeGroupFromSafe() throws Exception {
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("testacc02", "group1","write");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountGroup);
        String responseJson = "{\"messages\":[\"Group is successfully removed from Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(serviceAccountsService.removeGroupFromServiceAccount(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(ServiceAccountGroup.class), eq(userDetails))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/serviceaccounts/group").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_associateApproletoSvcAcc() throws Exception {
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testacc02", "role1","write");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountApprole);
        String responseJson = "{\"messages\":[\"Approle is successfully associated with Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(serviceAccountsService.associateApproletoSvcAcc(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(ServiceAccountApprole.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/serviceaccounts/approle").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_removeApproleFromSvcAcc() throws Exception {
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testacc02", "role1","write");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountApprole);
        String responseJson = "{\"messages\":[\"Approle is successfully removed from Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(serviceAccountsService.removeApproleFromSvcAcc(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(ServiceAccountApprole.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/serviceaccounts/approle").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_getServiceAccountMeta_success() throws Exception {
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        String path = "ad/roles/testacc01";

        String expected = "{\n" +
                "  \"app-roles\": {\n" +
                "    \"role1\": \"read\"\n" +
                "  },\n" +
                "  \"managedBy\": \"user11\",\n" +
                "  \"name\": \"testacc01\",\n" +
                "  \"users\": {\n" +
                "    \"user11\": \"sudo\"\n" +
                "  }\n" +
                "}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(serviceAccountsService.getServiceAccountMeta(token, userDetails, path)).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/serviceaccounts/meta?path=ad/roles/testacc01").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expected)));

    }

    @Test
    public void test_associateAWSroletoSvcAcc() throws Exception {
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testacc02", "role1","write");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountApprole);
        String responseJson = "{\"messages\":[\"AWS Role successfully associated with Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(serviceAccountsService.addAwsRoleToSvcacc(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(ServiceAccountAWSRole.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/serviceaccounts/role").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_removeAWSRoleFromSvcacc() throws Exception {
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testacc02", "role1","write");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountApprole);
        String responseJson = "{\"messages\":[\"AWS Role is successfully removed from Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(serviceAccountsService.removeAWSRoleFromSvcacc(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(ServiceAccountAWSRole.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/serviceaccounts/role").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_createRole() throws Exception {
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testacc02", "role1","write");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountApprole);
        String responseJson = "{\"messages\":[\"AWS Role created \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(serviceAccountsService.createRole(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AWSLoginRole.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/serviceaccounts/aws/role").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_createIAMRole() throws Exception {
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testacc02", "role1","write");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountApprole);
        String responseJson = "{\"messages\":[\"AWS Role created \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(serviceAccountsService.createIAMRole(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AWSIAMRole.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/serviceaccounts/aws/iam/role").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }
}
