package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.model.UserpassUser;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class UserPassServiceTest {

    @InjectMocks
    UserPassService userPassService;

    @Mock
    RequestProcessor reqProcessor;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
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
    public void test_createUser_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserpassUser user = new UserpassUser("testuser", "testuser", "default");
        String jsonStr = "{  \"username\": \"testuser\",  \"password\": \"testuser\",  \"policies\": \"default\"}";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Username User created\"]}");

        when(JSONUtil.getJSON(user)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/create", jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.createUser(token, user);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createUser_failure_500() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserpassUser user = new UserpassUser("testuser", "testuser", "default");
        String jsonStr = "{  \"username\": \"testuser\",  \"password\": \"testuser\",  \"policies\": \"default\"}";
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"Created username failed\"]}");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Created username failed\"]}");

        when(JSONUtil.getJSON(user)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/create", jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.createUser(token, user);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_readUser_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "\"{  \\\"data\\\": { \\\"bound_cidrs\\\": [], \\\"max_ttl\\\": 0,\\\"policies\\\": [  \\\"default\\\" ], \\\"ttl\\\": 0  }}\"";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser\"}",token)).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.readUser(token,"testuser");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteUser_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserLogin user = new UserLogin();
        user.setUsername("testuser");
        String jsonStr = "{\"username\":\"testuser\",\"password\":null}";

        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Username User deleted\"]}");

        when(JSONUtil.getJSON(Mockito.any(UserLogin.class))).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/delete",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.deleteUser(token,"testuser");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteUser_failed_500() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserLogin user = new UserLogin();
        user.setUsername("testuser");
        String jsonStr = "{\"username\":\"testuser\",\"password\":null}";

        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"Delete User failed\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Delete User failed\"]}");

        when(JSONUtil.getJSON(Mockito.any(UserLogin.class))).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/delete",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.deleteUser(token,"testuser");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updatePassword_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserpassUser user = new UserpassUser("testuser", "testuser", "default");
        String jsonStr = "{  \"username\": \"testuser\",  \"password\": \"testuser\",  \"policies\": \"default\"}";

        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Password for the user updated\"]}");

        when(JSONUtil.getJSON(user)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/update",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.updatePassword(token,user);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updatePassword_failure_500() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserpassUser user = new UserpassUser("testuser", "testuser", "default");
        String jsonStr = "{  \"username\": \"testuser\",  \"password\": \"testuser\",  \"policies\": \"default\"}";

        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"Update password failed\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Update password failed\"]}");

        when(JSONUtil.getJSON(user)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/update",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.updatePassword(token,user);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_listUsers_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.OK, true, "{  \"data\": { \"keys\": [ \"safeadmin\",\"testuser1\", \"testuser2\", \"vaultadmin\"] }}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{  \"data\": { \"keys\": [ \"safeadmin\",\"testuser1\", \"testuser2\", \"vaultadmin\"] }}");

        when(reqProcessor.process("/auth/userpass/list","{}",token)).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.listUsers(token);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_login_successfully() {
        UserLogin user = new UserLogin("testuser", "testuser");
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\"}";

        String responseJson = "{\"client_token\":\"1sGWOpjPOuZezcIgxVFAm1Oh\",\"admin\":\"no\",\"access\":{},\"policies\":[\"default\"],\"lease_duration\":1800000}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(JSONUtil.getJSON(user)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/login",jsonStr,"")).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.login(user);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_login_failure_400() {
        UserLogin user = new UserLogin("testuser", "testuser");
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\"}";

        String responseJson = "{\"errors\": [\"User Authentication failed\", \"Invalid username or password. Please retry again after correcting username or password.\"]}";
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);

        when(JSONUtil.getJSON(user)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/login",jsonStr,"")).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.login(user);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_login_failure_500() {
        UserLogin user = new UserLogin("testuser", "testuser");
        String jsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\"}";

        String responseJson = "{\"errors\": [\"User Authentication failed\", \"This may be due to vault services are down or vault services are not reachable\"]}";
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson);

        when(JSONUtil.getJSON(user)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/userpass/login",jsonStr,"")).thenReturn(response);

        ResponseEntity<String> responseEntity = userPassService.login(user);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
}
