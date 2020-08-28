package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class, EntityUtils.class, HttpClientBuilder.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class WorkloadDetailsServiceTest {

    private MockMvc mockMvc;

    @InjectMocks
    WorkloadDetailsService workloadDetailsService;

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

    @Before
    public void setUp() {
        mockStatic(ControllerUtil.class);
        mockStatic(JSONUtil.class);
        mockStatic(HttpClientBuilder.class);
        mockStatic(EntityUtils.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/ad");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        when(vaultAuthService.lookup(anyString())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
    }

    @Test
    public void test_getWorkloadDetails_success() throws Exception {
        String responseStr = "{\"items\": [{\"spec\": {\"id\": \"aac\",\"summary\": \"app1\"}}]}";
        String workloadResponse = "[{\"appName\":\"Other\",\"appTag\":\"Other\",\"appID\":\"oth\"},{\"appName\":\"app1\",\"appTag\":\"aac\",\"appID\":\"aac\"}]";

        ReflectionTestUtils.setField(workloadDetailsService, "workloadEndpoint", "http://appdetails.com");
        when(ControllerUtil.getCwmToken()).thenReturn("dG9rZW4=");
        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(any())).thenReturn(httpResponse);

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        InputStream inputStream = new ByteArrayInputStream(responseStr.getBytes());
        when(mockHttpEntity.getContent()).thenReturn(inputStream);
        when(JSONUtil.getJSON(anyList())).thenReturn(workloadResponse);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("[{\"appName\":\"Other\",\"appTag\":\"Other\",\"appID\":\"oth\"},{\"appName\":\"app1\",\"appTag\":\"aac\",\"appID\":\"aac\"}]");

        ResponseEntity<String> responseEntityActual = workloadDetailsService.getWorkloadDetails(token, userDetails);
        assertEquals(responseEntityExpected.getStatusCode(), responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_getWorkloadDetails_failed() throws Exception {
        String workloadResponse = "[{\"appName\":\"Other\",\"appTag\":\"Other\",\"appID\":\"oth\"}]";

        ReflectionTestUtils.setField(workloadDetailsService, "workloadEndpoint", "http://appdetails.com");
        when(ControllerUtil.getCwmToken()).thenReturn("dG9rZW4=");
        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(any())).thenReturn(httpResponse);

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        when(JSONUtil.getJSON(anyList())).thenReturn(workloadResponse);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("[{\"appName\":\"Other\",\"appTag\":\"Other\",\"appID\":\"oth\"}]");

        ResponseEntity<String> responseEntityActual = workloadDetailsService.getWorkloadDetails(token, userDetails);
        assertEquals(responseEntityExpected.getStatusCode(), responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_getWorkloadDetails_failed_token() throws Exception {
        String workloadResponse = "[{\"appName\":\"Other\",\"appTag\":\"Other\",\"appID\":\"oth\"}]";

        ReflectionTestUtils.setField(workloadDetailsService, "workloadEndpoint", "http://appdetails.com");
        when(ControllerUtil.getCwmToken()).thenReturn("");
        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(any())).thenReturn(httpResponse);

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        when(JSONUtil.getJSON(anyList())).thenReturn(workloadResponse);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("[{\"appName\":\"Other\",\"appTag\":\"Other\",\"appID\":\"oth\"}]");

        ResponseEntity<String> responseEntityActual = workloadDetailsService.getWorkloadDetails(token, userDetails);
        assertEquals(responseEntityExpected.getStatusCode(), responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

}
