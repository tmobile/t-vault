package com.tmobile.cso.vault.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tmobile.cso.vault.api.model.*;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.AzureServiceAccountConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;

import com.tmobile.cso.vault.api.model.AzureSecrets;
import com.tmobile.cso.vault.api.model.AzureSecretsMetadata;
import com.tmobile.cso.vault.api.model.AzureServiceAccount;
import com.tmobile.cso.vault.api.model.AzureServiceAccountAWSRole;
import com.tmobile.cso.vault.api.model.AzureServiceAccountApprole;
import com.tmobile.cso.vault.api.model.AzureServiceAccountGroup;
import com.tmobile.cso.vault.api.model.AzureServiceAccountMetadataDetails;
import com.tmobile.cso.vault.api.model.AzureServiceAccountOffboardRequest;
import com.tmobile.cso.vault.api.model.AzureServiceAccountUser;
import com.tmobile.cso.vault.api.model.AzureSvccAccMetadata;
import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.model.DirectoryObjectsList;
import com.tmobile.cso.vault.api.model.DirectoryUser;
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
import com.tmobile.cso.vault.api.model.OIDCGroup;
import com.tmobile.cso.vault.api.model.OIDCLookupEntityRequest;
import com.tmobile.cso.vault.api.model.UserDetails;

