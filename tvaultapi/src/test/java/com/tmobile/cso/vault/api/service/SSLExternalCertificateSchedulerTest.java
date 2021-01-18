package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.CertificateUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class,EntityUtils.class,HttpClientBuilder.class, OIDCUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class SSLExternalCertificateSchedulerTest {

    private MockMvc mockMvc;

    @InjectMocks
    SSLExternalCertificateScheduler sslExternalCertificateScheduler;

    @Mock
    SSLCertificateService sslCertificateService;

    @Mock
    ControllerUtil controllerUtil;

    @Mock
    private RequestProcessor reqProcessor;

    @Mock
    UserDetails userDetails;

    String token;

    @Mock
    CertificateUtils certificateUtils;

    @Mock
    ObjectMapper obj;

    @Mock
    TokenUtils tokenUtils;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));

        ReflectionTestUtils.setField(sslExternalCertificateScheduler, "isSSLExtProcessScheduleEnabled", true);

        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "ssl application change Scheduler");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }

    private UserDetails getUserDetailsForScheduler() {
        ThreadLocalContext.getCurrentMap().put(LogMessage.APIURL, "processApprovedExternalCertificates");
        ThreadLocalContext.getCurrentMap().put(LogMessage.USER, "");
        String token = tokenUtils.getSelfServiceToken();
        UserDetails userDetails = new UserDetails();
        userDetails.setAdmin(false);
        userDetails.setSelfSupportToken(token);
        return userDetails;
    }

    @Test
    public void processApprovedExternalCertificates_success() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.APPROVED);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }


    @Test
    public void processApprovedExternalCertificates_without_external_certs_null() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(null);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.APPROVED);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }


    @Test
    public void processApprovedExternalCertificates_failed_with_actionid_zero() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.APPROVED);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }

    @Test
    public void processApprovedExternalCertificates_schedule_notenabled() throws Exception {
        ReflectionTestUtils.setField(sslExternalCertificateScheduler, "isSSLExtProcessScheduleEnabled", false);
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.APPROVED);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }

    @Test
    public void processApprovedExternalCertificates_failed_metadata_null() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.APPROVED);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();

    }


    @Test
    public void processApprovedExternalCertificates_failed_during_validate() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.APPROVED);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }

    @Test
    public void processApprovedExternalCertificates_success_with_rejected() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.STATUS_REJECTED);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }

    @Test
    public void processApprovedExternalCertificates_success_with_waiting() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.STATUS_WAITING);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }

    @Test
    public void processApprovedExternalCertificates_failed_updateMetaData() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.FALSE);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.STATUS_WAITING);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }

    @Test
    public void processApprovedExternalCertificates_failed() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"containerName\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.FALSE);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.STATUS_WAITING);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }

    @Test
    public void processApprovedExternalCertificates_failed_with_empty_values() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"certificateStatus\":\"\",\"\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.FALSE);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        when(sslCertificateService.getExternalCertReqStatus(anyObject())).thenReturn(SSLCertificateConstants.STATUS_WAITING);
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
    }


    @Test
    public void processApprovedExternalCertificates_failed_throwexception() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        token = userDetails.getSelfSupportToken();
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":" +
                "\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"certCreatedBy\":\"testuser1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880," +
                "\"containerId\":123,\"actionId\":123456.0,\"certificateName\":\"CertificateName.t-mobile.com\"," +
                "\"requestStatus\":\"Pending Approval\",\"certificateStatus\":\"\",\"\":" +
                "\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\"," +
                "\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\"," +
                "\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);
        when(ControllerUtil.updateMetaDataOnPath(anyString(), anyMap(), anyString())).thenReturn(Boolean.FALSE);
        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");
        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.anyString())).thenReturn(certResponse);
        doThrow(new Exception("Exception")).when(sslCertificateService).getExternalCertReqStatus(anyObject());
        String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expected);
        when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
        sslExternalCertificateScheduler.processApprovedExternalCertificates();
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
}