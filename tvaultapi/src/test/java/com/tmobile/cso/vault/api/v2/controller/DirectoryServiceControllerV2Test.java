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
package com.tmobile.cso.vault.api.v2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.service.DirectoryService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@WebAppConfiguration
public class DirectoryServiceControllerV2Test {

    private MockMvc mockMvc;

    @Mock
    private DirectoryService directoryService;

    @InjectMocks
    private DirectoryServiceControllerV2 directoryServiceControllerV2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(directoryServiceControllerV2).build();
    }

    @Test
    public void test_searchByUPN() throws Exception {
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
        String responseMessage =new ObjectMapper().writeValueAsString(users);

        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(users);
        when(directoryService.searchByUPN(Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ldap/users?UserPrincipalName=test_principal_name")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_searchByCorpId() throws Exception {
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
        String responseMessage =new ObjectMapper().writeValueAsString(users);

        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(users);
        when(directoryService.searchByCorpId(Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ldap/corpusers?CorpId=corpid")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_searchByGroupName() throws Exception {
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
        String responseMessage =new ObjectMapper().writeValueAsString(groups);

        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(groups);
        when(directoryService.searchByGroupName(Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ldap/groups?groupName=group1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_searchUserInGSM() throws Exception {
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
        String responseMessage =new ObjectMapper().writeValueAsString(users);

        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(users);
        when(directoryService.searchByDisplayNameAndId(Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ldap/ntusers?displayName=corpid")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_searchByEmailInCorpAD() throws Exception {
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
        String responseMessage =new ObjectMapper().writeValueAsString(users);

        ResponseEntity<DirectoryObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(users);
        when(directoryService.searchByEmailInCorp(Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/corp/users?email=testUser@t-mobile.com")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

}
