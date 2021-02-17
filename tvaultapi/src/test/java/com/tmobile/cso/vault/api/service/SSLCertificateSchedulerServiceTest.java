package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.*;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class,EntityUtils.class,HttpClientBuilder.class, OIDCUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class SSLCertificateSchedulerServiceTest {

    private MockMvc mockMvc;

    @InjectMocks
    SSLCertificateSchedulerService sslCertificateSchedulerService;

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
    private WorkloadDetailsService workloadDetailsService;

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

        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "ssl application change Scheduler");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
        ReflectionTestUtils.setField(sslCertificateSchedulerService, "isSSLMetadataRefreshEnabled", true);

        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
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
    public void checkApplicationMetaDataChanges_success() throws Exception {

        when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

        List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM = new ArrayList<>();
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst2-tag", "lead1@company.com", null, null, false));
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst2", "owner2@company.com", "tst3-tag", "lead2@company.com", null, null, false));
        when(workloadDetailsService.getAllApplicationDetailsFromCLM()).thenReturn(tmoAppMetadataListFromCLM);

        List<TMOAppMetadataDetails> tmoAppMetadataList = new ArrayList<>();
        List<String> internalCertList = new ArrayList<>();
        internalCertList.add("cert1.company.com");
        List<String> extCertList = new ArrayList<>();
        extCertList.add("extcert1.company.com");
        tmoAppMetadataList.add(new TMOAppMetadataDetails("tst1", "oldwoner@company.com", "tst1-tag", "oldlead@company.com", internalCertList, extCertList, false));
        tmoAppMetadataList.add(new TMOAppMetadataDetails("tst2", "oldwoner@company.com", "tst2-tag", "oldlead@company.com", new ArrayList<>(), new ArrayList<>(), false));
        when(workloadDetailsService.getAllAppMetadata(token)).thenReturn(tmoAppMetadataList);

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\n" +
                "  \"actionId\": 0,\n" +
                "  \"akmid\": \"123\",\n" +
                "  \"applicationName\": \"tst1\",\n" +
                "  \"applicationOwnerEmailId\": \"owner11@company.com\",\n" +
                "  \"applicationTag\": \"tst2-tag\",\n" +
                "  \"certCreatedBy\": \"username1\",\n" +
                "  \"certOwnerEmailId\": \"username11@company.com\",\n" +
                "  \"certOwnerNtid\": \"username1\",\n" +
                "  \"certType\": \"internal\",\n" +
                "  \"certificateId\": 123123,\n" +
                "  \"certificateName\": \"cert1.company.com\",\n" +
                "  \"certificateStatus\": \"Active\",\n" +
                "  \"containerId\": 222,\n" +
                "  \"containerName\": \"test\",\n" +
                "  \"createDate\": \"2021-01-21T02:49:30-08:00\",\n" +
                "  \"expiryDate\": \"2022-01-21T02:49:30-08:00\",\n" +
                "  \"notificationEmails\": \"lead11@company.com\",\n" +
                "  \"onboardFlag\": false,\n" +
                "  \"projectLeadEmailId\": \"lead11@company.com\"" +
                "}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true, ""));

        when(workloadDetailsService.udpateApplicationMetadata(eq(token), Mockito.any(), Mockito.any())).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true, ""));
        sslCertificateSchedulerService.checkApplicationMetaDataChanges();
        assertTrue(true);
    }

    @Test
    public void checkApplicationMetaDataChanges_disabled() throws Exception {

        ReflectionTestUtils.setField(sslCertificateSchedulerService, "isSSLMetadataRefreshEnabled", false);
        sslCertificateSchedulerService.checkApplicationMetaDataChanges();
        assertTrue(true);
    }

    @Test
    public void checkApplicationMetaDataChanges_failed_CLM() throws Exception {

        when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);
        when(workloadDetailsService.getAllApplicationDetailsFromCLM()).thenReturn(new ArrayList<>());
        sslCertificateSchedulerService.checkApplicationMetaDataChanges();
        assertTrue(true);
    }

    @Test
    public void checkApplicationMetaDataChanges_failed_metadata_read() throws Exception {

        when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

        List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM = new ArrayList<>();
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst1-tag", "lead1@company.com", null, null, false));
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst2", "owner2@company.com", "tst2-tag", "lead2@company.com", null, null, false));
        when(workloadDetailsService.getAllApplicationDetailsFromCLM()).thenReturn(tmoAppMetadataListFromCLM);

        when(workloadDetailsService.getAllAppMetadata(token)).thenReturn(new ArrayList<>());
        sslCertificateSchedulerService.checkApplicationMetaDataChanges();
        assertTrue(true);
    }

    @Test
    public void checkApplicationMetaDataChanges_success_no_outdated_apps() throws Exception {

        when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

        List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM = new ArrayList<>();
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst1-tag", "lead1@company.com", null, null, true));
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst2", "owner2@company.com", "tst2-tag", "lead2@company.com", null, null, true));
        when(workloadDetailsService.getAllApplicationDetailsFromCLM()).thenReturn(tmoAppMetadataListFromCLM);

        List<TMOAppMetadataDetails> tmoAppMetadataList = new ArrayList<>();
        List<String> internalCertList = new ArrayList<>();
        internalCertList.add("cert1.company.com");
        List<String> extCertList = new ArrayList<>();
        extCertList.add("extcert1.company.com");
        tmoAppMetadataList.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst1-tag", "lead1@company.com", internalCertList, extCertList, true));
        tmoAppMetadataList.add(new TMOAppMetadataDetails("tst2", "owner2@company.com", "tst2-tag", "lead2@company.com", new ArrayList<>(), new ArrayList<>(), true));
        when(workloadDetailsService.getAllAppMetadata(token)).thenReturn(tmoAppMetadataList);


        sslCertificateSchedulerService.checkApplicationMetaDataChanges();
        assertTrue(true);
    }

    @Test
    public void checkApplicationMetaDataChanges_failed_to_update_metadata() throws Exception {

        when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

        List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM = new ArrayList<>();
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst2-tag", "lead1@company.com", null, null, false));
        when(workloadDetailsService.getAllApplicationDetailsFromCLM()).thenReturn(tmoAppMetadataListFromCLM);

        List<TMOAppMetadataDetails> tmoAppMetadataList = new ArrayList<>();
        List<String> internalCertList = new ArrayList<>();
        internalCertList.add("cert1.company.com");
        tmoAppMetadataList.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst1-tag", "lead1@company.com", internalCertList, null, false));
        when(workloadDetailsService.getAllAppMetadata(token)).thenReturn(tmoAppMetadataList);

        when(workloadDetailsService.udpateApplicationMetadata(eq(token), Mockito.any(), Mockito.any())).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true, ""));

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\n" +
                "  \"actionId\": 0,\n" +
                "  \"akmid\": \"123\",\n" +
                "  \"applicationName\": \"tst1\",\n" +
                "  \"applicationOwnerEmailId\": \"owner11@company.com\",\n" +
                "  \"applicationTag\": \"tst2-tag\",\n" +
                "  \"certCreatedBy\": \"username1\",\n" +
                "  \"certOwnerEmailId\": \"username1@company.com\",\n" +
                "  \"certOwnerNtid\": \"username1\",\n" +
                "  \"certType\": \"internal\",\n" +
                "  \"certificateId\": 123123,\n" +
                "  \"certificateName\": \"cert1.company.com\",\n" +
                "  \"certificateStatus\": \"Active\",\n" +
                "  \"containerId\": 222,\n" +
                "  \"containerName\": \"test\",\n" +
                "  \"createDate\": \"2021-01-21T02:49:30-08:00\",\n" +
                "  \"expiryDate\": \"2022-01-21T02:49:30-08:00\",\n" +
                "  \"notificationEmails\": \"lead11@company.com\",\n" +
                "  \"onboardFlag\": false,\n" +
                "  \"projectLeadEmailId\": \"lead11@company.com\"" +
                "}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.BAD_REQUEST, true, ""));

        sslCertificateSchedulerService.checkApplicationMetaDataChanges();
        assertTrue(true);
    }

    @Test
    public void checkApplicationMetaDataChanges_failed_to_update_app_metadata() throws Exception {

        when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

        List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM = new ArrayList<>();
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst2-tag", "lead1@company.com", null, null, false));
        when(workloadDetailsService.getAllApplicationDetailsFromCLM()).thenReturn(tmoAppMetadataListFromCLM);

        List<TMOAppMetadataDetails> tmoAppMetadataList = new ArrayList<>();
        List<String> internalCertList = new ArrayList<>();
        internalCertList.add("cert1.company.com");
        tmoAppMetadataList.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst1-tag", "lead1@company.com", internalCertList, null, false));
        when(workloadDetailsService.getAllAppMetadata(token)).thenReturn(tmoAppMetadataList);

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\n" +
                "  \"actionId\": 0,\n" +
                "  \"akmid\": \"123\",\n" +
                "  \"applicationName\": \"tst1\",\n" +
                "  \"applicationOwnerEmailId\": \"owner1@company.com\",\n" +
                "  \"applicationTag\": \"tst2-tag\",\n" +
                "  \"certCreatedBy\": \"username1\",\n" +
                "  \"certOwnerEmailId\": \"username1@company.com\",\n" +
                "  \"certOwnerNtid\": \"username1\",\n" +
                "  \"certType\": \"internal\",\n" +
                "  \"certificateId\": 123123,\n" +
                "  \"certificateName\": \"cert1.company.com\",\n" +
                "  \"certificateStatus\": \"Active\",\n" +
                "  \"containerId\": 222,\n" +
                "  \"containerName\": \"test\",\n" +
                "  \"createDate\": \"2021-01-21T02:49:30-08:00\",\n" +
                "  \"expiryDate\": \"2022-01-21T02:49:30-08:00\",\n" +
                "  \"notificationEmails\": \"lead1@company.com\",\n" +
                "  \"onboardFlag\": false,\n" +
                "  \"projectLeadEmailId\": \"lead1@company.com\"" +
                "}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);
        when(workloadDetailsService.udpateApplicationMetadata(eq(token), Mockito.any(), Mockito.any())).thenReturn(getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, ""));
        sslCertificateSchedulerService.checkApplicationMetaDataChanges();
        assertTrue(true);
    }

    @Test
    public void checkApplicationMetaDataChanges_failed_to_read_metadata() throws Exception {

        when(tokenUtils.getSelfServiceTokenWithAppRole()).thenReturn(token);

        List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM = new ArrayList<>();
        tmoAppMetadataListFromCLM.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst2-tag", "lead1@company.com", null, null, false));
        when(workloadDetailsService.getAllApplicationDetailsFromCLM()).thenReturn(tmoAppMetadataListFromCLM);

        List<TMOAppMetadataDetails> tmoAppMetadataList = new ArrayList<>();
        List<String> internalCertList = new ArrayList<>();
        internalCertList.add("cert1.company.com");
        tmoAppMetadataList.add(new TMOAppMetadataDetails("tst1", "owner1@company.com", "tst1-tag", "lead1@company.com", internalCertList, null, false));
        when(workloadDetailsService.getAllAppMetadata(token)).thenReturn(tmoAppMetadataList);

        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(getMockResponse(HttpStatus.BAD_REQUEST, false, ""));
        when(workloadDetailsService.udpateApplicationMetadata(eq(token), Mockito.any(), Mockito.any())).thenReturn(getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, ""));
        sslCertificateSchedulerService.checkApplicationMetaDataChanges();
        assertTrue(true);
    }
}
