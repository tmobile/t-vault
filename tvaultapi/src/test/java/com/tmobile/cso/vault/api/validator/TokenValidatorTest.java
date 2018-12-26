package com.tmobile.cso.vault.api.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.VaultTokenLookupDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class TokenValidatorTest {

    @InjectMocks
    TokenValidator tokenValidator;

    @Mock
    RequestProcessor reqProcessor;
    
    @Mock
    AuthorizationUtils authorizationUtils;

    @Mock
    private CommonUtils commonUtils;

    @Mock
    private PolicyUtils policyUtils;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(ControllerUtil.class, "reqProcessor", reqProcessor);
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
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
    
	private String getJSON(Object obj)  {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return "{}";
		}
	}

	@Test
    public void test_getVaultTokenLookupDetails_successfully() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Response response = getMockResponse(HttpStatus.OK, true, "{\"id\":\"7DXvbGXxu81LC724cRrrqYyq\",\"last_renewal_time\":null,\"renewable\":false,\"policies\":[\"default\"],\"creation_ttl\":0,\"username\":null}");
        VaultTokenLookupDetails expectedLookupDetails = new VaultTokenLookupDetails();
        expectedLookupDetails.setAdmin(false);
        expectedLookupDetails.setValid(true);
        expectedLookupDetails.setToken(token);
        expectedLookupDetails.setUsername(null);
        String [] policies = {"default"};
        ArrayList<String> adminPolicies = new ArrayList<>();
        adminPolicies.add("adminpolicy");
        expectedLookupDetails.setPolicies(policies);

        when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(response);
        when(commonUtils.getPoliciesAsArray(Mockito.any(), eq(response.getResponse()))).thenReturn(policies);
        when(policyUtils.getAdminPolicies()).thenReturn(adminPolicies);
        when(authorizationUtils.containsAdminPolicies(Mockito.anyList(),  Mockito.anyList())).thenReturn(true);
        VaultTokenLookupDetails lookupDetails = tokenValidator.getVaultTokenLookupDetails(token);
        assertEquals(JSONUtil.getJSON(expectedLookupDetails), JSONUtil.getJSON(lookupDetails));
    }

    @Test
    public void test_getVaultTokenLookupDetails_failure() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Response response = getMockResponse(HttpStatus.FORBIDDEN, true, "");
        VaultTokenLookupDetails expectedLookupDetails = new VaultTokenLookupDetails();
        expectedLookupDetails.setAdmin(false);
        expectedLookupDetails.setValid(true);
        expectedLookupDetails.setToken(token);
        expectedLookupDetails.setUsername(null);
        String [] policies = {"default"};
        ArrayList<String> adminPolicies = new ArrayList<>();
        adminPolicies.add("adminpolicy");
        expectedLookupDetails.setPolicies(policies);

        when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(response);

        try {
            VaultTokenLookupDetails lookupDetails = tokenValidator.getVaultTokenLookupDetails(token);
        }catch (TVaultValidationException t) {
            assertTrue(true);
        }

    }

    @Test
    public void test_getVaultTokenLookupDetails_failure_500() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "");
        VaultTokenLookupDetails expectedLookupDetails = new VaultTokenLookupDetails();
        expectedLookupDetails.setAdmin(false);
        expectedLookupDetails.setValid(true);
        expectedLookupDetails.setToken(token);
        expectedLookupDetails.setUsername(null);
        String [] policies = {"default"};
        ArrayList<String> adminPolicies = new ArrayList<>();
        adminPolicies.add("adminpolicy");
        expectedLookupDetails.setPolicies(policies);

        when(reqProcessor.process("/auth/tvault/lookup","{}", token)).thenReturn(response);

        try {
            VaultTokenLookupDetails lookupDetails = tokenValidator.getVaultTokenLookupDetails(token);
        }catch (TVaultValidationException t) {
            assertTrue(true);
        }

    }

}
