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
import com.tmobile.cso.vault.api.model.AzureServiceAccountMetadataDetails;
import com.tmobile.cso.vault.api.model.AzureSvccAccMetadata;
import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.model.DirectoryObjectsList;
import com.tmobile.cso.vault.api.model.DirectoryUser;
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
public class AzureServicePrinicipalAccountsServiceTest {
	
	@InjectMocks
	AzureServicePrinicipalAccountsService azureServicePrinicipalAccountsService;
	
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
	
	@Before
    public void setUp()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(OIDCUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(OIDCUtil.class, "log", LogManager.getLogger(OIDCUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");
        ReflectionTestUtils.setField(azureServicePrinicipalAccountsService, "vaultAuthMethod", "ldap");
		ReflectionTestUtils.setField(azureServicePrinicipalAccountsService, "azureMasterPolicyName", "azure_master_policy");
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
		azureServiceAccount.setServicePrinicipalName(servicePrincipalName);
		azureServiceAccount.setServicePrinicipalClientId("a987b078-a5a7-55re-8975-8945c545b76d");
		azureServiceAccount.setServicePrinicipalId("a987b078-a5a7-55re-8975-8945c545b76d");
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
		azureServiceAccountMetadataDetails.setServicePrinicipalName(azureServiceAccount.getServicePrinicipalName());
		azureServiceAccountMetadataDetails.setServicePrinicipalId(azureServiceAccount.getServicePrinicipalId());
		azureServiceAccountMetadataDetails.setServicePrinicipalClientId(azureServiceAccount.getServicePrinicipalClientId());
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
		String azureSvcAccName = serviceAccount.getServicePrinicipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

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
		rqstParams.put("servicePrinicipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrinicipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrinicipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
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
		when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"testUser\"}", token)).thenReturn(userResponse);

		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPUser(eq("testUser"), any(), any(), eq(token)))
				.thenReturn(ldapConfigureResponse);
		when(ControllerUtil.updateMetadata(any(), any())).thenReturn(responseNoContent);

		// System under test
		String expectedResponse = "{\"messages\":[\"Successfully completed onboarding of Azure service account\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

		when(reqProcessor.process(eq("/sdb"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true,
				"{\"data\":{\"isActivated\":true,\"managedBy\":\"normaluser\",\"name\":\"svc_vault_test5\",\"users\":{\"normaluser\":\"sudo\"}}}"));

		DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUserfirstname,lastname");
        directoryUser.setGivenName("testUser");
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

		ReflectionTestUtils.setField(azureServicePrinicipalAccountsService, "supportEmail", "support@abc.com");
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

		ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.onboardAzureServiceAccount(token,
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

		ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	@Test
	public void testOnboardAzureServiceAccountAlreadyExists() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
		AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");
		
		String azureSvcAccName = serviceAccount.getServicePrinicipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

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
		rqstParams.put("servicePrinicipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrinicipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrinicipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
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

		ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	
	
	@Test
	public void testOnboardAzureServiceAccountMetaDataCreationFailed() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
	AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");
		
		String azureSvcAccName = serviceAccount.getServicePrinicipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

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
		rqstParams.put("servicePrinicipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrinicipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrinicipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
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

		ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	
	@Test
	public void testOnboardAzureServiceAccountPolicyCreationFailed() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
        AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");
		
		String azureSvcAccName = serviceAccount.getServicePrinicipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

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
		rqstParams.put("servicePrinicipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrinicipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrinicipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
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

		ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	
	@Test
	public void testOnboardAzureServiceAccountAddOwnerFailed() {
		userDetails = getMockUser(true);
		token = userDetails.getClientToken();
		AzureServiceAccount serviceAccount = generateAzureServiceAccount("svc_cce_usertestrr16");

		String azureSvcAccName = serviceAccount.getServicePrinicipalName();
		String azureSvccAccPath = AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH + azureSvcAccName;

		String metaDataStr = "{ \"data\": {}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String metadatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\":{}}";

		String iamMetaDataStr = "{ \"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\",\"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}, \"path\": \"azuresvcacc/svc_cce_usertestrr16\"}";
		String iamMetaDatajson = "{\"path\":\"azuresvcacc/svc_cce_usertestrr16\",\"data\": {\"servicePrinicipalName\": \"svc_cce_usertestrr16\", \"servicePrinicipalId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"tenantId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"servicePrinicipalClientId\": \"c865a078-a5a7-44d8-9e43-0764c545b76d\", \"createdAtEpoch\": 12345L, \"owner_ntid\": \"normaluser\", \"owner_email\": \"normaluser@testmail.com\", \"application_id\": \"app1\", \"application_name\": \"App1\", \"application_tag\": \"App1\", \"isActivated\": false, \"secret\":[{\"secretKeyId\":\"testaccesskey\", \"expiryDuration\":12345L}]}}";

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
		rqstParams.put("servicePrinicipalName", "svc_cce_usertestrr16");
		rqstParams.put("servicePrinicipalId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
		rqstParams.put("servicePrinicipalClientId", "c865a078-a5a7-44d8-9e43-0764c545b76d");
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
		when(reqProcessor.process("/auth/ldap/users", "{\"username\":\"testUser\"}", token)).thenReturn(userResponse);

		try {
			List<String> resList = new ArrayList<>();
			resList.add("default");
			when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		when(ControllerUtil.configureLDAPUser(eq("testUser"), any(), any(), eq(token)))
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

		ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.onboardAzureServiceAccount(token,
				serviceAccount, userDetails);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}
	
	
	 @Test
	    public void test_getAzureServicePrinicipalList_successfully() {
	        userDetails = getMockUser(false);
	        token = userDetails.getClientToken();
	        String [] policies = {"r_users_s1", "w_users_s2", "r_shared_s3", "w_shared_s4", "r_apps_s5", "w_apps_s6", "d_apps_s7", "w_svcacct_test", "r_azuresvcacc_svc_cce_usertestrr16"};
	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"shared\":[{\"s3\":\"read\"},{\"s4\":\"write\"}],\"users\":[{\"s1\":\"read\"},{\"s2\":\"write\"}],\"svcacct\":[{\"test\":\"read\"}],\"azuresvcacc\":[{\"svc_cce_usertestrr16\":\"read\"}],\"apps\":[{\"s5\":\"read\"},{\"s6\":\"write\"},{\"s7\":\"deny\"}]}");
	       
	        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
	        when(JSONUtil.getJSON(Mockito.any())).thenReturn("{\"shared\":[{\"s3\":\"read\"},{\"s4\":\"write\"}],\"users\":[{\"s1\":\"read\"},{\"s2\":\"write\"}],\"svcacct\":[{\"test\":\"read\"}],\"azuresvcacc\":[{\"svc_cce_usertestrr16\":\"read\"}],\"apps\":[{\"s5\":\"read\"},{\"s6\":\"write\"},{\"s7\":\"deny\"}]}");
	        ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.getAzureServicePrinicipalList(userDetails);
	        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	        assertEquals(responseEntityExpected, responseEntity);
	    }
	 
	 @Test
		public void test_readFolders_successfully() throws IOException {
			String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
			String path = "testiamsvcacc01";
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(
					"{\"folders\":[\"testiamsvcacc01_01\",\"testiamsvcacc01_02\"],\"path\":\"testiamsvcacc01\",\"servicePrinicipalName\":\"testiamsvcacc01\"}");

			when(reqProcessor.process(eq("/azure/list"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testiamsvcacc01_01\",\"testiamsvcacc01_02\"]}"));
			ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.readFolders(token, path);
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
			ResponseEntity<String> responseEntity = azureServicePrinicipalAccountsService.getAzureServiceAccountSecretKey(token, iamSvcaccName, folderName);
			assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		}



}
