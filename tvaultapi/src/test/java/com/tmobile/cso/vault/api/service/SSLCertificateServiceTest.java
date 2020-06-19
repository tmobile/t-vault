package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.CertResponse;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.TVaultSSLCertificateException;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SSLCertificateServiceTest {

    private MockMvc mockMvc;

    @InjectMocks
    SSLCertificateService sSLCertificateService;

    @Mock
    private RequestProcessor reqProcessor;

    @Mock
    UserDetails userDetails;

    @Mock
    VaultAuthService vaultAuthService;

    String token;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/v2/sslcert");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "userpass");
        ReflectionTestUtils.setField(sSLCertificateService, "certManagerDomain", "https://mobile.com:3004/");
        ReflectionTestUtils.setField(sSLCertificateService, "tokenGenerator", "token?grant_type=client_credentials");
        ReflectionTestUtils.setField(sSLCertificateService, "targetSystemGroups", "targetsystemgroups/");
        ReflectionTestUtils.setField(sSLCertificateService, "certificateEndpoint", "certificates/");
        ReflectionTestUtils.setField(sSLCertificateService, "targetSystems", "targetsystems");
        ReflectionTestUtils.setField(sSLCertificateService, "targetSystemServies", "targetsystemservices");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollUrl", "enroll?entityId=entityid&entityRef=SERVICE");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollCAUrl", "policy/ca?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=0");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollTemplateUrl", "policy/ca/caid/templates?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=0");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollKeysUrl", "policy/keytype?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=templateId");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollCSRUrl", "policy/csr?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=templateId");
        ReflectionTestUtils.setField(sSLCertificateService, "findTargetSystem", "targetsystemgroups/tsgid/targetsystems");
        ReflectionTestUtils.setField(sSLCertificateService, "findTargetSystemService", "targetsystems/tsgid/targetsystemservices");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollUpdateCSRUrl", "policy/csr?entityRef=SERVICE&entityId=entityid&allowedOnly=true&enroll=true");
        ReflectionTestUtils.setField(sSLCertificateService, "findCertificate", "certificates?freeText=certname&containerId=cid");
        ReflectionTestUtils.setField(sSLCertificateService, "certManagerUsername", "dGVzdGluZw==");
        ReflectionTestUtils.setField(sSLCertificateService, "certManagerPassword", "dGVzdGluZw==");

        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        when(vaultAuthService.lookup(anyString())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
    }

    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        Response response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        if (expectedBody != "") {
            response.setResponse(expectedBody);
        }
        return response;
    }


    @Test
    public void login_failure() throws Exception, TVaultSSLCertificateException {
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        CertManagerLogin  certManagerLogin= sSLCertificateService.login(certManagerLoginRequest);
        assertEquals(certManagerLogin,null);
    }


    @Test
    public void test_authenticate_success() throws Exception, TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername\",  \"password\": \"testpassword\"}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        ResponseEntity<String> responseEntity = sSLCertificateService.authenticate(certManagerLoginRequest);
        assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void test_authenticate_Unauthorized() throws Exception, TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.UNAUTHORIZED);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        ResponseEntity<String> responseEntity = sSLCertificateService.authenticate(certManagerLoginRequest);
        assertEquals(responseEntity.getStatusCode(), HttpStatus.UNAUTHORIZED);

    }

    @Test
    public void generateSSLCertificate_Success() throws Exception, TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        UserDetails userDetails = new UserDetails();
        userDetails.setSelfSupportToken("tokentTest");
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);
        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"," +
                "\"targetSystemID\": \"29\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        // loadTargetSystemServiceData();

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr1 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr1);
        response2.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGET SYSTEM Service\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGET SYSTEM SERVICE HOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        ResponseEntity<CertResponse> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }




    @Test
    public void generateSSLCertificate_With_Target_System_Failure() throws Exception, TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);

        CertResponse response2 = new CertResponse();
        response2.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response2.setResponse(jsonStr);
        response2.setSuccess(false);

        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        ResponseEntity<CertResponse> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }


    @Test
    public void generateSSLCertificate_With_Target_System_Service_Failure() throws Exception,
            TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);
        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        // loadTargetSystemServiceData();

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr1 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response2.setResponse(jsonStr1);
        response2.setSuccess(false);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGET SYSTEM Service\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGET SYSTEM SERVICE HOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);


        ResponseEntity<CertResponse> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_Certificate_Already_Exists() throws Exception, TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr1 = "{\"certificates\":[{\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"VenafiBin_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);


        Map<String, Object> requestMap1= new HashMap<>();
        requestMap1.put("certificates", "certificates");
        requestMap1.put("certificateStatus", "Active");
        when(ControllerUtil.parseJson(jsonStr1)).thenReturn(requestMap1);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.BAD_REQUEST);
        response1.setResponse(jsonStr1);
        response1.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response1);

        ResponseEntity<CertResponse> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }



    @Test
    public void generateSSLCertificate_With_PolicyFailure() throws Exception, TVaultSSLCertificateException {

        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);
        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        // loadTargetSystemServiceData();

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr1 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr1);
        response2.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGET SYSTEM Service\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGET SYSTEM SERVICE HOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        ResponseEntity<CertResponse> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_With_existing_target_system() throws Exception, TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        
        UserDetails userDetails = new UserDetails();
        userDetails.setSelfSupportToken("tokentTest");
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        String jsonStr1 ="{\"targetSystems\":[{\"address\":\"abcUser.t-mobile.com\",\"allowedOperations\":[\"targetsystems_delete\"],\"name\":\"Target Name\",\"targetSystemGroupID\":29,\"targetSystemID\":7239}]}";
        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr1);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);


        Map<String, Object> requestMap1= new HashMap<>();
        requestMap1.put("targetSystems", "targetSystems");
        requestMap1.put("name", "Target Name");
        requestMap1.put("targetSystemID", "29");
        when(ControllerUtil.parseJson(jsonStr1)).thenReturn(requestMap1);

        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr2 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr2);
        response2.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGET SYSTEM Service\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGET SYSTEM SERVICE HOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        ResponseEntity<CertResponse> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }


    @Test
    public void generateSSLCertificate_With_existing_target_Service_system() throws Exception,
            TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        
        UserDetails userDetails = new UserDetails();
        userDetails.setSelfSupportToken("tokentTest");
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        String jsonStr1 ="{\"targetSystems\":[{\"address\":\"abcUser.t-mobile.com\",\"allowedOperations\":[\"targetsystems_delete\"],\"name\":\"Target Name\",\"targetSystemGroupID\":29,\"targetSystemID\":7239}]}";
        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr1);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);


        Map<String, Object> requestMap1= new HashMap<>();
        requestMap1.put("targetSystems", "targetSystems");
        requestMap1.put("name", "Target Name");
        requestMap1.put("targetSystemID", "29");
        when(ControllerUtil.parseJson(jsonStr1)).thenReturn(requestMap1);

        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        String createTargetSystemServiceResponse =
                "{\"targetsystemservices\":[{\"name\":\"Target System Service Name\",\"targetSystemGroupId\":29,\"targetSystemId\":7239,\"targetSystemServiceId\":9990}]}";

        CertResponse response2 = new CertResponse();
        String jsonStr2 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr2);
        response2.setSuccess(true);
        response2.setResponse(createTargetSystemServiceResponse);

       when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);

        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("targetsystemservices", "targetsystemservices");
        createTargetSystemServiceMap.put("name", "Target System Service Name");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);


        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        ResponseEntity<CertResponse> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_With_MetaDataFailure() throws Exception, TVaultSSLCertificateException {

        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);
        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        // loadTargetSystemServiceData();

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr1 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr1);
        response2.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGET SYSTEM Service\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGET SYSTEM SERVICE HOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(false);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        ResponseEntity<CertResponse> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }






    @Test
    public void generateSSLCertificate_Failure() throws Exception, TVaultSSLCertificateException {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse(jsonStr);
        response.setSuccess(true);
       doThrow(new TVaultSSLCertificateException(HttpStatus.INTERNAL_SERVER_ERROR, "Exception while creating certificate"))
                .when(reqProcessor).processCert(anyString(), anyObject(), anyString(), anyString());
        ResponseEntity<CertResponse> enrollResponse = sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());

    }

    private CertResponse getEnrollResonse() {
        CertResponse enrollResponse = new CertResponse();
        enrollResponse.setHttpstatus(HttpStatus.OK);
        enrollResponse.setResponse("Certificate Created Successfully In NCLM");
        enrollResponse.setSuccess(Boolean.TRUE);

        return enrollResponse;
    }


    private CertResponse getEnrollCSRResponse() {
        String enrollCSRResponse = "{\"subject\":{\"items\":[{\"typeName\":\"cn\",\"parameterId\":0," +
                "\"removable\":false,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"\",\"entityId\":0,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"\",\"entityId\":0,\"displayName\":\"\"}},\"value\":[{\"id\":0,\"parentId\":null,\"locked\":false,\"value\":\"\",\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"c\",\"parameterId\":119,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31216,\"parentId\":null,\"locked\":false,\"value\":\"US\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"o\",\"parameterId\":122,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31219,\"parentId\":null,\"locked\":false,\"value\":\"T-Mobile USA, Inc.\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"ou\",\"parameterId\":123,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31215,\"parentId\":null,\"locked\":false,\"value\":\"Business Systems\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"l\",\"parameterId\":121,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31218,\"parentId\":null,\"locked\":false,\"value\":\"Bothell\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"st\",\"parameterId\":126,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31217,\"parentId\":null,\"locked\":false,\"value\":\"Washington\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null}]}}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(enrollCSRResponse);
        response.setSuccess(true);

        return response;
    }


    private CertResponse getEnrollKeysResponse() {
        String enrollKeyResponse = "{\"keyType\":{\"selectedId\":57,\"items\":[{\"id\":22598,\"displayName\":\"RSA " +
                "2048\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":57,\"linkId\":2,\"linkType\":\"key\"}]}}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(enrollKeyResponse);
        response.setSuccess(true);

        return response;
    }

    private CertResponse getEnrollCAResponse() {

        String getEnrollCAResponse = "{\"ca\":{\"selectedId\":40,\"items\":[{\"id\":46,\"displayName\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":40,\"linkId\":4,\"linkType\":\"CA\"," +
                "\"hasTemplates\":true}]}}";

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(getEnrollCAResponse);
        response.setSuccess(true);

        return response;
    }


    private CertResponse getEnrollTemplateResponse() {

        String getEnrollCAResponse = "{\"template\":{\"selectedId\":46,\"items\":[{\"id\":49," +
                "\"displayName\":\"BarnacleDomainControllerAuthenticationNCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":44,\"linkId\":15,\"linkType\":\"TEMPLATE\"},{\"id\":50,\"displayName\":" +
                "\"T-Mobile USA Mutual Web Authentication2 NCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":51,\"linkId\":18,\"linkType\":\"TEMPLATE\"},{\"id\":52,\"displayName\":" +
                "\"T-MobileUSAConcentratorIPSec(Offlinerequest)NCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":49,\"linkId\":16," +
                "\"linkType\":\"TEMPLATE\"},{\"id\":51,\"displayName\":\"T-MobileUSASimpleClientAuthNCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":54,\"linkId\":19,\"linkType\":\"TEMPLATE\"},{\"id\":53,\"displayName\":\"T-MobileUSAWebServerOfflineNCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":46," +
                "\"linkId\":26,\"linkType\":\"TEMPLATE\"}]}}";

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(getEnrollCAResponse);
        response.setSuccess(true);

        return response;
    }


    private CertManagerLoginRequest getCertManagerLoginRequest() {
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest();
        certManagerLoginRequest.setPassword("password");
        certManagerLoginRequest.setUsername("username");
        return certManagerLoginRequest;
    }

    private SSLCertificateRequest getSSLCertificateRequest() {
        SSLCertificateRequest sSLCertificateRequest = new SSLCertificateRequest();
        TargetSystem targetSystem = new TargetSystem();
        targetSystem.setAddress("Target System address");
        targetSystem.setDescription("Target System Description");
        targetSystem.setName("Target Name");

        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setHostname("Target System Service Host name");
        targetSystemServiceRequest.setName("Target System Service Name");
        targetSystemServiceRequest.setPort(443);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(false);
        targetSystemServiceRequest.setMonitoringEnabled(false);
        targetSystemServiceRequest.setDescription("Target Service Description");
        targetSystemServiceRequest.setMonitoringEnabled(true);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(true);

        sSLCertificateRequest.setCertificateName("CertificateName");
        sSLCertificateRequest.setTargetSystem(targetSystem);
        sSLCertificateRequest.setTargetSystemServiceRequest(targetSystemServiceRequest);
        return sSLCertificateRequest;
    }


}
