package com.tmobile.cso.vault.api.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
//import static org.powermock.api.support.membermodification.MemberMatcher.method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeBasicDetails;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class AuthorizationUtilsTest {

    @InjectMocks
    AuthorizationUtils authorizationUtils;

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
    public void test_isAuthorized_adminuser_success() {
    	String username = "testuser1";
    	String powerToken = "self_support_token";
    	String userToken = "ordinary_client_token";
    	boolean admin = true;
    	String safename = "mysafe01";
    	String safeType = "shared";
    	String path = "shared/mysafe01";
    	String latestPoliciesStr = "s_shared_mysafe01, safeadmin";
    	String policiesTobeCheckedStr = "s_shared_mysafe01";
    	String safeOwner = "owner@someorg.com";
    	String safeOwnerId = "normaluser";
    	
    	UserDetails userDetails = createUserDetails(username, powerToken, userToken, admin);
    	Safe safeMetaData = createSafe(safename, safeType, path, safeOwner, safeOwnerId);
    	String[] latestPolicies = createLatestPolicies(latestPoliciesStr);
    	ArrayList<String> policiesTobeChecked = createPoliciesTobeChecked(policiesTobeCheckedStr);
    	
    	boolean forceCapabilityCheck = true;
    	
    	boolean actual = authorizationUtils.isAuthorized(userDetails, safeMetaData, latestPolicies, policiesTobeChecked, forceCapabilityCheck);
    	assertEquals(actual, true);
    }
    
    
    @Test
    public void test_isAuthorized_safeOwner_nocapabilitycheck_success() {
    	String username = "normaluser";
    	String powerToken = "self_support_token";
    	String userToken = "ordinary_client_token";
    	boolean admin = false;
    	String safename = "mysafe01";
    	String safeType = "shared";
    	String safeOwner = "owner@someorg.com";
    	String safeOwnerId = "normaluser";
    	String path = "shared/mysafe01";
    	String latestPoliciesStr = "s_shared_mysafe01, safeadmin";
    	String policiesTobeCheckedStr = "s_shared_mysafe01";
    	
    	UserDetails userDetails = createUserDetails(username, powerToken, userToken, admin);
    	Safe safeMetaData = createSafe(safename, safeType, path, safeOwner, safeOwnerId);
    	String[] latestPolicies = createLatestPolicies(latestPoliciesStr);
    	ArrayList<String> policiesTobeChecked = createPoliciesTobeChecked(policiesTobeCheckedStr);
    	
    	boolean forceCapabilityCheck = false;
    	
    	boolean actual = authorizationUtils.isAuthorized(userDetails, safeMetaData, latestPolicies, policiesTobeChecked, forceCapabilityCheck);
    	assertEquals(actual, true);
    }
    
    @Test
    public void test_isAuthorized_safeOwner_withcapabilitycheck_success() throws Exception {
    	String username = "normaluser";
    	String powerToken = "self_support_token";
    	String userToken = "ordinary_client_token";
    	boolean admin = false;
    	String safename = "mysafe01";
    	String safeType = "shared";
    	String safeOwner = "owner@someorg.com";
    	String safeOwnerId = "normaluser";
    	String path = "shared/mysafe01";
    	String latestPoliciesStr = "s_shared_mysafe01, safeadmin";
    	String policiesTobeCheckedStr = "s_shared_mysafe01";
    	boolean forceCapabilityCheck = true;
    	UserDetails userDetails = createUserDetails(username, powerToken, userToken, admin);
    	Safe safeMetaData = createSafe(safename, safeType, path, safeOwner, safeOwnerId);
    	String[] latestPolicies = createLatestPolicies(latestPoliciesStr);
    	ArrayList<String> policiesTobeChecked = createPoliciesTobeChecked(policiesTobeCheckedStr);
    	
    	LinkedHashMap<String, Object> capRes = new LinkedHashMap<String, Object>();
    	LinkedHashMap<String, Object> pathsMap = new LinkedHashMap<String, Object>();
    	LinkedHashMap<String, Object> capPathMap = new  LinkedHashMap<String, Object>();
    	LinkedHashMap<String, Object> capMap = new  LinkedHashMap<String, Object>();
    	capMap.put("policy", "sudo");
    	capPathMap.put("metadata/shared/s1/*", capMap);
    	capPathMap.put("shared/s1/*", capMap);
    	pathsMap.put("path", capPathMap);
    	capRes.put("name", "s_shared_s1");
    	capRes.put("rules", getJSON(pathsMap));
    	String expectedBody = getJSON(capRes);
    	Response capabilitiesResponse = getMockResponse(HttpStatus.OK, true, expectedBody);
    	
    	when(reqProcessor.process("/access","{\"accessid\":\""+"s_shared_mysafe01"+"\"}",powerToken)).thenReturn(capabilitiesResponse);
    	
    	boolean actual = authorizationUtils.isAuthorized(userDetails, safeMetaData, latestPolicies, policiesTobeChecked, forceCapabilityCheck);
    	assertEquals(actual, true);
    }
    
    private UserDetails createUserDetails(String username, String powerToken, String userToken, boolean admin) {
    	UserDetails userDetails = new UserDetails();
    	userDetails.setUsername(username);
    	userDetails.setAdmin(admin);
    	userDetails.setSelfSupportToken(powerToken);
    	userDetails.setClientToken(userToken);
    	return userDetails;
    }
    
    private Safe createSafe(String safename, String safeType, String path, String owner, String ownerid) {
    	Safe safeMetaData = new Safe();
    	SafeBasicDetails safeBasicDetails = new SafeBasicDetails();
    	safeBasicDetails.setName(safename);
    	safeBasicDetails.setType(safeType);
    	safeBasicDetails.setOwner(owner);
    	safeBasicDetails.setOwnerid(ownerid);
    	safeMetaData.setSafeBasicDetails(safeBasicDetails);
    	safeMetaData.setPath(path);
    	return safeMetaData;
    }
    
    private String[] createLatestPolicies(String policiesStr) {
    	return policiesStr.split(",");
    }
    
    private ArrayList<String> createPoliciesTobeChecked(String policiesStr) {
    	List<String> policies = Arrays.asList(policiesStr.split(","));
    	return new ArrayList<String>(policies);
    }
}