import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.AzureServiceAccountUtils;
import com.tmobile.cso.vault.api.utils.EmailUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = { "com.tmobile.cso.vault.api" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ ControllerUtil.class, JSONUtil.class, PolicyUtils.class, OIDCUtil.class})
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class AzureServicePrincipalAccountsServiceTest {
	
	@InjectMocks
	AzureServicePrincipalAccountsService azureServicePrincipalAccountsService;
	
	@Mock
	private RequestProcessor reqProcessor;
	
	@Mock
	AccessService accessService;
	
	@Mock
	DirectoryService directoryService;
	
	@Mock
	EmailUtils emailUtils;
	
	@Mock
    TokenUtils tokenUtils;
	
	@Mock
	AzureServiceAccountUtils azureServiceAccountUtils;
	
	String token;

    @Mock
    PolicyUtils policyUtils;
    
    @Mock
    OIDCUtil OIDCUtil;

	@Mock
	UserDetails userDetails;
	
	@Mock
    AppRoleService appRoleService;
	
	@Mock
	AWSAuthService awsAuthService;

	@Mock
	AWSIAMAuthService awsiamAuthService;
	
	@Before
    public void setUp()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(OIDCUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(OIDCUtil.class, "log", LogManager.getLogger(OIDCUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");
        ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "ldap");
		ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "azureMasterPolicyName", "azure_master_policy");
        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/identity");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }

    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        Response response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        if (!expectedBody.equals("")) {
            response.setResponse(expectedBody);
        }
        return response;
    }

	UserDetails getMockUser(boolean isAdmin) {
		token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		userDetails = new UserDetails();
		userDetails.setUsername("normaluser");
		userDetails.setAdmin(isAdmin);
		userDetails.setClientToken(token);
		userDetails.setSelfSupportToken(token);
		return userDetails;
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
	
	private AzureServiceAccountMetadataDetails populateAzureSvcAccMetaData(AzureServiceAccount azureServiceAccount) {

		AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails = new AzureServiceAccountMetadataDetails();
		List<AzureSecretsMetadata> azureSecretsMetadatas = new ArrayList<>();
		azureServiceAccountMetadataDetails.setServicePrincipalName(azureServiceAccount.getServicePrincipalName());
		azureServiceAccountMetadataDetails.setServicePrincipalId(azureServiceAccount.getServicePrincipalId());
		azureServiceAccountMetadataDetails.setServicePrincipalClientId(azureServiceAccount.getServicePrincipalClientId());
		azureServiceAccountMetadataDetails.setApplicationId(azureServiceAccount.getApplicationId());
		azureServiceAccountMetadataDetails.setApplicationName(azureServiceAccount.getApplicationName());
		azureServiceAccountMetadataDetails.setApplicationTag(azureServiceAccount.getApplicationTag());
		azureServiceAccountMetadataDetails.setCreatedAtEpoch(azureServiceAccount.getCreatedAtEpoch());
		azureServiceAccountMetadataDetails.setOwnerEmail(azureServiceAccount.getOwnerEmail());
		azureServiceAccountMetadataDetails.setOwnerNtid(azureServiceAccount.getOwnerNtid());
		for (AzureSecrets azureSecrets : azureServiceAccount.getSecret()) {
			AzureSecretsMetadata azureSecretsMetadata = new AzureSecretsMetadata();
			azureSecretsMetadata.setSecretKeyId(azureSecrets.getSecretKeyId());
			azureSecretsMetadata.setExpiryDuration(azureSecrets.getExpiryDuration());
			azureSecretsMetadatas.add(azureSecretsMetadata);
		}
		azureServiceAccountMetadataDetails.setSecret(azureSecretsMetadatas);
		azureServiceAccountMetadataDetails.setTenantId(azureServiceAccount.getTenantId());
		return azureServiceAccountMetadataDetails;
	}

	@Test
	public void testOnboardAzureServiceAccountSuccss() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
		AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");
		String azureSvcAccName = serviceAccount.getServicePrincipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		Map<String, Object> iamSvcAccPolicyMap = new HashMap<>();
		iamSvcAccPolicyMap.put("isActivated", false);

		AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails = populateAzureSvcAccMetaData(serviceAccount);
		AzureSvccAccMetadata iamSvccAccMetadata = new AzureSvccAccMetadata(azureSvccAccPath,
				azureServiceAccountMetadataDetails);

		when(reqProcessor.process(eq("/azure/onboardedlist"), Mockito.any(), eq(token))).thenReturn(getMockResponse(
				HttpStatus.OK, true, "{\"keys\":[\"svc_cce_usertestrr12\",\"svc_cce_usertestrr13\"]}"));

		when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
		when(ControllerUtil.parseJson(metaDataStr)).thenReturn(iamSvcAccPolicyMap);
		when(ControllerUtil.convetToJson(iamSvcAccPolicyMap)).thenReturn(metadatajson);
		when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

		// create metadata
		when(JSONUtil.getJSON(iamSvccAccMetadata)).thenReturn(iamMetaDataStr);
		Map<String, Object> rqstParams = new HashMap<>();
		rqstParams.put("isActivated", false);
		rqstParams.put("servicePrincipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrincipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrincipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("tenantId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("createdAtEpoch", 12345L);
		rqstParams.put("owner_ntid", "normaluser");
		rqstParams.put("owner_email", "normaluser@testmail.com");
		rqstParams.put("application_id", "app1");
		rqstParams.put("application_name", "App1");

		when(ControllerUtil.parseJson(iamMetaDataStr)).thenReturn(rqstParams);
		when(ControllerUtil.convetToJson(rqstParams)).thenReturn(iamMetaDatajson);
		when(ControllerUtil.createMetadata(any(), eq(token))).thenReturn(true);

		// CreateIAMServiceAccountPolicies
		ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
		when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

		// Add User to Service Account
		Response userResponse = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
		Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
		when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"testuser\"}", token)).thenReturn(userResponse);

		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPUser(eq("testuser"), any(), any(), eq(token)))
				.thenReturn(ldapConfigureResponse);
		when(ControllerUtil.updateMetadata(any(), any())).thenReturn(responseNoContent);

		// System under test
		String expectedResponse = "{\"messages\":[\"Successfully completed onboarding of Azure service account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));

		DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUserfirstname,lastname");
        directoryUser.setGivenName("testuser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("normaluser");
        directoryUser.setUserName("normaluser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        ResponseEntity<DirectoryObjects> responseEntityCorpExpected = ResponseEntity.status(HttpStatus.OK).body(users);
        when(directoryService.searchByCorpId(Mockito.any())).thenReturn(responseEntityCorpExpected);

		ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "supportEmail", "support@abc.com");
		Mockito.doNothing().when(emailUtils).sendHtmlEmalFromTemplate(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any());
		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"iamportal_master_policy \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();
		currentPolicies.add("azure_master_policy");
		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	@Test
	public void testOnboardAzureServiceAccountNotAuthorized() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
		AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");

		Map<String, Object> iamSvcAccPolicyMap = new HashMap<>();
		iamSvcAccPolicyMap.put("isActivated", false);
		// System under test
		String expectedResponse = "{\"errors\":[\"Access denied. Not authorized to perform onboarding for Azure service accounts.\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();

		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	@Test
	public void testOnboardAzureServiceAccountAlreadyExists() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
		AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");
		
		String azureSvcAccName = serviceAccount.getServicePrincipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		Map<String, Object> iamSvcAccPolicyMap = new HashMap<>();
		iamSvcAccPolicyMap.put("isActivated", false);

		AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails = populateAzureSvcAccMetaData(serviceAccount);
		AzureSvccAccMetadata iamSvccAccMetadata = new AzureSvccAccMetadata(azureSvccAccPath,
				azureServiceAccountMetadataDetails);

		when(reqProcessor.process(eq("/azure/onboardedlist"), Mockito.any(), eq(token))).thenReturn(getMockResponse(
				HttpStatus.OK, true, "{\"keys\":[\"svc_cce_usertestrr16\",\"svc_cce_usertestrr13\"]}"));


		when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
		when(ControllerUtil.parseJson(metaDataStr)).thenReturn(iamSvcAccPolicyMap);
		when(ControllerUtil.convetToJson(iamSvcAccPolicyMap)).thenReturn(metadatajson);
		when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

		// create metadata
		when(JSONUtil.getJSON(iamSvccAccMetadata)).thenReturn(iamMetaDataStr);
		Map<String, Object> rqstParams = new HashMap<>();
		rqstParams.put("isActivated", false);
		rqstParams.put("servicePrincipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrincipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrincipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("tenantId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("createdAtEpoch", 12345L);
		rqstParams.put("owner_ntid", "normaluser");
		rqstParams.put("owner_email", "normaluser@testmail.com");
		rqstParams.put("application_id", "app1");
		rqstParams.put("application_name", "App1");

		when(ControllerUtil.parseJson(iamMetaDataStr)).thenReturn(rqstParams);
		when(ControllerUtil.convetToJson(rqstParams)).thenReturn(iamMetaDatajson);
		when(ControllerUtil.createMetadata(any(), eq(token))).thenReturn(true);

		String expectedResponse = "{\"errors\":[\"Failed to onboard Azure Service Account. Azure Service account is already onboarded\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();
		currentPolicies.add("azure_master_policy");
		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	
	
	@Test
	public void testOnboardAzureServiceAccountMetaDataCreationFailed() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
	AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");
		
		String azureSvcAccName = serviceAccount.getServicePrincipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		Map<String, Object> iamSvcAccPolicyMap = new HashMap<>();
		iamSvcAccPolicyMap.put("isActivated", false);

		AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails = populateAzureSvcAccMetaData(serviceAccount);
		AzureSvccAccMetadata iamSvccAccMetadata = new AzureSvccAccMetadata(azureSvccAccPath,
				azureServiceAccountMetadataDetails);

		when(reqProcessor.process(eq("/azure/onboardedlist"), Mockito.any(), eq(token))).thenReturn(getMockResponse(
				HttpStatus.OK, true, "{\"keys\":[\"svc_cce_usertestrr12\",\"svc_cce_usertestrr13\"]}"));

		when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
		when(ControllerUtil.parseJson(metaDataStr)).thenReturn(iamSvcAccPolicyMap);
		when(ControllerUtil.convetToJson(iamSvcAccPolicyMap)).thenReturn(metadatajson);
		when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

		// create metadata
		when(JSONUtil.getJSON(iamSvccAccMetadata)).thenReturn(iamMetaDataStr);
		Map<String, Object> rqstParams = new HashMap<>();
		rqstParams.put("isActivated", false);
		rqstParams.put("servicePrincipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrincipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrincipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("tenantId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("createdAtEpoch", 12345L);
		rqstParams.put("owner_ntid", "normaluser");
		rqstParams.put("owner_email", "normaluser@testmail.com");
		rqstParams.put("application_id", "app1");
		rqstParams.put("application_name", "App1");

		when(ControllerUtil.parseJson(iamMetaDataStr)).thenReturn(rqstParams);
		when(ControllerUtil.convetToJson(rqstParams)).thenReturn(iamMetaDatajson);
		when(ControllerUtil.createMetadata(any(), eq(token))).thenReturn(false);

		// System under test
		String expectedResponse = "{\"errors\":[\"Metadata creation failed for Azure Service Account.\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);

		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();
		currentPolicies.add("azure_master_policy");
		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	
	@Test
	public void testOnboardAzureServiceAccountPolicyCreationFailed() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
        AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");
		
		String azureSvcAccName = serviceAccount.getServicePrincipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		Map<String, Object> iamSvcAccPolicyMap = new HashMap<>();
		iamSvcAccPolicyMap.put("isActivated", false);

		AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails = populateAzureSvcAccMetaData(serviceAccount);
		AzureSvccAccMetadata iamSvccAccMetadata = new AzureSvccAccMetadata(azureSvccAccPath,
				azureServiceAccountMetadataDetails);

		when(reqProcessor.process(eq("/azure/onboardedlist"), Mockito.any(), eq(token))).thenReturn(getMockResponse(
				HttpStatus.OK, true, "{\"keys\":[\"svc_cce_usertestrr12\",\"svc_cce_usertestrr13\"]}"));

		when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
		when(ControllerUtil.parseJson(metaDataStr)).thenReturn(iamSvcAccPolicyMap);
		when(ControllerUtil.convetToJson(iamSvcAccPolicyMap)).thenReturn(metadatajson);
		when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

		// create metadata
		when(JSONUtil.getJSON(iamSvccAccMetadata)).thenReturn(iamMetaDataStr);
		Map<String, Object> rqstParams = new HashMap<>();
		rqstParams.put("isActivated", false);
		rqstParams.put("servicePrincipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrincipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrincipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("tenantId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("createdAtEpoch", 12345L);
		rqstParams.put("owner_ntid", "normaluser");
		rqstParams.put("owner_email", "normaluser@testmail.com");
		rqstParams.put("application_id", "app1");
		rqstParams.put("application_name", "App1");
		
		when(ControllerUtil.parseJson(iamMetaDataStr)).thenReturn(rqstParams);
		when(ControllerUtil.convetToJson(rqstParams)).thenReturn(iamMetaDatajson);
		when(ControllerUtil.createMetadata(any(), eq(token))).thenReturn(true);

		// CreateIAMServiceAccountPolicies
		ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.MULTI_STATUS)
				.body("{\"messages\":[\"Failed to create some of the policies for Azure service account\"]}");
		when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

		// delete policy mock
		ResponseEntity<String> deletePolicyResponse = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
		when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(deletePolicyResponse);

		when(reqProcessor.process(eq("/delete"), Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true,""));		

		// System under test
		String expectedResponse = "{\"errors\":[\"Failed to onboard Azure service account. Policy creation failed.\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();
		currentPolicies.add("azure_master_policy");
		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	
	@Test
	public void testOnboardAzureServiceAccountAddOwnerFailed() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
		AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");

		String azureSvcAccName = serviceAccount.getServicePrincipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrincipalName\": \"svc_cce_usertestrr16\", \"servicePrincipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrincipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		Map<String, Object> iamSvcAccPolicyMap = new HashMap<>();
		iamSvcAccPolicyMap.put("isActivated", false);
		AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails = populateAzureSvcAccMetaData(serviceAccount);
		AzureSvccAccMetadata iamSvccAccMetadata = new AzureSvccAccMetadata(azureSvccAccPath,
				azureServiceAccountMetadataDetails);

		when(reqProcessor.process(eq("/azure/onboardedlist"), Mockito.any(), eq(token))).thenReturn(getMockResponse(
				HttpStatus.OK, true, "{\"keys\":[\"svc_cce_usertestrr12\",\"svc_cce_usertestrr13\"]}"));

		when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
		when(ControllerUtil.parseJson(metaDataStr)).thenReturn(iamSvcAccPolicyMap);
		when(ControllerUtil.convetToJson(iamSvcAccPolicyMap)).thenReturn(metadatajson);
		when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

		// create metadata
		when(JSONUtil.getJSON(iamSvccAccMetadata)).thenReturn(iamMetaDataStr);
		Map<String, Object> rqstParams = new HashMap<>();
		rqstParams.put("isActivated", false);
		rqstParams.put("servicePrincipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrincipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrincipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("tenantId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("createdAtEpoch", 12345L);
		rqstParams.put("owner_ntid", "normaluser");
		rqstParams.put("owner_email", "normaluser@testmail.com");
		rqstParams.put("application_id", "app1");
		rqstParams.put("application_name", "App1");
		
		
		when(ControllerUtil.parseJson(iamMetaDataStr)).thenReturn(rqstParams);
		when(ControllerUtil.convetToJson(rqstParams)).thenReturn(iamMetaDatajson);
		when(ControllerUtil.createMetadata(any(), eq(token))).thenReturn(true);

		// CreateIAMServiceAccountPolicies
		ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
		when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

		// Add User to Service Account
		Response userResponse = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
		Response ldapConfigureResponse = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, "{\"errors\":[\"Failed to add user to the Azure Service Account\"]}");
		when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"testuser\"}", token)).thenReturn(userResponse);

		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPUser(eq("testuser"), any(), any(), eq(token)))
				.thenReturn(ldapConfigureResponse);

		// delete policy mock
		ResponseEntity<String> deletePolicyResponse = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
		when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(deletePolicyResponse);

		when(reqProcessor.process(eq("/delete"), Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true,""));		

		// System under test
		String expectedResponse = "{\"errors\":[\"Failed to onboard Azure service account. Association of owner permission failed\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));

		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();
		currentPolicies.add("azure_master_policy");
		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	
	 @Test
	    public void test_getAzureServicePrincipalList_successfully() {
	        userDetails = getMockUser(false);
	        token = userDetails.getClientToken();
	        String [] policies = {"r_users_s1", "w_users_s2", "r_shared_s3", "w_shared_s4", "r_apps_s5", "w_apps_s6", "d_apps_s7", "w_svcacct_test", "r_azuresvcacc_svc_cce_usertestrr16"};
	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"shared\":[{\"s3\":\"read\"},{\"s4\":\"write\"}],\"users\":[{\"s1\":\"read\"},{\"s2\":\"write\"}],\"svcacct\":[{\"test\":\"read\"}],\"azuresvcacc\":[{\"svc_cce_usertestrr16\":\"read\"}],\"apps\":[{\"s5\":\"read\"},{\"s6\":\"write\"},{\"s7\":\"deny\"}]}");
	       
	        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
	        when(JSONUtil.getJSON(Mockito.any())).thenReturn("{\"shared\":[{\"s3\":\"read\"},{\"s4\":\"write\"}],\"users\":[{\"s1\":\"read\"},{\"s2\":\"write\"}],\"svcacct\":[{\"test\":\"read\"}],\"azuresvcacc\":[{\"svc_cce_usertestrr16\":\"read\"}],\"apps\":[{\"s5\":\"read\"},{\"s6\":\"write\"},{\"s7\":\"deny\"}]}");
	        ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.getAzureServicePrincipalList(userDetails);
	        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	        assertEquals(responseEntityExpected, responseEntity);
	    }
	 
	 @Test
		public void test_readFolders_successfully() throws IOException {
			String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
			String path = "testiamsvcacc01";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(
					"{\"folders\":[\"testiamsvcacc01_01\",\"testiamsvcacc01_02\"],\"path\":\"testiamsvcacc01\",\"servicePrincipalName\":\"testiamsvcacc01\"}");

			when(reqProcessor.process(eq("/azure/list"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testiamsvcacc01_01\",\"testiamsvcacc01_02\"]}"));
			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.readFolders(token, path);
			assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
	 
		@Test
		public void test_getAzureServiceAccountSecretKey_successfully() {
			String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
			String iamSvcaccName = "testiamsvcacc01";
			String folderName = "testiamsvc_01";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(
					"{\"accessKeyId\":\"1212zdasd\",\"accessKeySecret\":\"assOOetcHce1VugthF6KE9hqv2PWWbX3ULrpe1T\",\"awsAccountId\":\"123456789012\",\"expiryDateEpoch\":1609845308000,\"userName\":\"testiamsvcacc01_01\",\"expiryDate\":\"2021-01-05 16:45:08\"}");

			when(reqProcessor.process(eq("/azuresvcacct"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"accessKeyId\":\"1212zdasd\",\"accessKeySecret\":\"assOOetcHce1VugthF6KE9hqv2PWWbX3ULrpe1T\",\"awsAccountId\":\"123456789012\",\"expiryDateEpoch\":1609845308000,\"userName\":\"testiamsvcacc01_01\",\"expiryDate\":\"2021-01-05 16:45:08\"}}"));
			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.getAzureServiceAccountSecretKey(token, iamSvcaccName, folderName);
			assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		}
		
		@Test
		public void testoffboardAzureServiceAccountLdap_succss() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "ldap");
			AzureServiceAccountOffboardRequest serviceAccount = new AzureServiceAccountOffboardRequest("testaccount");
			String azureSvcAccName = serviceAccount.getAzureSvcAccName();
			String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

			// Mock approle permission check
			Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
			when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
			List<String> currentPolicies = new ArrayList<>();
			currentPolicies.add("azure_master_policy");
			try {
				when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// oidc mock
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<OIDCEntityResponse> oidcResponse = ResponseEntity.status(HttpStatus.OK).body(oidcEntityResponse);
			when(OIDCUtil.oidcFetchEntityDetails(any(), any(), any(), eq(true))).thenReturn(oidcResponse);

			// delete policy mock
			ResponseEntity<String> deletePolicyResponse = ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
			when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(deletePolicyResponse);

			// metadata mock
			when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"isActivated\":true,\"owner_ntid\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"},\"groups\":{\"testgroup1\":\"read\"},\"app-roles\":{\"approle1\":\"read\"}}}"));

			// Mock user response and config user
			Response userResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
			when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"normaluser\"}", token)).thenReturn(userResponse);

			try {
				List<String> resList = new ArrayList<>();
				resList.add("default");
				when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
			} catch (IOException e) {
				e.printStackTrace();
			}
			when(ControllerUtil.configureLDAPUser(eq("normaluser"), any(), any(), eq(token)))
					.thenReturn(ldapConfigureResponse);

			// Mock group response and config group
			Response groupResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"testgroup1\"}", token)).thenReturn(groupResponse);
			when(ControllerUtil.configureLDAPGroup(eq("testgroup1"), any(), eq(token))).thenReturn(ldapConfigureResponse);

			// Mock approle response and config approle
			Response approleResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}", token)).thenReturn(approleResponse);
			when(appRoleService.configureApprole(eq("approle1"), any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, ""));

			// System under test
			String expectedResponse = "{\"messages\":[\"Successfully offboarded Azure service account (if existed) from T-Vault\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

			String azureMetaDataStr = "{ \"data\": {\"userName\": \"svc_vault_test5\", \"awsAccountId\": \"1234567890\", \"awsAccountName\": \"testaccount1\", \"createdAtEpoch\": 1609754282000, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"accessKeyId\":\"testaccesskey\", \"expiryDuration\":12345}]}, \"path\": \"azuresvcacc/svc_vault_test5\"}";
			when(reqProcessor.process(eq("/read"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					azureMetaDataStr));

			when(reqProcessor.process(eq("/delete"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true,
					""));

			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.offboardAzureServiceAccount(token,
					serviceAccount, userDetails);
			assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		
		



		@Test
		public void testoffboardAzureServiceAccountOIDC_succss() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
			AzureServiceAccountOffboardRequest serviceAccount = new AzureServiceAccountOffboardRequest("testaccount");
			String azureSvcAccName =  serviceAccount.getAzureSvcAccName();

			// Mock approle permission check
			Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
			when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
			List<String> currentPolicies = new ArrayList<>();
			currentPolicies.add("azure_master_policy");
			try {
				when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// oidc mock
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<OIDCEntityResponse> oidcResponse = ResponseEntity.status(HttpStatus.OK).body(oidcEntityResponse);
			when(OIDCUtil.oidcFetchEntityDetails(any(), any(), any(), eq(true))).thenReturn(oidcResponse);

			// delete policy mock
			ResponseEntity<String> deletePolicyResponse = ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
			when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(deletePolicyResponse);

			// metadata mock
			when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"isActivated\":true,\"owner_ntid\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"},\"groups\":{\"testgroup1\":\"read\"},\"app-roles\":{\"approle1\":\"read\"}}}"));

			// Mock user response and config user
			Response userResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
			when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"normaluser\"}", token)).thenReturn(userResponse);

			try {
				List<String> resList = new ArrayList<>();
				resList.add("default");
				when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
			} catch (IOException e) {
				e.printStackTrace();
			}
			when(ControllerUtil.configureLDAPUser(eq("normaluser"), any(), any(), eq(token)))
					.thenReturn(ldapConfigureResponse);

			// Mock group response and config group
			List<String> currentpolicies = new ArrayList<>();
			currentpolicies.add("default");
			currentpolicies.add("w_shared_mysafe01");
			currentpolicies.add("w_shared_mysafe02");
			OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
			when(OIDCUtil.getIdentityGroupDetails("testgroup1", token)).thenReturn(oidcGroup);
			Response groupResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"testgroup1\"}", token)).thenReturn(groupResponse);
			when(ControllerUtil.configureLDAPGroup(eq("testgroup1"), any(), eq(token))).thenReturn(ldapConfigureResponse);

			// Mock approle response and config approle
			Response approleResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}", token)).thenReturn(approleResponse);
			when(appRoleService.configureApprole(eq("approle1"), any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, ""));

			// System under test
			String expectedResponse = "{\"messages\":[\"Successfully offboarded Azure service account (if existed) from T-Vault\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

			String iamMetaDataStr = "{ \"data\": {\"userName\": \"svc_vault_test5\", \"awsAccountId\": \"1234567890\", \"awsAccountName\": \"testaccount1\", \"createdAtEpoch\": 1609754282000, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"accessKeyId\":\"testaccesskey\", \"expiryDuration\":12345}]}, \"path\": \"iamsvcacc/1234567890_svc_vault_test5\"}";
			when(reqProcessor.process(eq("/read"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					iamMetaDataStr));

			when(reqProcessor.process(eq("/delete"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true,
					""));

			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.offboardAzureServiceAccount(token,
					serviceAccount, userDetails);
			assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		
		@Test
		public void testoffboardAzureServiceAccountOIDC_failed_403() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
			AzureServiceAccountOffboardRequest serviceAccount = new AzureServiceAccountOffboardRequest("testaccount");
			String azureSvcAccName = serviceAccount.getAzureSvcAccName();

			// Mock approle permission check
			Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
			when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
			List<String> currentPolicies = new ArrayList<>();
			currentPolicies.add("default");
			try {
				when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// System under test
			String expectedResponse = "{\"errors\":[\"Access denied. Not authorized to perform offboarding of Azure service accounts.\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body(expectedResponse);

			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.offboardAzureServiceAccount(token,
					serviceAccount, userDetails);
			assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		
		
		@Test
		public void testoffboardAzureServiceAccountOIDC_failed_to_delete_policy() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
			AzureServiceAccountOffboardRequest serviceAccount = new AzureServiceAccountOffboardRequest("testaccount");
			String azureSvcAccName =  serviceAccount.getAzureSvcAccName();

			// Mock approle permission check
			Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
			when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
			List<String> currentPolicies = new ArrayList<>();
			currentPolicies.add("azure_master_policy");
			try {
				when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// oidc mock
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<OIDCEntityResponse> oidcResponse = ResponseEntity.status(HttpStatus.OK).body(oidcEntityResponse);
			when(OIDCUtil.oidcFetchEntityDetails(any(), any(), any(), eq(true))).thenReturn(oidcResponse);

			// delete policy mock
			ResponseEntity<String> deletePolicyResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("");
			when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(deletePolicyResponse);

			// System under test
			String expectedResponse = "{\"errors\":[\"Failed to Offboard Azure service account. Policy deletion failed.\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.offboardAzureServiceAccount(token,
					serviceAccount, userDetails);
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		
		
		@Test
		public void testoffboardAzureServiceAccountOIDC_failed_to_delete_secret() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
			AzureServiceAccountOffboardRequest serviceAccount = new AzureServiceAccountOffboardRequest("testaccount");
			String azureSvcAccName =  serviceAccount.getAzureSvcAccName();

			// Mock approle permission check
			Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
			when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
			List<String> currentPolicies = new ArrayList<>();
			currentPolicies.add("azure_master_policy");
			try {
				when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// oidc mock
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<OIDCEntityResponse> oidcResponse = ResponseEntity.status(HttpStatus.OK).body(oidcEntityResponse);
			when(OIDCUtil.oidcFetchEntityDetails(any(), any(), any(), eq(true))).thenReturn(oidcResponse);

			// delete policy mock
			ResponseEntity<String> deletePolicyResponse = ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
			when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(deletePolicyResponse);

			// metadata mock
			when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"isActivated\":true,\"owner_ntid\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"},\"groups\":{\"testgroup1\":\"read\"},\"app-roles\":{\"approle1\":\"read\"}}}"));

			// Mock user response and config user
			Response userResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
			when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"normaluser\"}", token)).thenReturn(userResponse);

			try {
				List<String> resList = new ArrayList<>();
				resList.add("default");
				when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
			} catch (IOException e) {
				e.printStackTrace();
			}
			when(ControllerUtil.configureLDAPUser(eq("normaluser"), any(), any(), eq(token)))
					.thenReturn(ldapConfigureResponse);

			// Mock group response and config group
			List<String> currentpolicies = new ArrayList<>();
			currentpolicies.add("default");
			currentpolicies.add("w_shared_mysafe01");
			currentpolicies.add("w_shared_mysafe02");
			OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
			when(OIDCUtil.getIdentityGroupDetails("testgroup1", token)).thenReturn(oidcGroup);
			Response groupResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"testgroup1\"}", token)).thenReturn(groupResponse);
			when(ControllerUtil.configureLDAPGroup(eq("testgroup1"), any(), eq(token))).thenReturn(ldapConfigureResponse);

			// Mock approle response and config approle
			Response approleResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}", token)).thenReturn(approleResponse);
			when(appRoleService.configureApprole(eq("approle1"), any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, ""));

			// System under test
			String expectedResponse = "{\"errors\":[\"Failed to offboard Azure service account from TVault\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);

			String iamMetaDataStr = "{ \"data\": {\"userName\": \"svc_vault_test5\", \"awsAccountId\": \"1234567890\", \"awsAccountName\": \"testaccount1\", \"createdAtEpoch\": 1609754282000, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"accessKeyId\":\"testaccesskey\", \"expiryDuration\":12345}]}, \"path\": \"iamsvcacc/1234567890_svc_vault_test5\"}";
			when(reqProcessor.process(eq("/read"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					iamMetaDataStr));

			when(reqProcessor.process(eq("/delete"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.MULTI_STATUS, true,
					""));

			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.offboardAzureServiceAccount(token,
					serviceAccount, userDetails);
			assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		
		@Test
		public void testoffboardAzureServiceAccountOIDC_failed_to_delete_folder() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
			AzureServiceAccountOffboardRequest serviceAccount = new AzureServiceAccountOffboardRequest("testaccount");
			String azureSvcAccName = serviceAccount.getAzureSvcAccName();

			// Mock approle permission check
			Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
			when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
			List<String> currentPolicies = new ArrayList<>();
			currentPolicies.add("azure_master_policy");
			try {
				when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// oidc mock
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<OIDCEntityResponse> oidcResponse = ResponseEntity.status(HttpStatus.OK).body(oidcEntityResponse);
			when(OIDCUtil.oidcFetchEntityDetails(any(), any(), any(), eq(true))).thenReturn(oidcResponse);

			// delete policy mock
			ResponseEntity<String> deletePolicyResponse = ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
			when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(deletePolicyResponse);

			// metadata mock
			when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"isActivated\":true,\"owner_ntid\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"},\"groups\":{\"testgroup1\":\"read\"},\"app-roles\":{\"approle1\":\"read\"}}}"));

			// Mock user response and config user
			Response userResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
			when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"normaluser\"}", token)).thenReturn(userResponse);

			try {
				List<String> resList = new ArrayList<>();
				resList.add("default");
				when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
			} catch (IOException e) {
				e.printStackTrace();
			}
			when(ControllerUtil.configureLDAPUser(eq("normaluser"), any(), any(), eq(token)))
					.thenReturn(ldapConfigureResponse);

			// Mock group response and config group
			List<String> currentpolicies = new ArrayList<>();
			currentpolicies.add("default");
			currentpolicies.add("w_shared_mysafe01");
			currentpolicies.add("w_shared_mysafe02");
			OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
			when(OIDCUtil.getIdentityGroupDetails("testgroup1", token)).thenReturn(oidcGroup);
			Response groupResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"testgroup1\"}", token)).thenReturn(groupResponse);
			when(ControllerUtil.configureLDAPGroup(eq("testgroup1"), any(), eq(token))).thenReturn(ldapConfigureResponse);

			// Mock approle response and config approle
			Response approleResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}", token)).thenReturn(approleResponse);
			when(appRoleService.configureApprole(eq("approle1"), any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, ""));

			// System under test
			String expectedResponse = "{\"errors\":[\"Failed to offboard Azure service account from TVault\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);

			String iamMetaDataStr = "{ \"data\": {\"userName\": \"svc_vault_test5\", \"awsAccountId\": \"1234567890\", \"awsAccountName\": \"testaccount1\", \"createdAtEpoch\": 1609754282000, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"accessKeyId\":\"testaccesskey\", \"expiryDuration\":12345}]}, \"path\": \"iamsvcacc/1234567890_svc_vault_test5\"}";
			when(reqProcessor.process(eq("/read"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					iamMetaDataStr));

			when(reqProcessor.process(eq("/delete"), Mockito.any(), eq(token))).thenAnswer(new Answer() {
				private int count = 0;

				public Object answer(InvocationOnMock invocation) {
					if (count++ == 2)
						return getMockResponse(HttpStatus.MULTI_STATUS, true,"");

					return getMockResponse(HttpStatus.NO_CONTENT, true,"");
				}

			});

			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.offboardAzureServiceAccount(token,
					serviceAccount, userDetails);
			assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		
		
		@Test
		public void testoffboardAzureServiceAccountOIDC_failed_to_delete_metadata() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
			AzureServiceAccountOffboardRequest serviceAccount = new AzureServiceAccountOffboardRequest("testaccount");
			String azureSvcAccName = serviceAccount.getAzureSvcAccName();

			// Mock approle permission check
			Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"azure_master_policy \"]}");
			when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
			List<String> currentPolicies = new ArrayList<>();
			currentPolicies.add("azure_master_policy");
			try {
				when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// oidc mock
			OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
			oidcEntityResponse.setEntityName("entity");
			List<String> policies = new ArrayList<>();
			policies.add("safeadmin");
			oidcEntityResponse.setPolicies(policies);
			ResponseEntity<OIDCEntityResponse> oidcResponse = ResponseEntity.status(HttpStatus.OK).body(oidcEntityResponse);
			when(OIDCUtil.oidcFetchEntityDetails(any(), any(), any(), eq(true))).thenReturn(oidcResponse);

			// delete policy mock
			ResponseEntity<String> deletePolicyResponse = ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"Successfully created policies for Azure service account\"]}");
			when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(deletePolicyResponse);

			// metadata mock
			when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"isActivated\":true,\"owner_ntid\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"},\"groups\":{\"testgroup1\":\"read\"},\"app-roles\":{\"approle1\":\"read\"}}}"));

			// Mock user response and config user
			Response userResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
			when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"normaluser\"}", token)).thenReturn(userResponse);

			try {
				List<String> resList = new ArrayList<>();
				resList.add("default");
				when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
			} catch (IOException e) {
				e.printStackTrace();
			}
			when(ControllerUtil.configureLDAPUser(eq("normaluser"), any(), any(), eq(token)))
					.thenReturn(ldapConfigureResponse);

			// Mock group response and config group
			List<String> currentpolicies = new ArrayList<>();
			currentpolicies.add("default");
			currentpolicies.add("w_shared_mysafe01");
			currentpolicies.add("w_shared_mysafe02");
			OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
			when(OIDCUtil.getIdentityGroupDetails("testgroup1", token)).thenReturn(oidcGroup);
			Response groupResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"testgroup1\"}", token)).thenReturn(groupResponse);
			when(ControllerUtil.configureLDAPGroup(eq("testgroup1"), any(), eq(token))).thenReturn(ldapConfigureResponse);

			// Mock approle response and config approle
			Response approleResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
			when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"approle1\"}", token)).thenReturn(approleResponse);
			when(appRoleService.configureApprole(eq("approle1"), any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, ""));

			// System under test
			String expectedResponse = "{\"errors\":[\"Failed to offboard Azure service account from TVault\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);

			String iamMetaDataStr = "{ \"data\": {\"userName\": \"svc_vault_test5\", \"awsAccountId\": \"1234567890\", \"awsAccountName\": \"testaccount1\", \"createdAtEpoch\": 1609754282000, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"accessKeyId\":\"testaccesskey\", \"expiryDuration\":12345}]}, \"path\": \"iamsvcacc/1234567890_svc_vault_test5\"}";
			when(reqProcessor.process(eq("/read"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					iamMetaDataStr));

			when(reqProcessor.process(eq("/delete"), Mockito.any(), eq(token))).thenAnswer(new Answer() {
				private int count = 0;

				public Object answer(InvocationOnMock invocation) {
					if (count++ == 1)
						return getMockResponse(HttpStatus.MULTI_STATUS, true,"");

					return getMockResponse(HttpStatus.NO_CONTENT, true,"");
				}
			});

			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.offboardAzureServiceAccount(token,
					serviceAccount, userDetails);
			assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		
		
		@Test
		public void testRemoveUserFromAzureSvcAccLdapSuccess() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			AzureServiceAccountUser iamSvcAccUser = new AzureServiceAccountUser("testaccount", "testuser1", "read");
			Response userResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_azuresvcacc_testaccount\"],\"ttl\":0,\"groups\":\"admin\"}}");
			Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
			when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"testuser1\"}", token)).thenReturn(userResponse);
			try {
				List<String> resList = new ArrayList<>();
				resList.add("default");
				resList.add("o_azuresvcacc_testaccount");
				when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
			} catch (IOException e) {
				e.printStackTrace();
			}
			when(ControllerUtil.configureLDAPUser(eq("testuser1"), any(), any(), eq(token))).thenReturn(responseNoContent);
			when(ControllerUtil.updateMetadata(any(), any())).thenReturn(responseNoContent);
			// System under test
			String expectedResponse = "{\"messages\":[\"Successfully removed user from the Azure Service Account\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
			String[] latestPolicies = { "o_azuresvcacc_testaccount" };
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "ldap");
			when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails))
					.thenReturn(latestPolicies);
			when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.removeUserFromAzureServiceAccount(token,
					iamSvcAccUser, userDetails);
			assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		
		@Test
		public void testRemoveUserFromIAMSvcAccOidcSuccess() {
			userDetails = getMockUser(true);
			token = userDetails.getClientToken();
			AzureServiceAccountUser iamSvcAccUser = new AzureServiceAccountUser("testaccount", "testuser1", "read");
			Response userResponse = getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_azuresvcacc_testaccount\"],\"ttl\":0,\"groups\":\"admin\"}}");
			Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
			when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"testuser1\"}", token)).thenReturn(userResponse);
			try {
				List<String> resList = new ArrayList<>();
				resList.add("default");
				resList.add("o_azuresvcacc_testaccount");
				when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
			} catch (IOException e) {
				e.printStackTrace();
			}
			when(ControllerUtil.configureLDAPUser(eq("testuser1"), any(), any(), eq(token))).thenReturn(responseNoContent);
			when(ControllerUtil.updateMetadata(any(), any())).thenReturn(responseNoContent);
			// System under test
			String expectedResponse = "{\"messages\":[\"Successfully removed user from the Azure Service Account\"]}";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
			String[] latestPolicies = { "o_azuresvcacc_testaccount" };
			when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails))
					.thenReturn(latestPolicies);
			when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
					"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));
			// oidc test cases
			ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
			String mountAccessor = "auth_oidc";
			DirectoryUser directoryUser = new DirectoryUser();
			directoryUser.setDisplayName("testUser1");
			directoryUser.setGivenName("testUser");
			directoryUser.setUserEmail("testUser@t-mobile.com");
			directoryUser.setUserId("testuser1");
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

			Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true,
					"{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
			when(OIDCUtil.updateOIDCEntity(any(), any())).thenReturn(responseEntity3);
			when(OIDCUtil.oidcFetchEntityDetails(any(), any(), any(), eq(true))).thenReturn(responseEntity2);
			when(tokenUtils.getSelfServiceToken()).thenReturn(token);
			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.removeUserFromAzureServiceAccount(token,
					iamSvcAccUser, userDetails);
			assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
			assertEquals(responseEntityExpected, responseEntity);
		}
		@Test
		public void test_getOnboardedAzureServiceAccounts_successfully() throws IOException {
			String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
			UserDetails userDetails=new UserDetails();
			String[] latestPolicies= {"o_azuresvcacctestpolicy"} ;
			when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(),
					userDetails.getUsername(), userDetails)).thenReturn(latestPolicies);
			ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.getOnboardedAzureServiceAccounts(token, userDetails);
			assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		}
		@Test
	    public void test_addAwsRoleToAzureSvcacc_succssfully_iam() throws Exception {

	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with Azure Service Account\"]}");
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        UserDetails userDetails = getMockUser(false);
	        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

	        String [] policies = {"o_azuresvcacc_testsvcname"};
	        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
	        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
	                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
	                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
	                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
	        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
	        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
	        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
	        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
	        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
	        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

	        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
	        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
	        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.addAwsRoleToAzureSvcacc(userDetails, token, serviceAccountAWSRole);

	        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
	        assertEquals(responseEntityExpected, responseEntityActual);

	    }
		
		@Test
	    public void test_addAwsRoleToAzureSvcacc_succssfully_ec2() throws Exception {

	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with Azure Service Account\"]}");
	        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	        UserDetails userDetails = getMockUser(false);
	        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

	        String [] policies = {"o_azuresvcacc_testsvcname"};
	        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
	        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
	                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
	                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
	                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
	        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
	        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
	        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
	        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
	        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
	        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

	        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
	        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
	        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.addAwsRoleToAzureSvcacc(userDetails, token, serviceAccountAWSRole);

	        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
	        assertEquals(responseEntityExpected, responseEntityActual);

	    }
		
		 @Test
		    public void test_addAwsRoleToAzureSvcacc_ec2_metadata_failure() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        String [] policies = {"o_azuresvcacc_testsvcname"};
		        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
		                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
		                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
		                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
		        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
		        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
		        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
		        Response updateMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
		        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.addAwsRoleToAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }
		 
		    @Test
		    public void test_addAwsRoleToAzureSvcacc_iam_metadata_failure() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        String [] policies = {"o_azuresvcacc_testsvcname"};
		        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
		                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
		                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
		                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
		        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
		        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
		        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
		        Response updateMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
		        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.addAwsRoleToAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }
		    
		    
		    @Test
		    public void test_addAwsRoleToIAMSvcacc_failure() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed. Try Again\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        String [] policies = {"o_azuresvcacc_testsvcname"};
		        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
		                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
		                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
		                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
		        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
		        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
		        Response configureAWSRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
		        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.addAwsRoleToAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }
		    
		    @Test
		    public void test_addAwsRoleToIAMSvcacc_failure_403() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add AWS Role to this Azure service account\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.addAwsRoleToAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }

    @Test
	public void testAddGroupToAzureSvcAccSuccessfully() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "rotate");
		userDetails = getMockUser(false);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Group is successfully associated with Azure Service Principal\"]}");
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		String[] policies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "ldap");
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(responseNoContent);
		when(ControllerUtil.updateMetadata(any(), eq(token))).thenReturn(responseNoContent);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));
		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcACcOidcSuccessfully() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "read");
		userDetails = getMockUser(false);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Group is successfully associated with Azure Service Principal\"]}");
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		String[] policies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(responseNoContent);
		when(ControllerUtil.updateMetadata(any(), eq(token))).thenReturn(responseNoContent);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));

		ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
		List<String> policie = new ArrayList<>();
		policie.add("default");
		policie.add("w_shared_mysafe02");
		policie.add("r_shared_mysafe01");
		List<String> currentpolicies = new ArrayList<>();
		currentpolicies.add("default");
		currentpolicies.add("w_shared_mysafe01");
		currentpolicies.add("w_shared_mysafe02");
		OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
		currentpolicies.addAll(oidcGroup.getPolicies());
		when(OIDCUtil.getIdentityGroupDetails("mygroup01", token)).thenReturn(oidcGroup);

		Response response = new Response();
		response.setHttpstatus(HttpStatus.NO_CONTENT);
		when(OIDCUtil.updateGroupPolicies(any(), any(), any(), any(), any())).thenReturn(response);

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcAccMetadataFailure() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "rotate");
		userDetails = getMockUser(false);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

		String[] policies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(responseNoContent);
		when(ControllerUtil.updateMetadata(any(), eq(token))).thenReturn(response404);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcAccFailure() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "read");
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"errors\":[\"Group configuration failed.Try Again\"]}");
		Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(response404);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));
		String[] latestPolicies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails))
				.thenReturn(latestPolicies);

		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcAccFailure403() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "read");
		userDetails = getMockUser(false);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Access denied: No permission to add groups to this Azure service principal\"]}");
		Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

		String[] policies = { "w_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(response404);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));
		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcAccFailureInitialActivate() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "read");
		userDetails = getMockUser(false);
		token = userDetails.getClientToken();
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
				"{\"errors\":[\"Failed to add group permission to Azure service principal. Azure service principal is not activated. Please activate this account and try again.\"]}");
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":false,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));
		String[] latestPolicies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails))
				.thenReturn(latestPolicies);
		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void test_activateAzureServicePrincipal_successfull() throws IOException {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String path = "metadata/azuresvcacc/svc_vault_test5";
		String azureSecret = "abcdefgh";
		String secretKeyId = "12345678-1234-1234-1234-123456789098";
		String [] policies = {"o_azuresvcacc_svc_vault_test5"};
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		String azureMetaDataStr = getAzureMockMetadata(false);
		String azureMetaDataStrActivated = getAzureMockMetadata(true);

		Response metaResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStr);
		Response metaActivatedResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStrActivated);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);

		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenAnswer(new Answer() {
			private int count = 0;

			public Object answer(InvocationOnMock invocation) {
				if (count++ == 1)
					return metaActivatedResponse;

				return metaResponse;
			}
		});

		when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}", token)).thenReturn(getMockResponse(HttpStatus.OK, true,
				azureMetaDataStr));

		AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret, 604800000L, "Thu Jan 08 05:30:00 IST 1970", "8765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098");

		//when(azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.rotateAzureServicePrincipalSecret(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.writeAzureSPSecret(token, "azuresvcacc/svc_vault_test5/secret_1", servicePrincipal, azureServiceAccountSecret)).thenReturn(true);
		when(azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(eq(token), eq(servicePrincipal), eq(secretKeyId), Mockito.any())).thenReturn(responseNoContent);
		when(azureServiceAccountUtils.updateActivatedStatusInMetadata(token, servicePrincipal)).thenReturn(responseNoContent);


		// Add User to Azure service principal
		Response userResponse = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
		Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
		when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"testuser1\"}", token)).thenReturn(userResponse);

		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPUser(eq("testuser1"), any(), any(), eq(token)))
				.thenReturn(ldapConfigureResponse);
		when(ControllerUtil.updateMetadata(any(), any())).thenReturn(responseNoContent);

		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Azure Service Principal activated successfully\"]}");
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.activateAzureServicePrincipal(token, userDetails, servicePrincipal);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void test_activateAzureServicePrincipal_failed_403() {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String [] policies = {"defaullt"};
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);

		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: No permission to activate this Azure Service Principal\"]}");
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.activateAzureServicePrincipal(token, userDetails, servicePrincipal);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void test_activateAzureServicePrincipal_failure_already_activated() {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String [] policies = {"o_azuresvcacc_svc_vault_test5"};
		String azureMetaDataStrActivated = getAzureMockMetadata(true);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		Response metaActivatedResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStrActivated);
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(metaActivatedResponse);

		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Azure Service Principal is already activated. You can now grant permissions from Permissions menu\"]}");
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.activateAzureServicePrincipal(token, userDetails, servicePrincipal);
		assertEquals(expectedResponse, actualResponse);
	}
		
		    @Test
		    public void test_removeAwsRoleToAzureSvcacc_succssfully_iam() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully removed from Azure Service Account\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        String [] policies = {"o_azuresvcacc_testsvcname"};
		        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
		                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
		                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
		                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
		        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
		        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
		        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
		        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
		        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.removeAwsRoleFromAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }
	 
		    @Test
		    public void test_removeAwsRoleToAzureSvcacc_succssfully_ec2() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully removed from Azure Service Account\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        String [] policies = {"o_azuresvcacc_testsvcname"};
		        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
		                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
		                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
		                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
		        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
		        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
		        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
		        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
		        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.removeAwsRoleFromAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }
			
		    @Test
		    public void test_removeAwsRoleToAzureSvcacc_ec2_metadata_failure() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        String [] policies = {"o_azuresvcacc_testsvcname"};
		        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
		                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
		                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
		                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
		        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
		        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
		        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
		        Response updateMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
		        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.removeAwsRoleFromAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }

		    @Test
		    public void test_removeAwsRoleToAzureSvcacc_iam_metadata_failure() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        String [] policies = {"o_azuresvcacc_testsvcname"};
		        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
		                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
		                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
		                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
		        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
		        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
		        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
		        Response updateMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
		        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.removeAwsRoleFromAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }
		    @Test
		    public void test_removeAwsRoleToIAMSvcacc_failure() throws Exception {

		        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed. Try Again\"]}");
		        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		        UserDetails userDetails = getMockUser(false);
		        AzureServiceAccountAWSRole serviceAccountAWSRole = new AzureServiceAccountAWSRole("testsvcname", "role1", "read");

		        String [] policies = {"o_azuresvcacc_testsvcname"};
		        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
		                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
		                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
		                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
		        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
		        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
		        Response configureAWSRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
		        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
		        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
		        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.removeAwsRoleFromAzureSvcacc(userDetails, token, serviceAccountAWSRole);

		        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
		        assertEquals(responseEntityExpected, responseEntityActual);

		    }

	@Test
	public void test_activateAzureServicePrincipal_failed_owner_association() throws IOException {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String path = "metadata/azuresvcacc/svc_vault_test5";
		String [] policies = {"o_azuresvcacc_svc_vault_test5"};
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		String azureMetaDataStr = "{ \"data\": {" +
				"  \"application_id\": \"tvt\"," +
				"  \"application_name\": \"tvt\"," +
				"  \"application_tag\": \"tvt\"," +
				"  \"createdAtEpoch\": 1601894197," +
				"  \"isActivated\": false," +
				"  \"owner_email\": \"abc@company.com\"," +
				"  \"secret\": [" +
				"    {" +
				"      \"expiryDuration\": 604800000," +
				"      \"secretKeyId\": \"12345678-1234-1234-1234-123456789098\"" +
				"    }], \"servicePrincipalClientId\": \"34521345-1234-1234-1234-123456789098\", " +
				"\"servicePrincipalId\": \"98765432-1234-1234-1234-123456789098\", \"servicePrincipalName\": " +
				"\"svc_vault_test5\",  \"tenantId\": \"abcd1234-1234-1234-1234-123456789098\"}}";

		String azureMetaDataStrActivated = "{ \"data\": {" +
				"  \"application_id\": \"tvt\"," +
				"  \"application_name\": \"tvt\"," +
				"  \"application_tag\": \"tvt\"," +
				"  \"createdAtEpoch\": 1601894197," +
				"  \"isActivated\": true," +
				"  \"owner_email\": \"abc@company.com\"," +
				"  \"secret\": [" +
				"    {" +
				"      \"expiryDuration\": 604800000," +
				"      \"secretKeyId\": \"12345678-1234-1234-1234-123456789098\"" +
				"    }], \"servicePrincipalClientId\": \"34521345-1234-1234-1234-123456789098\", " +
				"\"servicePrincipalId\": \"98765432-1234-1234-1234-123456789098\", \"servicePrincipalName\": " +
				"\"svc_vault_test5\",  \"tenantId\": \"abcd1234-1234-1234-1234-123456789098\"}}";
		Response metaResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStr);
		Response metaActivatedResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStrActivated);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);

		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenAnswer(new Answer() {
			private int count = 0;

			public Object answer(InvocationOnMock invocation) {
				if (count++ == 1)
					return metaActivatedResponse;

				return metaResponse;
			}
		});

		when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}", token)).thenReturn(getMockResponse(HttpStatus.OK, true,
				azureMetaDataStr));

		String azureSecret = "abcdefgh";
		String secretKeyId = "12345678-1234-1234-1234-123456789098";
		AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret, 604800000L, "Thu Jan 08 05:30:00 IST 1970", "8765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098");

		//when(azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.rotateAzureServicePrincipalSecret(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.writeAzureSPSecret(token, "azuresvcacc/svc_vault_test5/secret_1", servicePrincipal, azureServiceAccountSecret)).thenReturn(true);
		when(azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(eq(token), eq(servicePrincipal), eq(secretKeyId), Mockito.any())).thenReturn(responseNoContent);
		when(azureServiceAccountUtils.updateActivatedStatusInMetadata(token, servicePrincipal)).thenReturn(responseNoContent);

		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"Failed to activate Azure Service Principal. Azure secrets are rotated and saved in T-Vault. However failed to add permission to owner. Owner info not found in Metadata.\"]}");
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.activateAzureServicePrincipal(token, userDetails, servicePrincipal);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void test_activateAzureServicePrincipal_failed_add_user() throws IOException {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String path = "metadata/azuresvcacc/svc_vault_test5";
		String [] policies = {"o_azuresvcacc_svc_vault_test5"};
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		String azureMetaDataStr = getAzureMockMetadata(false);
		String azureMetaDataStrActivated = getAzureMockMetadata(false);

		Response metaResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStr);
		Response metaActivatedResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStrActivated);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);

		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenAnswer(new Answer() {
			private int count = 0;

			public Object answer(InvocationOnMock invocation) {
				if (count++ == 1)
					return metaActivatedResponse;

				return metaResponse;
			}
		});

		when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}", token)).thenReturn(getMockResponse(HttpStatus.OK, true,
				azureMetaDataStr));

		String azureSecret = "abcdefgh";
		String secretKeyId = "12345678-1234-1234-1234-123456789098";
		AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret, 604800000L, "Thu Jan 08 05:30:00 IST 1970", "8765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098");

		//when(azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.rotateAzureServicePrincipalSecret(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.writeAzureSPSecret(token, "azuresvcacc/svc_vault_test5/secret_1", servicePrincipal, azureServiceAccountSecret)).thenReturn(true);
		when(azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(eq(token), eq(servicePrincipal), eq(secretKeyId), Mockito.any())).thenReturn(responseNoContent);
		when(azureServiceAccountUtils.updateActivatedStatusInMetadata(token, servicePrincipal)).thenReturn(responseNoContent);


		// Add User to Service Account
		Response userResponse = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
		Response ldapConfigureResponse = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"policies\":null}");
		when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"normaluser\"}", token)).thenReturn(userResponse);

		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPUser(eq("normaluser"), any(), any(), eq(token)))
				.thenReturn(ldapConfigureResponse);

		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate Azure Service Principal. Azure secrets are rotated and saved in T-Vault. However owner permission update failed.\"]}");
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.activateAzureServicePrincipal(token, userDetails, servicePrincipal);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void test_activateAzureServicePrincipal_failed_to_save_secret() throws IOException {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String path = "metadata/azuresvcacc/svc_vault_test5";
		String [] policies = {"o_azuresvcacc_svc_vault_test5"};
		String azureMetaDataStr = getAzureMockMetadata(false);
		String azureMetaDataStrActivated = getAzureMockMetadata(false);
		Response metaResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStr);
		Response metaActivatedResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStrActivated);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);

		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenAnswer(new Answer() {
			private int count = 0;

			public Object answer(InvocationOnMock invocation) {
				if (count++ == 1)
					return metaActivatedResponse;

				return metaResponse;
			}
		});

		when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}", token)).thenReturn(getMockResponse(HttpStatus.OK, true,
				azureMetaDataStr));

		//when(azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(Mockito.any())).thenReturn(null);
		when(azureServiceAccountUtils.rotateAzureServicePrincipalSecret(Mockito.any())).thenReturn(null);


		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate Azure Service Principal. Failed to rotate secrets for one or more SecretKeyIds.\"]}");
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.activateAzureServicePrincipal(token, userDetails, servicePrincipal);
		assertEquals(expectedResponse, actualResponse);
	}

	private String getAzureMockMetadata(boolean isActivated) {
		return  "{ \"data\": {" +
				"  \"application_id\": \"tvt\"," +
				"  \"application_name\": \"tvt\"," +
				"  \"application_tag\": \"tvt\"," +
				"  \"createdAtEpoch\": 1601894197," +
				"  \"isActivated\": "+isActivated+"," +
				"  \"owner_email\": \"abc@company.com\"," +
				"  \"owner_ntid\": \"testuser1\"," +
				"  \"secret\": [" +
				"    {" +
				"      \"expiryDuration\": 604800000," +
				"      \"secretKeyId\": \"12345678-1234-1234-1234-123456789098\"" +
				"    }], \"servicePrincipalClientId\": \"34521345-1234-1234-1234-123456789098\", " +
				"\"servicePrincipalId\": \"98765432-1234-1234-1234-123456789098\", \"servicePrincipalName\": " +
				"\"svc_vault_test5\",  \"tenantId\": \"abcd1234-1234-1234-1234-123456789098\"}}";
	}

	@Test
	public void test_activateAzureServicePrincipal_failed_metadata_update() throws IOException {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String path = "metadata/azuresvcacc/svc_vault_test5";
		String [] policies = {"o_azuresvcacc_svc_vault_test5"};
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		String azureMetaDataStr = getAzureMockMetadata(false);
		String azureMetaDataStrActivated = getAzureMockMetadata(false);

		Response metaResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStr);
		Response metaActivatedResponse = getMockResponse(HttpStatus.OK, true, azureMetaDataStrActivated);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);

		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenAnswer(new Answer() {
			private int count = 0;

			public Object answer(InvocationOnMock invocation) {
				if (count++ == 1)
					return metaActivatedResponse;

				return metaResponse;
			}
		});

		when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}", token)).thenReturn(getMockResponse(HttpStatus.OK, true,
				azureMetaDataStr));

		String azureSecret = "abcdefgh";
		String secretKeyId = "12345678-1234-1234-1234-123456789098";
		AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret, 604800000L, "Thu Jan 08 05:30:00 IST 1970", "8765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098");

		//when(azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.rotateAzureServicePrincipalSecret(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.writeAzureSPSecret(token, "azuresvcacc/svc_vault_test5/secret_1", servicePrincipal, azureServiceAccountSecret)).thenReturn(true);
		when(azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(eq(token), eq(servicePrincipal), eq(secretKeyId), Mockito.any())).thenReturn(responseNoContent);
		when(azureServiceAccountUtils.updateActivatedStatusInMetadata(token, servicePrincipal)).thenReturn(getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, ""));

		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to activate Azure Service Principal. Azure secrets are rotated and saved in T-Vault. However metadata update failed.\"]}");
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.activateAzureServicePrincipal(token, userDetails, servicePrincipal);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void test_rotateSecret_successfull() throws IOException {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String path = "metadata/azuresvcacc/svc_vault_test5";
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		String azureMetaDataStr = getAzureMockMetadata(false);

		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"w_azuresvcacc_svc_vault_test5 \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();
		currentPolicies.add("w_azuresvcacc_svc_vault_test5");
		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			when(policyUtils.getIdentityPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(new ArrayList<>());
		} catch (IOException e) {
			e.printStackTrace();
		}

		when(tokenUtils.getSelfServiceToken()).thenReturn(token);

		when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}", token)).thenReturn(getMockResponse(HttpStatus.OK, true,
				azureMetaDataStr));

		String azureSecret = "abcdefgh";
		String secretKeyId = "12345678-1234-1234-1234-123456789098";
		Long expiryDurationMs = 63738393L;
		AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret, 604800000L, "Thu Jan 08 05:30:00 IST 1970", "8765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098");

		//when(azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.rotateAzureServicePrincipalSecret(Mockito.any())).thenReturn(azureServiceAccountSecret);
		when(azureServiceAccountUtils.writeAzureSPSecret(token, "azuresvcacc/svc_vault_test5/secret_1", servicePrincipal, azureServiceAccountSecret)).thenReturn(true);
		when(azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(eq(token), eq(servicePrincipal), eq(secretKeyId), Mockito.any())).thenReturn(responseNoContent);
		when(azureServiceAccountUtils.updateActivatedStatusInMetadata(token, servicePrincipal)).thenReturn(responseNoContent);

		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Azure Service Principal secret rotated successfully\"]}");
		AzureServicePrincipalRotateRequest azureServicePrincipalRotateRequest = new AzureServicePrincipalRotateRequest(servicePrincipal, secretKeyId, "98765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098", expiryDurationMs);
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.rotateSecret(token, azureServicePrincipalRotateRequest);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void test_rotateSecret_failed_403() {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";

		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"w_azuresvcacc_svc_vault_test1 \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();
		currentPolicies.add("w_azuresvcacc_svc_vault_test1");
		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			when(policyUtils.getIdentityPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(new ArrayList<>());
		} catch (IOException e) {
			e.printStackTrace();
		}

		String secretKeyId = "12345678-1234-1234-1234-123456789098";
		Long expiryDurationMs = 63738393L;
		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: No permission to rotate secret for this Azure Service Principal.\"]}");
		AzureServicePrincipalRotateRequest azureServicePrincipalRotateRequest = new AzureServicePrincipalRotateRequest(servicePrincipal, secretKeyId, "98765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098", expiryDurationMs);
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.rotateSecret(token, azureServicePrincipalRotateRequest);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void test_rotateIAMServiceAccount_faile_to_rotate_secret() throws IOException {

		String servicePrincipal = "svc_vault_test5";
		String token = "123123123123";
		String path = "metadata/azuresvcacc/svc_vault_test5";
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		String azureMetaDataStr = getAzureMockMetadata(false);

		// Mock approle permission check
		Response lookupResponse = getMockResponse(HttpStatus.OK, true, "{\"policies\":[\"w_iamsvcacc_1234567890_svc_vault_test5 \"]}");
		when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(lookupResponse);
		List<String> currentPolicies = new ArrayList<>();
		currentPolicies.add("w_azuresvcacc_svc_vault_test5");
		try {
			when(azureServiceAccountUtils.getTokenPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(currentPolicies);
			when(policyUtils.getIdentityPoliciesAsListFromTokenLookupJson(Mockito.any(),Mockito.any())).thenReturn(new ArrayList<>());
		} catch (IOException e) {
			e.printStackTrace();
		}

		when(tokenUtils.getSelfServiceToken()).thenReturn(token);

		when(reqProcessor.process("/read", "{\"path\":\""+path+"\"}", token)).thenReturn(getMockResponse(HttpStatus.OK, true,
				azureMetaDataStr));

		String azureSecret = "abcdefgh";
		String secretKeyId = "12345678-1234-1234-1234-123456789098";
		Long expiryDurationMs = 63738393L;
		AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret(secretKeyId, azureSecret, 604800000L, "Thu Jan 08 05:30:00 IST 1970", "8765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098");

		//when(azureServiceAccountUtils.rotateAzureServicePrincipalSecretMOCK(Mockito.any())).thenReturn(null);
		when(azureServiceAccountUtils.rotateAzureServicePrincipalSecret(Mockito.any())).thenReturn(null);
		when(azureServiceAccountUtils.writeAzureSPSecret(token, "azuresvcacc/svc_vault_test5/secret_1", servicePrincipal, azureServiceAccountSecret)).thenReturn(true);
		when(azureServiceAccountUtils.updateAzureSPSecretKeyInfoInMetadata(eq(token), eq(servicePrincipal), eq(secretKeyId), Mockito.any())).thenReturn(responseNoContent);
		when(azureServiceAccountUtils.updateActivatedStatusInMetadata(token, servicePrincipal)).thenReturn(responseNoContent);

		ResponseEntity<String> expectedResponse =  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to rotate secret for Azure Service Principal\"]}");
		AzureServicePrincipalRotateRequest azureServicePrincipalRotateRequest = new AzureServicePrincipalRotateRequest(servicePrincipal, secretKeyId, "98765432-1234-1234-1234-123456789098", "abcd1234-1234-1234-1234-123456789098", expiryDurationMs);
		ResponseEntity<String> actualResponse = azureServicePrincipalAccountsService.rotateSecret(token, azureServicePrincipalRotateRequest);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void testRemoveGroupFromAzureSvcAccSuccessfully() {
		token = userDetails.getClientToken();
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "rotate");
		userDetails = getMockUser(false);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Group is successfully removed from Azure Service Principal\"]}");
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		String[] policies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(responseNoContent);
		when(ControllerUtil.updateMetadata(any(), eq(token))).thenReturn(responseNoContent);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.removeGroupFromAzureServiceAccount(token, azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testRemoveGroupFromAzureSvcAccOidcSuccessfully() {
		userDetails = getMockUser(false);
		token = userDetails.getClientToken();
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "rotate");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Group is successfully removed from Azure Service Principal\"]}");
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

		String[] policies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(responseNoContent);
		when(ControllerUtil.updateMetadata(any(), eq(token))).thenReturn(responseNoContent);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));

		ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
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
		when(OIDCUtil.updateGroupPolicies(any(), any(), any(), any(), any())).thenReturn(response1);

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.removeGroupFromAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcAccFailureInvalidAccess() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "test");
		userDetails = getMockUser(false);
		token = userDetails.getClientToken();
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
				"{\"errors\":[\"Invalid value specified for access. Valid values are read, rotate, deny\"]}");
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcAccFailureAuthTypeNotSupport() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "read");
		userDetails = getMockUser(false);
		token = userDetails.getClientToken();
		ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "userpass");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
				"{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcAccMetadataRevertFailure() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "rotate");
		userDetails = getMockUser(false);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"errors\":[\"Group configuration failed. Contact Admin \"]}");
		Response responseNoContent = getMockResponse(HttpStatus.OK, true, "");
		Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

		String[] policies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(responseNoContent);
		when(ControllerUtil.updateMetadata(any(), eq(token))).thenReturn(response404);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void testAddGroupToAzureSvcACcOidcFailed() {
		AzureServiceAccountGroup azureSvcAccGroup = new AzureServiceAccountGroup("testaccount", "group1", "read");
		userDetails = getMockUser(false);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"errors\":[\"Group configuration failed. Contact Admin \"]}");
		Response responseNoContent = getMockResponse(HttpStatus.FORBIDDEN, true, "");

		String[] policies = { "o_azuresvcacc_testaccount" };
		when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
		Response groupResp = getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
		when(reqProcessor.process("/auth/ldap/groups", "{\"groupname\":\"group1\"}", token)).thenReturn(groupResp);
		ObjectMapper objMapper = new ObjectMapper();
		String responseJson = groupResp.getResponse();
		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			resList.add("w_shared_mysafe01");
			resList.add("w_shared_mysafe02");
			when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPGroup(any(), any(), any())).thenReturn(responseNoContent);
		when(ControllerUtil.updateMetadata(any(), eq(token))).thenReturn(responseNoContent);
		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));

		ReflectionTestUtils.setField(azureServicePrincipalAccountsService, "vaultAuthMethod", "oidc");
		List<String> policie = new ArrayList<>();
		policie.add("default");
		policie.add("w_shared_mysafe02");
		policie.add("r_shared_mysafe01");
		List<String> currentpolicies = new ArrayList<>();
		currentpolicies.add("default");
		currentpolicies.add("w_shared_mysafe01");
		currentpolicies.add("w_shared_mysafe02");
		OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
		when(OIDCUtil.getIdentityGroupDetails("mygroup01", token)).thenReturn(oidcGroup);

		Response response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		when(OIDCUtil.updateGroupPolicies(any(), any(), any(), any(), any())).thenReturn(response);

		ResponseEntity<String> responseEntity = azureServicePrincipalAccountsService.addGroupToAzureServiceAccount(token,
				azureSvcAccGroup, userDetails);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

    @Test
    public void testAssociateAppRoleToAzureSvcAccSuccssfully() {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Azure Service Principal\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole azureServiceAccountApprole = new AzureServiceAccountApprole("svc_vault_test2", "role1", "rotate");

        String [] policies = {"o_azuresvcacc_svc_vault_test2"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_azuresvcacc_testsvcname\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.associateApproletoAzureServiceAccount(userDetails, token, azureServiceAccountApprole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToAzureAccFailedInvalidAccess() {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, rotate, deny\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole azureServiceAccountApprole = new AzureServiceAccountApprole("svc_vault_test2", "role1", "write");
        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.associateApproletoAzureServiceAccount(userDetails, token, azureServiceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testAssociateAppRoleToAzureAccFailedInvalidAppRole() {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any Azure Service Principal\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole azureServiceAccountApprole = new AzureServiceAccountApprole("svc_vault_test2", "azure_master_approle", "read");
        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.associateApproletoAzureServiceAccount(userDetails, token, azureServiceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testAssociateAppRoleToAzureAccFailedNoPermission() {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add Approle to this azure service principal\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole azureServiceAccountApprole = new AzureServiceAccountApprole("svc_vault_test2", "role1", "rotate");

        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(null);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_azuresvcacc_testsvcname\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.associateApproletoAzureServiceAccount(userDetails, token, azureServiceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testAssociateAppRoleToAzureAccFailedNoAppRole() {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either Approle doesn't exists or you don't have enough permission to add this approle to Azure Service Principal\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole azureServiceAccountApprole = new AzureServiceAccountApprole("svc_vault_test2", "role1", "rotate");
        String [] policies = {"o_azuresvcacc_svc_vault_test2"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.FORBIDDEN, true, "{\"data\": {\"policies\":\"w_azuresvcacc_testsvcname\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.associateApproletoAzureServiceAccount(userDetails, token, azureServiceAccountApprole);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testAssociateAppRoleToAzureSvcAccConfigureApproleFailed() {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add Approle to the Azure Service Principal\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole azureServiceAccountApprole = new AzureServiceAccountApprole("svc_vault_test2", "role1", "rotate");

        String [] policies = {"o_azuresvcacc_svc_vault_test2"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_azuresvcacc_testsvcname\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.FORBIDDEN, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.associateApproletoAzureServiceAccount(userDetails, token, azureServiceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToAzureSvcAccMetaDataUpdateFailed() {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole azureServiceAccountApprole = new AzureServiceAccountApprole("svc_vault_test2", "role1", "rotate");

        String [] policies = {"o_azuresvcacc_svc_vault_test2"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_azuresvcacc_testsvcname\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.FORBIDDEN, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.associateApproletoAzureServiceAccount(userDetails, token, azureServiceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToAzureSvcAccMetaDataRevertFailed() {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole azureServiceAccountApprole = new AzureServiceAccountApprole("svc_vault_test2", "role1", "rotate");

        String [] policies = {"o_azuresvcacc_svc_vault_test2"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_azuresvcacc_testsvcname\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.FORBIDDEN, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));
        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.associateApproletoAzureServiceAccount(userDetails, token, azureServiceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
    
    @Test
    public void test_removeApproleFromAzureSvcAcc_succssfully() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle is successfully removed(if existed) from Azure Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AzureServiceAccountApprole serviceAccountApprole = new AzureServiceAccountApprole("testsvcname", "role1", "read");

        String [] policies = {"o_azuresvcacc_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(reqProcessor.process(eq("/sdb"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"data\":{\"initialPasswordReset\":true,\"managedBy\":\"smohan11\",\"name\":\"svc_vault_test5\",\"users\":{\"smohan11\":\"sudo\"}}}"));

        ResponseEntity<String> responseEntityActual =  azureServicePrincipalAccountsService.removeApproleFromAzureSvcAcc(userDetails, token, serviceAccountApprole);

        
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
}
