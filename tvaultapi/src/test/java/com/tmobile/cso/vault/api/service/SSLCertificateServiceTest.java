package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.CertResponse;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class,EntityUtils.class,HttpClientBuilder.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
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

    @Mock
    CloseableHttpResponse httpResponse;

    @Mock
    HttpClientBuilder httpClientBuilder;

    @Mock
    StatusLine statusLine;

    @Mock
    HttpEntity mockHttpEntity;

    @Mock
    CloseableHttpClient httpClient1;

    @Mock
    CertificateData certificateData;
    @Mock
    private WorkloadDetailsService workloadDetailsService;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(HttpClientBuilder.class);
        PowerMockito.mockStatic(EntityUtils.class);


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
        ReflectionTestUtils.setField(sSLCertificateService, "retrycount", 1);
        ReflectionTestUtils.setField(sSLCertificateService, "getCertifcateReasons", "certificates/certID/revocationreasons");
        ReflectionTestUtils.setField(sSLCertificateService, "issueRevocationRequest", "certificates/certID/revocationrequest");
        
        
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        when(vaultAuthService.lookup(anyString())).thenReturn(new ResponseEntity<>(HttpStatus.OK));


        ReflectionTestUtils.setField(sSLCertificateService, "workloadEndpoint", "http://appdetails.com");
       // when(ControllerUtil.getCwmToken()).thenReturn("dG9rZW4=");
        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(any())).thenReturn(httpResponse);
        String responseStr = "{\"spec\":{\"akmid\":\"103001\",\"brtContactEmail\":\" contacteops@email.com\",\"businessUnit\":\"\"," +
                "\"classification\":\"\",\"directorEmail\":\"john.mathew@email.com\",\"directorName\":\"test jin\",\"executiveSponsor\":" +
                "\"robert sam\",\"executiveSponsorEmail\":\"kim.tim@email.com\",\"id\":\"tvt\"," +
                "\"intakeDate\":\"2018-01-01\",\"opsContactEmail\":\"abc.def@gmail.com\",\"projectLeadEmail\":\"abc.def@gmail.com\"," +
                "\"tag\":\"T-Vault\",\"tier\":\"Tier II\",\"workflowStatus\":\"Open_CCP\",\"workload\":\"Adaptive Security\"}}";
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        InputStream inputStream = new ByteArrayInputStream(responseStr.getBytes());
        when(mockHttpEntity.getContent()).thenReturn(inputStream);

        String workloadApiResponse = "{\"kind\":\"Application\",\"spec\":{\"akmid\":\"103001\",\"brtContactEmail\":\"" +
                " testspec@mail.com\",\"businessUnit\":\"\",\"classification\":\"\",\"directorEmail\":\"abc.joe@mail.com\"," +
                "\"directorName\":\"abc amith\",\"executiveSponsor\":\"Dar Web\",\"opsContactEmail\":\"rick.nick@test.com\"," +
                "\"organizationalUnits\":[\"tvt\"],\"projectLeadEmail\":\"rick.nick@test.com\",\"scope\":\"Production\",\"summary\":" +
                "\"T-Vault\",\"tag\":\"T-Vault\",\"tier\":\"Tier II\",\"workflowStatus\":\"Open_CCP\",\"workload\":\"Adaptive Security\"}}";
        when(workloadDetailsService.getWorkloadDetailsByAppName(anyString())).
                thenReturn(ResponseEntity.status(HttpStatus.OK).body(workloadApiResponse));

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
    public void login_failure() throws Exception {
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        CertManagerLogin  certManagerLogin= sSLCertificateService.login(certManagerLoginRequest);
        assertNull(certManagerLogin);
    }

    @Test
    public void login_success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        CertManagerLogin  certManagerLogin= sSLCertificateService.login(certManagerLoginRequest);
        assertNotNull(certManagerLogin);
        assertEquals(certManagerLogin.getAccess_token(),"12345");
    }


    @Test
    public void test_validateInputData(){
        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("qeqeqwe");
        ResponseEntity<?> enrollResponse = sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());

        sslCertificateRequest.setCertificateName("abc.t-mobile.com");
        sslCertificateRequest.getTargetSystem().setAddress("abc def");
        ResponseEntity<?> enrollResponse1= sSLCertificateService.generateSSLCertificate(sslCertificateRequest,
                userDetails,token);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse1.getStatusCode());


        sslCertificateRequest.setCertificateName("qeqeqwe.t-mobile.com");
        sslCertificateRequest.getTargetSystem().setAddress("abcdef");
        sslCertificateRequest.getTargetSystemServiceRequest().setHostname("abc abc");
        ResponseEntity<?> enrollResponse2= sSLCertificateService.generateSSLCertificate(sslCertificateRequest,
                userDetails,token);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse2.getStatusCode());
    }


    @Test
    public void test_authenticate_success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername\",  \"password\": \"testpassword\"}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        ResponseEntity<String> responseEntity = sSLCertificateService.authenticate(certManagerLoginRequest);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
    }

    @Test
    public void test_authenticate_Unauthorized() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.UNAUTHORIZED);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        ResponseEntity<String> responseEntity = sSLCertificateService.authenticate(certManagerLoginRequest);
        assertEquals(HttpStatus.UNAUTHORIZED,responseEntity.getStatusCode());

    }

    @Test
    public void generateSSLCertificate_Success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

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



        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

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
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
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

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }




    @Test
    public void generateSSLCertificate_With_Target_System_Failure() throws Exception {
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

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }


    @Test
    public void generateSSLCertificate_With_Target_System_Service_Failure() throws Exception   {
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
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);


        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_Certificate_Already_Exists() throws Exception {
        String jsonStr = "{ \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr1 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
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

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }



    @Test
    public void generateSSLCertificate_With_PolicyFailure() throws Exception {

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
        createTargetSystemMap.put("name", "TARGETSYSTEM1");
        createTargetSystemMap.put("description", "TARGETSYSTEM1");
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
                "{  \"name\": \"TARGETSYSTEMService\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGETSYSTEMSERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        UserDetails userDetails1 = new UserDetails();
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails1.setUsername("admin");
        userDetails1.setAdmin(true);
        userDetails1.setClientToken(token);
        userDetails1.setSelfSupportToken(token);

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

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails1,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_With_existing_target_system() throws Exception {
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
        requestMap1.put("targetSystemID", 29);
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
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.OK, true, "");
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

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }



    @Test
    public void generateSSLCertificate_With_existing_target_Service_system() throws Exception {
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
        requestMap1.put("targetSystemID", 29);
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

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_With_MetaDataFailure() throws Exception {

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
                "{  \"name\": \"TARGETSYSTEMService\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGETSYSTEMSERVICE");
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

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }






    @Test
    public void generateSSLCertificate_Failure() throws Exception {
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
       doThrow(new TVaultValidationException("Exception while creating certificate"))
                .when(reqProcessor).processCert(anyString(), anyObject(), anyString(), anyString());
        ResponseEntity<?> enrollResponse = sSLCertificateService.generateSSLCertificate(sslCertificateRequest,
                userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());

    }
    
    @Test
    public void getSSLCertificate_Succes()throws Exception{
    	 String token = "12345";    
                  
         Response response =getMockResponse(HttpStatus.OK, true, "{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
          		+ "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
          		+ "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
          		+ " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
          		+ " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
          		+ "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
          		+ "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
          		+ "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
         Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
         
         token = "5PDrOhsy4ig8L3EpsJZSLAMg";
         UserDetails user1 = new UserDetails();
         user1.setUsername("normaluser");
         user1.setAdmin(true);
         user1.setClientToken(token);
         user1.setSelfSupportToken(token);         
         
         
         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);         
         
         when(reqProcessor.process("/sslcert", "{\"path\":\"metadata/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);
         
         ResponseEntity<String> responseEntityActual = sSLCertificateService.getServiceCertificates(token, user1, "");

         assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
    }
    
    @Test
    public void getSSLCertificate_Failure()throws Exception{
    	 String token = "12345";    
                  
         Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
          		+ "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
          		+ "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
          		+ " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
          		+ " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
          		+ "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
          		+ "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
          		+ "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
         Response certResponse =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
         
         token = "5PDrOhsy4ig8L3EpsJZSLAMg";
         UserDetails user1 = new UserDetails();
         user1.setUsername("normaluser");
         user1.setAdmin(true);
         user1.setClientToken(token);
         user1.setSelfSupportToken(token);         
         
         
         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);         
         
         when(reqProcessor.process("/sslcert", "{\"path\":\"metadata/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);
         
         ResponseEntity<String> responseEntityActual = sSLCertificateService.getServiceCertificates(token, user1, "");

         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
    }

    private CertResponse getEnrollResonse() {
        CertResponse enrollResponse = new CertResponse();
        enrollResponse.setHttpstatus(HttpStatus.NO_CONTENT);
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
        targetSystem.setAddress("TargetSystemaddress");
        targetSystem.setDescription("TargetSystemDescription");
        targetSystem.setName("TargetName");

        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setHostname("TargetSystemServiceHostname");
        targetSystemServiceRequest.setName("Target System Service Name");
        targetSystemServiceRequest.setPort(443);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(false);
        targetSystemServiceRequest.setMonitoringEnabled(false);
        targetSystemServiceRequest.setDescription("TargetServiceDescription");
        targetSystemServiceRequest.setMonitoringEnabled(true);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(true);

        sSLCertificateRequest.setCertificateName("CertificateName.t-mobile.com");
        sSLCertificateRequest.setAppName("xyz");
        sSLCertificateRequest.setCertOwnerEmailId("testing@mail.com");
        sSLCertificateRequest.setCertType("internal");
        sSLCertificateRequest.setTargetSystem(targetSystem);
        sSLCertificateRequest.setTargetSystemServiceRequest(targetSystemServiceRequest);
        return sSLCertificateRequest;
    }

    @Test
    public void get_sslCertificateMetadataDetails(){
        SSLCertificateMetadataDetails ssCertificateMetadataDetails = new SSLCertificateMetadataDetails();
        ssCertificateMetadataDetails.setContainerName("containername");
        ssCertificateMetadataDetails.setCertificateStatus("active");
        ssCertificateMetadataDetails.setCertType("internal");
        ssCertificateMetadataDetails.setApplicationName("abc");
        ssCertificateMetadataDetails.setCertOwnerEmailId("owneremail@test.com");
        ssCertificateMetadataDetails.setApplicationTag("tag");
        ssCertificateMetadataDetails.setProjectLeadEmailId("project@email.com");
        ssCertificateMetadataDetails.setAkmid("12345");
        ssCertificateMetadataDetails.setCertCreatedBy("rob");
        ssCertificateMetadataDetails.setAuthority("authority");
        ssCertificateMetadataDetails.setCreateDate("10-20-2020");
        ssCertificateMetadataDetails.setCertificateName("testcert.com");
        ssCertificateMetadataDetails.setCertificateId(111);
        ssCertificateMetadataDetails.setExpiryDate("10-20-2030");
        ssCertificateMetadataDetails.setApplicationOwnerEmailId("abcdef@mail.com");
        assertNotNull(ssCertificateMetadataDetails);
    }
    
    @Test
    public void getRevocationReasons_Success() throws Exception {
    	Integer certficateId = 123;
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	
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
        String jsonStr2 ="{\"time_enabled\":false,\"details_enabled\":false,\"reasons\":[{\"reason\":\"unspecified\",\"displayName\":\"Unspecified\"},{\"reason\":\"keyCompromise\",\"displayName\":\"Key compromise\"},{\"reason\":\"cACompromise\",\"displayName\":\"CA compromise\"},{\"reason\":\"affiliationChanged\",\"displayName\":\"Affiliation changed\"},{\"reason\":\"superseded\",\"displayName\":\"Superseded\"},{\"reason\":\"cessationOfOperation\",\"displayName\":\"Cessation of operation\"},{\"reason\":\"certificateHold\",\"displayName\":\"Certificate hold\"}]}";
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(jsonStr2);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates​/revocationreasons"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);
        
        ResponseEntity<?> enrollResponse =
                sSLCertificateService.getRevocationReasons(certficateId, token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }
    
    @Test
    public void getRevocationReasons_Failure() throws Exception {
    	Integer certficateId = 123;
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
 
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
        String errorJson ="{\"errors\":[\"Forbidden\"]}";
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.FORBIDDEN);
        revocationResponse.setResponse(errorJson);
        revocationResponse.setSuccess(false);
        when(reqProcessor.processCert(eq("/certificates​/revocationreasons"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);
        
        ResponseEntity<?> revocResponse =
                sSLCertificateService.getRevocationReasons(certficateId, token);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.FORBIDDEN, revocResponse.getStatusCode());
    }
    
    @Test
    public void issueRevocationRequest_Success() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	
    	RevocationRequest revocationRequest = new RevocationRequest();
    	revocationRequest.setReason("unspecified");
    	
    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(true);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);
 
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

        

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(null);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/revocationrequest"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);
        
        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        
        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.OK, revocResponse.getStatusCode());
    }
    
    @Test
    public void issueRevocationRequest_Non_Admin_Success() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	
    	RevocationRequest revocationRequest = new RevocationRequest();
    	revocationRequest.setReason("unspecified");
    	
    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(false);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);
 
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\":\"normaluser\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

        

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(null);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/revocationrequest"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);
        
        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        
        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.OK, revocResponse.getStatusCode());
    }
    
    @Test
    public void issueRevocationRequest_Admin_Failure() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	
    	RevocationRequest revocationRequest = new RevocationRequest();
    	revocationRequest.setReason("unspecified");
    	
    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(false);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);
 
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJsonError = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.BAD_REQUEST);
        response.setResponse(metaDataJsonError);
        response.setSuccess(false);
        
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

        

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(null);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/revocationrequest"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);
        
        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        
        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.BAD_REQUEST, revocResponse.getStatusCode());
    }


    @Test
    public void test_getgetTargetSystemList_success()throws Exception{
        String token = "12345";
        String jsonStr = "{\"targetSystems\": [ {" +
                "  \"name\" : \"abc.com\"," +
                "  \"description\" : \"\"," +
                "  \"address\" : \"abc.com\"," +
                "  \"targetSystemID\" : \"234\"" +
                "}, {" +
                "  \"name\" : \"cde.com\"," +
                "  \"description\" : \"cde.com\"," +
                "  \"address\" : \"cde.com\"," +
                "  \"targetSystemID\" : \"123\"" +
                "}]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/findTargetSystem"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);

        when(JSONUtil.getJSONasDefaultPrettyPrint(Mockito.any())).thenReturn(jsonStr);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+jsonStr+"}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true));

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getgetTargetSystemList_failed()throws Exception{
        String token = "12345";
        String jsonStr = "{\"targetSystems\": [ {" +
                "  \"name\" : \"abc.com\"," +
                "  \"description\" : \"\"," +
                "  \"address\" : \"abc.com\"," +
                "  \"targetSystemID\" : \"234\"" +
                "}, {" +
                "  \"name\" : \"cde.com\"," +
                "  \"description\" : \"cde.com\"," +
                "  \"address\" : \"cde.com\"," +
                "  \"targetSystemID\" : \"123\"" +
                "}]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse("{\"errors\":[\"Failed to get Target system list from NCLM\"]}");
        response.setSuccess(false);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/findTargetSystem"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system list from NCLM\"]}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getgetTargetSystemList_empty()throws Exception{
        String token = "12345";
        String jsonStr = "{\"data\": [ ]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(false);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/findTargetSystem"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        when(JSONUtil.getJSONasDefaultPrettyPrint(Mockito.any())).thenReturn("[]");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\": []}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true));

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getTargetSystemServiceList_success()throws Exception{
        String token = "12345";
        String jsonStr = "{\"targetsystemservices\": [ {\n" +
                "  \"name\" : \"testservice1\",\n" +
                "  \"description\" : \"\",\n" +
                "  \"targetSystemServiceId\" : \"1234\",\n" +
                "  \"hostname\" : \"testhostname\",\n" +
                "  \"monitoringEnabled\" : false,\n" +
                "  \"multiIpMonitoringEnabled\" : false,\n" +
                "  \"port\" : 22\n" +
                "} ]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/targetsystemservicelist"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);

        when(JSONUtil.getJSONasDefaultPrettyPrint(Mockito.any())).thenReturn(jsonStr);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+jsonStr+"}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemServiceList(token, getMockUser(true), "123");

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getTargetSystemServiceList_failed()throws Exception{
        String token = "12345";
        String jsonStr = "{\"targetsystemservices\": [ {\n" +
                "  \"name\" : \"testservice1\",\n" +
                "  \"description\" : \"\",\n" +
                "  \"targetSystemServiceId\" : \"1234\",\n" +
                "  \"hostname\" : \"testhostname\",\n" +
                "  \"monitoringEnabled\" : false,\n" +
                "  \"multiIpMonitoringEnabled\" : false,\n" +
                "  \"port\" : 22\n" +
                "} ]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse("{\"errors\":[\"Failed to get Target system service list from NCLM\"]}");
        response.setSuccess(false);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/targetsystemservicelist"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system service list from NCLM\"]}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemServiceList(token, getMockUser(true), "123");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getTargetSystemServiceList_empty()throws Exception{
        String token = "12345";
        String jsonStr = "{\"data\": [ ]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(false);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/targetsystemservicelist"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        when(JSONUtil.getJSONasDefaultPrettyPrint(Mockito.any())).thenReturn("[]");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\": []}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemServiceList(token, getMockUser(true), "123");

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
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
}
