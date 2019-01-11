package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.LDAPGroup;
import com.tmobile.cso.vault.api.model.LDAPUser;
import com.tmobile.cso.vault.api.model.UserLogin;
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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class LDAPAuthServiceTest {

    @InjectMocks
    LDAPAuthService ldapAuthService;

    @Mock
    RequestProcessor reqProcessor;

    @Before
    public void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
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
        if (expectedBody != "") {
            response.setResponse(expectedBody);
        }
        return response;
    }

    @Test
    public void test_configureLdapUser_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String jsonStr = "{ \"policies\": \"admin,default\",\"username\": \"safeadmin\"}";
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP user configured\"]}");
        LDAPUser ldapUser = new LDAPUser("safeadmin", "admin,default");
        when(JSONUtil.getJSON(ldapUser)).thenReturn(jsonStr);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(responseOk);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"safeadmin\"}",token)).thenReturn(responseNotFound);
        when(reqProcessor.process("/auth/ldap/users/configure",jsonStr,token)).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = ldapAuthService.configureLdapUser(token, ldapUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_configureLdapUser_existingUser_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String jsonStr = "{ \"policies\": \"admin,default\",\"username\": \"safeadmin\"}";
        Response responseUser = getMockResponse(HttpStatus.OK, true, "{  \"data\": {\"groups\": \"\",\"policies\": [ \"admin\", \"default\" ]  }}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP user configured\"]}");
        LDAPUser ldapUser = new LDAPUser("safeadmin", "admin,default");
        when(JSONUtil.getJSON(ldapUser)).thenReturn(jsonStr);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(responseOk);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"safeadmin\"}",token)).thenReturn(responseUser);
        when(reqProcessor.process("/auth/ldap/users/configure",jsonStr,token)).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = ldapAuthService.configureLdapUser(token, ldapUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_configureLdapUser_failure_500() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String jsonStr = "{ \"policies\": \"admin,default\",\"username\": \"safeadmin\"}";
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        Response responseFailure = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, "{\"errors\":[\"Configuring of LDAP user failed\"]}");
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Configuring of LDAP user failed\"]}");
        LDAPUser ldapUser = new LDAPUser("safeadmin", "admin,default");
        when(JSONUtil.getJSON(ldapUser)).thenReturn(jsonStr);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(responseOk);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"safeadmin\"}",token)).thenReturn(responseNotFound);
        when(reqProcessor.process("/auth/ldap/users/configure",jsonStr,token)).thenReturn(responseFailure);

        ResponseEntity<String> responseEntity = ldapAuthService.configureLdapUser(token, ldapUser);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
   @Test
    public void test_listLdapUsers_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\": [\"safeadmin\",\"vaultadmin\"]]");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"keys\": [\"safeadmin\",\"vaultadmin\"]]");
        when(reqProcessor.process("/auth/ldap/users/list","{}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.listLdapUsers(token);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_fetchLdapUser_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response responseUser = getMockResponse(HttpStatus.OK, true, "{  \"data\": {\"groups\": \"\",\"policies\": [ \"admin\", \"default\" ]  }}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{  \"data\": {\"groups\": \"\",\"policies\": [ \"admin\", \"default\" ]  }}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"safeadmin\"}",token)).thenReturn(responseUser);
        ResponseEntity<String> responseEntity = ldapAuthService.fetchLdapUser(token, "safeadmin");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_deleteLdapUser_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP User deleted\"]}");
        when(reqProcessor.process("/auth/ldap/users/delete","{\"username\":\"safeadmin\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.deleteLdapUser(token, "safeadmin");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteLdapUser_failed_500() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"LDAP User delete failed\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"LDAP User delete failed\"]}");
        when(reqProcessor.process("/auth/ldap/users/delete","{\"username\":\"safeadmin\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.deleteLdapUser(token, "safeadmin");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_configureLdapUser_successfully_metaUpdate_failure() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String jsonStr = "{ \"policies\": \"admin,default\",\"username\": \"safeadmin\"}";
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseOk = getMockResponse(HttpStatus.MULTI_STATUS, false, "Meta data update failed");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"LDAP user configured\",\"Meta data update failed\"]}");
        LDAPUser ldapUser = new LDAPUser("safeadmin", "admin,default");
        when(JSONUtil.getJSON(ldapUser)).thenReturn(jsonStr);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(responseOk);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"safeadmin\"}",token)).thenReturn(responseNotFound);
        when(reqProcessor.process("/auth/ldap/users/configure",jsonStr,token)).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = ldapAuthService.configureLdapUser(token, ldapUser);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_configureLdapGroup_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String jsonStr = "{\"groupname\":\"admin\",\"policies\": \"admin,default\"}";
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP Group configured\"]}");
        LDAPGroup ldapGroup = new LDAPGroup("admin", "admin,default");
        when(JSONUtil.getJSON(ldapGroup)).thenReturn(jsonStr);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(responseOk);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"admin\"}",token)).thenReturn(responseNotFound);
        when(reqProcessor.process("/auth/ldap/groups/configure",jsonStr,token)).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = ldapAuthService.configureLdapGroup(token, ldapGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_configureLdapGroup_successfully_metaUpdate_failure() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String jsonStr = "{\"groupname\":\"admin\",\"policies\": \"admin,default\"}";
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response = getMockResponse(HttpStatus.MULTI_STATUS, true, "Meta data update failed");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"LDAP group configured\",\"Meta data update failed\"]}");
        LDAPGroup ldapGroup = new LDAPGroup("admin", "admin,default");
        when(JSONUtil.getJSON(ldapGroup)).thenReturn(jsonStr);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"admin\"}",token)).thenReturn(responseNotFound);
        when(reqProcessor.process("/auth/ldap/groups/configure",jsonStr,token)).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = ldapAuthService.configureLdapGroup(token, ldapGroup);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_configureLdapGroup_existing_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String jsonStr = "{\"groupname\":\"admin\",\"policies\": \"admin,default\"}";
        Response responseGroup = getMockResponse(HttpStatus.OK, true, "{\"data\": { \"policies\": [ \"admin\", \"default\"] }}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP Group configured\"]}");
        LDAPGroup ldapGroup = new LDAPGroup("admin", "admin,default");
        when(JSONUtil.getJSON(ldapGroup)).thenReturn(jsonStr);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(responseOk);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"admin\"}",token)).thenReturn(responseGroup);
        when(reqProcessor.process("/auth/ldap/groups/configure",jsonStr,token)).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = ldapAuthService.configureLdapGroup(token, ldapGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_configureLdapGroup_failed_500() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        String jsonStr = "{\"groupname\":\"admin\",\"policies\": \"admin,default\"}";
        Response responseGroup = getMockResponse(HttpStatus.OK, true, "{\"data\": { \"policies\": [ \"admin\", \"default\"] }}");
        Response responseFailure = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, "{\"errors\":[\"LDAP Group configuration failed\"]}");
        Response responseOk = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"LDAP Group configured\"]}");
        LDAPGroup ldapGroup = new LDAPGroup("admin", "admin,default");
        when(JSONUtil.getJSON(ldapGroup)).thenReturn(jsonStr);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(responseOk);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"admin\"}",token)).thenReturn(responseGroup);
        when(reqProcessor.process("/auth/ldap/groups/configure",jsonStr,token)).thenReturn(responseFailure);

        ResponseEntity<String> responseEntity = ldapAuthService.configureLdapGroup(token, ldapGroup);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        //assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_listLdapGroups_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\": [\"admin\"]]");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"keys\": [\"admin\"]]");
        when(reqProcessor.process("/auth/ldap/groups/list","{}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.listLdapGroups(token);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_listLdapGroups_failure_400() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, false, "{\"errors\":[\"Missing token \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Missing token \"]}");
        when(reqProcessor.process("/auth/ldap/groups/list","{}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.listLdapGroups("");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_fetchLdapGroup_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response responseGroup = getMockResponse(HttpStatus.OK, true, "{  \"data\": { \"policies\": [ \"admin\", \"default\" ] }}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{  \"data\": { \"policies\": [ \"admin\", \"default\" ] }}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"admin\"}",token)).thenReturn(responseGroup);
        ResponseEntity<String> responseEntity = ldapAuthService.fetchLdapGroup(token, "admin");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteLdapGroup_successfully() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"LDAP Group deleted\"]}");
        when(reqProcessor.process("/auth/ldap/groups/delete","{\"groupname\":\"admin\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.deleteLdapGroup(token, "admin");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteLdapGroup_failed_500() {
        String token = "4EpPYDSfgN2D4Gf7UmNO3nuL";
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, "{\"errors\":[\"LDAP Group delete failed\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"LDAP Group delete failed\"]}");
        when(reqProcessor.process("/auth/ldap/groups/delete","{\"groupname\":\"admin\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.deleteLdapGroup(token, "admin");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_authenticateLdap_successfully() {
        UserLogin userLogin = new UserLogin("safeadmin", "safeadmin");

        Response response = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Authentication Successful\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Authentication Successful\"]}");
        String jsonStr = "{  \"username\": \"safeadmin\", \"password\": \"safeadmin\"}";
        when(JSONUtil.getJSON(userLogin)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/ldap/login",jsonStr,"")).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.authenticateLdap(userLogin);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_authenticateLdap_failure_400() {
        UserLogin userLogin = new UserLogin("safeadmin", "safeadmin");

        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"errors\": [\"User Authentication failed\", \"Invalid username or password. Please retry again after correcting username or password.\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\": [\"User Authentication failed\", \"Invalid username or password. Please retry again after correcting username or password.\"]}");
        String jsonStr = "{  \"username\": \"safeadmin\", \"password\": \"safeadmin\"}";
        when(JSONUtil.getJSON(userLogin)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/ldap/login",jsonStr,"")).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.authenticateLdap(userLogin);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_authenticateLdap_failure_500() {
        UserLogin userLogin = new UserLogin("safeadmin", "safeadmin");

        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\": [\"User Authentication failed\", \"This may be due to vault services are down or vault services are not reachable\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\": [\"User Authentication failed\", \"This may be due to vault services are down or vault services are not reachable\"]}");
        String jsonStr = "{  \"username\": \"safeadmin\", \"password\": \"safeadmin\"}";
        when(JSONUtil.getJSON(userLogin)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/ldap/login",jsonStr,"")).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.authenticateLdap(userLogin);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_authenticateLdap_failure_204() {
        UserLogin userLogin = new UserLogin("safeadmin", "safeadmin");

        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"errors\":[\"Username Authentication Failed.\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NO_CONTENT).body("{\"errors\":[\"Username Authentication Failed.\"]}");
        String jsonStr = "{  \"username\": \"safeadmin\", \"password\": \"safeadmin\"}";
        when(JSONUtil.getJSON(userLogin)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/ldap/login",jsonStr,"")).thenReturn(response);
        ResponseEntity<String> responseEntity = ldapAuthService.authenticateLdap(userLogin);
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
}