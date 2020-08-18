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
import com.tmobile.cso.vault.api.service.SafesService;
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
public class SDBControllerV2Test {

    private MockMvc mockMvc;

    @Mock
    private SafesService safesService;

    @InjectMocks
    private SDBControllerV2 sdbControllerV2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(sdbControllerV2).build();
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

    @Test
    public void test_getFolders() throws Exception {
        String responseJson = "{  \"keys\": [    \"mysafe01\"  ]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.getFolders("5PDrOhsy4ig8L3EpsJZSLAMg", "users/safe1")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/sdb/folder?path=users/safe1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_getInfo() throws Exception {
        String responseJson = "{\"data\": { \"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\", \"type\": \"\" }}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.getInfo("5PDrOhsy4ig8L3EpsJZSLAMg", "users")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/sdb/folder/users?path=users")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_createfolder() throws Exception {
        String inputJson ="{\"path\":\"users/safe1\",\"data\":{\"default\":\"default\"}}";
        String responseJson = "{\"messages\":[\"Folder created \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.createfolder(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sdb/folder?path=users/safe1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_deletefolder() throws Exception {
        String responseJson = "{\"messages\":[\"SDB deleted\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.deletefolder("5PDrOhsy4ig8L3EpsJZSLAMg", "users/safe1")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/sdb/delete?path=users/safe1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_updateSafe() throws Exception {
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String inputJson =new ObjectMapper().writeValueAsString(safe);
        String responseJson = "{\"messages\":[\"Safe updated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.updateSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/sdb")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_createSafe() throws Exception {
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String inputJson =new ObjectMapper().writeValueAsString(safe);
        String responseJson = "{\"messages\":[\"Safe updated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.createSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sdb")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_deleteSafe() throws Exception {
        String responseJson = "{\"messages\":[\"Safe deleted\"]}";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String inputJson =new ObjectMapper().writeValueAsString(safe);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.deleteSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/sdb")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_getSafe() throws Exception {
        String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.getSafe("5PDrOhsy4ig8L3EpsJZSLAMg", "users/safe1")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/sdb?path=users/safe1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_addUserToSafe() throws Exception {
        SafeUser safeUser = new SafeUser("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(safeUser);
        String responseJson = "{\"messages\":[\"User is successfully associated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.addUserToSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), eq(null))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sdb/user")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_removeUserFromSafe() throws Exception {
        String responseJson = "{\"Message\":\"User association is removed \"}";
        SafeUser safeUser = new SafeUser("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(safeUser);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.removeUserFromSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), Mockito.any(UserDetails.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/sdb/user")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_addGroupToSafe() throws Exception {
        SafeGroup safeGroup = new SafeGroup("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(safeGroup);
        String responseJson = "{\"messages\":[\"Group is successfully associated with Safe\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.addGroupToSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), Mockito.any(UserDetails.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sdb/group")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_removeGroupFromSafe() throws Exception {
        SafeGroup safeGroup = new SafeGroup("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(safeGroup);
        String responseJson = "{\"messages\":[\"Group association is removed \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.removeGroupFromSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), Mockito.any(UserDetails.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/sdb/group")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_addAwsRoleToSafe() throws Exception {
        AWSRole awsRole = new AWSRole("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(awsRole);
        String responseJson = "{\"messages\":[\"Role is successfully associated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.addAwsRoleToSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sdb/role")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_removeAWSRoleFromSafe() throws Exception {
        AWSRole awsRole = new AWSRole("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(awsRole);
        String responseJson = "{\"messages\":[\"Role association is removed \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.removeAWSRoleFromSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), eq(false), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/sdb/role")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_detachAwsRoleFromSafe() throws Exception {
        AWSRole awsRole = new AWSRole("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(awsRole);
        String responseJson = "{\"messages\":[\"Role association is removed \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.removeAWSRoleFromSafe(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), eq(true), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/sdb/role")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_getFoldersRecursively() throws Exception {
        String responseJson = "{  \"keys\": [    \"mysafe01\"  ]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.getFoldersRecursively("5PDrOhsy4ig8L3EpsJZSLAMg", "users/safe1")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/sdb/list?path=users/safe1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_createNestedfolder() throws Exception {
        String responseJson = "{\"messages\":[\"Folder created \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.createNestedfolder(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sdb/createfolder?path=users/safe1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_associateApproletoSDB() throws Exception {
        String responseJson = "{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}";
        String inputJson = "{\"role\":\"approle1\",\"path\":\"users/safe1\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(safesService.associateApproletoSDB(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sdb/approle")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }
}
