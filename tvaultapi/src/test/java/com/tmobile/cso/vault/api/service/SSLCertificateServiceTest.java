package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.CertResponse;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.*;
import com.tmobile.cso.vault.api.validator.TokenValidator;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class,EntityUtils.class,HttpClientBuilder.class, OIDCUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class SSLCertificateServiceTest {

    private MockMvc mockMvc;

    @Mock
    DirectoryService directoryService;

    @Mock
    LdapTemplate ldapTemplate;

    @InjectMocks
    SSLCertificateService sSLCertificateService;
    
    @Mock
    ControllerUtil controllerUtil;

    @Mock
    private RequestProcessor reqProcessor;

    @Mock
    UserDetails userDetails;
    
    @Mock
    NCLMMockUtil nclmMockUtil;

    @Mock
    VaultAuthService vaultAuthService;

    @Mock
    PolicyUtils policyUtils;
    
    @Mock
    TokenValidator tokenValidator;

    String token;

    @Mock
    CloseableHttpResponse httpResponse;

    @Mock
    HttpClientBuilder httpClientBuilder;
    
    @Mock
    CloseableHttpClient httpClient;
    
    @Mock
    HttpClient httpClient2;
    
    @Mock
    HttpResponse httpResponse2;

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

    @Mock
    CertificateUtils certificateUtils;

    @Mock
	private AppRoleService appRoleService;

    @Mock
    ObjectMapper obj;    

    @Mock
    EmailUtils emailUtils;
    
    @Mock
    OIDCUtil OIDCUtil;
    
    @Mock
    TokenUtils tokenUtils;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(HttpClientBuilder.class);
        PowerMockito.mockStatic(EntityUtils.class);
        PowerMockito.mockStatic(OIDCUtil.class);


        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(OIDCUtil.class, "log", LogManager.getLogger(OIDCUtil.class));

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

        ReflectionTestUtils.setField(sSLCertificateService, "certificateNameTailText", ".t-mobile.com");
        ReflectionTestUtils.setField(sSLCertificateService, "renewDelayTime", 3000);
		ReflectionTestUtils.setField(sSLCertificateService, "renewCertificateEndpoint", "certificates/certID/renew");
        ReflectionTestUtils.setField(sSLCertificateService, "getTemplateParamUrl", "policy/template/templateId/parameters?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=templateId");
        ReflectionTestUtils.setField(sSLCertificateService, "putTemplateParamUrl", "policy/template/templateId/parameters?entityRef=SERVICE&entityId=entityid&allowedOnly=true&enroll=true");
        ReflectionTestUtils.setField(sSLCertificateService, "approvalUrl", "actions/actionId/finalize");
        ReflectionTestUtils.setField(sSLCertificateService, "unassignCertificateEndpoint", "certificates/certID/services/assigned");
        ReflectionTestUtils.setField(sSLCertificateService, "deleteCertificateEndpoint", "certificates/certID");
        ReflectionTestUtils.setField(sSLCertificateService, "supportEmail", "support@abc.com");
        ReflectionTestUtils.setField(sSLCertificateService, "requestStatusUrl", "actions/actionid");
        ReflectionTestUtils.setField(sSLCertificateService, "fromEmail", "no-reply@t-mobile.com");
        ReflectionTestUtils.setField(sSLCertificateService, "nclmErrorMessage", "Your request cannot be processed now due to some technical issue. Please try after some time");
        ReflectionTestUtils.setField(sSLCertificateService, "nclmMockEnabled", "false");
        ReflectionTestUtils.setField(sSLCertificateService, "findAllCertificate", "certificates?freeText=&stateCurrent=false&limit=100&stateDeploying=false&stateWaiting=false&stateLastDeployed=false&stateAssigned=false&stateUnattached=false&expiresAfter=&expiresBefore=&sortAttribute=createdAt&sortOrder=desc&containerId=");
        ReflectionTestUtils.setField(sSLCertificateService, "container_name", "VenafiBin_12345");
        ReflectionTestUtils.setField(sSLCertificateService, "private_single_san_ts_gp_id", 29);
        ReflectionTestUtils.setField(sSLCertificateService, "pacbotGetCertEndpoint", "/pacbot/test");

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
        when(httpClient2.execute(any())).thenReturn(httpResponse2);
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

        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUserfirstname,lastname");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testUser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        String responseMessage =new ObjectMapper().writeValueAsString(users);

        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(users);
        when(directoryService.searchByCorpId(Mockito.any())).thenReturn(responseEntityExpected);

        Mockito.doNothing().when(emailUtils).sendHtmlEmalFromTemplateForDelete(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.doNothing().when(emailUtils).sendEmailForExternalCert(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.doNothing().when(emailUtils).sendEmailForExternalCert(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any());
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "external")).thenReturn(certificateMetadata);
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
        String[] dnsNames = {"test.sample1@t-mobile.com"};
        sslCertificateRequest.setCertificateName("qeqeqwe");
        sslCertificateRequest.setCertType("test");
        sslCertificateRequest.setNotificationEmail("test.sample1@t-mobile.com");
        sslCertificateRequest.setDnsList(dnsNames);
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

        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=CertificateName.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        sslCertificateRequest.setNotificationEmail("test.sample1@t-mobile.com");
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

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

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "CertificateName.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");


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

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        Response responseObj = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataStr);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);
        
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_Failed_with_WrongDNSname() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=CertificateName.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();
        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
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
        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponseWithWrongDNSName());
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);
        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }
    @Test
    public void generateSSLCertificate_Failed_with_WrongCertificatename() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=CertificateName.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();
        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
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
        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponseWithWrongCertificateName());
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);
        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }
    @Test
    public void generateSSLCertificate_External_Success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        sslCertificateRequest.setCertType("external");
        sslCertificateRequest.setNotificationEmail("notificationemail@t-mobile.com");
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

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
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse_For_External_Certificate());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse_External());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getTemplateParameter Validation
        when(reqProcessor.processCert(eq("/certmanager/getTemplateParameter"), anyObject(), anyString(), anyString())).thenReturn(getTemplateParametersResponse());

        ///putTemplateParameter Validation
        when(reqProcessor.processCert(eq("/certmanager/putTemplateParameter"), anyObject(), anyString(), anyString())).thenReturn(putTemplateParameterResponse());


        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse_For_ExternalCertificate());
        when(reqProcessor.processCert(eq("/certmanager/approvalrequest"), anyObject(), anyString(), anyString())).thenReturn(getApproveResponse());

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        Response responseObj = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataStr);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);
        
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        createCertPolicyMap.put("notificationEmail", "certificatename@t-mobile.com");

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);
        String metaDataStr1 = "{\"actionId\":111}";
        Map<String, Object> createCertPolicyMap1 = new HashMap<>();
        createCertPolicyMap1.put("actionId", 111);
        when(ControllerUtil.parseJson(metaDataStr1)).thenReturn(createCertPolicyMap1);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        String certType = "internal";
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", certType)).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_External_Success_Scenerio2() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = {"test1","test2" };
        sslCertificateRequest.setDnsList(dnsNames);
        sslCertificateRequest.setCertType("external");
        sslCertificateRequest.setNotificationEmail("test.sample1@t-mobile.com");
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        
        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

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
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse_For_External_Certificate());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse_External());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getTemplateParameter Validation
        when(reqProcessor.processCert(eq("/certmanager/getTemplateParameter"), anyObject(), anyString(), anyString())).thenReturn(getTemplateParametersResponse());

        ///putTemplateParameter Validation
        when(reqProcessor.processCert(eq("/certmanager/putTemplateParameter"), anyObject(), anyString(), anyString())).thenReturn(putTemplateParameterResponse());


        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse_For_ExternalCertificate());
        when(reqProcessor.processCert(eq("/certmanager/approvalrequest"), anyObject(), anyString(), anyString())).thenReturn(getApproveResponse());

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": " +
                "\"tvt\", \"certType\": \"external\", \"certOwnerNtid\": \"testusername1\"}, \"path\": " +
                "\"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        String metadatajsonStr = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "external");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajsonStr);
        
        Response responseObj = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataStr);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);
        when(reqProcessor.process("/write", metadatajsonStr, token)).thenReturn(responseNoContent);
        String metaDataStr1 = "{\"actionId\":111}";
        Map<String, Object> createCertPolicyMap1 = new HashMap<>();
        createCertPolicyMap1.put("actionId", 111);
        when(ControllerUtil.parseJson(metaDataStr1)).thenReturn(createCertPolicyMap1);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        String certType = "external";
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", certType)).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

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
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
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
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
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
        String jsonStr1 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
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

        SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
        when(certificateUtils.getCertificateMetaData(any(), anyString(), anyString())).thenReturn(certificateMetadata);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_External_Already_Exists_in_MetaData() throws Exception {
        String jsonStr = "{ \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr1 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertType("external");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
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
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr1);
        response1.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        when(certificateUtils.getCertificateMetaData(any(), anyString(), anyString())).thenReturn(certificateMetadata);

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
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
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

        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);

        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        sslCertificateRequest.setNotificationEmail("test.sample1@t-mobile.com");
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

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

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

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");

        Response responseOkContent = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseOkContent);
        
        when(reqProcessor.process(eq("/write"),any(),eq(userDetailToken))).thenReturn(responseOkContent);

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

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response responseObj = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataStr);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

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

        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);

        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        sslCertificateRequest.setNotificationEmail("test.sample1@t-mobile.com");
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

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

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

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");

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

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response responseObj = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataStr);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

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
        sslCertificateRequest.setCertificateName("certificatename");
        sslCertificateRequest.setNotificationEmail("testCert@t-mobile.com");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
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
        sslCertificateRequest.setCertificateName("certificatename");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
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

         ResponseEntity<String> responseEntityActual = sSLCertificateService.getServiceCertificates(token, user1, "",1,0,"internal");

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

         ResponseEntity<String> responseEntityActual = sSLCertificateService.getServiceCertificates(token, user1, "",1,0,"internal");

         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
    }

    private CertResponse getEnrollResonse_For_ExternalCertificate() {
        CertResponse enrollResponse = new CertResponse();
        enrollResponse.setHttpstatus(HttpStatus.ACCEPTED);
        String jsonStr = "{\"actionId\":111}";
        enrollResponse.setResponse(jsonStr);
        enrollResponse.setSuccess(Boolean.TRUE);
        return enrollResponse;
    }
    private CertResponse getEnrollResonse() {
        CertResponse enrollResponse = new CertResponse();
        enrollResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        enrollResponse.setResponse("Certificate Created Successfully");
        enrollResponse.setSuccess(Boolean.TRUE);

        return enrollResponse;
    }

    private CertResponse getApproveResponse(){
        String approveResponse =   "{\"actionId\":304,\"action\":{\"type\":90,\"displayName\":\"Renew certificate\"," +
                "\"data\":[{\"type\":\"link\",\"displayName\":\"Certificate\",\"value\":\"View certificate\",\"linkType\":\"certificate\",\"certificateId\":66601}]},\"response\":{},\"note\":\"Approve Test\",\"finalized\":true,\"conclusion\":\"waiting\",\"verdicts\":[]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(approveResponse);
        response.setSuccess(true);
        return response;
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

    private CertResponse getEnrollCSRResponseWithWrongCertificateName() {
        String enrollCSRResponse = "{\"containsErrors\":true,\"subject\":{\"items\":[{\"typeName\":\"cn\"," +
                "\"parameterId\":120,\"removable\":false,\"   denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"value\":[{\"id\":315441,\"parentId\":null,\"linkId\":46,\"locked\":false,\"value\":\"certtest..dns.t-mobile.com\",\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"qeqwe\"},\"disabled\":false,\"validationResult\":{\"results\":[{\"category\":\"subject\",\"field\":\"cn\",\"severity\":\"content\",\"severityEnum\":300,\"description\":\"must be a valid DNS name (certtest..dns.t-mobile.com)\"}]}}],\"whitelist\":null,\"blacklist\":null}]},\"subjectAlternativeName\":{\"items\":[{\"typeName\":\"dns\",\"parameterId\":0,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"value\":[{\"id\":0,\"parentId\":null,\"locked\":false,\"value\":\"certtest..dns1.t-mobile.com\",\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"Policy\"},\"disabled\":false,\"validationResult\":{\"results\":[{\"category\":\"subjectAlt\",\"field\":\"dns\",\"severity\":\"content\",\"severityEnum\":300,\"description\":\"must be a valid DNS name (certtest..dns1.t-mobile.com)\"}]}},{\"id\":0,\"parentId\":null,\"locked\":false,\"value\":\"certtest..dns2.t-mobile.com\",\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"Policy\"},\"disabled\":false,\"validationResult\":{\"results\":[{\"category\":\"subjectAlt\",\"field\":\"dns\",\"severity\":\"content\",\"severityEnum\":300,\"description\":\"must be a valid DNS name (certtest..dns2.t-mobile.com)\"}]}}],\"whitelist\":null,\"blacklist\":null}]}}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(enrollCSRResponse);
        response.setSuccess(true);
        return response;
    }
    private CertResponse getEnrollCSRResponseWithWrongDNSName() {
        String enrollCSRResponse = "{\"containsErrors\":true,\"subject\":{\"items\":[{\"typeName\":\"cn\"," +
                "\"parameterId\":120,\"removable\":false,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"value\":[{\"id\":315441,\"parentId\":null,\"linkId\":46,\"locked\":false,\"value\":\"dns1.t-mobile.com\",\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"qeqwe\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"l\",\"parameterId\":121,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"value\":[{\"id\":31218,\"parentId\":null,\"locked\":false,\"value\":\"Bothell\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"st\",\"parameterId\":126,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"value\":[{\"id\":31217,\"parentId\":null,\"locked\":false,\"value\":\"Washington\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null}]},\"subjectAlternativeName\":{\"items\":[{\"typeName\":\"dns\",\"parameterId\":0,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"\"}},\"value\":[{\"id\":0,\"parentId\":null,\"locked\":false,\"value\":\"dns2..t-mobile.com\",\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"Policy\"},\"disabled\":false,\"validationResult\":{\"results\":[{\"category\":\"subjectAlt\",\"field\":\"dns\",\"severity\":\"content\",\"severityEnum\":300,\"description\":\"must be a valid DNS name (dns2..t-mobile.com)\"}]}},{\"id\":0,\"parentId\":null,\"locked\":false,\"value\":\"dns3.t-mobile.com\",\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":15116,\"displayName\":\"Policy\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null}]}}";
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


    private CertResponse getEnrollCAResponse_For_External_Certificate() {
        String getEnrollCAResponse = "{\"ca\":{\"selectedId\":0,\"items\":[{\"id\":38,\"displayName\":\"Entrust CA\"," +
                "\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":38,\"linkId\":2,\"linkType\":\"CA\",\"hasTemplates\":true}]}}";
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


    private CertResponse getEnrollTemplateResponse_External() {
        String getEnrollCAResponse = "{\"template\":{\"selectedId\":0,\"items\":[{\"id\":40," +
                "\"displayName\":\"Advantage SSL Certificate\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":50,\"linkId\":2,\"linkType\":\"META_TEMPLATE\",\"hasParameters\":true},{\"id\":42,\"displayName\":\"Unified Communication Multi-Domain SSL Certificate\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":47,\"linkId\":4,\"linkType\":\"META_TEMPLATE\",\"hasParameters\":true}]}}";
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

        sSLCertificateRequest.setCertificateName("certificatename");
        sSLCertificateRequest.setAppName("xyz");
        sSLCertificateRequest.setCertOwnerEmailId("testing@mail.com");
        sSLCertificateRequest.setCertOwnerNtid("testuser2");
        sSLCertificateRequest.setCertType("internal");
        sSLCertificateRequest.setTargetSystem(targetSystem);
        sSLCertificateRequest.setTargetSystemServiceRequest(targetSystemServiceRequest);
        sSLCertificateRequest.setNotificationEmail("testing@mail.com");
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
        ssCertificateMetadataDetails.setOnboardFlag(Boolean.FALSE);
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
        when(reqProcessor.processCert(eq("/certificates/revocationreasons"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

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
        when(reqProcessor.processCert(eq("/certificates/revocationreasons"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.getRevocationReasons(certficateId, token);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.FORBIDDEN, revocResponse.getStatusCode());
    }

    @Test
    public void issueRevocationRequest_Success() throws Exception {
    	String certficateName = "testCert.t-mobile.com";
    	String certficateType = "internal";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"containerId\":123,\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"targetSystemServiceIds\":[]}]}";
    	

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
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"containerId\":123,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
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
        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(null);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/revocationrequest"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "testCert.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.getCertificateMetaData(token, "testCert.t-mobile.com", "external")).thenReturn(certificateMetadata);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateType, certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
    }

    @Test
    public void issueRevocationRequest_Non_Admin_Success() throws Exception {
    	String certficateName = "testCert.t-mobile.com";
    	String certficateType = "internal";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"containerId\":123,\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"targetSystemServiceIds\":[]}]}";
    	

    	RevocationRequest revocationRequest = new RevocationRequest();
    	revocationRequest.setReason("unspecified");

    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(false);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);
         String[] policies = {"o_cert_testCert.t-mobile.com"};
         userDetails.setPolicies(policies);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"containerId\":123,\"certOwnerNtid\":\"normaluser\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
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
        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(null);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/revocationrequest"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "testCert.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.getCertificateMetaData(token, "testCert.t-mobile.com", "external")).thenReturn(certificateMetadata);


        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateType,certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
    }

    @Test
    public void issueRevocationRequest_Admin_Failure() throws Exception {
    	String certficateName = "testCert.t-mobile.com";
    	String certficateType = "internal";
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
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "testCert@t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.getCertificateMetaData(token, "testCert@t-mobile.com", "external")).thenReturn(certificateMetadata);



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

        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateType,certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.BAD_REQUEST, revocResponse.getStatusCode());
    }

    @Test
    public void testAddUserToCertificateSuccessfully() {
        CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("r_cert_certificatename.t-mobile.com");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(certUser, userDetail, false);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    
    
    @Test
    public void testAddUserToCertificateOIDCSuccessfully() {
        CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("r_cert_certificatename.t-mobile.com");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        //oidc test cases
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "oidc");
        String mountAccessor = "auth_oidc";
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUser,testuser");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
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
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
					.body(oidcEntityResponse);

			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
	
			Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
			when(OIDCUtil.updateOIDCEntity(any(), any()))
					.thenReturn(responseEntity3);
        when(OIDCUtil.oidcFetchEntityDetails(anyString(), anyString(), any(), eq(true))).thenReturn(responseEntity2);
        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(certUser, userDetail, false);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailureAllCerts() {
        CertificateUser certUser = new CertificateUser("testuser2","read", "certtest250630.t-mobile.com", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certtest250630.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,r_cert_certtest250630.t-mobile.com");
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return response_404;

                return response_404;
            }
        });

        when(certificateUtils.getCertificateMetaData(token, "certtest250630.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(certUser, userDetail, false);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailure() throws IOException {
        CertificateUser certUser = new CertificateUser("testuser2","write", "certtest250630.t-mobile.com", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_cert_certtest250630.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("w_cert_certtest250630.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(responseNotFound);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(responseNotFound);

        when(certificateUtils.getCertificateMetaData(token, "certtest250630.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(certUser, userDetail, false);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailureBadrequest() {
        CertificateUser certUser = new CertificateUser("testuser1","write", "CertificateName", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        userDetails.setUsername("testuser1");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        when(certificateUtils.getCertificateMetaData(token, "CertificateName", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(certUser, null, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateForNonAdminFailed() throws IOException {
        CertificateUser certUser = new CertificateUser("testuser2","deny", "certificatename.t-mobile.com", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        certificateMetadata.setCertOwnerNtid("testuser2");
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"d_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate owner cannot be added as a user to the certificate owned by him\"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("d_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(certUser, userDetail, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailedIfEmptyUserDetails() {
        CertificateUser certUser = new CertificateUser("testuser1", "read", "certificatename.t-mobile.com", "internal");
        userDetails = null;
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(certUser, null, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailedForNotAuthorizedUser() {
        CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com", "internal");
        SSLCertificateMetadataDetails certificateMetadata = null;
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(false);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(certUser, userDetail, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test(expected = Exception.class)
    public void testAddUserToCertificatePolicyDataFailed() {
        CertificateUser certUser = new CertificateUser("testuser2","deny", "certificatename.t-mobile.com", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"key\":[\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"d_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        sSLCertificateService.addUserToCertificate(certUser, userDetail, false);
    }

    @Test
    public void test_getgetTargetSystemList_success()throws Exception{
        String token = "12345";
        String certType= "internal";
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
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true), certType);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }
    
    @Test
    public void test_getgetTargetSystemList_External_success()throws Exception{
        String token = "12345";
        String certType= "external";
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
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true), certType);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getgetTargetSystemList_failed()throws Exception{
        String token = "12345";
        String certType= "internal";
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
        response.setResponse("{\"errors\":[\"Your request cannot be processed now due to some technical issue. Please try after some time\"]}");
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


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Your request cannot be processed now due to some technical issue. Please try after some time\"]}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true), certType);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getgetTargetSystemList_empty()throws Exception{
        String token = "12345";
        String certType= "internal";
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
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true), certType);

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
        response.setResponse("{\"errors\":[\"NCLM services are down. Please try after some time\"]}");
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


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Your request cannot be processed now due to some technical issue. Please try after some time\"]}");
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

    SSLCertificateMetadataDetails getSSLCertificateMetadataDetails() {
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("testuser1");
        certDetails.setCertificateName("certificatename@t-mobile.com");
        certDetails.setCertOwnerNtid("testuser1");
        certDetails.setCertOwnerEmailId("owneremail@test.com");
        certDetails.setExpiryDate("10-20-2030");
        certDetails.setCreateDate("10-20-2030");
        certDetails.setNotificationEmails("test@abc.com");
        certDetails.setOnboardFlag(Boolean.FALSE);
        return certDetails;
    }

    SSLCertificateMetadataDetails getSSLExternalCertificateRequest() {
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("external");
        certDetails.setCertCreatedBy("testuser1");
        certDetails.setCertificateName("certificatename.t-mobile.com");
        certDetails.setCertOwnerNtid("testuser1");
        certDetails.setCertOwnerEmailId("owneremail@test.com");
        certDetails.setExpiryDate("10-20-2030");
        certDetails.setRequestStatus(SSLCertificateConstants.REQUEST_PENDING_APPROVAL);
        return certDetails;
    }

	@Test
    public void testAssociateAppRoleToCertificateSuccssfully() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
	
	@Test
    public void testDeleteAppRoleFromCertificateSuccssfully() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle is successfully removed(if existed) from Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
       // Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
       // when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        when( appRoleService.configureApprole(any(),any(),any())).thenReturn(appRoleResponse);
        ResponseEntity<String> responseEntityActual =  sSLCertificateService.deleteApproleFromCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertificateFailure400() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename", "role1", "read", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertFailureMasterApprole() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "selfservicesupportrole", "read", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertificateFailure() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add Approle to the Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "external");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        certificateMetadata.setCertType("external");
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "external")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_externalcerts_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertificateFailure403() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add Approle to this Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertMetadataFailure() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertMetadataFailureRevokeFailure() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response configureAppRoleResponse404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return configureAppRoleResponse404;

                return configureAppRoleResponse;
            }
        });
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

	@Test
    public void testAssociateAppRoleToCertificateSuccssfullyForAdmin() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(true);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test
    public void testAssociateAppRoleToCertificateFailedForEmptyUserDetails() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add approle to this certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = null;
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test
    public void testAssociateAppRoleToCertificateFailedIfNoRoleExists() {

		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either Approle doesn't exists or you don't have enough permission to add this approle to Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(true);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "{}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test(expected = Exception.class)
    public void testAssociateAppRoleToCertificateFailedIfReadApprole() {

		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(true);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test
    public void testAssociateAppRoleToCertificateFailureIfEmptyInput() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = null;
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test
    public void testAssociateAppRoleToCertificateFailureIfInvalidAccess() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "revoke", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    void mockNclmLogin() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), any(), any(), any())).thenReturn(certResponse);
    }

    @Test
    public void test_downloadCertificateWithPrivateKey_success() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false,"internal");

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};

        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("certname");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        certDetails.setCertificateId(123123);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);
        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname.pem\"")
                .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithPrivateKey_success_pkcs12pem() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pkcs12pem", false,"internal");

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname.pfx\"")
                .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithPrivateKey_success_default() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "default", false,"internal");

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname.pfx\"")
                .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithPrivateKey_failure() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false,"internal");

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(null);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        InputStreamResource resource = null;
        ResponseEntity<InputStreamResource> responseEntityExpected =
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithPrivateKey_post_failure() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pkcs12der", false,"internal");

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        InputStreamResource resource = null;
        ResponseEntity<InputStreamResource> responseEntityExpected =
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }


    @Test
    public void test_downloadCertificateWithPrivateKey_failure_httpClient() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pkcs12der", false,"internal");

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenThrow(new IOException());

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName.t-mobile.com");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        InputStreamResource resource = null;
        ResponseEntity<InputStreamResource> responseEntityExpected =
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificates_success() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);


        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false,"internal");

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname.pem\"")
                .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8")).body(resource);

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificates_failed_403() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();
        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false,"internal");

        String policyList [] = {};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.FORBIDDEN, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificates_failed_invalid_token() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
        certResponse.setResponse(null);
        certResponse.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), any(), any(), any())).thenReturn(certResponse);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false,"internal");

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithoutPrivateKey_success() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.areDownloadInputsValid(any(),any())).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname\"")
                .contentType(MediaType.parseMediaType("application/x-pem-file;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem","internal");
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithoutPrivateKey_success_der() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.areDownloadInputsValid(any(),any())).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname\"")
                .contentType(MediaType.parseMediaType("application/pkix-cert;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "der","external");
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithoutPrivateKey_failed() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.areDownloadInputsValid(any(),any())).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem","internal");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }


    @Test
    public void test_downloadCertificateWithoutPrivateKey_failed_entity_null() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(null);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.areDownloadInputsValid(any(),any())).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem","internal");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithoutPrivateKey_failed_httpClient() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenThrow(new IOException());

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.areDownloadInputsValid(any(),any())).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem","internal");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificate_failed_403() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        String policyList [] = {};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.areDownloadInputsValid(any(),any())).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser1");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(false), "certname", "pem","internal");
        assertEquals(HttpStatus.FORBIDDEN, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificate_failed_invalid_token() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
        certResponse.setResponse(null);
        certResponse.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), any(), any(), any())).thenReturn(certResponse);

        String policyList [] = {};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);
        when(controllerUtil.areDownloadInputsValid(any(),any())).thenReturn(true);
        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser1");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem","internal");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void renewCertificate_Success() throws Exception {
    	String certficateName = "testCert.t-mobile.com";
    	String certficateType = "internal";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"containerId\":123,\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

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
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"containerId\":123,\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
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


        CertResponse renewResponse = new CertResponse();
        renewResponse.setHttpstatus(HttpStatus.OK);
        renewResponse.setResponse(null);
        renewResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/renew"), anyObject(), anyString(), anyString())).thenReturn(renewResponse);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> renewCertResponse =
                sSLCertificateService.renewCertificate(certficateType,certficateName, userDetails, token);

        //Assert
        assertNotNull(renewCertResponse);
    }

    @Test
    public void renewCertificate_Non_Admin_Success() throws Exception {
    	String certficateName = "testCert.t-mobile.com";
    	String certficateType = "internal";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

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


        CertResponse renewResponse = new CertResponse();
        renewResponse.setHttpstatus(HttpStatus.OK);
        renewResponse.setResponse(null);
        renewResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/renew"), anyObject(), anyString(), anyString())).thenReturn(renewResponse);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> renewCertResponse =
                sSLCertificateService.renewCertificate(certficateType, certficateName, userDetails, token);

        //Assert
        assertNotNull(renewCertResponse);
    }
    
    @Test
    public void renewCertificate_Non_Admin_External_Success() throws Exception {
    	String certficateName = "testCert.t-mobile.com";
    	String certficateType = "external";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

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


        CertResponse renewResponse = new CertResponse();
        renewResponse.setHttpstatus(HttpStatus.OK);
        renewResponse.setResponse(null);
        renewResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/renew"), anyObject(), anyString(), anyString())).thenReturn(renewResponse);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> renewCertResponse =
                sSLCertificateService.renewCertificate(certficateType, certficateName, userDetails, token);

        //Assert
        assertNotNull(renewCertResponse);
    }

    @Test
    public void renewCertificate_Admin_Failure() throws Exception {
    	String certficateName = "testCert.t-mobile.com";
    	String certficateType = "internal";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

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

        CertResponse renewResponse = new CertResponse();
        renewResponse.setHttpstatus(HttpStatus.OK);
        renewResponse.setResponse(null);
        renewResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/renew"), anyObject(), anyString(), anyString())).thenReturn(renewResponse);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateType, certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.BAD_REQUEST, revocResponse.getStatusCode());
    }

    @Test
    public void testRemoveUserFromCertificateForLdapAuthSuccess() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com", "internal");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String expectedResponse = "{\"messages\":[\"Successfully removed user from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}", token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    
    @Test
    public void testRemoveUserFromCertificateForOIDCAuthSuccess() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "oidc");
    	CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com", "internal");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String expectedResponse = "{\"messages\":[\"Successfully removed user from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}", token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        //oidc test cases
        String mountAccessor = "auth_oidc";
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUser,testuser");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
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
			when(OIDCUtil.fetchMountAccessorForOidc(token)).thenReturn(mountAccessor);

			ResponseEntity<OIDCEntityResponse> responseEntity2 = ResponseEntity.status(HttpStatus.OK)
					.body(oidcEntityResponse);

			when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
	
			Response responseEntity3 = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"data\": [\"safeadmin\",\"vaultadmin\"]]");
			when(OIDCUtil.updateOIDCEntity(any(), any()))
					.thenReturn(responseEntity3);
        when(OIDCUtil.oidcFetchEntityDetails(anyString(), anyString(), any(), eq(true))).thenReturn(responseEntity2);
        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateUserpassAuthSuccess() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	CertificateUser certUser = new CertificateUser("testuser2","write", "certificatename.t-mobile.com", "internal");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("w_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
    	String expectedResponse = "{\"messages\":[\"Successfully removed user from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateFailureIfNotauthorized() {
    	SSLCertificateMetadataDetails certificateMetadata = null;
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
    	CertificateUser certUser = new CertificateUser("testuser1","write", "certificatename.t-mobile.com", "internal");
        String expectedResponse = "{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(false);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateFailure400() {
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	CertificateUser certUser = new CertificateUser("testuser1", "deny", "certificatename", "internal");
        String expectedResponse = "{\"errors\":[\"Invalid input values\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateFailureIfNotvalidUser() {
        UserDetails userDetail = null;
    	CertificateUser certUser = new CertificateUser("testuser1","write", "certificatename.t-mobile.com", "internal");
        String expectedResponse = "{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test(expected = Exception.class)
    public void testRemoveUserFromCertificatePolicyDataFailed() {
        CertificateUser certUser = new CertificateUser("testuser2", "deny", "certificatename.t-mobile.com", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"key\":[\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"d_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"]}");

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
    }

    @Test
    public void testRemoveUserFromCertificateConfigureLdapUserFailed() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com", "internal");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        String expectedResponse = "{\"errors\":[\"Failed to remvoe the user from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}", token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateUpdateMetadataFailed() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com", "internal");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
    	Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        String expectedResponse = "{\"errors\":[\"Failed to remove the user from the certificate. Metadata update failed\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}", token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateSuccess() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read", "internal");

    	Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String expectedResponse = "{\"messages\":[\"Group is successfully removed from certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"testgroup\"}", token)).thenReturn(groupResp);

        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        String metaDataJson = "{\"data\":{\"groups\": {\"testgroup\": \"read\"},\"app-roles\":{\"selfserviceoidcsupportrole\":\"read\"},\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.OK);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
		Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metaDataJson, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateFailureIfNotauthorized() {
    	SSLCertificateMetadataDetails certificateMetadata = null;
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read", "internal");

        String expectedResponse = "{\"errors\":[\"Access denied: No permission to remove groups from this certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(false);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateFailure400() {
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename", "testgroup","read", "internal");
        String expectedResponse = "{\"errors\":[\"Invalid input values\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateFailureIfNotvalidUser() {
        UserDetails userDetail = null;
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read", "internal");
        String expectedResponse = "{\"errors\":[\"Access denied: No permission to remove group from this certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test(expected = Exception.class)
    public void testRemoveGroupFromCertificatePolicyDataFailed() {
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","deny", "internal");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");

        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"key\":[\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"d_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"]}");

        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"testgroup\"}", token)).thenReturn(groupResp);

        String metaDataJson = "{\"data\":{\"groups\": {\"testgroup\": \"deny\"},\"app-roles\":{\"selfserviceoidcsupportrole\":\"read\"},\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.OK);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
		Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metaDataJson, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
    }

    @Test
    public void testRemoveGroupFromCertificateConfigureLdapGroupFailed() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read", "internal");
    	Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        String expectedResponse = "{\"errors\":[\"Group configuration failed.Try Again\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"testgroup\"}", token)).thenReturn(groupResp);
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNotFound);

        String metaDataJson = "{\"data\":{\"groups\": {\"testgroup\": \"read\"},\"app-roles\":{\"selfserviceoidcsupportrole\":\"read\"},\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.OK);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
		Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metaDataJson, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateUpdateMetadataFailed() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read", "internal");
    	Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
    	Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        String expectedResponse = "{\"errors\":[\"Group configuration failed. Please try again\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"testgroup\"}", token)).thenReturn(groupResp);
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNotFound);

        when(certificateUtils.getCertificateMetaData(token, "testCert@t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.getCertificateMetaData(token, "testCert@t-mobile.com", "external")).thenReturn(certificateMetadata);

        String metaDataJson = "{\"data\":{\"groups\": {\"testgroup\": \"read\"},\"app-roles\":{\"selfserviceoidcsupportrole\":\"read\"},\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.OK);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
		Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(metaDataJson, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToCertificate_success()  {
    	String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	String policies="r_cert_certmsivadasample.t-mobile.com";
    	CertificateGroup certificateGroup = new CertificateGroup("certmsivadasample.t-mobile.com","r_safe_w_vault_demo","read", "internal");
    	UserDetails userDetails = getMockUser(false);
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.arecertificateGroupInputsValid(certificateGroup)).thenReturn(true);
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
        when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"r_safe_w_vault_demo\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            String groupName="r_safe_w_vault_demo";
            when(ControllerUtil.getPoliciesAsListFromJson(obj,policies)).thenReturn(resList);
            when(ControllerUtil.arecertificateGroupInputsValid(certificateGroup)).thenReturn(true);
            when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
            when(ControllerUtil.configureLDAPGroup(groupName, policies, token)).thenReturn(responseNoContent);
            when(reqProcessor.process("/auth/ldap/groups/configure",policies, token)).thenReturn(userResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.updateSslCertificateMetadata(any(),eq(token))).thenReturn(responseNoContent);
    	ResponseEntity<String> responseEntity = sSLCertificateService.addingGroupToCertificate( token, certificateGroup, userDetails);

    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
    
    
    @Test
    public void test_addGroupToCertificate_oidc_success()  {
    	String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	String policies="r_cert_certmsivadasample.t-mobile.com";
    	CertificateGroup certificateGroup = new CertificateGroup("certmsivadasample.t-mobile.com","r_safe_w_vault_demo","read", "internal");
    	UserDetails userDetails = getMockUser(false);
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.arecertificateGroupInputsValid(certificateGroup)).thenReturn(true);
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
        when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"r_safe_w_vault_demo\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            String groupName="r_safe_w_vault_demo";
            when(ControllerUtil.getPoliciesAsListFromJson(obj,policies)).thenReturn(resList);
            when(ControllerUtil.arecertificateGroupInputsValid(certificateGroup)).thenReturn(true);
            when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
            when(ControllerUtil.configureLDAPGroup(groupName, policies, token)).thenReturn(responseNoContent);
            when(reqProcessor.process("/auth/ldap/groups/configure",policies, token)).thenReturn(userResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.updateSslCertificateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "oidc");
        List<String> policiess = new ArrayList<>();
        policiess.add("default");
        policiess.add("w_shared_mysafe02");
        policiess.add("r_shared_mysafe01");
        List<String> currentpolicies = new ArrayList<>();
        currentpolicies.add("default");
        currentpolicies.add("w_shared_mysafe01");
        currentpolicies.add("w_shared_mysafe02");
        OIDCGroup oidcGroup = new OIDCGroup("123-123-123", currentpolicies);
        when(OIDCUtil.getIdentityGroupDetails(any(), any())).thenReturn(oidcGroup);

        Response response = new Response();
        response.setHttpstatus(HttpStatus.NO_CONTENT);
        when(OIDCUtil.updateGroupPolicies(any(), any(), any(), any(), any())).thenReturn(response);

        
    	ResponseEntity<String> responseEntity = sSLCertificateService.addingGroupToCertificate( token, certificateGroup, userDetails);

    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void test_addGroupToCertificate_Badrequest() {
    	CertificateGroup certGroup = new CertificateGroup("certmsivadasample.t-mobile.com","r_safe_w_vault_demo","read", "internal");
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
    	UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);



        when(ControllerUtil.arecertificateGroupInputsValid(certGroup)).thenReturn(true);
        when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"r_vault_demo\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("r_cert_certificatename.t-mobile.com");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
            when(ControllerUtil.arecertificateGroupInputsValid(certGroup)).thenReturn(true);
            when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addingGroupToCertificate(token, certGroup, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

    }


    @Test
    public void test_addGroupToCertificate_isAdmin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String certificateName = "certsample.t-mobile.com";
        CertificateGroup certificateGroup = new CertificateGroup();
        certificateGroup.setAccess("read");
        certificateGroup.setCertificateName("certsample.t-mobile.com");
        certificateGroup.setGroupname("group1");
        certificateGroup.setCertType("internal");
        System.out.println("certgroup is :"+certificateGroup);
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
        when(ControllerUtil.arecertificateGroupInputsValid(certificateGroup)).thenReturn(true);
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");

        ResponseEntity<String> responseEntity = sSLCertificateService.addingGroupToCertificate( token, certificateGroup, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void getListOfCertificates_Succes()throws Exception{
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
         String certificateType = "internal";

         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

         when(reqProcessor.process("/sslcert", "{\"path\":\"/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);

         ResponseEntity<String> responseEntityActual = sSLCertificateService.getListOfCertificates(token, certificateType);

         assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
    }
    
    @Test
    public void transferSSLCertificate_Success() throws Exception {
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");

    	SSLCertificateMetadataDetails sslCertificateRequest = getSSLCertificateMetadataDetails();
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
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
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

        
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        
        DirectoryObjects obj =new DirectoryObjects();
        DirectoryObjectsList objList = new DirectoryObjectsList();
        DirectoryUser user = new DirectoryUser();
        user.setDisplayName("name");
        user.setUserName("213");
        Object[] values = null;
        objList.setValues(values);
        obj.setData(objList);
        
        when(directoryService.searchByUPN(anyString())).
                thenReturn(ResponseEntity.status(HttpStatus.OK).body(obj));
        
        ResponseEntity<?> transferCertResponse =
                sSLCertificateService.updateCertOwner("internal","certificatename.t-mobile.com","owneremail@test.com",userDetails);

        //Assert
        assertNotNull(transferCertResponse);        
    }

    @Test
    public void transferSSLCertificate_Success_with_SendTransfer_Email() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");

        SSLCertificateMetadataDetails sslCertificateRequest = getSSLCertificateMetadataDetails();
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
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\"," +
                "\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\"," +
                "\"notificationEmails\":\"testcert@t-mobile.com\" , " +
                "\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\"," +
                "\"dnsNames\":\"[d1.t-mbobile.com]\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\"," +
                "\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";

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


        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        when(reqProcessor.process(eq("/auth/userpass/read"),anyObject(), anyString())).thenReturn(userResponse);

        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(ControllerUtil.configureUserpassUser(eq("testusername1"),any(),eq(token))).thenReturn(idapConfigureResponse);

        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testusername1,testusername1");
        directoryUser.setGivenName("testusername1");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testusername1");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        Mockito.doNothing().when(emailUtils).sendTransferEmail(Mockito.any(),Mockito.any(),Mockito.any());
        when(directoryService.searchByUPN(anyString())).
                thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));

        ResponseEntity<?> transferCertResponse =
                sSLCertificateService.updateCertOwner("internal","certificatename.t-mobile.com","owneremail@t" +
                        "-mobile.com" ,userDetails);

        //Assert
        assertNotNull(transferCertResponse);
    }


    @Test
    public void transferSSLCertificate_Failure() throws Exception {
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

    	SSLCertificateMetadataDetails sslCertificateRequest = getSSLCertificateMetadataDetails();
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

        
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        DirectoryObjects obj =new DirectoryObjects();
        DirectoryObjectsList objList = new DirectoryObjectsList();
        DirectoryUser user = new DirectoryUser();
        user.setDisplayName("name");
        user.setUserName("213");
        Object[] values = null;
        objList.setValues(values);
        obj.setData(objList);
        
        when(directoryService.searchByUPN(anyString())).
                thenReturn(ResponseEntity.status(HttpStatus.OK).body(obj));

        ResponseEntity<?> transferCertResponse =
                sSLCertificateService.updateCertOwner("internal","certificatename.t-mobile.com","owneremail@test.com",userDetails);

        //Assert
        assertNotNull(transferCertResponse);   
        assertEquals(HttpStatus.BAD_REQUEST, transferCertResponse.getStatusCode());
    }
    
    @Test
    public void transferSSLCertificate_External_Failure() throws Exception {
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

    	SSLCertificateMetadataDetails sslCertificateRequest = getSSLCertificateMetadataDetails();
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

        
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        DirectoryObjects obj =new DirectoryObjects();
        DirectoryObjectsList objList = new DirectoryObjectsList();
        DirectoryUser user = new DirectoryUser();
        user.setDisplayName("name");
        user.setUserName("213");
        Object[] values = null;
        objList.setValues(values);
        obj.setData(objList);
        
        when(directoryService.searchByUPN(anyString())).
                thenReturn(ResponseEntity.status(HttpStatus.OK).body(obj));

        ResponseEntity<?> transferCertResponse =
                sSLCertificateService.updateCertOwner("external","certificatename.t-mobile.com","owneremail@test.com",userDetails);

        //Assert
        assertNotNull(transferCertResponse);   
        assertEquals(HttpStatus.BAD_REQUEST, transferCertResponse.getStatusCode());
    }
    
    private CertResponse getTemplateParametersResponse(){
        String enrollCSRResponse = "{\"templateParameters\":{\"templateId\":47,\"typeName\":\"META_TEMPLATE\"," +
                "\"items\":[{\"id\":86586,\"parameterId\":105,\"displayName\":\"Client ID\",\"name\":\"clientid\",\"value\":\"1\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86589,\"parameterId\":95,\"displayName\":\"Requester Name\",\"name\":\"appname\",\"value\":\"\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86590,\"parameterId\":94,\"displayName\":\"Requester email\",\"name\":\"appemail\",\"value\":\"\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86588,\"parameterId\":96,\"displayName\":\"Requester telephone number\",\"name\":\"apptelephone\",\"value\":\"\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86584,\"parameterId\":104,\"displayName\":\"The lifetime of the certificate in years\",\"name\":\"certyears\",\"value\":\"\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86599,\"parameterId\":93,\"displayName\":\"Additional emails\",\"name\":\"additionalemails\",\"value\":\"\",\"required\":false,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}}]}}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(enrollCSRResponse);
        response.setSuccess(true);

        return response;
    }

    private CertResponse putTemplateParameterResponse(){
        String enrollCSRResponse = "{\"templateParameters\":{\"templateId\":47,\"typeName\":\"META_TEMPLATE\"," +
                "\"items\":[{\"id\":86586,\"parameterId\":105,\"displayName\":\"Client ID\",\"name\":\"clientid\",\"value\":\"1\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86589,\"parameterId\":95,\"displayName\":\"Requester Name\",\"name\":\"appname\",\"value\":\"testappname\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86590,\"parameterId\":94,\"displayName\":\"Requester email\",\"name\":\"appemail\",\"value\":\"testemail@gmail.com\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86588,\"parameterId\":96,\"displayName\":\"Requester telephone number\",\"name\":\"apptelephone\",\"value\":\"111-111-1111\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86584,\"parameterId\":104,\"displayName\":\"The lifetime of the certificate in years\",\"name\":\"certyears\",\"value\":\"2\",\"required\":true,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\",\"entityId\":0,\"displayName\":\"Global policy\"}},{\"id\":86599,\"parameterId\":93,\"displayName\":\"Additional emails\",\"name\":\"additionalemails\",\"value\":\"\",\"required\":false,\"hidden\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"GLOBAL\"," +
                "\"entityId\":0,\"displayName\":\"Global policy\"}}]}}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(enrollCSRResponse);
        response.setSuccess(true);

        return response;
    }
    @Test
    public void generateSSLCertificate_Internal_MultiSAN_Success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();
        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("certificatename");
        sslCertificateRequest.setNotificationEmail("testCert@t-mobile.com");
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        String[] dnsNames = {"internal","second","third" };
        sslCertificateRequest.setDnsList(dnsNames);
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
        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
       
        Response responseObj = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataStr);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);
        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        String certType = "external";
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "external")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

	@Test
	public void testValidateApprovalStatusAndGetCertDetailsSuccess() throws Exception {
		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

		String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
				+ "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
				+ "\"certificateId\":57258,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
		SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

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
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(response);

		CertResponse findCertResponse = new CertResponse();
		findCertResponse.setHttpstatus(HttpStatus.OK);
		findCertResponse.setResponse(jsonStr2);
		findCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
				.thenReturn(findCertResponse);

		Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
		when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);

		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.OK);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);

        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);
        Map<String, Object> status = new HashMap<>();
        Map<String, Object> requestCertMap1 = new HashMap<>();
        requestCertMap1.put("conclusion", "waiting");
        when(ControllerUtil.parseJson(metaDataJson1)).thenReturn(requestCertMap1);
        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);
        Response metadataDeleteResponse = new Response();
        metadataDeleteResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        metadataDeleteResponse.setResponse(null);
        metadataDeleteResponse.setSuccess(true);
        when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);
		String certType = "external";
		String certName = "certificatename.t-mobile.com";
		when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
		when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
		assertNotNull(enrollResponse);
		assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
	}

    @Test
    public void testValidateApprovalStatusAndGetCertDetailsSuccess_Renew_Pending() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
                + "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
                + "\"certificateId\":57258,\"certificateStatus\":\"Active\","
                + "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

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
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
                .thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
                .thenReturn(findCertResponse);

        Map<String, Object> requestCertMap = new HashMap<>();
        Map<String, Object> certificates = new HashMap<>();
        certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
        certificates.put("certificateId", "123");
        certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
        certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
        certificates.put("containerName", "VenafiBin_12345");
        certificates.put("certificateStatus", "Active");
        requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);

        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response readResponse = new Response();
        readResponse.setHttpstatus(HttpStatus.OK);
        readResponse.setResponse(metaDataJson);
        readResponse.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);

        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);
        Map<String, Object> status = new HashMap<>();
        Map<String, Object> requestCertMap1 = new HashMap<>();
        requestCertMap1.put("conclusion", "rejected");
        when(ControllerUtil.parseJson(metaDataJson1)).thenReturn(requestCertMap1);
        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);
        Response metadataDeleteResponse = new Response();
        metadataDeleteResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        metadataDeleteResponse.setResponse(null);
        metadataDeleteResponse.setSuccess(true);
        when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);
        String certType = "external";
        String certName = "certificatename.t-mobile.com";
        when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
        certificateMetadata.setRequestStatus(SSLCertificateConstants.RENEW_PENDING);
        when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
        ResponseEntity<?> enrollResponse = sSLCertificateService
                .validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, enrollResponse.getStatusCode());
    }

    @Test
    public void testValidateApprovalStatusAndGetCertDetailsSuccess_Delete_MetaData_Success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
                + "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
                + "\"certificateId\":57258,\"certificateStatus\":\"Active\","
                + "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

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
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
                .thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
                .thenReturn(findCertResponse);

        Map<String, Object> requestCertMap = new HashMap<>();
        Map<String, Object> certificates = new HashMap<>();
        certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
        certificates.put("certificateId", "123");
        certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
        certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
        certificates.put("containerName", "VenafiBin_12345");
        certificates.put("certificateStatus", "Active");
        requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);

        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response readResponse = new Response();
        readResponse.setHttpstatus(HttpStatus.OK);
        readResponse.setResponse(metaDataJson);
        readResponse.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);

        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);
        Map<String, Object> status = new HashMap<>();
        Map<String, Object> requestCertMap1 = new HashMap<>();
        requestCertMap1.put("conclusion", "rejected");
        when(ControllerUtil.parseJson(metaDataJson1)).thenReturn(requestCertMap1);
        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);
        Response metadataDeleteResponse = new Response();
        metadataDeleteResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        metadataDeleteResponse.setResponse(null);
        metadataDeleteResponse.setSuccess(true);
        when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);


        when(reqProcessor.process(eq("/access/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);


        String certType = "external";
        String certName = "certificatename.t-mobile.com";
        when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
        when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
        String metadatajson =  "{\"path\":{\"sslcerts/certtest.int.delete01.t-mobile.com\":{\"policy\":\"sudo\"}," +
                "\"metadata/sslcerts/certtest.int.delete01.t-mobile.com\":{\"policy\":\"write\"}}}";
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);


        ResponseEntity<?> enrollResponse = sSLCertificateService
                .validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
        assertNotNull(enrollResponse);
    }

    @Test
    public void testValidateApprovalStatusAndGetCertDetails_With_DeleteMetaData_Failed() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
                + "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
                + "\"certificateId\":57258,\"certificateStatus\":\"Active\","
                + "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
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
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
                .thenReturn(response);
        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
                .thenReturn(findCertResponse);
        Map<String, Object> requestCertMap = new HashMap<>();
        Map<String, Object> certificates = new HashMap<>();
        certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
        certificates.put("certificateId", "123");
        certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
        certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
        certificates.put("containerName", "VenafiBin_12345");
        certificates.put("certificateStatus", "Active");
        requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response readResponse = new Response();
        readResponse.setHttpstatus(HttpStatus.OK);
        readResponse.setResponse(metaDataJson);
        readResponse.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);
        Map<String, Object> status = new HashMap<>();
        Map<String, Object> requestCertMap1 = new HashMap<>();
        requestCertMap1.put("conclusion", "rejected");
        when(ControllerUtil.parseJson(metaDataJson1)).thenReturn(requestCertMap1);
        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);
        String path="externalcerts/certificatename.t-mobile.com";
        String pathjson ="{\"path\":\"metadata/"+path+"\"}";
        Response metadataDeleteResponse = new Response();
        metadataDeleteResponse.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        metadataDeleteResponse.setResponse(null);
        metadataDeleteResponse.setSuccess(true);
        when(reqProcessor.process("/delete", pathjson, "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(metadataDeleteResponse);
        String certType = "external";
        String certName = "certificatename.t-mobile.com";
        when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
        when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
        ResponseEntity<?> enrollResponse = sSLCertificateService
                .validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }
    @Test
    public void testValidateApprovalStatusAndGetCertDetails_With_DeleteMetaPermissinData_Failed() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
                + "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
                + "\"certificateId\":57258,\"certificateStatus\":\"Active\","
                + "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
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
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
                .thenReturn(response);
        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
                .thenReturn(findCertResponse);
        Map<String, Object> requestCertMap = new HashMap<>();
        Map<String, Object> certificates = new HashMap<>();
        certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
        certificates.put("certificateId", "123");
        certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
        certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
        certificates.put("containerName", "VenafiBin_12345");
        certificates.put("certificateStatus", "Active");
        requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response readResponse = new Response();
        readResponse.setHttpstatus(HttpStatus.OK);
        readResponse.setResponse(metaDataJson);
        readResponse.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);
        Map<String, Object> status = new HashMap<>();
        Map<String, Object> requestCertMap1 = new HashMap<>();
        requestCertMap1.put("conclusion", "rejected");
        when(ControllerUtil.parseJson(metaDataJson1)).thenReturn(requestCertMap1);
        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);
        String path="externalcerts/certificatename.t-mobile.com";
        String pathjson ="{\"path\":\"metadata/"+path+"\"}";
        Response metadataDeleteResponse = new Response();
        metadataDeleteResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        metadataDeleteResponse.setResponse(null);
        metadataDeleteResponse.setSuccess(true);
        when(reqProcessor.process("/delete", pathjson, "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(metadataDeleteResponse);
        String pathjson1 ="{\"path\":\"externalcerts/certificatename.t-mobile.com\"}";
        Response metadataDeleteResponse1 = new Response();
        metadataDeleteResponse1.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        metadataDeleteResponse1.setResponse(null);
        metadataDeleteResponse1.setSuccess(true);
        when(reqProcessor.process("/delete", pathjson1, "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(metadataDeleteResponse1);
        String certType = "external";
        String certName = "certificatename.t-mobile.com";
        when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
        when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
        ResponseEntity<?> enrollResponse = sSLCertificateService
                .validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }


	@Test
	public void testValidateApprovalStatusAndGetCertDetailsFailed() throws Exception {
		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

		String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
				+ "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
				+ "\"certificateId\":57258,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body("{\"errors\":[\"Certificate may not be approved \"]}");
		SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

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
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(response);
        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);

		CertResponse findCertResponse = new CertResponse();
		findCertResponse.setHttpstatus(HttpStatus.OK);
		findCertResponse.setResponse(jsonStr2);
		findCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
				.thenReturn(findCertResponse);

		when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(null);

		String certType = "external";
		String certName = "certificatename.t-mobile.com";
		when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, enrollResponse.getStatusCode());
		assertEquals(responseEntityExpected, enrollResponse);
	}

	@Test
	public void testValidateApprovalStatusForEmptyMetadata() throws Exception {
		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"No certificate available\"]}");
		UserDetails userDetail = getMockUser(false);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

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
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(response);
        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);

        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);
		CertResponse findCertResponse = new CertResponse();
		findCertResponse.setHttpstatus(HttpStatus.OK);
		findCertResponse.setResponse(jsonStr);
		findCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
				.thenReturn(findCertResponse);

		when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(null);

		String certType = "external";
		String certName = "certificatename.t-mobile.com";
		when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(null);
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
		assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
		assertEquals(responseEntityExpected, enrollResponse);
	}

	@Test
	public void testValidateApprovalStatusForInvalidUser() {
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"errors\":[\"Access denied: No permission to access this certificate\"]}");
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		String certType = "external";
		String certName = "certificatename.t-mobile.com";
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.validateApprovalStatusAndGetCertificateDetails(certName, certType, null);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
		assertEquals(responseEntityExpected, enrollResponse);
	}

	@Test
	public void testValidateApprovalStatusForNoMetadata() throws Exception {
		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

		String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
				+ "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
				+ "\"certificateId\":57258,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
		SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

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
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(response);

		CertResponse findCertResponse = new CertResponse();
		findCertResponse.setHttpstatus(HttpStatus.OK);
		findCertResponse.setResponse(jsonStr2);
		findCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
				.thenReturn(findCertResponse);

        Response metadataDeleteResponse = new Response();
        metadataDeleteResponse.setHttpstatus(HttpStatus.OK);
        metadataDeleteResponse.setResponse(null);
        metadataDeleteResponse.setSuccess(true);
        when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);
		Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
		when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);

		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.FORBIDDEN);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);

		String certType = "external";
		String certName = "certificatename.t-mobile.com";
		when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
		when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
		assertNotNull(enrollResponse);
		assertEquals(HttpStatus.FORBIDDEN, enrollResponse.getStatusCode());
	}

	@Test
	public void testValidateApprovalStatusForMetadataUpdateFailed() throws Exception {
		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

		String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
				+ "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
				+ "\"certificateId\":57258,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"errors\":[\"Metadata update failed\"]}");
		SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

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
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(response);

		CertResponse findCertResponse = new CertResponse();
		findCertResponse.setHttpstatus(HttpStatus.OK);
		findCertResponse.setResponse(jsonStr2);
		findCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
				.thenReturn(findCertResponse);

		Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		certificates.put("previous", "122");
		requestCertMap.put("certificates", certificates);
		when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);

		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.OK);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
        String metaDataJson1 = "{\"conclusion\":\"rejected\"}" ;
        CertResponse reqStatusResponse = new CertResponse();
        reqStatusResponse.setHttpstatus(HttpStatus.OK);
        reqStatusResponse.setResponse(metaDataJson1);
        reqStatusResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/actionRequestStatus"), anyObject(), anyString(), anyString()))
                .thenReturn(reqStatusResponse);

		String certType = "external";
		String certName = "certificatename.t-mobile.com";
		when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(false);
		when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.validateApprovalStatusAndGetCertificateDetails(certName, certType, userDetail);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
		assertEquals(responseEntityExpected, enrollResponse);
	}

	@Test
	public void testValidateApprovalStatusForInvalidInputs() {
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Invalid input values\"]}");
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		String certType = "external";
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.validateApprovalStatusAndGetCertificateDetails(null, certType, userDetail);
		assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
		assertEquals(responseEntityExpected, enrollResponse);
	}
	
	@Test
	public void testdeleteCertDetailsSuccess() throws Exception {
		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

		String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
				+ "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
				+ "\"certificateId\":57258,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";		
		String jsonStr3 = "{\"data\": [ ],  \"href\": \"\",\"limit\": 50, \"offset\": 0, \"totalCount\": 1}";
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

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
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(response);

		CertResponse findCertResponse = new CertResponse();
		findCertResponse.setHttpstatus(HttpStatus.OK);
		findCertResponse.setResponse(jsonStr2);
		findCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
				.thenReturn(findCertResponse);

		Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
		when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);

		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"containerId\":123,\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.OK);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);

		String certType = "internal";
		String certName = "certificatename.t-mobile.com";
		when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
		when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
		
		CertResponse unassignCertResponse = new CertResponse();
		unassignCertResponse.setHttpstatus(HttpStatus.OK);
		unassignCertResponse.setResponse(jsonStr3);
		unassignCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certificates/services/assigned"), anyObject(), anyString(), anyString()))
				.thenReturn(unassignCertResponse);
		
		CertResponse deleteCertResponse = new CertResponse();
		deleteCertResponse.setHttpstatus(HttpStatus.NO_CONTENT);
		deleteCertResponse.setResponse(null);
		deleteCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certificates"), anyObject(), anyString(), anyString()))
				.thenReturn(deleteCertResponse);
		
		Response metadataDeleteResponse = new Response();
		metadataDeleteResponse.setHttpstatus(HttpStatus.OK);
		metadataDeleteResponse.setResponse(null);
		metadataDeleteResponse.setSuccess(true);
		when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);
		
		Response metadataPathDeleteResponse = new Response();
		metadataPathDeleteResponse.setHttpstatus(HttpStatus.OK);
		metadataPathDeleteResponse.setResponse(null);
		metadataPathDeleteResponse.setSuccess(true);
		when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataPathDeleteResponse);
		
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.deleteCertificate(token, certType, certName, userDetail);
		assertNotNull(enrollResponse);		
	}
	
	 @Test
	    public void getALLCertificate_Succes()throws Exception{
	    	 String token = "12345";

	         Response response =getMockResponse(HttpStatus.OK, true, "{  \"data\":{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
	          		+ "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
	          		+ "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
	          		+ " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
	          		+ " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
	          		+ "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
	          		+ "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
	          		+ "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}}");
	         Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");

	         token = "5PDrOhsy4ig8L3EpsJZSLAMg";
	         UserDetails user1 = new UserDetails();
	         user1.setUsername("normaluser");
	         user1.setAdmin(true);
	         user1.setClientToken(token);
	         user1.setSelfSupportToken(token);

	         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

	         when(reqProcessor.process("/sslcert", "{\"path\":\"metadata/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);

	         ResponseEntity<String> responseEntityActual = sSLCertificateService.getAllCertificates(token, "",1,0);

	         assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
	    }
	 
	 
	 
	 @Test
	    public void test_getAllCertificatesOnCertType_successfully() {
	        String certificateType = "internal";
	        UserDetails userDetails = new UserDetails();
	        userDetails.setUsername("normaluser");
	        userDetails.setAdmin(false);
	        userDetails.setClientToken(token);
	        userDetails.setSelfSupportToken(token);
	        String [] policies = {"r_users_s1", "w_users_s2", "r_shared_s3", "w_shared_s4", "r_apps_s5", "w_apps_s6", "d_apps_s7", "w_svcacct_test", "w_cert_certtest.t-mobile.com"};
	        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"shared\":[{\"s3\":\"read\"},{\"s4\":\"write\"}],\"users\":[{\"s1\":\"read\"},{\"s2\":\"write\"}],\"cert\":[{\"certtest.t-mobile.com\":\"read\"}],\"apps\":[{\"s5\":\"read\"},{\"s6\":\"write\"},{\"s7\":\"deny\"}]}");

	        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
	        when(JSONUtil.getJSON(Mockito.any())).thenReturn("{\"shared\":[{\"s3\":\"read\"},{\"s4\":\"write\"}],\"users\":[{\"s1\":\"read\"},{\"s2\":\"write\"}],\"cert\":[{\"certtest.t-mobile.com\":\"read\"}],\"apps\":[{\"s5\":\"read\"},{\"s6\":\"write\"},{\"s7\":\"deny\"}]}");
	        ResponseEntity<String> responseEntity = sSLCertificateService.getAllCertificatesOnCertType(userDetails, certificateType);
	        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	        assertEquals(responseEntityExpected, responseEntity);
	    }
	 
	 @Test	
		public void testcheckCertificateStatusFailure() throws Exception {	
			String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";	
			String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "	
					+ "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","	
					+ "\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerId\":123,"	
					+ "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";	
			SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();	
			UserDetails userDetail = getMockUser(true);	
			userDetail.setUsername("testuser1");	
			ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");	
			CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();	
			certManagerLoginRequest.setUsername("username");	
			certManagerLoginRequest.setPassword("password");	
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
			when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))	
					.thenReturn(response);	
			CertResponse findCertResponse = new CertResponse();	
			findCertResponse.setHttpstatus(HttpStatus.OK);	
			findCertResponse.setResponse(jsonStr2);	
			findCertResponse.setSuccess(true);	
			when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))	
					.thenReturn(findCertResponse);	
			Map<String, Object> requestCertMap = new HashMap<>();	
			Map<String, Object> certificates = new HashMap<>();	
			certificates.put("sortedSubjectName", "certificatename.t-mobile.com");	
			certificates.put("certificateId", "123");	
			certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");	
			certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");	
			certificates.put("containerName", "VenafiBin_12345");	
			certificates.put("certificateStatus", "Revoked");	
			requestCertMap.put("certificates", certificates);	
			when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);	
			String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";	
			Response readResponse = new Response();	
			readResponse.setHttpstatus(HttpStatus.OK);	
			readResponse.setResponse(metaDataJson);	
			readResponse.setSuccess(true);	
			when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);	
	        	
			String certType = "external";	
			String certName = "certificatename.t-mobile.com";				
			when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);	
			ResponseEntity<?> enrollResponse = sSLCertificateService	
					.checkCertificateStatus(certName, certType, userDetail);	
			assertNotNull(enrollResponse);	
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());	
		}
	 
	 @Test
	    public void onboardSSLCertificate_Success() throws Exception {
	        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
	        
	        String certResponseStr ="{\"certificates\":[{\"sortedSubjectName\":\"CN=CertificateName.t-mobile.com, C=US," +
	                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";


	        String jsonStr2 ="{\"data\":{\"response\":[{\"certType\":\"internal\",\"commonname\":\"testcert\",\"certificateStatus\":\"Active\",\"containerPath\":\"VenafiBin_12345 > Entrust Single SAN > Public Certificates\","
	        		+ "\"subjectaltname_dns\":\"testcert\"}]}}";

	       
	        Response response = new Response();
	        response.setHttpstatus(HttpStatus.OK);
	        response.setSuccess(true);
	        response.setResponse(null);

	        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
	                "certname", "password", "pembundle", false,"internal");

	        mockNclmLogin();

	        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.build()).thenReturn(httpClient1);
	        when(httpClient1.execute(any())).thenReturn(httpResponse);
	        when(httpResponse.getStatusLine()).thenReturn(statusLine);
	        when(statusLine.getStatusCode()).thenReturn(200);
	        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
	        String responseString = "teststreamdata";
	        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

	        String policyList [] = {"r_cert_certname"};
	        String bearerToken = "12345";
	        
	        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.build()).thenReturn(httpClient1);
	        when(httpClient1.execute(any())).thenReturn(httpResponse);
	        when(httpResponse.getStatusLine()).thenReturn(statusLine);
	        when(statusLine.getStatusCode()).thenReturn(200);
	        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
	        String responseDataStr = certResponseStr;
	        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseDataStr);

	        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
	        sslCertificateRequest.setCertificateName("certificatename");
	        String[] dnsNames = { };
	        sslCertificateRequest.setDnsList(dnsNames);
	        Map<String, Object> requestMap = new HashMap<>();
	        requestMap.put("access_token", "12345");
	        requestMap.put("token_type", "type");
	        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

	        CertManagerLogin certManagerLogin = new CertManagerLogin();
	        certManagerLogin.setToken_type("token type");
	        certManagerLogin.setAccess_token("1234");

	        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
	        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

	        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

	       
	        CertResponse findCertResponse = new CertResponse();
	        findCertResponse.setHttpstatus(HttpStatus.OK);
	        findCertResponse.setResponse(jsonStr2);
	        findCertResponse.setSuccess(true);
	        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

	        CertResponse response1 = new CertResponse();
	        response1.setHttpstatus(HttpStatus.NOT_FOUND);
	        response1.setResponse(jsonStr);
	        response1.setSuccess(true);	        

	        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
	        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
	        Map<String, Object> createCertPolicyMap = new HashMap<>();
	        createCertPolicyMap.put("certificateName", "CertificateName.t-mobile.com");
	        createCertPolicyMap.put("appName", "tvt");
	        createCertPolicyMap.put("certType", "internal");
	        createCertPolicyMap.put("certOwnerNtid", "testusername1");

	        String userDetailToken = "strToken";
	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
	        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
	        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
	        

	        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

	        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
	        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
	        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
	        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

	        List<String> resList = new ArrayList<>();
	        resList.add("default");
	        resList.add("r_cert_CertificateName.t-mobile.com");
	        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

	        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
	        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
	        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
	        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

	        ResponseEntity<?> enrollResponse =
	                sSLCertificateService.onboardCerts(userDetails, userDetailToken, 0, 1);
	        //Assert
	        assertNotNull(enrollResponse);
	       
	    }
	 
	 @Test
	    public void onboardSSLCertificate_FAILURE() throws Exception {
	        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
	        
	        String certResponseStr ="{\"certificates\":[{\"sortedSubjectName\":\"CN=CertificateName.t-mobile.com, C=US," +
	                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";


	        String jsonStr2 ="{\"data\":{\"response\":[]}}";

	       
	        Response response = new Response();
	        response.setHttpstatus(HttpStatus.OK);
	        response.setSuccess(true);
	        response.setResponse(null);

	        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
	                "certname", "password", "pembundle", false,"internal");

	        mockNclmLogin();

	        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.build()).thenReturn(httpClient1);
	        when(httpClient1.execute(any())).thenReturn(httpResponse);
	        when(httpResponse.getStatusLine()).thenReturn(statusLine);
	        when(statusLine.getStatusCode()).thenReturn(200);
	        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
	        String responseString = "teststreamdata";
	        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

	        String policyList [] = {"r_cert_certname"};
	        String bearerToken = "12345";
	        
	        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.build()).thenReturn(httpClient1);
	        when(httpClient1.execute(any())).thenReturn(httpResponse);
	        when(httpResponse.getStatusLine()).thenReturn(statusLine);
	        when(statusLine.getStatusCode()).thenReturn(200);
	        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
	        String responseDataStr = certResponseStr;
	        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseDataStr);

	        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
	        sslCertificateRequest.setCertificateName("certificatename");
	        String[] dnsNames = { };
	        sslCertificateRequest.setDnsList(dnsNames);
	        Map<String, Object> requestMap = new HashMap<>();
	        requestMap.put("access_token", "12345");
	        requestMap.put("token_type", "type");
	        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

	        CertManagerLogin certManagerLogin = new CertManagerLogin();
	        certManagerLogin.setToken_type("token type");
	        certManagerLogin.setAccess_token("1234");

	        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
	        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

	        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

	       
	        CertResponse findCertResponse = new CertResponse();
	        findCertResponse.setHttpstatus(HttpStatus.OK);
	        findCertResponse.setResponse(jsonStr2);
	        findCertResponse.setSuccess(true);
	        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

	        CertResponse response1 = new CertResponse();
	        response1.setHttpstatus(HttpStatus.OK);
	        response1.setResponse(jsonStr);
	        response1.setSuccess(true);	        

	        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
	        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
	        Map<String, Object> createCertPolicyMap = new HashMap<>();
	        createCertPolicyMap.put("certificateName", "CertificateName.t-mobile.com");
	        createCertPolicyMap.put("appName", "tvt");
	        createCertPolicyMap.put("certType", "internal");
	        createCertPolicyMap.put("certOwnerNtid", "testusername1");

	        String userDetailToken = "strToken";
	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
	        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
	        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
	        

	        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

	        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
	        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
	        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
	        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

	        List<String> resList = new ArrayList<>();
	        resList.add("default");
	        resList.add("r_cert_CertificateName.t-mobile.com");
	        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

	        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
	        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
	        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
	        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

	        ResponseEntity<?> enrollResponse =
	                sSLCertificateService.onboardCerts(userDetails, userDetailToken, 0, 1);
	        //Assert
	        assertNotNull(enrollResponse);
	        assertEquals(HttpStatus.NO_CONTENT, enrollResponse.getStatusCode());	
	    }

    @Test
    public void unLinkCertificate_Success_Test() throws Exception {
        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"containerId\":123,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

        Response metadataDeleteResponse = new Response();
        metadataDeleteResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        metadataDeleteResponse.setResponse(null);
        metadataDeleteResponse.setSuccess(true);
        when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);
        when(ControllerUtil.validateInputs(anyString(),anyString())).thenReturn(true);
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userInfo = new UserDetails();
        userInfo.setUsername("admin");
        userInfo.setAdmin(true);
        userInfo.setClientToken(token);
        userInfo.setSelfSupportToken(token);
        String role = "role1";
        Response roleResponse = new Response();
        roleResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        roleResponse.setSuccess(true);
        when( reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token)).thenReturn(roleResponse);

        when(reqProcessor.process(eq("/access/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);

        ResponseEntity<?> enrollResponse = sSLCertificateService.unLinkCertificate(userInfo,  "test.t-mobile" +
                ".com", "internal","Test");
        assertEquals(enrollResponse.getStatusCode(), HttpStatus.OK);

    }

    @Test
    public void unLinkCertificate_Success_Test_Failure_Normal_User() throws Exception {
        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"containerId\":123,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

        Response metadataDeleteResponse = new Response();
        metadataDeleteResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        metadataDeleteResponse.setResponse(null);
        metadataDeleteResponse.setSuccess(true);
        when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);
        when(ControllerUtil.validateInputs(anyString(),anyString())).thenReturn(true);
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userInfo = new UserDetails();
        userInfo.setUsername("normaluser");
        userInfo.setAdmin(false);
        userInfo.setClientToken(token);
        userInfo.setSelfSupportToken(token);

        String role = "role1";
        Response roleResponse = new Response();
        roleResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        roleResponse.setSuccess(true);
        when( reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token)).thenReturn(roleResponse);

        when(reqProcessor.process(eq("/access/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);

        ResponseEntity<?> enrollResponse = sSLCertificateService.unLinkCertificate(userInfo,  "test.t-mobile" +
                ".com", "internal","Test");
        assertEquals(enrollResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);

    }

	@Test
	public void testGetAllOnboardPendingCertificatesSucces() throws Exception {
		String token = "12345";

		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

		String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, ST=Washington, L=Bellevue, O=T-Mobile USA, Inc\","
				+ "\"certificateId\":57258,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"VenafiBin_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\": \"2020-11-15T21:35:59-08:00\",\"containerPath\": [{\"containerId\": 1284,\"containerName\": \"Private Certificates\",\"distance\": 2},{\"containerId\": 2213,\"containerName\": \"VenafiBin\",\"distance\": 1},{\"containerId\": 29,\"containerName\": \"VenafiBin_12345\",\"distance\": 0}],\"subjectAltName\": {\"dns\": [\"test223.t-mobile.com\"]}},"
				+ "{\"sortedSubjectName\": \"CN=certificatename1.t-mobile.com, C=US, ST=Washington, L=Bellevue, O=T-Mobile USA, Inc\", \"certificateId\":57259,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"VenafiBin_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\": \"2020-11-15T21:35:59-08:00\",\"containerPath\": [{\"containerId\": 1284,\"containerName\": \"Private Certificates\",\"distance\": 2},{\"containerId\": 2213,\"containerName\": \"VenafiBin\",\"distance\": 1},{\"containerId\": 29,\"containerName\": \"VenafiBin_12345\",\"distance\": 0}],\"subjectAltName\": {\"dns\": [\"test2234.t-mobile.com\"]}},"
				+ "{\"sortedSubjectName\": \"CN=certificatename2.t-mobile.com, C=US, ST=Washington, L=Bellevue, O=T-Mobile USA, Inc\", \"certificateId\":57260,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"VenafiBin_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\": \"2020-11-15T21:35:59-08:00\",\"containerPath\": [{\"containerId\": 1333,\"containerName\": \"Public Certificates\",\"distance\": 2},{\"containerId\": 2213,\"containerName\": \"VenafiBin\",\"distance\": 1},{\"containerId\": 99,\"containerName\": \"VenafiBin_12345\",\"distance\": 0}],\"subjectAltName\": {\"dns\": [\"test555.t-mobile.com\"]}}],"
				+ "\"next\": \"/certificates?advanced=false&containerId=&expiresAfter=&expiresBefore=&freeText=&limit=50&offset=50&sortAttribute=createdAt&sortOrder=desc&stateAssigned=false&stateCurrent=false&stateDeploying=false&stateLastDeployed=false&stateUnattached=false&stateWaiting=false\""
				+ "}";
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("access_token", "12345");
		requestMap.put("token_type", "type");
		when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
		CertManagerLogin certManagerLogin = new CertManagerLogin();
		certManagerLogin.setToken_type("token type");
		certManagerLogin.setAccess_token("1234");

		CertResponse nclmresponse = new CertResponse();
		nclmresponse.setHttpstatus(HttpStatus.OK);
		nclmresponse.setResponse(jsonStr);
		nclmresponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(nclmresponse);

		Response getCertresponse = getMockResponse(HttpStatus.OK, true,
				"{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
						+ "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
						+ "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
						+ " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
						+ " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
						+ "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
						+ "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
						+ "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
		Response internalcertResponse = getMockResponse(HttpStatus.OK, true,
				"{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");

		token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		UserDetails user1 = new UserDetails();
		user1.setUsername("normaluser");
		user1.setAdmin(true);
		user1.setClientToken(token);
		user1.setSelfSupportToken(token);
		String certificateType = "internal";

		when(reqProcessor.process(Mockito.eq("/sslcert"), Mockito.anyString(), Mockito.eq(token)))
				.thenReturn(internalcertResponse);
		when(reqProcessor.process("/sslcert", "{\"path\":\"/sslcerts/CertificateName.t-mobile.com\"}", token))
				.thenReturn(getCertresponse);

		CertResponse findCertResponse = new CertResponse();
		findCertResponse.setHttpstatus(HttpStatus.OK);
		findCertResponse.setResponse(jsonStr2);
		findCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certmanager/findAllCertificates"), anyObject(), anyString(),
				anyString())).thenReturn(findCertResponse);

		Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
		when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);

		ResponseEntity<?> enrollResponse = sSLCertificateService.getAllOnboardPendingCertificates(token,
				userDetail);
		assertNotNull(enrollResponse);
		assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
	}

	@Test
	public void testGetAllOnboardPendingCertificatesFailed() throws Exception {
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Access denied: No permission to get the pending certificates\"]}");

		ResponseEntity<?> enrollResponse = sSLCertificateService.getAllOnboardPendingCertificates(token, null);
		assertNotNull(enrollResponse);
		assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
		assertEquals(responseEntityExpected, enrollResponse);
	}

	@Test
	public void testGetAllOnboardPendingCertificatesFailedNclmDown() throws Exception {
		String token = "12345";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				"{\"errors\":[\"Your request cannot be processed now due to some technical issue. Please try after some time\"]}");
		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("access_token", "12345");
		requestMap.put("token_type", "type");
		when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
		CertManagerLogin certManagerLogin = new CertManagerLogin();
		certManagerLogin.setToken_type("token type");
		certManagerLogin.setAccess_token("1234");

		CertResponse nclmresponse = new CertResponse();
		nclmresponse.setHttpstatus(HttpStatus.OK);
		nclmresponse.setResponse(jsonStr);
		nclmresponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(null);

		ResponseEntity<?> enrollResponse = sSLCertificateService.getAllOnboardPendingCertificates(token, userDetail);
		assertNotNull(enrollResponse);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
		assertEquals(responseEntityExpected, enrollResponse);
	}

	@Test
    public void testOnboardSSLcertificateSuccess() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotBefore\":\"2020-08-06T06:38:06-07:00\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
        BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
        sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
        sslCertOnboardRequest.setNotificationEmail("test123@test.com");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificateName.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        String metaDataJson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        Response responseMetadata = new Response();
        responseMetadata.setHttpstatus(HttpStatus.FORBIDDEN);
        responseMetadata.setResponse(metaDataJson);
        responseMetadata.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseMetadata);
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testusername1,testusername1");
        directoryUser.setGivenName("testusername1");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testuser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        when(directoryService.searchByUPN(anyString())).thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser\"}",token)).thenReturn(userResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        Response responseObj = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataStr);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.onboardSSLcertificate(userDetails,token, sslCertOnboardRequest);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

	@Test
    public void testOnboardExternalSSLcertificateSuccess() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotBefore\":\"2020-08-06T06:38:06-07:00\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
        BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
        sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
        sslCertOnboardRequest.setNotificationEmail("test123@test.com");
        sslCertOnboardRequest.setCertType("external");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"external\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificateName.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "external");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        String metaDataJson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        
        Response responseMetadata = new Response();
        responseMetadata.setHttpstatus(HttpStatus.OK);
        responseMetadata.setResponse(metaDataJson);
        responseMetadata.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseMetadata);
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testusername1,testusername1");
        directoryUser.setGivenName("testusername1");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testuser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        when(directoryService.searchByUPN(anyString())).thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser\"}",token)).thenReturn(userResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        Response responseObj = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataStr);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.onboardSSLcertificate(userDetails,token, sslCertOnboardRequest);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

	@Test
    public void testOnboardExternalSSLcertificateFailedInValid() throws Exception {
        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
        BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
        sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
        String[] dnsNames = { };
        sslCertOnboardRequest.setDnsList(dnsNames);
        sslCertOnboardRequest.setCertType("test");

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.onboardSSLcertificate(userDetails,token, sslCertOnboardRequest);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }

	@Test
    public void testOnboardSSLcertificateFailedAlreadyExists() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotBefore\":\"2020-08-06T06:38:06-07:00\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
        BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
        sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
        sslCertOnboardRequest.setNotificationEmail("test123@test.com");
        sslCertOnboardRequest.setCertType("external");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);

        Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"external\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"external\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificateName.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "external");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");

        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"external\",\"certificateId\":59880,\"containerId\":99,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response responseMetadata = new Response();
        responseMetadata.setHttpstatus(HttpStatus.OK);
        responseMetadata.setResponse(metaDataJson);
        responseMetadata.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseMetadata);

        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testusername1,testusername1");
        directoryUser.setGivenName("testusername1");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testuser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        when(directoryService.searchByUPN(anyString())).thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser\"}",token)).thenReturn(userResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.onboardSSLcertificate(userDetails,token, sslCertOnboardRequest);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }

	@Test
    public void testOnboardExternalSSLcertificateFailedNotAvailable() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotBefore\":\"2020-08-06T06:38:06-07:00\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
        BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
        sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
        sslCertOnboardRequest.setNotificationEmail("test123@test.com");
        sslCertOnboardRequest.setCertType("external");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.FORBIDDEN);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(null);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"external\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"external\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificateName.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "external");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"external\",\"certificateId\":59880,\"containerId\":99,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response responseMetadata = new Response();
        responseMetadata.setHttpstatus(HttpStatus.FORBIDDEN);
        responseMetadata.setResponse(metaDataJson);
        responseMetadata.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseMetadata);
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testusername1,testusername1");
        directoryUser.setGivenName("testusername1");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testuser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        when(directoryService.searchByUPN(anyString())).thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser\"}",token)).thenReturn(userResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.onboardSSLcertificate(userDetails,token, sslCertOnboardRequest);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }

	@Test
    public void testOnboardSSLcertificatePolicyCreationFailed() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotBefore\":\"2020-08-06T06:38:06-07:00\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
        BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
        sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
        sslCertOnboardRequest.setNotificationEmail("test123@test.com");
        sslCertOnboardRequest.setCertType("external");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"external\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"external\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificateName.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "external");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"external\",\"certificateId\":59880,\"containerId\":99,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response responseMetadata = new Response();
        responseMetadata.setHttpstatus(HttpStatus.FORBIDDEN);
        responseMetadata.setResponse(metaDataJson);
        responseMetadata.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseMetadata);
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testusername1,testusername1");
        directoryUser.setGivenName("testusername1");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testuser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        when(directoryService.searchByUPN(anyString())).thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseForbidden = getMockResponse(HttpStatus.FORBIDDEN, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser\"}",token)).thenReturn(userResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseForbidden);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.onboardSSLcertificate(userDetails,token, sslCertOnboardRequest);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }

	@Test
    public void testOnboardSSLcertificateMetadataCreationFailed() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotBefore\":\"2020-08-06T06:38:06-07:00\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
        BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
        sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
        sslCertOnboardRequest.setNotificationEmail("test123@test.com");
        sslCertOnboardRequest.setCertType("external");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);

        Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"external\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"external\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificateName.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "external");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"external\",\"certificateId\":59880,\"containerId\":99,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response responseMetadata = new Response();
        responseMetadata.setHttpstatus(HttpStatus.FORBIDDEN);
        responseMetadata.setResponse(metaDataJson);
        responseMetadata.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseMetadata);
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testusername1,testusername1");
        directoryUser.setGivenName("testusername1");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testuser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        when(directoryService.searchByUPN(anyString())).thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(false);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser\"}",token)).thenReturn(userResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);

        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.onboardSSLcertificate(userDetails,token, sslCertOnboardRequest);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }

	@Test
    public void testOnboardSSLcertificateSudoPermissionFailed() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename.t-mobile.com, C=US," +
                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"Active\",\"containerName\":\"cont_12345\",\"NotBefore\":\"2020-08-06T06:38:06-07:00\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
        BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
        sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
        sslCertOnboardRequest.setNotificationEmail("test123@test.com");
        sslCertOnboardRequest.setCertType("external");
        String[] dnsNames = { };
        sslCertificateRequest.setDnsList(dnsNames);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response configureResponse = getMockResponse(HttpStatus.FORBIDDEN, false, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);
        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
        when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);
        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"external\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"external\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificateName.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "external");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"external\",\"certificateId\":59880,\"containerId\":99,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response responseMetadata = new Response();
        responseMetadata.setHttpstatus(HttpStatus.FORBIDDEN);
        responseMetadata.setResponse(metaDataJson);
        responseMetadata.setSuccess(true);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseMetadata);
        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testusername1,testusername1");
        directoryUser.setGivenName("testusername1");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testuser");
        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);
        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        when(directoryService.searchByUPN(anyString())).thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser\"}",token)).thenReturn(userResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);
        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        when(ControllerUtil.configureUserpassUser(eq("testuser"),any(),eq(token))).thenReturn(configureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<?> enrollResponse =
                sSLCertificateService.onboardSSLcertificate(userDetails,token, sslCertOnboardRequest);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }
	
	
	 @Test
	    public void updateSSLCertificate_Success_foradmin() throws Exception {
	    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
	    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
	                "ST=Washington, " +
	                "L=Bellevue, O=T-Mobile USA, Inc\"," +
	                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
	                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
	    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");

	    	SSLCertificateMetadataDetails sslCertificateRequest = getSSLCertificateMetadataDetails();
	    	 UserDetails userDetails = new UserDetails();
	         userDetails.setSelfSupportToken("tokentTest");
	         userDetails.setUsername("normaluser");
	         userDetails.setAdmin(true);
	         userDetails.setClientToken(token);
	         userDetails.setSelfSupportToken(token);
	        
	        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
	        Response response = new Response();
	        response.setHttpstatus(HttpStatus.OK);
	        response.setResponse(metaDataJson);
	        response.setSuccess(true);

	        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

	        CertResponse certResponse = new CertResponse();
	        certResponse.setHttpstatus(HttpStatus.OK);
	        certResponse.setResponse(jsonStr);
	        certResponse.setSuccess(true);

	        
	        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

	        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
	        
	        CertificateUpdateRequest certificateUpdateRequest = getCertUpdateRequest();        
	        ResponseEntity<?> transferCertResponse =
	                sSLCertificateService.updateSSLCertificate(certificateUpdateRequest, userDetails, token);

	        //Assert
	        assertNotNull(transferCertResponse);        
	    }
	 
	 @Test
	    public void updateSSLCertificate_Success_fornonadmin() throws Exception {
	    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
	    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
	                "ST=Washington, " +
	                "L=Bellevue, O=T-Mobile USA, Inc\"," +
	                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
	                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
	    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");

	    	SSLCertificateMetadataDetails sslCertificateRequest = getSSLCertificateMetadataDetails();
	    	 UserDetails userDetails = new UserDetails();
	         userDetails.setSelfSupportToken("tokentTest");
	         userDetails.setUsername("normaluser");
	         userDetails.setAdmin(false);
	         userDetails.setClientToken(token);
	         userDetails.setSelfSupportToken(token);
	         String[] policies = {"o_cert_certificatename.t-mobile.com"};
	         userDetails.setPolicies(policies);
	       
	        
	        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
	        Response response = new Response();
	        response.setHttpstatus(HttpStatus.OK);
	        response.setResponse(metaDataJson);
	        response.setSuccess(true);

	        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

	        CertResponse certResponse = new CertResponse();
	        certResponse.setHttpstatus(HttpStatus.OK);
	        certResponse.setResponse(jsonStr);
	        certResponse.setSuccess(true);

	        
	        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

	        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
	        
	        CertificateUpdateRequest certificateUpdateRequest = getCertUpdateRequest();        
	        ResponseEntity<?> transferCertResponse =
	                sSLCertificateService.updateSSLCertificate(certificateUpdateRequest, userDetails, token);

	        //Assert
	        assertNotNull(transferCertResponse);        
	    }
	 
	 private CertificateUpdateRequest getCertUpdateRequest() {
		 CertificateUpdateRequest certificateUpdateRequest = new CertificateUpdateRequest();       

		 certificateUpdateRequest.setCertificateName("certificatename.t-mobile.com");
		 certificateUpdateRequest.setProjectLeadEmail("testlead@mail.com");
		 certificateUpdateRequest.setApplicationOwnerEmail("testing@mail.com");
		 certificateUpdateRequest.setNotificationEmail("testing1@mail.com,testing2@mail.com");
		 certificateUpdateRequest.setCertType("internal");
	        return certificateUpdateRequest;
	    }
	 
	 @Test
	    public void test_onboardSingleCert() throws Exception {
		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.BAD_REQUEST);
		response.setResponse(metaDataJson);
		response.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
		ResponseEntity<String> responseOutput = sSLCertificateService.onboardSingleCert(userDetails,
				"5PDrOhsy4ig8L3EpsJZSLAMg", "internal", "certificatename.t-mobile.com", "tvt");
		assertNotNull(responseOutput);
	 }
	 
	 @Test
	    public void test_onboardSingleCert_nonadmin() throws Exception {
		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.BAD_REQUEST);
		response.setResponse(metaDataJson);
		response.setSuccess(true);
		 UserDetails userDetails1 = new UserDetails();
         userDetails1.setSelfSupportToken("tokentTest");
         userDetails1.setUsername("normaluser");
         userDetails1.setAdmin(false);
         userDetails1.setClientToken(token);
         userDetails1.setSelfSupportToken(token);
         SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
         SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
         BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
         sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
         sslCertOnboardRequest.setNotificationEmail("test123@test.com");
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
		ResponseEntity<String> responseOutput = sSLCertificateService.onboardSSLcertificate(userDetails1,
				"5PDrOhsy4ig8L3EpsJZSLAMg", sslCertOnboardRequest);
		assertNotNull(responseOutput);
	 }
	 
	 @Test
	 public void test_onboardCerts() throws Exception{
		 int from = 1;
		 int size = 2;
		    when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
	        when(httpClientBuilder.build()).thenReturn(httpClient);
	        when(httpClient.execute(any())).thenReturn(httpResponse);
		 ResponseEntity<String> responseOutput = sSLCertificateService.onboardCerts(userDetails, "5PDrOhsy4ig8L3EpsJZSLAMg", from, size);
		 assertNotNull(responseOutput);
	 }
	 
	 @Test
		public void testdeleteCertificate_fornonadmin() throws Exception {
			String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

			String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
					+ "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
					+ "\"certificateId\":57258,\"certificateStatus\":\"Active\","
					+ "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";		
			String jsonStr3 = "{\"data\": [ ],  \"href\": \"\",\"limit\": 50, \"offset\": 0, \"totalCount\": 1}";
			SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
			UserDetails userDetail = getMockUser(true);
			userDetail.setUsername("testuser1");
			userDetail.setAdmin(false);
			ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
			CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
			certManagerLoginRequest.setUsername("username");
			certManagerLoginRequest.setPassword("password");

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
			when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
					.thenReturn(response);

			CertResponse findCertResponse = new CertResponse();
			findCertResponse.setHttpstatus(HttpStatus.OK);
			findCertResponse.setResponse(jsonStr2);
			findCertResponse.setSuccess(true);
			when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
					.thenReturn(findCertResponse);

			Map<String, Object> requestCertMap = new HashMap<>();
			Map<String, Object> certificates = new HashMap<>();
			certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
			certificates.put("certificateId", "123");
			certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
			certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
			certificates.put("containerName", "VenafiBin_12345");
			certificates.put("certificateStatus", "Active");
			requestCertMap.put("certificates", certificates);
			when(ControllerUtil.parseJson(findCertResponse.getResponse())).thenReturn(requestCertMap);

			String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"containerId\":123,\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
			Response readResponse = new Response();
			readResponse.setHttpstatus(HttpStatus.OK);
			readResponse.setResponse(metaDataJson);
			readResponse.setSuccess(true);
			when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);

			String certType = "internal";
			String certName = "certificatename.t-mobile.com";
			when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
			when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
			
			CertResponse unassignCertResponse = new CertResponse();
			unassignCertResponse.setHttpstatus(HttpStatus.OK);
			unassignCertResponse.setResponse(jsonStr3);
			unassignCertResponse.setSuccess(true);
			when(reqProcessor.processCert(eq("/certificates/services/assigned"), anyObject(), anyString(), anyString()))
					.thenReturn(unassignCertResponse);
			
			CertResponse deleteCertResponse = new CertResponse();
			deleteCertResponse.setHttpstatus(HttpStatus.NO_CONTENT);
			deleteCertResponse.setResponse(null);
			deleteCertResponse.setSuccess(true);
			when(reqProcessor.processCert(eq("/certificates"), anyObject(), anyString(), anyString()))
					.thenReturn(deleteCertResponse);
			
			Response metadataDeleteResponse = new Response();
			metadataDeleteResponse.setHttpstatus(HttpStatus.OK);
			metadataDeleteResponse.setResponse(null);
			metadataDeleteResponse.setSuccess(true);
			when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);
			
			Response metadataPathDeleteResponse = new Response();
			metadataPathDeleteResponse.setHttpstatus(HttpStatus.OK);
			metadataPathDeleteResponse.setResponse(null);
			metadataPathDeleteResponse.setSuccess(true);
			when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataPathDeleteResponse);
			ReflectionTestUtils.setField(sSLCertificateService, "nclmMockEnabled", "true");
			ResponseEntity<?> enrollResponse = sSLCertificateService
					.deleteCertificate(token, certType, certName, userDetail);
			assertNotNull(enrollResponse);		
		}
	 
	 @Test
	    public void getSSLCertificate_Success_for_nonadmin()throws Exception{
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
	         user1.setAdmin(false);
	         user1.setClientToken(token);
	         user1.setSelfSupportToken(token);

	         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

	         when(reqProcessor.process("/sslcert", "{\"path\":\"metadata/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(certResponse);

	         ResponseEntity<String> responseEntityActual = sSLCertificateService.getServiceCertificates(token, user1, "",1,0,"internal");

	         assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
	    }
	 
	@Test
	public void test_onboardCertificate_failure() {
		 SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
		 sslCertificateRequest.setAppName(null);
		 int containerId = 2;
		 String tagsOwner = "string";
		ResponseEntity<String> responseEntityActual = sSLCertificateService.onboardCertificate(sslCertificateRequest, userDetails, containerId, tagsOwner);
		assertNotNull(responseEntityActual);	
	}
	
	@Test
	public void test_onboardCertificate_failure1() throws Exception {
		   String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename, C=US," +
	                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"ACTIVE\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";
		 SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
		 int containerId = 2;
		 String tagsOwner = "string";
		   String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
	        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
	        certManagerLoginRequest.setUsername("username");
	        certManagerLoginRequest.setPassword("password");
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
		 when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
		 CertResponse findCertResponse = new CertResponse();
	        findCertResponse.setHttpstatus(HttpStatus.OK);
	        findCertResponse.setResponse(jsonStr2);
	        findCertResponse.setSuccess(true);
	        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);
	        Map<String, Object> requestCertMap = new HashMap<>();
			Map<String, Object> certificates = new HashMap<>();
			certificates.put("sortedSubjectName", "certificatename");
			certificates.put("certificateId", "123");
			certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
			certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
			certificates.put("containerName", "VenafiBin_12345");
			certificates.put("certificateStatus", "ACTIVE");
			requestCertMap.put("certificates", certificates);
			when(ControllerUtil.parseJson(jsonStr2)).thenReturn(requestCertMap);

		 when(ControllerUtil.getNclmUsername()).thenReturn("username");
		 when(ControllerUtil.getNclmPassword()).thenReturn("password");
		 when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);

	        DirectoryUser directoryUser = new DirectoryUser();
	        directoryUser.setDisplayName("testusername1,testusername1");
	        directoryUser.setGivenName("testusername1");
	        directoryUser.setUserEmail("testUser@t-mobile.com");
	        directoryUser.setUserId("testuser01");
	        directoryUser.setUserName("testusername1");

	        List<DirectoryUser> persons = new ArrayList<>();
	        persons.add(directoryUser);
	        DirectoryObjects users = new DirectoryObjects();
	        DirectoryObjectsList usersList = new DirectoryObjectsList();
	        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
	        users.setData(usersList);
			String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"containerId\":123,\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";

	        when(directoryService.searchByUPN(anyString())).
	                thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));
		   when(ControllerUtil.convetToJson(anyMap())).thenReturn(metaDataJson);
		 ResponseEntity<String> responseEntityActual = sSLCertificateService.onboardCertificate(sslCertificateRequest, userDetails, containerId, tagsOwner);
		assertNotNull(responseEntityActual);	
	}
	
	@Test
	public void test_onboardCertificate_failure2() throws Exception {
		   String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename, C=US," +
	                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"ACTIVE\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";
		 SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
		 int containerId = 2;
		 String tagsOwner = "string";
		   String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
	        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
	        certManagerLoginRequest.setUsername("username");
	        certManagerLoginRequest.setPassword("password");
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
		 when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(false);
		 CertResponse findCertResponse = new CertResponse();
	        findCertResponse.setHttpstatus(HttpStatus.OK);
	        findCertResponse.setResponse(jsonStr2);
	        findCertResponse.setSuccess(true);
	        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);
	        Map<String, Object> requestCertMap = new HashMap<>();
			Map<String, Object> certificates = new HashMap<>();
			certificates.put("sortedSubjectName", "certificatename");
			certificates.put("certificateId", "123");
			certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
			certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
			certificates.put("containerName", "VenafiBin_12345");
			certificates.put("certificateStatus", "ACTIVE");
			requestCertMap.put("certificates", certificates);
			when(ControllerUtil.parseJson(jsonStr2)).thenReturn(requestCertMap);

		 when(ControllerUtil.getNclmUsername()).thenReturn("username");
		 when(ControllerUtil.getNclmPassword()).thenReturn("password");
		 when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);

	        DirectoryUser directoryUser = new DirectoryUser();
	        directoryUser.setDisplayName("testusername1,testusername1");
	        directoryUser.setGivenName("testusername1");
	        directoryUser.setUserEmail("testUser@t-mobile.com");
	        directoryUser.setUserId("testuser01");
	        directoryUser.setUserName("testusername1");

	        List<DirectoryUser> persons = new ArrayList<>();
	        persons.add(directoryUser);
	        DirectoryObjects users = new DirectoryObjects();
	        DirectoryObjectsList usersList = new DirectoryObjectsList();
	        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
	        users.setData(usersList);
			String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"external\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"containerId\":123,\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";

	        when(directoryService.searchByUPN(anyString())).
	                thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));
		   when(ControllerUtil.convetToJson(anyMap())).thenReturn(metaDataJson);
		  
			SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

			when(certificateUtils.getCertificateMetaData(anyString(), anyString(), anyString())).thenReturn(certificateMetadata);
		
			ResponseEntity<String> responseEntityActual = sSLCertificateService.onboardCertificate(sslCertificateRequest, userDetails, containerId, tagsOwner);
		assertNotNull(responseEntityActual);	
	}
	
	
	@Test
	public void testdeleteCertDetailsfailure() throws Exception {

		String certType = "internal";
		String certName = "certificatename";
		
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.deleteCertificate(token, certType, certName, userDetails);
		assertNotNull(enrollResponse);		
	}
	
	@Test
	public void testdeleteCertDetailsfailure2() throws Exception {

		String certType = "internal";
		String certName = "certificatename.t-mobile.com";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
		readResponse.setResponse("failure");
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.deleteCertificate(token, certType, certName, userDetail);
		assertNotNull(enrollResponse);		
	}
	
	@Test
	public void testdeleteCertDetailsfailure3() throws Exception {
		String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

		String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, "
				+ "ST=Washington, " + "L=Bellevue, O=T-Mobile USA, Inc\","
				+ "\"certificateId\":57258,\"certificateStatus\":\"Active\","
				+ "\"containerName\":\"cont_12345\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";		
		String jsonStr3 = "{\"data\": [ ],  \"href\": \"\",\"limit\": 50, \"offset\": 0, \"totalCount\": 1}";
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
		UserDetails userDetail = getMockUser(true);
		userDetail.setUsername("testuser1");
		ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
		CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
		certManagerLoginRequest.setUsername("username");
		certManagerLoginRequest.setPassword("password");

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
		when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString()))
				.thenReturn(response);
		ReflectionTestUtils.setField(sSLCertificateService, "nclmMockEnabled", "true");
		CertResponse findCertResponse = new CertResponse();
		findCertResponse.setHttpstatus(HttpStatus.OK);
		findCertResponse.setResponse(jsonStr2);
		findCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString()))
				.thenReturn(findCertResponse);

		Map<String, Object> requestCertMap = new HashMap<>();
		Map<String, Object> certificates = new HashMap<>();
		certificates.put("sortedSubjectName", "certificatename.t-mobile.com");
		certificates.put("certificateId", "123");
		certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
		certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
		certificates.put("containerName", "VenafiBin_12345");
		certificates.put("certificateStatus", "Active");
		requestCertMap.put("certificates", certificates);
		when(ControllerUtil.parseJson(jsonStr2)).thenReturn(requestCertMap);

		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"containerId\":123,\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"},\"groups\":{\"group1\":\"write\",\"group2\":\"read\"},\"app-roles\":{\"appRole1\":\"write\",\"testuser2\":\"read\"},\"aws-roles\":{\"awsRole1\":\"write\",\"testuser2\":\"read\"}}}";
		Response readResponse = new Response();
		readResponse.setHttpstatus(HttpStatus.OK);
		readResponse.setResponse(metaDataJson);
		readResponse.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(readResponse);

		String certType = "internal";
		String certName = "certificatename.t-mobile.com";
		when(ControllerUtil.updateMetaDataOnPath(any(), any(), eq(token))).thenReturn(true);
		when(certificateUtils.getCertificateMetaData(token, certName, certType)).thenReturn(certificateMetadata);
		
		CertResponse unassignCertResponse = new CertResponse();
		unassignCertResponse.setHttpstatus(HttpStatus.OK);
		unassignCertResponse.setResponse(jsonStr3);
		unassignCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certificates/services/assigned"), anyObject(), anyString(), anyString()))
				.thenReturn(unassignCertResponse);
		
		CertResponse deleteCertResponse = new CertResponse();
		deleteCertResponse.setHttpstatus(HttpStatus.NO_CONTENT);
		deleteCertResponse.setResponse(null);
		deleteCertResponse.setSuccess(true);
		when(reqProcessor.processCert(eq("/certificates"), anyObject(), anyString(), anyString()))
				.thenReturn(deleteCertResponse);
		
		Response metadataDeleteResponse = new Response();
		metadataDeleteResponse.setHttpstatus(HttpStatus.OK);
		metadataDeleteResponse.setResponse(null);
		metadataDeleteResponse.setSuccess(true);
		when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataDeleteResponse);
		
		Response metadataPathDeleteResponse = new Response();
		metadataPathDeleteResponse.setHttpstatus(HttpStatus.OK);
		metadataPathDeleteResponse.setResponse(null);
		metadataPathDeleteResponse.setSuccess(true);
		when(reqProcessor.process(eq("/delete"), anyObject(), anyString())).thenReturn(metadataPathDeleteResponse);
		CertificateData certificateData = new CertificateData();
		certificateData.setCertificateId(1234);
		certificateData.setExpiryDate("2020-06-26T05:10:41-07:00");
		certificateData.setCreateDate("2021-06-26T05:10:41-07:00");
		when(nclmMockUtil.getDeleteCertMockResponse(anyMap())).thenReturn(certificateData);
		Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
		when(reqProcessor.process(eq("/auth/ldap/users"), anyString(), anyString())).thenReturn(ldapConfigureResponse);
		
		when(reqProcessor.process(eq("/auth/ldap/groups"), anyString(), anyString())).thenReturn(ldapConfigureResponse);
		when(ControllerUtil.configureLDAPUser(anyString(), anyString(), anyString(), anyString())).thenReturn(ldapConfigureResponse);
		when(ControllerUtil.configureLDAPGroup(anyString(), anyString(), anyString())).thenReturn(ldapConfigureResponse);
		when(nclmMockUtil.getDeleteMockResponse()).thenReturn(deleteCertResponse);
		when(reqProcessor.process(eq("/access/delete"), anyString(), anyString())).thenReturn(ldapConfigureResponse);
		when(reqProcessor.process(eq("/auth/approle/role/read"), anyString(), anyString())).thenReturn(ldapConfigureResponse);
		when(reqProcessor.process(eq("/auth/aws/roles/delete"), anyString(), anyString())).thenReturn(ldapConfigureResponse);
		when(appRoleService.configureApprole(anyString(), anyString(), anyString())).thenReturn(ldapConfigureResponse);
		ResponseEntity<?> enrollResponse = sSLCertificateService
				.deleteCertificate(token, certType, certName, userDetail);
		assertNotNull(enrollResponse);	
	}
	
	@Test
    public void test_onboardSSLcertificate_Failure() throws Exception {
	String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
	Response response = new Response();
	response.setHttpstatus(HttpStatus.BAD_REQUEST);
	response.setResponse(metaDataJson);
	response.setSuccess(true);
	 UserDetails userDetails1 = new UserDetails();
     userDetails1.setSelfSupportToken("tokentTest");
     userDetails1.setUsername("normaluser");
     userDetails1.setAdmin(true);
     userDetails1.setClientToken(token);
     userDetails1.setSelfSupportToken(token);
     SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
     SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
     BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
     sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
     sslCertOnboardRequest.setNotificationEmail("test123@test.com");
     when(workloadDetailsService.getWorkloadDetailsByAppName(anyString())).
     thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("failure"));
	when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
	ResponseEntity<String> responseOutput = sSLCertificateService.onboardSSLcertificate(userDetails1,
			"5PDrOhsy4ig8L3EpsJZSLAMg", sslCertOnboardRequest);
	assertNotNull(responseOutput);
 }
	
	@Test
    public void test_onboardSSLcertificate_Failure1() throws Exception {
	String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
	Response response = new Response();
	response.setHttpstatus(HttpStatus.BAD_REQUEST);
	response.setResponse(metaDataJson);
	response.setSuccess(true);
	 UserDetails userDetails1 = new UserDetails();
     userDetails1.setSelfSupportToken("tokentTest");
     userDetails1.setUsername("normaluser");
     userDetails1.setAdmin(true);
     userDetails1.setClientToken(token);
     userDetails1.setSelfSupportToken(token);
     SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
     SSLCertificateOnboardRequest sslCertOnboardRequest = new SSLCertificateOnboardRequest();
     BeanUtils.copyProperties(sslCertOnboardRequest, sslCertificateRequest);
     sslCertOnboardRequest.setCertificateName("certificatename.t-mobile.com");
     sslCertOnboardRequest.setNotificationEmail("test123@test.com");
     when(workloadDetailsService.getWorkloadDetailsByAppName(anyString())).
     thenReturn(ResponseEntity.status(HttpStatus.OK).body("success"));
     DirectoryObjects users = new DirectoryObjects();
     users.setData(null);
     when(directoryService.searchByUPN(anyString())).
     thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(users));
	when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
	ResponseEntity<String> responseOutput = sSLCertificateService.onboardSSLcertificate(userDetails1,
			"5PDrOhsy4ig8L3EpsJZSLAMg", sslCertOnboardRequest);
	assertNotNull(responseOutput);
 }

	
	@Test
	public void test_onboardSingleCert_failure() throws Exception {
		ResponseEntity<String> responseOutput = sSLCertificateService.onboardSingleCert(userDetails,
				"5PDrOhsy4ig8L3EpsJZSLAMg", "internal", "certificatename", "tvt");
		assertNotNull(responseOutput);
	}
	
	@Test
	public void test_onboardSingleCert_failure1() throws Exception {
		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		Response response = new Response();
		response.setHttpstatus(HttpStatus.OK);
		response.setResponse(metaDataJson);
		response.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
		ResponseEntity<String> responseOutput = sSLCertificateService.onboardSingleCert(userDetails,
				"5PDrOhsy4ig8L3EpsJZSLAMg", "internal", "certificatename.t-mobile.com", "tvt");
		assertNotNull(responseOutput);
	}
	
	@Test
	public void test_onboardSingleCert_extrenal() throws Exception {
		String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
		 String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
		   String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename, C=US," +
	                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"ACTIVE\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}"; 
		 CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
	        certManagerLoginRequest.setUsername("username");
	        certManagerLoginRequest.setPassword("password");
	        Map<String, Object> requestMap = new HashMap<>();
	        requestMap.put("access_token", "12345");
	        requestMap.put("token_type", "type");
	        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

	        CertManagerLogin certManagerLogin = new CertManagerLogin();
	        certManagerLogin.setToken_type("token type");
	        certManagerLogin.setAccess_token("1234");

	        
	        CertResponse response1 = new CertResponse();
	        response1.setHttpstatus(HttpStatus.OK);
	        response1.setResponse(jsonStr);
	        response1.setSuccess(true);
	   	 when(ControllerUtil.getNclmUsername()).thenReturn("username");
		 when(ControllerUtil.getNclmPassword()).thenReturn("password");
		 when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response1);
		 CertResponse findCertResponse = new CertResponse();
	        findCertResponse.setHttpstatus(HttpStatus.OK);
	        findCertResponse.setResponse(jsonStr2);
	        findCertResponse.setSuccess(true);
	        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);
	        Map<String, Object> requestCertMap = new HashMap<>();
			Map<String, Object> certificates = new HashMap<>();
			certificates.put("sortedSubjectName", "certificatename");
			certificates.put("certificateId", "123");
			certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
			certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
			certificates.put("containerName", "VenafiBin_12345");
			certificates.put("certificateStatus", "ACTIVE");
			requestCertMap.put("certificates", certificates);
			when(ControllerUtil.parseJson(jsonStr2)).thenReturn(requestCertMap);
		Response response = new Response();
		response.setHttpstatus(HttpStatus.BAD_REQUEST);
		response.setResponse(metaDataJson);
		response.setSuccess(true);
		when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
		ResponseEntity<String> responseOutput = sSLCertificateService.onboardSingleCert(userDetails,
				"5PDrOhsy4ig8L3EpsJZSLAMg", "external", "certificatename.t-mobile.com", "tvt");
		assertNotNull(responseOutput);
	}
	
	@Test
	public void test_onboardCertificate_failure3() throws Exception {
		   String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename, C=US," +
	                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"ACTIVE\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";
		 SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
		 int containerId = 2;
		 String tagsOwner = "string";
		   String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
	        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
	        certManagerLoginRequest.setUsername("username");
	        certManagerLoginRequest.setPassword("password");
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
		 when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
		 CertResponse findCertResponse = new CertResponse();
	        findCertResponse.setHttpstatus(HttpStatus.OK);
	        findCertResponse.setResponse(jsonStr2);
	        findCertResponse.setSuccess(true);
	        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);
	        Map<String, Object> requestCertMap = new HashMap<>();
			Map<String, Object> certificates = new HashMap<>();
			certificates.put("sortedSubjectName", "certificatename");
			certificates.put("certificateId", "123");
			certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
			certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
			certificates.put("containerName", "VenafiBin_12345");
			certificates.put("certificateStatus", "ACTIVE");
			requestCertMap.put("certificates", certificates);
			when(ControllerUtil.parseJson(jsonStr2)).thenReturn(requestCertMap);

		 when(ControllerUtil.getNclmUsername()).thenReturn("username");
		 when(ControllerUtil.getNclmPassword()).thenReturn("password");
		 when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);

	        DirectoryUser directoryUser = new DirectoryUser();
	        directoryUser.setDisplayName("testusername1,testusername1");
	        directoryUser.setGivenName("testusername1");
	        directoryUser.setUserEmail("testUser@t-mobile.com");
	        directoryUser.setUserId("testuser01");
	        directoryUser.setUserName("testusername1");

	        List<DirectoryUser> persons = new ArrayList<>();
	        persons.add(directoryUser);
	        DirectoryObjects users = new DirectoryObjects();
	        DirectoryObjectsList usersList = new DirectoryObjectsList();
	        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
	        users.setData(usersList);
			String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"containerId\":123,\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
             Response response2 = new Response();
             response2.setHttpstatus(HttpStatus.NO_CONTENT);
             response2.setResponse("success");
			when(reqProcessor.process(eq("/write"), anyString(), anyString())).thenReturn(response2);
	        when(directoryService.searchByUPN(anyString())).
	                thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));
		   when(ControllerUtil.convetToJson(anyMap())).thenReturn(metaDataJson);
		   Response response3 = new Response();
           response3.setHttpstatus(HttpStatus.BAD_REQUEST);
           response3.setResponse("success");
		   when(reqProcessor.process(eq(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT), anyString(), anyString())).thenReturn(response3);
		 ResponseEntity<String> responseEntityActual = sSLCertificateService.onboardCertificate(sslCertificateRequest, userDetails, containerId, tagsOwner);
		assertNotNull(responseEntityActual);	
	}
	
	@Test
	public void test_onboardCertificate_failure4() throws Exception {
		   String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename, C=US," +
	                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"ACTIVE\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";
		 SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
		 int containerId = 2;
		 String tagsOwner = "string";
		   String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
	        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
	        certManagerLoginRequest.setUsername("username");
	        certManagerLoginRequest.setPassword("password");
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
		 when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
		 CertResponse findCertResponse = new CertResponse();
	        findCertResponse.setHttpstatus(HttpStatus.OK);
	        findCertResponse.setResponse(jsonStr2);
	        findCertResponse.setSuccess(true);
	        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);
	        Map<String, Object> requestCertMap = new HashMap<>();
			Map<String, Object> certificates = new HashMap<>();
			certificates.put("sortedSubjectName", "certificatename");
			certificates.put("certificateId", "123");
			certificates.put("NotAfter", "2021-08-06T06:38:06-07:00");
			certificates.put("NotBefore", "2020-08-06T06:38:06-07:00");
			certificates.put("containerName", "VenafiBin_12345");
			certificates.put("certificateStatus", "ACTIVE");
			requestCertMap.put("certificates", certificates);
			when(ControllerUtil.parseJson(jsonStr2)).thenReturn(requestCertMap);

		 when(ControllerUtil.getNclmUsername()).thenReturn("username");
		 when(ControllerUtil.getNclmPassword()).thenReturn("password");
		 when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);

	        DirectoryUser directoryUser = new DirectoryUser();
	        directoryUser.setDisplayName("testusername1,testusername1");
	        directoryUser.setGivenName("testusername1");
	        directoryUser.setUserEmail("testUser@t-mobile.com");
	        directoryUser.setUserId("testuser01");
	        directoryUser.setUserName("testusername1");

	        List<DirectoryUser> persons = new ArrayList<>();
	        persons.add(directoryUser);
	        DirectoryObjects users = new DirectoryObjects();
	        DirectoryObjectsList usersList = new DirectoryObjectsList();
	        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
	        users.setData(usersList);
			String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"certificatename.t-mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\": \"testusername1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"containerId\":123,\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
             Response response2 = new Response();
             response2.setHttpstatus(HttpStatus.NO_CONTENT);
             response2.setResponse("success");
			when(reqProcessor.process(eq("/write"), anyString(), anyString())).thenReturn(response2);
	        when(directoryService.searchByUPN(anyString())).
	                thenReturn(ResponseEntity.status(HttpStatus.OK).body(users));
		   when(ControllerUtil.convetToJson(anyMap())).thenReturn(metaDataJson);
		   Response response3 = new Response();
           response3.setHttpstatus(HttpStatus.NO_CONTENT);
           response3.setResponse("success");
		   when(reqProcessor.process(eq(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT), anyString(), anyString())).thenReturn(response3);
		 ResponseEntity<String> responseEntityActual = sSLCertificateService.onboardCertificate(sslCertificateRequest, userDetails, containerId, tagsOwner);
		assertNotNull(responseEntityActual);	
	}

    @Test
    public void test_getCertMetadata_success() {
        String metadataResponseString = "{ \"data\": {\n" +
                "  \"actionId\": 0,\n" +
                "  \"akmid\": \"123\",\n" +
                "  \"applicationName\": \"other\",\n" +
                "  \"applicationOwnerEmailId\": \"owner@company.com\",\n" +
                "  \"applicationTag\": \"Other\",\n" +
                "  \"authority\": \"company\",\n" +
                "  \"certCreatedBy\": \"user1\",\n" +
                "  \"certOwnerEmailId\": \"certowner1@company.com\",\n" +
                "  \"certOwnerNtid\": \"certowner1\",\n" +
                "  \"certType\": \"internal\",\n" +
                "  \"certificateId\": 1231,\n" +
                "  \"certificateName\": \"certtest.company.com\",\n" +
                "  \"certificateStatus\": \"Active\",\n" +
                "  \"containerId\": 456,\n" +
                "  \"containerName\": \"Other-Test\",\n" +
                "  \"createDate\": \"2021-01-06T20:23:24-08:00\",\n" +
                "  \"dnsNames\": [\n" +
                "    \"certtest.company.com\"\n" +
                "  ],\n" +
                "  \"expiryDate\": \"2022-01-06T20:23:24-08:00\",\n" +
                "  \"notificationEmails\": \"\",\n" +
                "  \"onboardFlag\": true,\n" +
                "  \"projectLeadEmailId\": \"lead@company.com\"\n" +
                "}}";
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("user1");
        certDetails.setCertificateName("certtest.company.com");
        certDetails.setCertOwnerNtid("certowner1");
        certDetails.setCertOwnerEmailId("ocertowner1@company.com");
        certDetails.setExpiryDate("2022-01-06T20:23:24-08:00");
        certDetails.setCreateDate("2021-01-06T20:23:24-08:00");
        certDetails.setNotificationEmails("");
        certDetails.setOnboardFlag(Boolean.FALSE);
        when(reqProcessor.process(eq("/read"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, metadataResponseString));
        SSLCertMetadataResponse sslCertMetadataResponse = sSLCertificateService.getCertMetadata(token, "testpath/certtest");
        assertEquals(certDetails.getCertificateName(), sslCertMetadataResponse.getSslCertificateMetadataDetails().getCertificateName());
    }

    @Test
    public void test_getCertMetadata_failed() {
        String metadataResponseString = "{ \"data1\": {\n" +
                "  \"actionId\": 0,\n" +
                "  \"akmid\": \"123\",\n" +
                "  \"applicationName\": \"other\",\n" +
                "  \"applicationOwnerEmailId\": \"owner@company.com\",\n" +
                "  \"applicationTag\": \"Other\",\n" +
                "  \"authority\": \"company\",\n" +
                "  \"certCreatedBy\": \"user1\",\n" +
                "  \"certOwnerEmailId\": \"certowner1@company.com\",\n" +
                "  \"certOwnerNtid\": \"certowner1\",\n" +
                "  \"certType\": \"internal\",\n" +
                "  \"certificateId\": 1231,\n" +
                "  \"certificateName\": \"certtest.company.com\",\n" +
                "  \"certificateStatus\": \"Active\",\n" +
                "  \"containerId\": 456,\n" +
                "  \"containerName\": \"Other-Test\",\n" +
                "  \"createDate\": \"2021-01-06T20:23:24-08:00\",\n" +
                "  \"dnsNames\": [\n" +
                "    \"certtest.company.com\"\n" +
                "  ],\n" +
                "  \"expiryDate\": \"2022-01-06T20:23:24-08:00\",\n" +
                "  \"notificationEmails\": \"\",\n" +
                "  \"onboardFlag\": true,\n" +
                "  \"projectLeadEmailId\": \"lead@company.com\"\n" +
                "}}";

        when(reqProcessor.process(eq("/read"), Mockito.any(), eq(token))).thenReturn(getMockResponse(HttpStatus.OK, true, metadataResponseString));
        SSLCertMetadataResponse sslCertMetadataResponse = sSLCertificateService.getCertMetadata(token, "testpath/certtest");
        assertNull(sslCertMetadataResponse.getSslCertificateMetadataDetails());
    }
    
    @Test
   	public void test_saveAppDetails_success() throws Exception {
   		   String jsonStr2 ="{\"certificates\":[{\"sortedSubjectName\":\"CN=certificatename, C=US," +
   	                "ST=Washington,L=Bellevue, O=T-Mobile USA, Inc\",\"certificateId\":57258,\"certificateStatus\":\"ACTIVE\",\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\",\"NotBefore\":\"2020-09-08T18:34:24-07:00\",\"subjectAltName\":{\"dns\":[\"test1.t-mobile.com\",\"test2.t-mobile.com\",\"test3.t-mobile.com\",\"certtest-dns.t-mobile.com\"]}}]}";
   		   	userDetails = new UserDetails();
   	        userDetails.setAdmin(true);
   	        userDetails.setClientToken(token);
   	        userDetails.setUsername("testusername1");
   	        userDetails.setSelfSupportToken(token);
   	        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"applicationName\": \"tvt\",\"applicationTag\":\"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
   	        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"applicationName\":\"tvt\",\"applicationTag\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
   	        
   	        Map<String, Object> createCertPolicyMap = new HashMap<>();
   	        createCertPolicyMap.put("certificateName", "CertificateName.t-mobile.com");
   	        createCertPolicyMap.put("applicationName", "tvt");
   	        createCertPolicyMap.put("certType", "internal");
   	        createCertPolicyMap.put("certOwnerNtid", "testusername1");
   	        
   	        Response response =getMockResponse(HttpStatus.OK, true, "{  \"data\":     {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
   	          		+ "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
   	          		+ "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
   	          		+ " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
   	          		+ " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
   	          		+ "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
   	          		+ "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
   	          		+ "    \"projectLeadEmailId\": \"project@email.com\"    }  }");
   	         Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");

   	         token = "5PDrOhsy4ig8L3EpsJZSLAMg";

   	         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

   	         when(reqProcessor.process("/sslcert", "{\"path\":\"metadata/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);
   	         
   	         when(reqProcessor.process("/sslcert", "{\"path\":\"metadata/externalcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);

   	         Response responseObj = new Response();
   	         response.setHttpstatus(HttpStatus.OK);
   	         response.setResponse(metaDataStr);
   	         response.setSuccess(true);

   	         when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(responseObj);

   	        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
   	        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
   	        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
   	        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
   	        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
   	        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);
   		   when(reqProcessor.process(eq(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT), anyString(), anyString())).thenReturn(responseNoContent);
   		 ResponseEntity<String> responseEntityActual = sSLCertificateService.saveAllAppDetailsForOldCerts(token, userDetails);
   		assertNotNull(responseEntityActual);	
   	}
    
}
