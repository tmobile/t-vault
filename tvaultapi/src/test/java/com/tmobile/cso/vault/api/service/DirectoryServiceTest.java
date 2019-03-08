// =========================================================================
// Copyright 2019 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================
package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.DirectoryGroup;
import com.tmobile.cso.vault.api.model.DirectoryObjects;
import com.tmobile.cso.vault.api.model.DirectoryObjectsList;
import com.tmobile.cso.vault.api.model.DirectoryUser;
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
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class DirectoryServiceTest {

    @InjectMocks
    DirectoryService directoryService;

    @Mock
    LdapTemplate ldapTemplate;

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
    public void test_searchByUPN_successfully() {

        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUser");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testUser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);

        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(users);

        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(persons);
        ResponseEntity<DirectoryObjects> responseEntity = directoryService.searchByUPN("test_principal_name");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected.getBody().getData().getValues()[0], responseEntity.getBody().getData().getValues()[0]);

    }

    @Test
    public void test_searchByCorpId_successfully() {

        DirectoryUser directoryUser = new DirectoryUser();
        directoryUser.setDisplayName("testUser");
        directoryUser.setGivenName("testUser");
        directoryUser.setUserEmail("testUser@t-mobile.com");
        directoryUser.setUserId("testuser01");
        directoryUser.setUserName("testUser");

        List<DirectoryUser> persons = new ArrayList<>();
        persons.add(directoryUser);

        DirectoryObjects users = new DirectoryObjects();
        DirectoryObjectsList usersList = new DirectoryObjectsList();
        usersList.setValues(persons.toArray(new DirectoryUser[persons.size()]));
        users.setData(usersList);
        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(users);

        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(persons);
        ResponseEntity<DirectoryObjects> responseEntity = directoryService.searchByCorpId("test_corpid");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected.getBody().getData().getValues()[0], responseEntity.getBody().getData().getValues()[0]);

    }

    @Test
    public void test_searchByGroupName_successfully() {

        DirectoryGroup dirGrp = new DirectoryGroup();
        dirGrp.setDisplayName("test_group");
        dirGrp.setEmail("testgroup@t-mobile.com");
        dirGrp.setGroupName("test_group");

        List<DirectoryGroup> allGroups = new ArrayList<>();
        allGroups.add(dirGrp);

        DirectoryObjects groups = new DirectoryObjects();
        DirectoryObjectsList groupsList = new DirectoryObjectsList();
        groupsList.setValues(allGroups.toArray(new DirectoryGroup[allGroups.size()]));
        groups.setData(groupsList);

        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(groups);

        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(allGroups);
        ResponseEntity<DirectoryObjects> responseEntity = directoryService.searchByGroupName("test_group");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected.getBody().getData().getValues()[0], responseEntity.getBody().getData().getValues()[0]);

    }
}