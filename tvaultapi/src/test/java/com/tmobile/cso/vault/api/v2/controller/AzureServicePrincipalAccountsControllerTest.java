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

import com.tmobile.cso.vault.api.model.*;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.service.AzureServicePrincipalAccountsService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class AzureServicePrincipalAccountsControllerTest {
	
	
	@InjectMocks
	private AzureServicePrincipalAccountsController azureServicePrincipalAccountsController;
	
	@Mock
	public AzureServicePrincipalAccountsService azureServicePrincipalAccountsService;
	
	private MockMvc mockMvc;
	
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
        this.mockMvc = MockMvcBuilders.standaloneSetup(azureServicePrincipalAccountsController).build();
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";  
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
    }
	
	@Test
	public void testOnboardAzureServiceAccountSuccess() throws Exception {
		AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");

		String expected = "{\"messages\":[\"Successfully completed onboarding of IAM service account into TVault for password rotation.\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
		when(azureServicePrincipalAccountsService.onboardAzureServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenReturn(responseEntityExpected);
		String inputJson = getJSON(serviceAccount);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.post("/v2/azureserviceaccounts/onboard").header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
						.requestAttr(USER_DETAILS_STRING, userDetails).content(inputJson))
				.andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	private AzureServiceAccount generateAzureServiceAccount(String servicePrincipalName) {
		AzureServiceAccount azureServiceAccount = new AzureServiceAccount();
		azureServiceAccount.setServicePrincipalName(servicePrincipalName);
		azureServiceAccount.setServicePrincipalClientId("a987b078-a5a7-55re-8975-8945c545b76d");
		azureServiceAccount.setServicePrincipalId("a987b078-a5a7-55re-8975-8945c545b76d");
		azureServiceAccount.setOwnerNtid("testUser");
		azureServiceAccount.setOwnerEmail("normaluser@testmail.com");
		azureServiceAccount.setApplicationId("app1");
		azureServiceAccount.setApplicationName("App1");
		azureServiceAccount.setApplicationTag("App1");
		azureServiceAccount.setCreatedAtEpoch(604800000L);
		azureServiceAccount.setSecret(generateAzureSecret());
		azureServiceAccount.setTenantId("a987b078-a5a7-55re-8975-8945c545b76d");
		return azureServiceAccount;
	}
	
	private String getJSON(Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return TVaultConstants.EMPTY_JSON;
		}
	}
	
	private List<AzureSecrets> generateAzureSecret() {
		List<AzureSecrets> azureSecrets = new ArrayList<>();
		AzureSecrets azureSecret = new AzureSecrets();
		azureSecret.setSecretKeyId("testaccesskey555");
		azureSecret.setExpiryDuration(604800000L);
		azureSecrets.add(azureSecret);
		return azureSecrets;
	}
	
	@Test
	public void test_getAzureServicePrincipalList_successful() throws Exception {
		String responseJson = "{\"keys\":[{\"userName\":\"testiamsvcacc01\",\"metaDataName\":\"123456789012_testiamsvcacc01\",\"accountID\":\"123456789012\"},{\"userName\":\"test_iamsvcacc2\",\"metaDataName\":\"123456789045_test_iamsvcacc2\",\"accountID\":\"123456789045\"}]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(azureServicePrincipalAccountsService.getAzureServicePrincipalList(userDetails))
				.thenReturn(responseEntityExpected);
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/azureserviceaccounts/list")
				.header(VAULT_TOKEN_STRING, token).header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
				.requestAttr(USER_DETAILS_STRING, userDetails)).andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	@Test
	public void test_readFolders_successful() throws Exception {
		String responseJson = "{\"application_id\":1222,\"createdAtEpoch\":1086073200000,\"isActivated\":true,\"owner_email\":\"Nithin.Nazeer1@T-mobile.com\",\"owner_ntid\":\"NNazeer1\",\"secret\":[{\"accessKeyId\":\"1212zdasd\",\"expiryDuration\":\"1973-11-15\",\"secretkey\":\"abcdefg123\"},{\"accessKeyId\":\"dsfdsfzdasd\",\"expiryDuration\":\"2009-01-19\",\"secretkey\":\"mnbcjddk987\"}],\"userName\":\"testiamsvcacc01\",\"createdDate\":\"2004-06-01\"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(azureServicePrincipalAccountsService.readFolders(token, "azuresvcacc/testiamsvcacc01"))
				.thenReturn(responseEntityExpected);

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(
				"/v2/azureserviceaccounts/folders/secrets?path=azuresvcacc/testiamsvcacc01")
				.header("vault-token", token).header("Content-Type", "application/json;charset=UTF-8")
				.requestAttr("UserDetails", userDetails)).andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	
	@Test
	public void test_getAzureServiceAccountSecretKey_successful() throws Exception {
		String responseJson = "{\"application_id\":1222,\"createdAtEpoch\":1086073200000,\"isActivated\":true,\"owner_email\":\"Nithin.Nazeer1@T-mobile.com\",\"owner_ntid\":\"NNazeer1\",\"secret\":[{\"accessKeyId\":\"1212zdasd\",\"expiryDuration\":\"1973-11-15\",\"secretkey\":\"abcdefg123\"},{\"accessKeyId\":\"dsfdsfzdasd\",\"expiryDuration\":\"2009-01-19\",\"secretkey\":\"mnbcjddk987\"}],\"userName\":\"testiamsvcacc01\",\"createdDate\":\"2004-06-01\"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(azureServicePrincipalAccountsService.getAzureServiceAccountSecretKey(token , "testiamsvcacc01", "secret_01"))
				.thenReturn(responseEntityExpected);		
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/azureserviceaccounts/secrets/testiamsvcacc01/secret_01")
				.header(VAULT_TOKEN_STRING, token).header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
				.requestAttr(USER_DETAILS_STRING, userDetails)).andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	

	@Test
	public void test_readSecrets_successful() throws Exception {
		String responseJson = "{\"accessKeySecret\":" + "assO/OetcHce1VugthF6KE9hqv2PWWbX3ULrpe1Tss" + "}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(azureServicePrincipalAccountsService.readSecret(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(responseEntityExpected);		
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/azureserviceaccounts/secret/testiamsvcacc01/1212zdasdssss")
				.header(VAULT_TOKEN_STRING, token).header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
				.requestAttr(USER_DETAILS_STRING, userDetails)).andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	@Test
	public void testoffboardAzureServiceAccountSuccess() throws Exception {

		AzureServiceAccountOffboardRequest azureServiceAccountOffboardRequest = new AzureServiceAccountOffboardRequest("testaccount");
		String expected = "{\"messages\":[\"Successfully offboarded Azure service account (if existed) from T-Vault\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
		when(azureServicePrincipalAccountsService.offboardAzureServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenReturn(responseEntityExpected);
		String inputJson = getJSON(azureServiceAccountOffboardRequest);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.post("/v2/azureserviceaccounts/offboard").header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
						.requestAttr(USER_DETAILS_STRING, userDetails).content(inputJson))
				.andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}

	@Test
	public void test_getOnboardedAzureServiceAccounts_successful() throws Exception {
		String responseJson = "{\"accessKeySecret\":" + "assO/OetcHce1VugthF6KE9hqv2PWWbX3ULrpe1Tss" + "}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		String expected = responseEntityExpected.getBody();

		when(azureServicePrincipalAccountsService.getOnboardedAzureServiceAccounts(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any()))
				.thenReturn(responseEntityExpected);		
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/azureserviceaccounts")
				.header(VAULT_TOKEN_STRING, token)
				.requestAttr(USER_DETAILS_STRING, userDetails)).andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
}
	
	@Test
	public void testRemoveUserFromAzureSvcAccSuccess() throws Exception {
		AzureServiceAccountUser iamSvcAccUser = new AzureServiceAccountUser("testaccount", "testuser1", "read");

		String expected = "{\"message\":[\"User is successfully Removed from Azure Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
		when(azureServicePrincipalAccountsService.removeUserFromAzureServiceAccount(Mockito.anyString(), Mockito.any(),
				Mockito.any())).thenReturn(responseEntityExpected);
		String inputJson = getJSON(iamSvcAccUser);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.delete("/v2/azureserviceaccounts/user").header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING)
						.requestAttr(USER_DETAILS_STRING, userDetails).content(inputJson))
				.andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	

	@Test
	public void test_createRole() throws Exception {
		AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		String inputJson = new ObjectMapper().writeValueAsString(serviceAccountAWSRole);
		String responseJson = "{\"messages\":[\"AWS Role created \"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(azureServicePrincipalAccountsService.createAWSRole(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"),
				Mockito.any(AWSLoginRole.class))).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/azureserviceaccounts/aws/role").header(VAULT_TOKEN_STRING, token)
				.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING).requestAttr(USER_DETAILS_STRING, userDetails)
				.content(inputJson)).andExpect(status().isOk()).andReturn();
	}
	
	@Test
	public void test_createIAMRole() throws Exception {
		AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

        String inputJson =new ObjectMapper().writeValueAsString(serviceAccountAWSRole);
		String responseJson = "{\"messages\":[\"AWS Role created \"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(azureServicePrincipalAccountsService.createIAMRole(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any()))
				.thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/azureserviceaccounts/aws/iam/role")
				.requestAttr("UserDetails", userDetails).header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8").content(inputJson)).andExpect(status().isOk())
				.andExpect(content().string(containsString(responseJson)));

	}
	
	@Test
	public void test_associateAWSroletoAzureSvcAcc() throws Exception {
		AzureServiceAccountAWSRole serviceAccountApprole = new AzureServiceAccountAWSRole();

		serviceAccountApprole.setAccess("read");
		serviceAccountApprole.setAzureSvcAccName("testaccount");
		serviceAccountApprole.setRolename("role1");

		String inputJson = new ObjectMapper().writeValueAsString(serviceAccountApprole);
		String responseJson = "{\"messages\":[\"AWS Role successfully associated with Azure Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

		when(azureServicePrincipalAccountsService.addAwsRoleToAzureSvcacc(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"),
				Mockito.any(AzureServiceAccountAWSRole.class))).thenReturn(responseEntityExpected);
		
		
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/azureserviceaccounts/role")
				.requestAttr("UserDetails", userDetails).header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8").content(inputJson)).andExpect(status().isOk())
				.andExpect(content().string(containsString(responseJson)));
	}

	@Test
	public void testAddGroupToAzureSvcAccSuccess() throws Exception {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "write");
		String inputJson = getJSON(azureSvcAccGroup);
		String responseJson = "{\"messages\":[\"Group is successfully associated with Azure Service Principal\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenReturn(responseEntityExpected);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.post("/v2/azureserviceaccounts/group")
						.requestAttr(USER_DETAILS_STRING, userDetails).header(VAULT_TOKEN_STRING, token)
						.header(CONTENT_TYPE_STRING, CONTENT_TYPE_VALUE_STRING).content(inputJson))
				.andExpect(status().isOk()).andReturn();
		String actual = result.getResponse().getContentAsString();
		assertEquals(responseJson, actual);
	}

	@Test
	public void test_activateAzureServicePrincipal() throws Exception {

		String responseJson = "{\"messages\":[\"Azure Service Principal activated successfully\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(azureServicePrincipalAccountsService.activateAzureServicePrincipal(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), eq(userDetails), Mockito.any())).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/azureserviceaccounts/activateAzureServicePrincipal?servicePrincipalName=testaureserviceprincipal").requestAttr("UserDetails", userDetails)
				.header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(responseJson)));
	}

	@Test
	public void test_rotateSecret() throws Exception {
		AzureServicePrincipalRotateRequest azureServicePrincipalRotateRequest = new AzureServicePrincipalRotateRequest("testaureserviceprincipal", "testsecretkeyid", "12345678-1234-1234-1234-123456789098", "12345678-1234-1234-97c8-123456789098");
		String inputJson = getJSON(azureServicePrincipalRotateRequest);
		String responseJson = "{\"messages\":[\"Azure Service Principal secret rotated successfully\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		when(azureServicePrincipalAccountsService.rotateSecret(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AzureServicePrincipalRotateRequest.class))).thenReturn(responseEntityExpected);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/azureserviceaccounts/rotate").requestAttr("UserDetails", userDetails)
				.header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8")
				.content(inputJson))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(responseJson)));
	}
	@Test
	public void test_removeAWSroletoAzureSvcAcc() throws Exception {
		AzureServiceAccountAWSRole serviceAccountApprole = new AzureServiceAccountAWSRole();

		serviceAccountApprole.setAccess("read");
		serviceAccountApprole.setAzureSvcAccName("testaccount");
		serviceAccountApprole.setRolename("role1");

		String inputJson = new ObjectMapper().writeValueAsString(serviceAccountApprole);
		String responseJson = "{\"messages\":[\"AWS Role successfully removed from Azure Service Account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

		when(azureServicePrincipalAccountsService.removeAwsRoleFromAzureSvcacc(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"),
				Mockito.any(AzureServiceAccountAWSRole.class))).thenReturn(responseEntityExpected);
		
		
		mockMvc.perform(MockMvcRequestBuilders.delete("/v2/azureserviceaccounts/role")
				.requestAttr("UserDetails", userDetails).header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
				.header("Content-Type", "application/json;charset=UTF-8").content(inputJson)).andExpect(status().isOk())
				.andExpect(content().string(containsString(responseJson)));
	}
	
}
