package com.tmobile.cso.vault.api.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.model.OIDCEntityResponse;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class OIDCUtilTest {
	
	@Mock
    RequestProcessor reqProcessor;
    
    @Before
    public void setUp() {
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(OIDCUtil.class, "log", LogManager.getLogger(OIDCUtil.class));
        Whitebox.setInternalState(OIDCUtil.class, "reqProcessor", reqProcessor);
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
    
    @Test
    public void test_fetchMountAccessorForOidc() {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String dataOutput = "{\"data\":{\"oidc/\":{\"accessor\":\"auth_oidc_8b51f292\",\"config\":{\"default_lease_ttl\":0,\"force_no_cache\":false,\"max_lease_ttl\":0,\"token_type\":\"default-service\"},\"description\":\"\",\"external_entropy_access\":false,\"local\":false,\"options\":null,\"seal_wrap\":false,\"type\":\"oidc\",\"uuid\":\"fbd45cc4-d6b6-8b49-6d1a-d4d931345df9\"}}}";
        Response responsemock = getMockResponse(HttpStatus.OK, true, dataOutput);
        when(reqProcessor.process(eq("/sys/list"),Mockito.any(),eq(token))).thenReturn(responsemock);
        String mountAccessor = OIDCUtil.fetchMountAccessorForOidc(token);
        assertEquals("auth_oidc_8b51f292", mountAccessor);
    }
    
    @Test
    public void test_getEntityLookUpResponse() {
        String authMountResponse = "{\"data\":{\"name\":\"entity_63f119d2\",\"policies\":[\"safeadmin\"]}}";
        OIDCEntityResponse expectedResposne = new OIDCEntityResponse();
        expectedResposne.setEntityName("entity_63f119d2");
        List<String> policies = new ArrayList<>();
        policies.add("safeadmin");
        expectedResposne.setPolicies(policies);
        OIDCEntityResponse oidcEntityResponse = OIDCUtil.getEntityLookUpResponse(authMountResponse);
        assertEquals(expectedResposne, oidcEntityResponse);
    }

}
