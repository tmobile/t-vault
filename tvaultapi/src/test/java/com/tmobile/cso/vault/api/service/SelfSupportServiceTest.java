package com.tmobile.cso.vault.api.service;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.AuthorizationUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.SafeUtils;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SelfSupportServiceTest {

    @InjectMocks
    SelfSupportService selfSupportService;

    @Mock
    SafesService safesService;

    @Mock
    AuthorizationUtils authorizationUtils;

    @Mock
    SafeUtils safeUtils;

    @Mock
    PolicyUtils policyUtils;

    @Mock
    VaultAuthService vaultAuthService;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
    }

    UserDetails getMockUser(boolean isAdmin) {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(isAdmin);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        return userDetails;
    }

    private void mockIsAuthorized(UserDetails userDetails, boolean isAuthorized) {
        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid("shared/mysafe01")).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(isAuthorized);
    }

    @Test
    public void test_createSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");

        when(safesService.createSafe(token, safe)).thenReturn(readResponse);

        ResponseEntity<String> responseEntity = selfSupportService.createSafe(userDetails, token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createSafe_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");

        when(safesService.createSafe(token, safe)).thenReturn(readResponse);

        ResponseEntity<String> responseEntity = selfSupportService.createSafe(userDetails, token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_getFoldersRecursively_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String[] policies = {"s_shared_mysafe01"};
        String[] safes = {"mysafe01"};
        String responseJson = "{\"keys\":[\"mysafe01\"]}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(safeUtils.getManagedSafesFromPolicies(policies, path)).thenReturn(safes);
        when(safesService.getFoldersRecursively(token, path)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.getFoldersRecursively(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getFoldersRecursively_admin() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String responseJson = "{\"keys\":[\"mysafe01\"]}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.getFoldersRecursively(token, path)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.getFoldersRecursively(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(false);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
        when(safesService.addUserToSafe(token, safeUser, userDetails)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.addUserToSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(true);

        ResponseEntity<String> response =               ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
        when(safesService.addUserToSafe(token, safeUser, userDetails)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.addUserToSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(true);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Can't add user. Possible reasons: Invalid path specified, 2. Changing access/permission of safe owner is not allowed\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Can't add user. Possible reasons: Invalid path specified, 2. Changing access/permission of safe owner is not allowed\"]}");

        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(false);
        when(safesService.addUserToSafe(token, safeUser, userDetails)).thenReturn(response);
        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.addUserToSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(false);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

        when(safesService.removeUserFromSafe(token, safeUser)).thenReturn(response);
        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "removeUser")).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.removeUserFromSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(true);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

        when(safesService.removeUserFromSafe(token, safeUser)).thenReturn(response);
        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "removeUser")).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.removeUserFromSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getInfo() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String responseJson = "{\"data\": { \"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\", \"type\": \"\" }}";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.getInfo(token, path)).thenReturn(response);
        mockIsAuthorized(userDetails, true);
        ResponseEntity<String> responseEntity = selfSupportService.getInfo(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getInfo_admin() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String responseJson = "{\"data\": { \"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\", \"type\": \"\" }}";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.getInfo(token, path)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.getInfo(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getInfo_failure_403() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String responseJson = "{\"errors\":[\"Access denied: no permission to get this safe info\"]}";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseJson);
        when(safesService.getInfo(token, path)).thenReturn(response);
        mockIsAuthorized(userDetails, false);
        ResponseEntity<String> responseEntity = selfSupportService.getInfo(userDetails, token, path);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSafe_failure_403() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String responseJson = "{\"errors\":[\"Not authorized to get Safe information\"]}";

        String[] policies = {"s_shared_mysafe01"};
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01", safeBasicDetails);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseJson);
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(safesService.getSafe(token, path)).thenReturn(response);

        when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);

        when(authorizationUtils.isAuthorized(eq(userDetails),eq(safe),eq(policies),Mockito.any(), eq(false))).thenReturn(false);
        ResponseEntity<String> responseEntity = selfSupportService.getSafe(userDetails, token, path);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSafe_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";

        String[] policies = {"s_shared_mysafe01"};
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01", safeBasicDetails);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(safesService.getSafe(token, path)).thenReturn(response);
        when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(authorizationUtils.isAuthorized(eq(userDetails),eq(safe),eq(policies),Mockito.any(), eq(false))).thenReturn(true);
        ResponseEntity<String> responseEntity = selfSupportService.getSafe(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSafe_successfully_admin() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.getSafe(token, path)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.getSafe(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");

        when(safesService.updateSafe(token, safe)).thenReturn(readResponse);
        mockIsAuthorized(userDetails, true);
        ResponseEntity<String> responseEntity = selfSupportService.updateSafe(userDetails, token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");

        when(safesService.updateSafe(token, safe)).thenReturn(readResponse);
        ResponseEntity<String> responseEntity = selfSupportService.updateSafe(userDetails, token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
        when(safesService.deletefolder(token, path)).thenReturn(response);
        mockIsAuthorized(userDetails, true);
        ResponseEntity<String> responseEntity = selfSupportService.deletefolder(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteSafe_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
        when(safesService.deletefolder(token, path)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.deletefolder(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup(path, "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");
        when(safesService.addGroupToSafe(token, safeGroup)).thenReturn(response);
        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid(path)).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.addGroupToSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_successfully_isAdmin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup(path, "group1", "write");
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");
        when(safesService.addGroupToSafe(token, safeGroup)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.addGroupToSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup(path, "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add group to the safe\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add group to the safe\"]}");
        when(safesService.addGroupToSafe(token, safeGroup)).thenReturn(response);

        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid(path)).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(false);

        ResponseEntity<String> responseEntity = selfSupportService.addGroupToSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");

        when(safesService.removeGroupFromSafe(token, safeGroup)).thenReturn(response);
        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid("shared/mysafe01")).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.removeGroupFromSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove group from the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove group from the safe\"]}");

        when(safesService.removeGroupFromSafe(token, safeGroup)).thenReturn(response);
        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid("shared/mysafe01")).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(false);

        ResponseEntity<String> responseEntity = selfSupportService.removeGroupFromSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_successfully_isAdmin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");

        when(safesService.removeGroupFromSafe(token, safeGroup)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.removeGroupFromSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");

        when(safesService.addAwsRoleToSafe(token, awsRole)).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.addAwsRoleToSafe(userDetails, token, awsRole);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add AWS role to the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add AWS role to the safe\"]}");

        when(safesService.addAwsRoleToSafe(token, awsRole)).thenReturn(response);
        mockIsAuthorized(userDetails, false);


        ResponseEntity<String> responseEntity = selfSupportService.addAwsRoleToSafe(userDetails, token, awsRole);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_successfully_isAdmin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");

        when(safesService.addAwsRoleToSafe(token, awsRole)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.addAwsRoleToSafe(userDetails, token, awsRole);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");

        when(safesService.removeAWSRoleFromSafe(token, awsRole, false)).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.removeAWSRoleFromSafe(userDetails, token, awsRole, false);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove AWS role from the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove AWS role from the safe\"]}");

        when(safesService.removeAWSRoleFromSafe(token, awsRole, false)).thenReturn(response);
        mockIsAuthorized(userDetails, false);

        ResponseEntity<String> responseEntity = selfSupportService.removeAWSRoleFromSafe(userDetails, token, awsRole, false);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");

        when(safesService.removeAWSRoleFromSafe(token, awsRole, true)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.removeAWSRoleFromSafe(userDetails, token, awsRole, true);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_associateApproletoSDB_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");

        when(safesService.associateApproletoSDB(token, jsonStr)).thenReturn(response);
        mockIsAuthorized(userDetails, true);
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(true);
        ResponseEntity<String> responseEntity = selfSupportService.associateApproletoSDB(userDetails, token, jsonStr);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_associateApproletoSDB_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");

        when(safesService.associateApproletoSDB(token, jsonStr)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.associateApproletoSDB(userDetails, token, jsonStr);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_associateApproletoSDB_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add Approle to the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add Approle to the safe\"]}");

        when(safesService.associateApproletoSDB(token, jsonStr)).thenReturn(response);
        mockIsAuthorized(userDetails, false);
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(true);
        ResponseEntity<String> responseEntity = selfSupportService.associateApproletoSDB(userDetails, token, jsonStr);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_associateApproletoSDB_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");

        when(safesService.associateApproletoSDB(token, jsonStr)).thenReturn(response);
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(false);
        ResponseEntity<String> responseEntity = selfSupportService.associateApproletoSDB(userDetails, token, jsonStr);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
}
