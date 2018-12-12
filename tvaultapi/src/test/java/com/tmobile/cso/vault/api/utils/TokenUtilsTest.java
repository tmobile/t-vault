package com.tmobile.cso.vault.api.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class TokenUtilsTest {

    @InjectMocks
    TokenUtils tokenUtils;

    @Mock
    RequestProcessor reqProcessor;
    
    @Mock
    Response response;
    
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
    public void test_generatePowerToken_success() {
        
        String roleName = "vault-power-user-role"; 
        String jsonStr = "{\"role_name\":\""+roleName+"\"}";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_id="123456789-0e0c-5a98-09f2-987654321";
        String secret_id="987654321s-0e0c-5a98-09f2-123456789s";
        LinkedHashMap<String, String> roleidMap = new LinkedHashMap<String, String>();
        roleidMap.put("role_id", role_id);
        LinkedHashMap<String, Object> appRoleResData = new LinkedHashMap<String, Object>();
        appRoleResData.put("data", roleidMap);
        
        String appRoleResBody = getJSON(appRoleResData);
        Response appRoleRes = getMockResponse(HttpStatus.OK, true, appRoleResBody);
        when(response.getResponse()).thenReturn(appRoleRes.toString());
        when(reqProcessor.process("/auth/approle/role/readRoleID",jsonStr,token)).thenReturn(appRoleRes);
        
        LinkedHashMap<String, Object> secidMap = new LinkedHashMap<String, Object>();
        secidMap.put("secret_id", secret_id);
        secidMap.put("secret_id_accessor", "987654321sa-0e0c-5a98-09f2-123456789sa");
        LinkedHashMap<String, Object> secidData = new LinkedHashMap<String, Object>();
        secidData.put("data", secidMap);
        String secidResBody = getJSON(secidData);
        Response secidRes = getMockResponse(HttpStatus.OK, true, secidResBody);
        
        when(reqProcessor.process("/auth/approle/secretid/lookup",jsonStr,token)).thenReturn(secidRes);
        when(response.getResponse()).thenReturn(secidRes.toString());
        
        AppRoleIdSecretId appRoleIdSecretId = new AppRoleIdSecretId(role_id, secret_id);
        jsonStr = getJSON(appRoleIdSecretId);
        when(JSONUtil.getJSON(appRoleIdSecretId)).thenReturn(jsonStr);
        
        
        LinkedHashMap<String, String> pwrTknResMap = new LinkedHashMap<String, String>();
        String expectedToken = "s.12345678";
        pwrTknResMap.put("client_token", expectedToken);
        LinkedHashMap<String, Object> pwrTknResData = new LinkedHashMap<String, Object>();
        pwrTknResData.put("auth", pwrTknResMap);
        String pwrTknResBody = getJSON(pwrTknResData);
        Response pwrTknRes = getMockResponse(HttpStatus.OK, true, pwrTknResBody );
        when(JSONUtil.getJSON((AppRoleIdSecretId)Mockito.anyObject())).thenReturn(jsonStr);
        
        when(reqProcessor.process("/auth/approle/login",jsonStr,"")).thenReturn(pwrTknRes);
        
        String generatedToken = tokenUtils.generatePowerToken(token);
        assertEquals(expectedToken, generatedToken);
    }
    
    @Test
    public void test_revokePowerToken_success() {
    	String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	Response res = getMockResponse(HttpStatus.OK, true, "");
    	when(reqProcessor.process("/auth/tvault/revoke","{}", token)).thenReturn(res);
    	tokenUtils.revokePowerToken(token);
    }
}
