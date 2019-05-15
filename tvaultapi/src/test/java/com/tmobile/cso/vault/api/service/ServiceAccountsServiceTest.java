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
package com.tmobile.cso.vault.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.*;

import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.TokenUtils;
import org.apache.commons.collections.CollectionUtils;
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
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;


@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class ServiceAccountsServiceTest {

    @InjectMocks
    ServiceAccountsService serviceAccountsService;

    @Mock
    AccessService accessService;

    @Mock
    AppRoleService appRoleService;

    @Mock
    AWSAuthService awsAuthService;

    @Mock
    AWSIAMAuthService awsiamAuthService;

    @Mock
    RequestProcessor reqProcessor;

    @Mock
    LdapTemplate ldapTemplate;

    @Mock
    PolicyUtils policyUtils;

    @Mock
    TokenUtils tokenUtils;


    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/ad");
        ReflectionTestUtils.setField(serviceAccountsService, "vaultAuthMethod", "ldap");
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

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

    private ADServiceAccount generateADServiceAccount(String  userid) {
        ADServiceAccount adServiceAccount = new ADServiceAccount();
        adServiceAccount.setDisplayName("testacc");
        adServiceAccount.setGivenName("testacc");
        adServiceAccount.setUserEmail("testacc01@t-mobile.com");
        adServiceAccount.setUserId(userid);
        adServiceAccount.setUserName("testaccr");
        adServiceAccount.setPurpose("This is a test user account");
        adServiceAccount.setAccountExpires("Never");
        adServiceAccount.setPwdLastSet("2019-05-14 07:09:32");
        adServiceAccount.setMaxPwdAge(31536000);
        adServiceAccount.setPasswordExpiry("2020-05-13 07:09:32 (358 days)");
        ADUserAccount adUserAccount = new ADUserAccount();
        adUserAccount.setUserName("user11");
        adServiceAccount.setManagedBy(adUserAccount);
        adServiceAccount.setAccountStatus("active");
        adServiceAccount.setLockStatus("unlocked");
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
    @Test
    public void test_getADServiceAccounts_excluded_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = true;
    	String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null))(!(CN=testacc02)))";
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));

        List<ADUserAccount> list = new ArrayList<>();
        ADUserAccount adUserAccount = new ADUserAccount();
        adUserAccount.setUserId("user.user11");
        adUserAccount.setUserName("user11");
        adUserAccount.setDisplayName("user user11");
        adUserAccount.setGivenName("user11");
        adUserAccount.setUserEmail("user11@abc.com");
        list.add(adUserAccount);

        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(list);
        ReflectionTestUtils.setField(serviceAccountsService, "adUserLdapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);


        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().getData().getValues().length);

    }


    @Test
    public void test_getADServiceAccounts_excluded_success_NoResults() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = true;
    	String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null))(!(CN=testacc02)))";
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(0, responseEntity.getBody().getData().getValues().length);

    }

    @Test
    public void test_getADServiceAccounts_excluded_success_NotFound_Onboarded() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = true;
    	String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null)))";
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        allServiceAccounts.add(generateADServiceAccount("testacc03"));
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));
        List<ADUserAccount> list = new ArrayList<>();
        ADUserAccount adUserAccount = new ADUserAccount();
        adUserAccount.setUserId("user.user11");
        adUserAccount.setUserName("user11");
        adUserAccount.setDisplayName("user user11");
        adUserAccount.setGivenName("user11");
        adUserAccount.setUserEmail("user11@abc.com");
        list.add(adUserAccount);
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(list);
        ReflectionTestUtils.setField(serviceAccountsService, "adUserLdapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "{\"keys\":[]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(3, responseEntity.getBody().getData().getValues().length);

    }

    @Test
    public void test_getADServiceAccounts_all_successfully_NoAccounts() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = false;
        List<ADServiceAccount> allServiceAccounts = null;
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));
        List<ADUserAccount> list = new ArrayList<>();
        ADUserAccount adUserAccount = new ADUserAccount();
        adUserAccount.setUserId("user.user11");
        adUserAccount.setUserName("user11");
        adUserAccount.setDisplayName("user user11");
        adUserAccount.setGivenName("user11");
        adUserAccount.setUserEmail("user11@abc.com");
        list.add(adUserAccount);
        String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null)))";
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(list);
        ReflectionTestUtils.setField(serviceAccountsService, "adUserLdapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(null);
        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(0, responseEntity.getBody().getData().getValues().length);

    }

    @Test
    public void test_getADServiceAccounts_all_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = false;
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));
        List<ADUserAccount> list = new ArrayList<>();
        ADUserAccount adUserAccount = new ADUserAccount();
        adUserAccount.setUserId("user.user11");
        adUserAccount.setUserName("user11");
        adUserAccount.setDisplayName("user user11");
        adUserAccount.setGivenName("user11");
        adUserAccount.setUserEmail("user11@abc.com");
        list.add(adUserAccount);
        String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null)))";
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(list);
        ReflectionTestUtils.setField(serviceAccountsService, "adUserLdapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected.getBody().getData().getValues()[0].toString(), responseEntity.getBody().getData().getValues()[0].toString());

    }

    public String getJSON(Object obj)  {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return TVaultConstants.EMPTY_JSON;
		}
	}
    private ServiceAccount generateServiceAccount(String svcAccName, String owner) {
    	ServiceAccount serviceAccount = new ServiceAccount();
    	serviceAccount.setName(svcAccName);
    	serviceAccount.setAutoRotate(true);
        serviceAccount.setTtl(89L);
        serviceAccount.setMax_ttl(90L);
    	serviceAccount.setOwner(owner);
    	return serviceAccount;
    }
    @Test
    public void test_onboardServiceAccount_succss_autorotate_off() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(false);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        // CreateRole
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(serviceAccount.getName());
        serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
        serviceAccountTTL.setTtl(serviceAccount.getTtl());
        String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);

        //create metadata
        ServiceAccountMetadataDetails serviceAccountMetadataDetails = new ServiceAccountMetadataDetails("testacc02");
        serviceAccountMetadataDetails.setManagedBy("testacc01");
        String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + "/testacc02";
        ServiceAccountMetadata serviceAccountMetadata =  new ServiceAccountMetadata(_path, serviceAccountMetadataDetails);
        when(JSONUtil.getJSON(serviceAccountMetadata)).thenReturn("{\"name\":\"testacc02\", \"managedBy\":\"testacc01\"}");
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("name", "testacc02");
        rqstParams.put("managedBy", "testacc01");
        when(ControllerUtil.parseJson(any())).thenReturn(rqstParams);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn("{\"name\":\"testacc02\", \"managedBy\":\"testacc01\", \"path\":"+_path+"}");
        when( ControllerUtil.createMetadata(any(), eq(token))).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.OK, true,"{}"));
        //CreateServiceAccountPolicies
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully created policies for service account\"]}");
        when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // Add User to Service Account
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user11\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("user11"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);



        // System under test
        String expectedResponse = "{\"messages\":[\"Successfully completed onboarding of AD service account into TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_failure_already_onboarded() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        serviceAccount.setTtl(89L);
        serviceAccount.setMax_ttl(90L);

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        String expectedResponse = "{\"errors\":[\"Failed to onboard Service Account. Service account is already onboarded\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(null);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_failure_to_read_ad_details() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        String expectedResponse = "{\"errors\":[\"Failed to onboard Service Account. Unable to read Service account details\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(null);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_succss_autorotate_on_ttl_biggerthan_maxttl() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);
        serviceAccount.setTtl(89L);
        serviceAccount.setMax_ttl(89L);
    	String expectedResponse = "{\"errors\":[\"Password TTL can't be more than MAX_TTL\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_failure_invalid_ttl() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = new ServiceAccount();
        serviceAccount.setName("testacc02");
        serviceAccount.setAutoRotate(true);
        serviceAccount.setOwner("testacc01");
        String expectedResponse = "{\"errors\":[\"Invalid or no value has been provided for TTL or MAX_TTL\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_succss_autorotate_on_ttl_biggerthan_maxallowed() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        serviceAccount.setTtl(1590897977L);
        serviceAccount.setMax_ttl(1590897977L);
        String expectedResponse = "{\"errors\":[\"Invalid value provided for TTL. TTL can't be more than 7775999 (89 days) for this Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        // CreateRole
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
		Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);

        //create metadata
        ServiceAccountMetadataDetails serviceAccountMetadataDetails = new ServiceAccountMetadataDetails("testacc02");
        serviceAccountMetadataDetails.setManagedBy("testacc01");
        String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + "/testacc02";
        ServiceAccountMetadata serviceAccountMetadata =  new ServiceAccountMetadata(_path, serviceAccountMetadataDetails);
        when(JSONUtil.getJSON(serviceAccountMetadata)).thenReturn("{\"name\":\"testacc02\", \"managedBy\":\"testacc01\"}");
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("name", "testacc02");
        rqstParams.put("managedBy", "testacc01");
        when(ControllerUtil.parseJson(any())).thenReturn(rqstParams);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn("{\"name\":\"testacc02\", \"managedBy\":\"testacc01\", \"path\":"+_path+"}");
        when( ControllerUtil.createMetadata(any(), eq(token))).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.OK, true,"{}"));
        //CreateServiceAccountPolicies
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully created policies for service account\"]}");
        when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // Add User to Service Account
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user11\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("user11"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);



        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully completed onboarding of AD service account into TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_onboardServiceAccount_metadata_failure() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        // CreateRole
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(serviceAccount.getName());
        serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
        serviceAccountTTL.setTtl(serviceAccount.getTtl());
        String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);

        //create metadata
        ServiceAccountMetadataDetails serviceAccountMetadataDetails = new ServiceAccountMetadataDetails("testacc02");
        serviceAccountMetadataDetails.setManagedBy("testacc01");
        String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + "/testacc02";
        ServiceAccountMetadata serviceAccountMetadata =  new ServiceAccountMetadata(_path, serviceAccountMetadataDetails);
        when(JSONUtil.getJSON(serviceAccountMetadata)).thenReturn("{\"name\":\"testacc02\", \"managedBy\":\"testacc01\"}");
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("name", "testacc02");
        rqstParams.put("managedBy", "testacc01");
        when(ControllerUtil.parseJson(any())).thenReturn(rqstParams);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn("{\"name\":\"testacc02\", \"managedBy\":\"testacc01\", \"path\":"+_path+"}");
        when( ControllerUtil.createMetadata(any(), eq(token))).thenReturn(false);

        // System under test
        String expectedResponse = "{\"errors\":[\"Successfully created Service Account Role. However creation of Metadata failed.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);
        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_onboardServiceAccount_BadRequest_for_AccountRole() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        // CreateRole
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = getJSON(serviceAccountTTL);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
		Response onboardResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"errors\":[\"Failed to create service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);

        // System under test
    	String expectedResponse = "{\"errors\":[\"Failed to onboard AD service account into TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_BadRequest_for_CreateServiceAccountPolicies() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        // CreateRole
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = getJSON(serviceAccountTTL);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
		Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);
        //CreateServiceAccountPolicies
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Unable to create Policy\"]}");
        when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);
        //Metadata
        String path="metadata/ad/roles/"+serviceAccount.getName();
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("path",path);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn(getJSON(rqstParams));
        when(ControllerUtil.createMetadata(Mockito.any(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.OK, true,"{}"));
        when(reqProcessor.process("/ad/serviceaccount/offboard", svc_account_payload, token)).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true,"{}"));
        // System under test
    	String expectedResponse = "{\"errors\":[\"Failed to onboard AD service account into TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_onboardServiceAccount_failed_owner_association() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        // CreateRole
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = getJSON(serviceAccountTTL);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
		Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);
        //CreateServiceAccountPolicies
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully created policies for service account\"]}");
        when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // Add User to Service Account
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user11\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Response ldapConfigureResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"errors\":[\"Failed to add user to the Service Account\"]}");
        when(ControllerUtil.configureLDAPUser(eq("user11"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // Metadata
        String path="metadata/ad/roles/"+serviceAccount.getName();
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("path",path);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn(getJSON(rqstParams));
        when(ControllerUtil.createMetadata(Mockito.any(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.OK, true,"{}"));

        // System under test
        String expectedResponse = "{\"messages\":[\"Successfully created Service Account Role and policies. However the association of owner information failed.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);
        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);
        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    private OnboardedServiceAccount generateOnboardedServiceAccount(String svcAccName, String owner) {
    	OnboardedServiceAccount onboardedServiceAccount = new OnboardedServiceAccount();
    	onboardedServiceAccount.setName(svcAccName);
    	onboardedServiceAccount.setOwner(owner);
    	return onboardedServiceAccount;
    }

    @Test
    public void test_offboardServiceAccount_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	OnboardedServiceAccount onboardedServiceAccount = generateOnboardedServiceAccount("testacc02","testacc01");

        ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc01", userDetails.getUsername(), "sudo");
        String [] policies = {"o_svcacct_testacc01"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user1\"}",token)).thenReturn(userResponse);
        Response userResponse2 = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"r_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user2\"}",token)).thenReturn(userResponse2);
        Response groupResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"policies\":[\"default\", \"r_svcacct_testacc02\"],\"ttl\":0}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResponse);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            List<String> groupResList = new ArrayList<>();
            resList.add("default");
            resList.add("r_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(groupResList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        // Delete policies...
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access is deleted\"]}");
        when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // delete user/group/role associations
        String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + "/testacc02";
        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\n" +
                "  \"groups\": {\n" +
                "    \"group1\": \"read\"\n" +
                "  },\n" +
                "  \"aws-roles\": {\n" +
                "    \"role1\": \"read\"\n" +
                "  },\n" +
                "  \"app-roles\": {\n" +
                "    \"role2\": \"read\"\n" +
                "  },\n" +
                "  \"managedBy\": \"user2\",\n" +
                "  \"name\": \"testacc02\",\n" +
                "  \"users\": {\n" +
                "    \"user1\": \"read\"\n" +
                "  }\n" +
                "}}");
        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(metaResponse);
        Response response_no_content = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process("/auth/aws/roles/delete","{\"role\":\"role1\"}",token)).thenReturn(response_no_content);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_svcacct_testacc02\"}}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role2\"}", token)).thenReturn(appRoleResponse);
        when(appRoleService.configureApprole(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(response_no_content);
        //Delete Account Role...
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(onboardedServiceAccount.getName());
		serviceAccountTTL.setService_account_name(onboardedServiceAccount.getName() + "@aaa.bbb.ccc.com") ;
		String svc_account_payload = getJSON(serviceAccountTTL);
		String deleteRoleResponseMsg = "{\"messages\":[\"Successfully deleted service account role.\"]}";
		Response deleteRoleResponse = getMockResponse(HttpStatus.OK, true, deleteRoleResponseMsg);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        when(reqProcessor.process("/ad/serviceaccount/offboard",svc_account_payload,token)).thenReturn(deleteRoleResponse);

        // delete meatadata
        when(reqProcessor.process(eq("/delete"),any(),eq(token))).thenReturn(responseNoContent);
        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully completed offboarding of AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.offboardServiceAccount(token, onboardedServiceAccount, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_offboardServiceAccount_failure() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        OnboardedServiceAccount onboardedServiceAccount = generateOnboardedServiceAccount("testacc02","testacc01");

        ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc01", userDetails.getUsername(), "sudo");
        String [] policies = {"o_svcacct_testacc01"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user1\"}",token)).thenReturn(userResponse);
        Response userResponse2 = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"r_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user2\"}",token)).thenReturn(userResponse2);
        Response groupResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"policies\":[\"default\", \"r_svcacct_testacc02\"],\"ttl\":0}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResponse);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            List<String> groupResList = new ArrayList<>();
            resList.add("default");
            resList.add("r_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(groupResList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);


        // Delete policies...
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access is deleted\"]}");
        when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // delete user/group/role associations
        String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + "/testacc02";
        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\n" +
                "  \"groups\": {\n" +
                "    \"group1\": \"read\"\n" +
                "  },\n" +
                "  \"managedBy\": \"user2\",\n" +
                "  \"name\": \"testacc02\",\n" +
                "  \"users\": {\n" +
                "    \"user1\": \"read\"\n" +
                "  }\n" +
                "}}");
        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(metaResponse);


        //Delete Account Role...
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(onboardedServiceAccount.getName());
        serviceAccountTTL.setService_account_name(onboardedServiceAccount.getName() + "@aaa.bbb.ccc.com") ;
        String svc_account_payload = getJSON(serviceAccountTTL);
        Response deleteRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        when(reqProcessor.process("/ad/serviceaccount/offboard",svc_account_payload,token)).thenReturn(deleteRoleResponse);

        // System under test
        String expectedResponse = "{\"errors\":[\"Failed to offboard AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.offboardServiceAccount(token, onboardedServiceAccount, userDetails);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_offboardServiceAccount_failure_metadata() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        OnboardedServiceAccount onboardedServiceAccount = generateOnboardedServiceAccount("testacc02","testacc01");

        ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc01", userDetails.getUsername(), "sudo");
        String [] policies = {"o_svcacct_testacc01"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user1\"}",token)).thenReturn(userResponse);
        Response userResponse2 = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"r_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user2\"}",token)).thenReturn(userResponse2);
        Response groupResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"policies\":[\"default\", \"r_svcacct_testacc02\"],\"ttl\":0}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResponse);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            List<String> groupResList = new ArrayList<>();
            resList.add("default");
            resList.add("r_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(groupResList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);


        // Delete policies...
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access is deleted\"]}");
        when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // delete user/group/role associations
        String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + "/testacc02";
        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\n" +
                "  \"groups\": {\n" +
                "    \"group1\": \"read\"\n" +
                "  },\n" +
                "  \"managedBy\": \"user2\",\n" +
                "  \"name\": \"testacc02\",\n" +
                "  \"users\": {\n" +
                "    \"user1\": \"read\"\n" +
                "  }\n" +
                "}}");
        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(metaResponse);


        //Delete Account Role...
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(onboardedServiceAccount.getName());
        serviceAccountTTL.setService_account_name(onboardedServiceAccount.getName() + "@aaa.bbb.ccc.com") ;
        String svc_account_payload = getJSON(serviceAccountTTL);
        String deleteRoleResponseMsg = "{\"messages\":[\"Successfully deleted service account role.\"]}";
        Response deleteRoleResponse = getMockResponse(HttpStatus.OK, true, deleteRoleResponseMsg);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        when(reqProcessor.process("/ad/serviceaccount/offboard",svc_account_payload,token)).thenReturn(deleteRoleResponse);

        // delete meatadata
        Response deleteMetaResponse = getMockResponse(HttpStatus.BAD_REQUEST, false, "");
        when(reqProcessor.process(eq("/delete"),any(),eq(token))).thenReturn(deleteMetaResponse);
        // System under test
        String expectedResponse = "{\"errors\":[\"Failed to offboard AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.offboardServiceAccount(token, onboardedServiceAccount, userDetails);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_offboardServiceAccount_failed_to_deletePolicies() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        OnboardedServiceAccount onboardedServiceAccount = generateOnboardedServiceAccount("testacc02","testacc01");

        ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc01", userDetails.getUsername(), "sudo");
        String [] policies = {"o_svcacct_testacc01"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user1\"}",token)).thenReturn(userResponse);
        Response userResponse2 = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"r_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"user2\"}",token)).thenReturn(userResponse2);
        Response groupResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"policies\":[\"default\", \"r_svcacct_testacc02\"],\"ttl\":0}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResponse);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            List<String> groupResList = new ArrayList<>();
            resList.add("default");
            resList.add("r_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(groupResList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        // Delete policies...
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Deletion of Policy information failed\"]}");
        when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // delete user/group/role associations
        String _path = TVaultConstants.SVC_ACC_ROLES_METADATA_MOUNT_PATH + "/testacc02";
        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\n" +
                "  \"groups\": {\n" +
                "    \"group1\": \"read\"\n" +
                "  },\n" +
                "  \"managedBy\": \"user2\",\n" +
                "  \"name\": \"testacc02\",\n" +
                "  \"users\": {\n" +
                "    \"user1\": \"read\"\n" +
                "  }\n" +
                "}}");
        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(metaResponse);


        //Delete Account Role...
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(onboardedServiceAccount.getName());
        serviceAccountTTL.setService_account_name(onboardedServiceAccount.getName() + "@aaa.bbb.ccc.com") ;
        String svc_account_payload = getJSON(serviceAccountTTL);
        String deleteRoleResponseMsg = "{\"messages\":[\"Successfully deleted service account role.\"]}";
        Response deleteRoleResponse = getMockResponse(HttpStatus.OK, true, deleteRoleResponseMsg);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        when(reqProcessor.process("/ad/serviceaccount/offboard",svc_account_payload,token)).thenReturn(deleteRoleResponse);

        // delete meatadata
        when(reqProcessor.process(eq("/delete"),any(),eq(token))).thenReturn(responseNoContent);
        // System under test
        String expectedResponse = "{\"messages\":[\"Successfully completed offboarding of AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.offboardServiceAccount(token, onboardedServiceAccount, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_addUserToServiceAccount_ldap_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully added user to the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_addUserToServiceAccount_userpass_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        ReflectionTestUtils.setField(serviceAccountsService,"vaultAuthMethod", "userpass");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureUserpassUser(eq("testacc01"),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully added user to the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToServiceAccount_failure_notauthorized() {
		UserDetails userDetails = getMockUser(false);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        ReflectionTestUtils.setField(serviceAccountsService,"vaultAuthMethod", "userpass");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureUserpassUser(eq("testacc01"),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // System under test
    	String expectedResponse = "{\"errors\":[\"Not authorized to perform\"]}";
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToServiceAccount_ldap_metadata_failure() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response response400 = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(response400);
        // System under test
        String expectedResponse = "{\"messages\":[\"Failed to add user to the Service Account. Metadata update failed\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromServiceAccount_ldap_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully removed user from the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromServiceAccount_userpass_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        ReflectionTestUtils.setField(serviceAccountsService,"vaultAuthMethod", "userpass");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureUserpassUser(eq("testacc01"),any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully removed user from the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromServiceAccount_failure_notauthorized() {
		UserDetails userDetails = getMockUser(false);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        // System under test
    	String expectedResponse = "{\"errors\":[\"Not authorized to perform\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromServiceAccount_failure_400() {
        UserDetails userDetails = getMockUser(false);
        String token = userDetails.getClientToken();
        ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "reads");
        // System under test
        String expectedResponse = "{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_resetSvcAccPassword_success() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc03";

        // for createAccountRole
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(serviceAccount.getName());
        serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
        serviceAccountTTL.setTtl(serviceAccount.getTtl());
        String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccount.class))).thenReturn(svc_account_payload);
        Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/onboard"), Mockito.anyString(), Mockito.eq(token))).thenReturn(onboardResponse);
        //

        // for getOnboarderdServiceAccountDetails
        Map<String,Object> rqstParams = new HashMap<>();

        rqstParams.put("service_account_name",svcAccName);
        rqstParams.put("ttl", 10);
        rqstParams.put("last_vault_rotation", "2018-05-24T17:14:38.677370855Z");
        rqstParams.put("password_last_set","2018-05-24T17:14:38.6038495Z");
        Response svcAccDetailsRes = getMockResponse(HttpStatus.OK, true, getJSON(rqstParams));

        when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDetailsRes);
        // end getOnboarderdServiceAccountDetails

        // System under test
        String pwdResetOutput = "{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password after setting ttl to 1 second.\"]}";
        Response pwdResetResponse = getMockResponse(HttpStatus.OK, true, pwdResetOutput);
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/resetpwd"),Mockito.anyString(),Mockito.eq(token))).thenReturn(pwdResetResponse);

        ADServiceAccountCreds adServiceAccountCreds = new ADServiceAccountCreds();
        adServiceAccountCreds.setCurrent_password("current_password");
        adServiceAccountCreds.setLast_password("last_password");
        adServiceAccountCreds.setUsername(svcAccName);
        String expectedOutput = getJSON(adServiceAccountCreds);
        Response pwdReadResponse = getMockResponse(HttpStatus.OK, true,expectedOutput);
        when(JSONUtil.getJSON(Mockito.any(ADServiceAccountCreds.class))).thenReturn(expectedOutput);
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/readpwd"),Mockito.anyString(),Mockito.eq(token))).thenReturn(pwdReadResponse);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedOutput);
        ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_resetSvcAccPassword_bad_request() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc03";

        // for createAccountRole
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(serviceAccount.getName());
        serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
        serviceAccountTTL.setTtl(serviceAccount.getTtl());
        String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);
        //

        // for getOnboarderdServiceAccountDetails
        Map<String,Object> rqstParams = new HashMap<>();

        rqstParams.put("service_account_name",svcAccName);
        rqstParams.put("ttl", 10);
        rqstParams.put("last_vault_rotation", "2018-05-24T17:14:38.677370855Z");
        rqstParams.put("password_last_set","2018-05-24T17:14:38.6038495Z");
        Response svcAccDetailsRes = getMockResponse(HttpStatus.OK, true, getJSON(rqstParams));

        when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDetailsRes);
        // end getOnboarderdServiceAccountDetails

        // System under test
        String expectedOutput = "{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password after setting ttl to 1 second.\"]}";
        Response pwdResetResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, expectedOutput);
        when(reqProcessor.process("/ad/serviceaccount/resetpwd","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(pwdResetResponse);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedOutput);
        ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_resetSvcAccPassword_service_account_not_onboarded() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String svcAccName = "testacc03";
        Response svcAccDetailsRes = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDetailsRes);

    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to reset password details for the given service account\"]}");
    	ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
    	assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_resetSvcAccPassword_reset_failure() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc03";
        // for createAccountRole
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(serviceAccount.getName());
        serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
        serviceAccountTTL.setTtl(serviceAccount.getTtl());
        String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccount.class))).thenReturn(svc_account_payload);
        Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/onboard"), Mockito.anyString(), Mockito.eq(token))).thenReturn(onboardResponse);
        // for getOnboarderdServiceAccountDetails
        Map<String,Object> rqstParams = new HashMap<>();

        rqstParams.put("service_account_name",svcAccName);
        rqstParams.put("ttl", 10);
        rqstParams.put("last_vault_rotation", "2018-05-24T17:14:38.677370855Z");
        rqstParams.put("password_last_set","2018-05-24T17:14:38.6038495Z");
        Response svcAccDetailsRes = getMockResponse(HttpStatus.OK, true, getJSON(rqstParams));
        when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDetailsRes);

        String pwdResetOutput = "{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password.\"]}";
        Response pwdResetResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/resetpwd"),Mockito.anyString(),Mockito.eq(token))).thenReturn(pwdResetResponse);


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password after setting ttl to 1 second.\"]}");
        ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_resetSvcAccPassword_createrole_failure() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc03";
        // for createAccountRole
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(serviceAccount.getName());
        serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
        serviceAccountTTL.setTtl(serviceAccount.getTtl());
        String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccount.class))).thenReturn(svc_account_payload);
        Response onboardResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/onboard"), Mockito.anyString(), Mockito.eq(token))).thenReturn(onboardResponse);
        // for getOnboarderdServiceAccountDetails
        Map<String,Object> rqstParams = new HashMap<>();

        rqstParams.put("service_account_name",svcAccName);
        rqstParams.put("ttl", 10);
        rqstParams.put("last_vault_rotation", "2018-05-24T17:14:38.677370855Z");
        rqstParams.put("password_last_set","2018-05-24T17:14:38.6038495Z");
        Response svcAccDetailsRes = getMockResponse(HttpStatus.OK, true, getJSON(rqstParams));
        when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDetailsRes);


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Unable to reset password details for the given service account\"]}");
        ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_resetSvcAccPassword_read_failure() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc03";

        // for createAccountRole
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(serviceAccount.getName());
        serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
        serviceAccountTTL.setTtl(serviceAccount.getTtl());
        String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccount.class))).thenReturn(svc_account_payload);
        Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/onboard"), Mockito.anyString(), Mockito.eq(token))).thenReturn(onboardResponse);
        //

        // for getOnboarderdServiceAccountDetails
        Map<String,Object> rqstParams = new HashMap<>();

        rqstParams.put("service_account_name",svcAccName);
        rqstParams.put("ttl", 10);
        rqstParams.put("last_vault_rotation", "2018-05-24T17:14:38.677370855Z");
        rqstParams.put("password_last_set","2018-05-24T17:14:38.6038495Z");
        Response svcAccDetailsRes = getMockResponse(HttpStatus.OK, true, getJSON(rqstParams));

        when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDetailsRes);
        // end getOnboarderdServiceAccountDetails

        // System under test
        String pwdResetOutput = "{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password after setting ttl to 1 second.\"]}";
        Response pwdResetResponse = getMockResponse(HttpStatus.OK, true, pwdResetOutput);
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/resetpwd"),Mockito.anyString(),Mockito.eq(token))).thenReturn(pwdResetResponse);


        String expectedOutput = "{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password.\"]}";
        Response pwdReadResponse = getMockResponse(HttpStatus.BAD_REQUEST, true,"");
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/readpwd"),Mockito.anyString(),Mockito.eq(token))).thenReturn(pwdReadResponse);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedOutput);
        ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_resetSvcAccPassword_reset_role_failure() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        String svcAccName = "testacc03";

        // for createAccountRole
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
        serviceAccount.setAutoRotate(true);
        ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
        serviceAccountTTL.setRole_name(serviceAccount.getName());
        serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
        serviceAccountTTL.setTtl(serviceAccount.getTtl());
        String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccount.class))).thenReturn(svc_account_payload);
        Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/onboard"), Mockito.anyString(), Mockito.eq(token))).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return getMockResponse(HttpStatus.BAD_REQUEST, false, "");

                return onboardResponse;
            }
        });

        // for getOnboarderdServiceAccountDetails
        Map<String,Object> rqstParams = new HashMap<>();

        rqstParams.put("service_account_name",svcAccName);
        rqstParams.put("ttl", 10);
        rqstParams.put("last_vault_rotation", "2018-05-24T17:14:38.677370855Z");
        rqstParams.put("password_last_set","2018-05-24T17:14:38.6038495Z");
        Response svcAccDetailsRes = getMockResponse(HttpStatus.OK, true, getJSON(rqstParams));

        when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDetailsRes);
        // end getOnboarderdServiceAccountDetails

        // System under test
        String pwdResetOutput = "{\"errors\":[\"Unable to reset password details for the given service account. Failed to read the updated password after setting ttl to 1 second.\"]}";
        Response pwdResetResponse = getMockResponse(HttpStatus.OK, true, pwdResetOutput);
        when(reqProcessor.process(Mockito.eq("/ad/serviceaccount/resetpwd"),Mockito.anyString(),Mockito.eq(token))).thenReturn(pwdResetResponse);


        String expectedOutput = "{\"errors\":[\"Unable to reset password details for the given service account. Failed to reset the service account with original ttl.\"]}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedOutput);
        ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_getOnboarderdServiceAccount_success() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String svcAccName = "testacc03";
    	OnboardedServiceAccountDetails onboardedServiceAccountDetails = new OnboardedServiceAccountDetails();
    	onboardedServiceAccountDetails.setLastVaultRotation("2018-05-24T17:14:38.677370855Z");
    	onboardedServiceAccountDetails.setName(svcAccName+"@aaa.bbb.ccc.com");
    	onboardedServiceAccountDetails.setPasswordLastSet("2018-05-24T17:14:38.6038495Z");
    	onboardedServiceAccountDetails.setTtl(100L);

        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("service_account_name", svcAccName+"@aaa.bbb.ccc.com");
        requestMap.put("last_vault_rotation", "2018-05-24T17:14:38.677370855Z");
        requestMap.put("password_last_set", "2018-05-24T17:14:38.6038495Z");
        requestMap.put("ttl", new Integer("100"));

    	// getOnboarderdServiceAccountDetails

    	Response svcAccDtlsResponse = getMockResponse(HttpStatus.OK, true, getJSON(requestMap));
    	when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDtlsResponse);

    	// System under test
    	String onboardedServiceAccountDetailsJSON  = getJSON(onboardedServiceAccountDetails);
    	when(JSONUtil.getJSON(Mockito.any(OnboardedServiceAccountDetails.class))).thenReturn(onboardedServiceAccountDetailsJSON);
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(onboardedServiceAccountDetailsJSON);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboarderdServiceAccount(token, svcAccName, userDetails);
    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getOnboarderdServiceAccount_notfound() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String svcAccName = "testacc03";

    	// getOnboarderdServiceAccountDetails

    	Response svcAccDtlsResponse = getMockResponse(HttpStatus.NOT_FOUND, true, getJSON(null));
    	when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDtlsResponse);

    	// System under test
    	String expectedMsg = "{\"errors\":[\"Either Service Account is not onbaorderd or you don't have enough permission to read\"]}";
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NOT_FOUND).body(expectedMsg);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboarderdServiceAccount(token, svcAccName, userDetails);
    	assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_canAddOrRemoveUser_admin_canadd() {
    	UserDetails userDetails = getMockUser(true);
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
    	String action = "addUser";
    	boolean expected = true;
    	// System under test
    	boolean actual = serviceAccountsService.canAddOrRemoveUser(userDetails, serviceAccountUser, action);
    	assertEquals(expected, actual);
    }

    @Test
    public void test_canAddOrRemoveUser_nonadmin_canadd() {
    	UserDetails userDetails = getMockUser(false);
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
    	String action = "addUser";
    	boolean expected = false;
    	// System under test
    	boolean actual = serviceAccountsService.canAddOrRemoveUser(userDetails, serviceAccountUser, action);
    	assertEquals(expected, actual);
    }

    @Test
    public void test_getOnboardedServiceAccounts_admin_success() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	// Bevavior setup
    	String expectedOutput = "{\n" +
    			"  \"keys\": [\n" +
    			"    \"testacc02\",\n" +
    			"    \"testacc03\",\n" +
    			"    \"testacc04\"\n" +
    			"  ]\n" +
    			"}";
    	Response onboardedSvsAccsResponse = getMockResponse(HttpStatus.OK, true, expectedOutput);
    	when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(onboardedSvsAccsResponse);

    	// System under test
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedOutput);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboardedServiceAccounts(token, userDetails);
    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);

    }
    @Test
    public void test_getOnboardedServiceAccounts_admin_notfound() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	// Bevavior setup
    	String expectedOutput = "{\"keys\":[]}";
    	Response onboardedSvsAccsResponse = getMockResponse(HttpStatus.NOT_FOUND, true, expectedOutput);
    	when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(onboardedSvsAccsResponse);

    	// System under test
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedOutput);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboardedServiceAccounts(token, userDetails);
    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_getOnboardedServiceAccounts_nonadmin_notfound() {
    	UserDetails userDetails = getMockUser(false);
    	String token = userDetails.getClientToken();
        String expectedOutput = "{\"keys\":[\"acc1\",\"acc2\"]}";
        String[] latestPolicies = {"o_svcacc_acc1", "o_svcacc_acc2"};
        when(JSONUtil.getJSON(Mockito.any(List.class))).thenReturn("[\"acc1\",\"acc2\"]");
        when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername())).thenReturn(latestPolicies);
    	// System under test
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedOutput);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboardedServiceAccounts(token, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_addGroupToServiceAccount_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Service Account\"]}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
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
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntity = serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToServiceAccount_metadata_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
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
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(response404);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntity = serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToServiceAccount_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to add group to the Service Account\"]}");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
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
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(response404);

        ResponseEntity<String> responseEntity = serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToServiceAccount_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add groups to this service account\"]}");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        String [] policies = {"w_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
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
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(response404);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntity = serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_removeGroupFromServiceAccount_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully removed from Service Account\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully removed from Service Account\"]}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
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
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromServiceAccount_metadata_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
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
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(response404);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromServiceAccount_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the group from the Service Account\"]}");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
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
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(response404);

        ResponseEntity<String> responseEntity = serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromServiceAccount_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove groups from this service account\"]}");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        String [] policies = {"w_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
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
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(response404);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_AssociateAppRole_succssfully() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure_400() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "writes");
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure_400_masterApprole() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "selfservicesupportrole", "write");
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add Approle to the Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure_403() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add Approle to this service account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");

        String [] policies = {"r_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_metadata_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_metadata_failure_revoke_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response configureAppRoleResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return configureAppRoleResponse_404;

                return configureAppRoleResponse;
            }
        });
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeApproleFromSvcAcc_succssfully() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle is successfully removed from Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeApproleFromSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeApproleFromSvcAcc_failure_422() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access. Valid values are read, reset, deny \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "");
        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeApproleFromSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeApproleFromSvcAcc_metadata_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeApproleFromSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeApproleFromSvcAcc_metadata_failure_revoke_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response configureAppRoleResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return configureAppRoleResponse_404;

                return configureAppRoleResponse;
            }
        });
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeApproleFromSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeApproleFromSvcAcc_failure_403() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove approle from Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeApproleFromSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeApproleFromSvcAcc_failure_400() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove approle from the Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("testsvcname", "role1", "write");
        Response configureAppRoleResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse_404);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeApproleFromSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_getServiceAccountMeta_success() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        String userPrincipalName = "test";
        String path = "/ad/roles/testacc01/";
        String _path = "metadata/ad/roles/testacc01";

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

        Response response = getMockResponse(HttpStatus.OK, true, expected);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(response.getResponse());

        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(response);

        ResponseEntity<String> responseEntity = serviceAccountsService.getServiceAccountMeta(token, userDetails, path);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_addAwsRoleToSvcacc_succssfully_iam() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
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
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.addAwsRoleToSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_addAwsRoleToSvcacc_succssfully_ec2() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
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
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.addAwsRoleToSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_addAwsRoleToSvcacc_ec2_metadata_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
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
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.addAwsRoleToSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_addAwsRoleToSvcacc_iam_metadata_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
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
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.addAwsRoleToSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_addAwsRoleToSvcacc_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed. Try Again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.addAwsRoleToSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_addAwsRoleToSvcacc_failure_403() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add AWS Role to this service account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.addAwsRoleToSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeAWSRoleFromSvcacc_succssfully_iam() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"w_svcacct_testsvcname\" ], \"auth_type\":\"iam\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeAWSRoleFromSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeAWSRoleFromSvcacc_succssfully_ec2() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"w_svcacct_testsvcname\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeAWSRoleFromSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_removeAWSRoleFromSvcacc_metadata_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"w_svcacct_testsvcname\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeAWSRoleFromSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_removeAWSRoleFromSvcacc_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove AWS Role from the Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "write");

        String [] policies = {"o_svcacct_testsvcname"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"w_svcacct_testsvcname\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeAWSRoleFromSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_removeAWSRoleFromSvcacc_failure_403() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access. Valid values are read, reset, deny\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountAWSRole serviceAccountAWSRole = new ServiceAccountAWSRole("testsvcname", "role1", "writes");
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.removeAWSRoleFromSvcacc(userDetails, token, serviceAccountAWSRole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_createRole_success() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");
        when(awsAuthService.createRole(token, awsLoginRole, userDetails)).thenReturn(responseEntityExpected);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.createAWSRole(userDetails, token, awsLoginRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_createIAMRole_success() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");
        when(awsiamAuthService.createIAMRole(awsiamRole, token, userDetails)).thenReturn(responseEntityExpected);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.createIAMRole(userDetails, token, awsiamRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_updateOnboardedServiceAccount_success() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully updated onboarded Service Account.\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        ServiceAccount serviceAccount = generateServiceAccount("testacc02", "testacc01");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process(eq("/ad/serviceaccount/onboard"), Mockito.any(), eq(token))).thenReturn(onboardResponse);

        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        ResponseEntity<String> responseEntityActual =  serviceAccountsService.updateOnboardedServiceAccount(token, serviceAccount, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_updateOnboardedServiceAccount_failure_not_onboarded() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to update onboarded Service Account. Please onboard this Service Account first and try again.\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        ServiceAccount serviceAccount = generateServiceAccount("testacc02", "testacc01");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc03\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        ResponseEntity<String> responseEntityActual =  serviceAccountsService.updateOnboardedServiceAccount(token, serviceAccount, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_updateOnboardedServiceAccount_failure_to_read_ad_details() {
        UserDetails userDetails = getMockUser(true);
        String token = userDetails.getClientToken();
        ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        String expectedResponse = "{\"errors\":[\"Failed to update onboarded Service Account. Unable to read Service account details\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(null);

        ResponseEntity<String> responseEntity = serviceAccountsService.updateOnboardedServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateOnboardedServiceAccount_failure_invalid_ttl() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value provided for TTL. TTL can't be more than 7775999 (89 days) for this Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        ServiceAccount serviceAccount = new ServiceAccount();
        serviceAccount.setName("testacc02");
        serviceAccount.setAutoRotate(true);
        serviceAccount.setTtl(TVaultConstants.PASSWORD_AUTOROTATE_TTL_MAX_VALUE);
        serviceAccount.setMax_ttl(TVaultConstants.PASSWORD_AUTOROTATE_TTL_MAX_VALUE);
        serviceAccount.setOwner("testacc0`");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.updateOnboardedServiceAccount(token, serviceAccount, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_updateOnboardedServiceAccount_failure_invalid_ttl_or_maxttl() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid or no value has been provided for TTL or MAX_TTL\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        ServiceAccount serviceAccount = new ServiceAccount();
        serviceAccount.setName("testacc02");
        serviceAccount.setAutoRotate(true);
        serviceAccount.setOwner("testacc0`");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.updateOnboardedServiceAccount(token, serviceAccount, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
    @Test
    public void test_updateOnboardedServiceAccount_failure_ttl_greaterthan_max() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Password TTL must be less than MAX_TTL\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        ServiceAccount serviceAccount = new ServiceAccount();
        serviceAccount.setName("testacc02");
        serviceAccount.setAutoRotate(true);
        serviceAccount.setTtl(89L);
        serviceAccount.setMax_ttl(89L);
        serviceAccount.setOwner("testacc0`");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.updateOnboardedServiceAccount(token, serviceAccount, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_updateOnboardedServiceAccount_createrole_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"errors\":[\"Failed to update onboarded Service Account.\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        ServiceAccount serviceAccount = generateServiceAccount("testacc02", "testacc01");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        Response onboardResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "{}");
        when(reqProcessor.process(eq("/ad/serviceaccount/onboard"), Mockito.any(), eq(token))).thenReturn(onboardResponse);
        List<ADServiceAccount> allServiceAccounts = new ArrayList<>();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ReflectionTestUtils.setField(serviceAccountsService, "ldapTemplate", ldapTemplate);
        when(ldapTemplate.search(Mockito.anyString(), Mockito.any(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.updateOnboardedServiceAccount(token, serviceAccount, userDetails);

        assertEquals(HttpStatus.MULTI_STATUS, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
}
