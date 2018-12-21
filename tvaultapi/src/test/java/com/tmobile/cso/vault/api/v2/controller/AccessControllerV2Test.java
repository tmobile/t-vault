package com.tmobile.cso.vault.api.v2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.model.AccessPolicy;
import com.tmobile.cso.vault.api.service.AccessService;
import com.tmobile.cso.vault.api.v2.controller.AccessControllerV2;
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

import java.util.HashMap;

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
public class AccessControllerV2Test {

    private MockMvc mockMvc;

    @Mock
    private AccessService accessService;

    @InjectMocks
    private AccessControllerV2 accessControllerV2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(accessControllerV2).build();
    }

   @Test
    public void test_listAllPolices() throws Exception {
        String responseJson = "{\"policies\":[\"test-access-policy\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(accessService.listAllPolices("5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/access").header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
               .header("Content-Type", "application/json;charset=UTF-8"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_getPolicyInfo() throws Exception {
        String responseJson =  "{\"policies\": [\"default\",\"my-test-policy\",\"root\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(accessService.getPolicyInfo("5PDrOhsy4ig8L3EpsJZSLAMg", "test-access-policy")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/access/test-access-policy")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_createPolicy() throws Exception {
        HashMap<String, String> access = new HashMap<>();
        access.put("users/*", "read");
        access.put("apps/*", "read");
        access.put("shared/*", "read");
        AccessPolicy accessPolicy = new AccessPolicy("my-test-policy", access);
        String inputJson =new ObjectMapper().writeValueAsString(accessPolicy);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access Policy created \"]}");

        when(accessService.createPolicy(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/access")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("{\"messages\":[\"Access Policy created \"]}")));
    }

    @Test
    public void test_updatePolicy() throws Exception {
        HashMap<String, String> access = new HashMap<>();
        access.put("users/*", "read");
        access.put("apps/*", "read");
        access.put("shared/*", "read");
        AccessPolicy accessPolicy = new AccessPolicy("my-test-policy", access);
        String inputJson =new ObjectMapper().writeValueAsString(accessPolicy);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Policy updated \"]}");

        when(accessService.updatePolicy(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/access")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("{\"messages\":[\"Policy updated \"]}")));
    }

    @Test
    public void test_deletePolicy() throws Exception {

        String inputJson ="{\"accessid\":\"my-test-policy\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access Policy created \"]}");

        when(accessService.deletePolicyInfo(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/access/my-test-policy")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("{\"messages\":[\"Access Policy created \"]}")));
    }
}
