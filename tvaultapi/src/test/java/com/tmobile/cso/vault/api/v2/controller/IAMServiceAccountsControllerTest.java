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
package com.tmobile.cso.vault.api.v2.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.model.IAMServiceAccountApprole;
import com.tmobile.cso.vault.api.model.ServiceAccountApprole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.IAMSecrets;
import com.tmobile.cso.vault.api.model.IAMServiceAccount;
import com.tmobile.cso.vault.api.model.IAMServiceAccountGroup;
import com.tmobile.cso.vault.api.model.IAMServiceAccountUser;

import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.service.IAMServiceAccountsService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class IAMServiceAccountsControllerTest {
	
	@Mock
	public IAMServiceAccountsService iamServiceAccountsService;
	
	private MockMvc mockMvc;
	
	@InjectMocks
	private IAMServiceAccountsController iamServiceAccountsController;
	
	@Mock
    HttpServletRequest httpServletRequest;
	
	@Mock
    UserDetails userDetails;
	
    String token;

    private static final String USER_DETAILS_STRING="UserDetails";
    private static final String VAULT_TOKEN_STRING="vault-token";
    private static final String CONTENT_TYPE_STRING="Content-Type";
    private static final String CONTENT_TYPE_VALUE_STRING="application/json;charset=UTF-8";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(iamServiceAccountsController).build();
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";  
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
    }

	private IAMServiceAccount generateIAMServiceAccount(String userName, String awsAccountId, String owner) {
		IAMServiceAccount iamServiceAccount = new IAMServiceAccount();
		iamServiceAccount.setUserName(userName);
		iamServiceAccount.setAwsAccountId(awsAccountId);
		iamServiceAccount.setAwsAccountName("testaccount1");
		iamServiceAccount.setOwnerNtid(owner);
		iamServiceAccount.setOwnerEmail("normaluser@testmail.com");
		iamServiceAccount.setApplicationId("app1");
		iamServiceAccount.setApplicationName("App1");
		iamServiceAccount.setApplicationTag("App1");
		iamServiceAccount.setCreatedAtEpoch(12345L);
		iamServiceAccount.setSecret(generateIAMSecret());
		return iamServiceAccount;
	}

	private List<IAMSecrets> generateIAMSecret() {
		List<IAMSecrets> iamSecrets = new ArrayList<>();
		IAMSecrets iamSecret = new IAMSecrets();
		iamSecret.setAccessKeyId("testaccesskey");
		iamSecret.setExpiryDuration(12345L);
		iamSecrets.add(iamSecret);
		return iamSecrets;
	}

	private String getJSON(Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return TVaultConstants.EMPTY_JSON;
		}
	}

	@Test
	public void test_getOnboardedIAMServiceAccounts_successful() throws Exception {
		String responseJson = "{\"keys\":[{\"userName\":\"testiamsvcacc01\",\"metaDataName\":\"123456789012_testiamsvcacc01\",\"accountID\":\"123456789012\"},{\"userName\":\"test_iamsvcacc2\",\"metaDataName\":\"123456789045_test_iamsvcacc2\",\"accountID\":\"123456789045\"}]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(iamServiceAccountsService.getOnboardedIAMServiceAccounts(token, userDetails))
				.thenReturn(responseEntityExpected);
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/iamserviceaccounts")
				.header(VAULT_TOKEN_STRING, token).header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
				.requestAttr(USER_DETAILS_STRING, userDetails)).andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	@Test
	public void test_getIAMServiceAccountsList_successful() throws Exception {
		String responseJson = "{\"keys\":[{\"userName\":\"testiamsvcacc01\",\"metaDataName\":\"123456789012_testiamsvcacc01\",\"accountID\":\"123456789012\"},{\"userName\":\"test_iamsvcacc2\",\"metaDataName\":\"123456789045_test_iamsvcacc2\",\"accountID\":\"123456789045\"}]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(iamServiceAccountsService.getIAMServiceAccountsList(userDetails, token))
				.thenReturn(responseEntityExpected);
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/iamserviceaccounts/list")
				.header(VAULT_TOKEN_STRING, token).header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
				.requestAttr(USER_DETAILS_STRING, userDetails)).andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	@Test
	public void test_getIAMServiceAccountDetail_successful() throws Exception {
		String responseJson = "{\"application_id\":1222,\"application_name\":\"T-Vault\",\"application_tag\":\"TVT\",\"awsAccountId\":\"123456789012\",\"awsAccountName\":\"AWS-SEC\",\"createdAtEpoch\":1086073200000,\"isActivated\":true,\"owner_email\":\"Nithin.Nazeer1@T-mobile.com\",\"owner_ntid\":\"NNazeer1\",\"secret\":[{\"accessKeyId\":\"1212zdasd\",\"expiryDuration\":\"2004-06-01\"},{\"accessKeyId\":\"abcdwww\",\"expiryDuration\":\"2004-06-01\"}],\"userName\":\"testiamsvcacc01\",\"users\":{\"nnazeer1\":\"write\"},\"createdDate\":\"2004-06-01\",\"secretData\":[{\"accessKeyId\":\"1212zdasd\",\"expiryDuration\":\"2004-06-01\"},{\"accessKeyId\":\"abcdwww\",\"expiryDuration\":\"2004-06-01\"}]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(iamServiceAccountsService.getIAMServiceAccountDetail(token , "123456789012_testiamsvcacc01"))
				.thenReturn(responseEntityExpected);
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/iamserviceaccounts/123456789012_testiamsvcacc01")
				.header(VAULT_TOKEN_STRING, token).header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
				.requestAttr(USER_DETAILS_STRING, userDetails)).andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	@Test
	public void test_getIAMServiceAccountSecretKey_successful() throws Exception {
		String responseJson = "{\"application_id\":1222,\"createdAtEpoch\":1086073200000,\"isActivated\":true,\"owner_email\":\"Nithin.Nazeer1@T-mobile.com\",\"owner_ntid\":\"NNazeer1\",\"secret\":[{\"accessKeyId\":\"1212zdasd\",\"expiryDuration\":\"1973-11-15\",\"secretkey\":\"abcdefg123\"},{\"accessKeyId\":\"dsfdsfzdasd\",\"expiryDuration\":\"2009-01-19\",\"secretkey\":\"mnbcjddk987\"}],\"userName\":\"testiamsvcacc01\",\"createdDate\":\"2004-06-01\"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(iamServiceAccountsService.getIAMServiceAccountSecretKey(token , "123456789012_testiamsvcacc01", "testiamsvcacc01_01"))
				.thenReturn(responseEntityExpected);		
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/iamserviceaccounts/secrets/123456789012_testiamsvcacc01/testiamsvcacc01_01")
				.header(VAULT_TOKEN_STRING, token).header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
				.requestAttr(USER_DETAILS_STRING, userDetails)).andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}

	@Test
	public void testOnboardIAMServiceAccountSuccess() throws Exception {
		IAMServiceAccount serviceAccount = generateIAMServiceAccount("testaccount", "1234567", "normaluser");

		String expected = "{\"messages\":[\"Successfully completed onboarding of IAM service account into TVault for password rotation.\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
		when(iamServiceAccountsService.onboardIAMServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenReturn(responseEntityExpected);
		String inputJson = getJSON(serviceAccount);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.post("/v2/iamserviceaccounts/onboard").header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
						.requestAttr(USER_DETAILS_STRING, userDetails).content(inputJson))
				.andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}

	@Test
	public void testAddUserToIAMSvcAccSuccess() throws Exception {
		IAMServiceAccountUser iamSvcAccUser = new IAMServiceAccountUser("testaccount", "testuser1", "read", "1234567");

		String expected = "{\"errors\":[\"Successfully added user to the IAM Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
		when(iamServiceAccountsService.addUserToIAMServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any(),
				eq(false))).thenReturn(responseEntityExpected);
		String inputJson = getJSON(iamSvcAccUser);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.post("/v2/iamserviceaccounts/user").header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
						.requestAttr(USER_DETAILS_STRING, userDetails).content(inputJson))
				.andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	@Test
	public void test_readFolders_successful() throws Exception {
		String responseJson = "{\"application_id\":1222,\"createdAtEpoch\":1086073200000,\"isActivated\":true,\"owner_email\":\"Nithin.Nazeer1@T-mobile.com\",\"owner_ntid\":\"NNazeer1\",\"secret\":[{\"accessKeyId\":\"1212zdasd\",\"expiryDuration\":\"1973-11-15\",\"secretkey\":\"abcdefg123\"},{\"accessKeyId\":\"dsfdsfzdasd\",\"expiryDuration\":\"2009-01-19\",\"secretkey\":\"mnbcjddk987\"}],\"userName\":\"testiamsvcacc01\",\"createdDate\":\"2004-06-01\"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(iamServiceAccountsService.readFolders(token, "iamsvcacc/123456789012_testiamsvcacc01"))
				.thenReturn(responseEntityExpected);

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(
				"/v2/iamserviceaccounts/folders/secrets?path=iamsvcacc/123456789012_testiamsvcacc01&fetchOption=all")
				.header("vault-token", token).header("Content-Type", "application/json;charset=UTF-8")
				.requestAttr("UserDetails", userDetails)).andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}

	@Test
	public void test_associateApproletoIAMSvcAcc() throws Exception {
		IAMServiceAccountApprole serviceAccountApprole = new IAMServiceAccountApprole("testacc02", "role1", "write",
				"1234567890");

		String inputJson = new ObjectMapper().writeValueAsString(serviceAccountApprole);
		String responseJson = "{\"messages\":[\"Approle is successfully associated with Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(iamServiceAccountsService.associateApproletoIAMsvcacc(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"),
				Mockito.any(IAMServiceAccountApprole.class))).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/iamserviceaccounts/approle")
				.requestAttr("UserDetails", userDetails).header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8").content(inputJson)).andExpect(status().isOk())
				.andExpect(content().string(containsString(responseJson)));
	}

	@Test
	public void test_removeApproleFromIAMSvcAcc() throws Exception {
		IAMServiceAccountApprole serviceAccountApprole = new IAMServiceAccountApprole("testacc02", "role1", "write",
				"1234567890");

		String inputJson = new ObjectMapper().writeValueAsString(serviceAccountApprole);
		String responseJson = "{\"messages\":[\"Approle is successfully removed from Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(iamServiceAccountsService.removeApproleFromIAMSvcAcc(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"),
				Mockito.any(IAMServiceAccountApprole.class))).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.delete("/v2/iamserviceaccounts/approle")
				.requestAttr("UserDetails", userDetails).header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8").content(inputJson)).andExpect(status().isOk())
				.andExpect(content().string(containsString(responseJson)));
	}

	@Test
	public void testRemoveUserFromIAMAvcAccSuccess() throws Exception {
		IAMServiceAccountUser iamSvcAccUser = new IAMServiceAccountUser("testaccount", "testuser1", "read", "1234567");

		String expected = "{\"message\":[\"User is successfully Removed from IAM Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
		when(iamServiceAccountsService.removeUserFromIAMServiceAccount(Mockito.anyString(), Mockito.any(),
				Mockito.any())).thenReturn(responseEntityExpected);
		String inputJson = getJSON(iamSvcAccUser);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.delete("/v2/iamserviceaccounts/user").header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
						.requestAttr(USER_DETAILS_STRING, userDetails).content(inputJson))
				.andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}

	@Test
	public void testAddGroupToIAMSvcAccSuccess() throws Exception {
		IAMServiceAccountGroup iamSvcAccGroup = new IAMServiceAccountGroup("testaccount", "group1", "write", "1234567");
		String inputJson = getJSON(iamSvcAccGroup);
		String responseJson = "{\"messages\":[\"Group is successfully associated with IAM Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(iamServiceAccountsService.addGroupToIAMServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenReturn(responseEntityExpected);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.post("/v2/iamserviceaccounts/group")
						.requestAttr(USER_DETAILS_STRING, userDetails).header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING).content(inputJson))
				.andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(responseJson, actual);
	}

	@Test
	public void testRemoveGroupFromIAMSvcAccSuccess() throws Exception {
		IAMServiceAccountGroup iamSvcAccGroup = new IAMServiceAccountGroup("testaccount", "group1", "write", "1234567");
		String inputJson = getJSON(iamSvcAccGroup);
		String responseJson = "{\"messages\":[\"Group is successfully removed from IAM Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(iamServiceAccountsService.removeGroupFromIAMServiceAccount(Mockito.anyString(), Mockito.any(),
				Mockito.any())).thenReturn(responseEntityExpected);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.delete("/v2/iamserviceaccounts/group")
						.requestAttr(USER_DETAILS_STRING, userDetails).header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING).content(inputJson))
				.andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(responseJson, actual);
	}

}
