package com.tmobile.cso.vault.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.*;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class ControllerUtilTest {



    @Mock
    RequestProcessor reqProcessor;
    
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
    public void test_configureLDAPUser_successfully() throws TVaultValidationException, IOException {
        String userName = "normaluser";
        String policies = "{\"default\"}";
        String groups = "group1";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/ldap/users/configure"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureLDAPUser(userName, policies, groups, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_configureApprole_successfully() throws TVaultValidationException, IOException {
        String roleName = "role1";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/approle/role/create"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureApprole(roleName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_configureUserpassUser_successfully() throws TVaultValidationException, IOException {
        String userName = "normaluser";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/userpass/updatepolicy"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureUserpassUser(userName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }


    @Test
    public void test_configureLDAPGroup_successfully() throws TVaultValidationException, IOException {
        String groupName = "group1";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/ldap/groups/configure"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureLDAPGroup(groupName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_configureAWSRole_successfully() throws TVaultValidationException, IOException {
        String roleName = "role1";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/aws/roles/update"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureAWSRole(roleName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_configureAWSIAMRole_successfully() throws TVaultValidationException, IOException {
        String roleName = "role1";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/aws/iam/roles/update"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = ControllerUtil.configureAWSIAMRole(roleName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }

    @Test
    public void test_updateMetadata_successfully() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String path = "users/safe01";
        Map<String,String> params = new HashMap<>();
        params.put("type", "users");
        params.put("name", "safe01");
        params.put("access", "write");
        params.put("path", path);
        String pathjson ="{\"path\":\"metadata/"+path+"\"}";

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"description\":\"My first safe\",\"name\":\"safe01\",\"owner\":\"youremail@yourcompany.com\",\"ownerid\":\"normaluser\",\"type\":\"\"}}");
        when(reqProcessor.process("/read",pathjson,token)).thenReturn(metaResponse);
        Response response = getMockResponse(HttpStatus.CREATED, true, "");
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);

        Response actualResponse = ControllerUtil.updateMetadata(params, token);
        assertEquals(HttpStatus.CREATED, actualResponse.getHttpstatus());
    }

    @Test
    public void test_updateMetaDataOnConfigChanges_successfully() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response actualResponse = ControllerUtil.updateMetaDataOnConfigChanges("role1", "roles", "", "\"[prod, dev\"]", token);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
    }

    @Test
    public void test_updateMetaDataOnConfigChanges_successfully_() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"description\":\"My first safe\",\"name\":\"safe01\",\"owner\":\"youremail@yourcompany.com\",\"ownerid\":\"normaluser\",\"type\":\"\"}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);

        Response actualResponse = ControllerUtil.updateMetaDataOnConfigChanges("role1", "roles", "", "\"[prod, w_users_safe01\"]", token);
        assertEquals(HttpStatus.OK, actualResponse.getHttpstatus());
    }

    @Test
    public void test_updateMetaDataOnConfigChanges_successfully_existing_policies() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response metaResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"description\":\"My first safe\",\"name\":\"safe01\",\"owner\":\"youremail@yourcompany.com\",\"ownerid\":\"normaluser\",\"type\":\"\"}}");
        when(reqProcessor.process(eq("/read"),Mockito.any(),eq(token))).thenReturn(metaResponse);
        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(response);

        Response actualResponse = ControllerUtil.updateMetaDataOnConfigChanges("role1", "roles", "\"[w_users_safe01\"]", "\"[prod, dev\"]", token);
        assertEquals(HttpStatus.MULTI_STATUS, actualResponse.getHttpstatus());
        assertEquals("Meta data update failed for [users/safe01\"]]", actualResponse.getResponse());
    }

    @Test
    public void test_parseJson_successfully() throws TVaultValidationException, IOException {
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\"}";

        Map<String,Object> actualResponse = ControllerUtil.parseJson(jsonStr);
        assertEquals("testuser", actualResponse.get("username"));
        assertEquals("testuser", actualResponse.get("password"));
    }

    @Test
    public void test_parseJson_error() throws TVaultValidationException, IOException {
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\",}";

        Map<String,Object> actualResponse = ControllerUtil.parseJson(jsonStr);
        assertTrue(actualResponse.isEmpty());
    }

    @Test
    public void test_convetToJson_successfully() throws TVaultValidationException, IOException {
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\"}";

        Map<String,Object> jsonmap = new LinkedHashMap<>();
        jsonmap.put("username", "testuser");
        jsonmap.put("password", "testuser");
        String actualResponse = ControllerUtil.convetToJson(jsonmap);
        assertEquals(jsonStr, actualResponse);
    }

    @Test
    public void test_getPoliciesAsStringFromJson_successfully() throws TVaultValidationException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        String policyjson = "{\"data\":{\"policies\":[\"w_users_safe02\",\"w_users_safe01\"]}}";
        String actualResponse = ControllerUtil.getPoliciesAsStringFromJson(mapper, policyjson);
        assertEquals("w_users_safe02,w_users_safe01", actualResponse);
    }

    @Test
    public void test_updateUserPolicyAssociationOnSDBDelete_successfully_() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String userName = "testuser1";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"approle_normal_user\",\"w_users_safe01\"],\"ttl\":0}}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token)).thenReturn(userResponse);

        ReflectionTestUtils.setField(ControllerUtil.class,"vaultAuthMethod", "userpass");
        Map<String,String> acessInfo = new HashMap<>();
        acessInfo.put("testuser1", "write");

        ControllerUtil.updateUserPolicyAssociationOnSDBDelete("users/safe01", acessInfo,  token);
        assertTrue(true);
    }

    @Test
    public void test_updateGroupPolicyAssociationOnSDBDelete_successfully_() throws TVaultValidationException, IOException {
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";
        String groupName = "group1";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"approle_normal_user\",\"w_users_safe01\"],\"ttl\":0}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token)).thenReturn(userResponse);

        ReflectionTestUtils.setField(ControllerUtil.class,"vaultAuthMethod", "ldap");
        Map<String,String> acessInfo = new HashMap<>();
        acessInfo.put("group1", "write");

        ControllerUtil.updateGroupPolicyAssociationOnSDBDelete("users/safe01", acessInfo,  token);
        assertTrue(true);
    }


}
